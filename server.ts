import 'dotenv/config';
import express, { type Request, type Response } from 'express';
import crypto from 'node:crypto';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const PORT = Number(process.env.PORT || 3000);
const MONTHLY_LICENSE_KES = 30;
const ASSIGNMENT_UPLOAD_DIR = path.resolve(__dirname, 'uploads', 'assignments');
const MAX_UPLOAD_BYTES = 5 * 1024 * 1024; // 5 MB

const ALLOWED_ASSIGNMENT_MIME: Record<
  string,
  { ext: string; contentType: string; inlineSafe: boolean }
> = {
  '.pdf': { ext: '.pdf', contentType: 'application/pdf', inlineSafe: true },
  '.png': { ext: '.png', contentType: 'image/png', inlineSafe: true },
  '.jpg': { ext: '.jpg', contentType: 'image/jpeg', inlineSafe: true },
  '.jpeg': { ext: '.jpeg', contentType: 'image/jpeg', inlineSafe: true },
  '.webp': { ext: '.webp', contentType: 'image/webp', inlineSafe: true },
  '.txt': { ext: '.txt', contentType: 'text/plain; charset=utf-8', inlineSafe: false },
  '.csv': { ext: '.csv', contentType: 'text/csv; charset=utf-8', inlineSafe: false },
  '.json': { ext: '.json', contentType: 'application/json; charset=utf-8', inlineSafe: false },
  '.docx': {
    ext: '.docx',
    contentType: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
    inlineSafe: false,
  },
};

function verifyMagicBytes(ext: string, buf: Buffer): boolean {
  if (buf.length === 0) return false;
  if (ext === '.pdf') return buf.subarray(0, 5).toString('ascii') === '%PDF-';
  if (ext === '.png') {
    return (
      buf.length >= 8 &&
      buf[0] === 0x89 &&
      buf[1] === 0x50 &&
      buf[2] === 0x4e &&
      buf[3] === 0x47
    );
  }
  if (ext === '.jpg' || ext === '.jpeg') {
    return buf.length >= 3 && buf[0] === 0xff && buf[1] === 0xd8 && buf[2] === 0xff;
  }
  if (ext === '.webp') {
    return (
      buf.length >= 12 &&
      buf.subarray(0, 4).toString('ascii') === 'RIFF' &&
      buf.subarray(8, 12).toString('ascii') === 'WEBP'
    );
  }
  if (ext === '.docx') {
    return buf.length >= 4 && buf[0] === 0x50 && buf[1] === 0x4b && buf[2] === 0x03 && buf[3] === 0x04;
  }
  return true;
}

interface PaystackSessionRecord {
  reference: string;
  childIds: string[];
  amountKes: number;
  email: string;
  phoneNumber: string;
  status: 'pending' | 'paid' | 'failed';
  channel: string | null;
  receiptRef: string | null;
  resultDesc: string;
  createdAtMs: number;
}

const paystackSessions = new Map<string, PaystackSessionRecord>();
const PAYSTACK_CHANNELS = ['mobile_money', 'card'] as const;

function pruneOldSessions(): void {
  const cutoff = Date.now() - 60 * 60 * 1000;
  for (const [key, session] of paystackSessions.entries()) {
    if (session.createdAtMs < cutoff) {
      paystackSessions.delete(key);
    }
  }
}

function getPaystackConfig() {
  const secretKey = process.env.PAYSTACK_SECRET_KEY?.trim() || '';
  const environment: 'test' | 'live' = secretKey.startsWith('sk_live_') ? 'live' : 'test';
  const explicitCallback = process.env.PAYSTACK_CALLBACK_URL?.trim() || '';
  const appUrl = process.env.APP_URL?.trim().replace(/\/+$/, '') || '';
  const missingVars: string[] = [];
  if (!secretKey) missingVars.push('PAYSTACK_SECRET_KEY');

  return {
    secretKey,
    environment,
    explicitCallback,
    appUrl,
    missingVars,
    configured: missingVars.length === 0,
  };
}

function resolveHttpsOrigin(req: Request, appUrl: string): string | null {
  const protoHeader = req.headers['x-forwarded-proto'];
  const proto = (Array.isArray(protoHeader) ? protoHeader[0] : protoHeader || req.protocol || '')
    .split(',')[0]
    .trim()
    .toLowerCase();
  const hostHeader = req.headers['x-forwarded-host'] || req.headers.host;
  const host = (Array.isArray(hostHeader) ? hostHeader[0] : hostHeader || '').split(',')[0].trim();
  const isLocalHost =
    !host ||
    host.startsWith('localhost') ||
    host.startsWith('127.0.0.1') ||
    host.startsWith('0.0.0.0');

  if (proto === 'https' && !isLocalHost && /^[a-zA-Z0-9.-]+(:[0-9]{1,5})?$/.test(host)) {
    return `https://${host}`;
  }
  if (appUrl.startsWith('https://')) {
    return appUrl;
  }
  return null;
}

function resolvePaystackCallbackUrl(
  req: Request,
  explicitCallback: string,
  appUrl: string,
  client: 'web' | 'android' = 'web'
): string | null {
  if (explicitCallback.startsWith('https://')) {
    return explicitCallback;
  }
  const origin = resolveHttpsOrigin(req, appUrl);
  return origin ? `${origin}/api/paystack/callback?client=${client}` : null;
}

function resolvePaystackWebhookUrl(req: Request, appUrl: string): string | null {
  const origin = resolveHttpsOrigin(req, appUrl);
  return origin ? `${origin}/api/paystack/webhook` : null;
}

function escapeHtml(raw: string): string {
  return raw
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

function buildLicenseExpiryIso(daysFromNow = 30): string {
  const dt = new Date(Date.now() + daysFromNow * 24 * 60 * 60 * 1000);
  return dt.toISOString().slice(0, 10);
}

function sanitizeEmail(raw: unknown): string | null {
  if (typeof raw !== 'string') return null;
  const cleaned = raw.trim().toLowerCase();
  if (cleaned.length > 160) return null;
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/.test(cleaned)) return null;
  return cleaned;
}

function normalizeKenyaPhoneE164(raw: unknown): string | null {
  if (typeof raw !== 'string') return null;
  const digits = raw.replace(/\D/g, '');
  if (/^254[71]\d{8}$/.test(digits)) return `+${digits}`;
  if (/^0[71]\d{8}$/.test(digits)) return `+254${digits.slice(1)}`;
  if (/^[71]\d{8}$/.test(digits)) return `+254${digits}`;
  return null;
}

async function startServer(): Promise<void> {
  const app = express();
  app.set('trust proxy', true);

  // Paystack Webhook MUST be mounted before express.json() so raw Buffer signature verification works
  app.post(
    '/api/paystack/webhook',
    express.raw({ type: 'application/json', limit: '512kb' }),
    (req: Request, res: Response) => {
      const cfg = getPaystackConfig();
      if (!cfg.configured) {
        res.status(503).json({ ok: false, error: 'Paystack webhook not configured.' });
        return;
      }

      const rawSig = req.headers['x-paystack-signature'];
      const signature = Array.isArray(rawSig) ? rawSig[0] : rawSig;
      if (!signature || typeof signature !== 'string' || !Buffer.isBuffer(req.body)) {
        res.status(401).json({ ok: false, error: 'Missing Paystack signature.' });
        return;
      }

      const expectedHex = crypto
        .createHmac('sha512', cfg.secretKey)
        .update(req.body)
        .digest('hex');

      const sigBuf = Buffer.from(signature.trim(), 'utf8');
      const expectedBuf = Buffer.from(expectedHex, 'utf8');
      if (sigBuf.length !== expectedBuf.length || !crypto.timingSafeEqual(sigBuf, expectedBuf)) {
        res.status(401).json({ ok: false, error: 'Invalid Paystack webhook signature.' });
        return;
      }

      let payload: {
        event?: string;
        data?: {
          reference?: string;
          status?: string;
          amount?: number;
          currency?: string;
          channel?: string;
          gateway_response?: string;
        };
      };
      try {
        payload = JSON.parse(req.body.toString('utf8'));
      } catch {
        res.status(400).json({ ok: false, error: 'Malformed webhook JSON.' });
        return;
      }

      const ref = typeof payload.data?.reference === 'string' ? payload.data.reference.trim() : '';
      if (ref && /^[a-zA-Z0-9_-]{6,80}$/.test(ref)) {
        const session = paystackSessions.get(ref);
        if (session) {
          const currency = String(payload.data?.currency || '').toUpperCase();
          const paidSubunits = Number(payload.data?.amount || 0);
          const expectedSubunits = session.amountKes * 100;
          if (
            payload.event === 'charge.success' &&
            payload.data?.status === 'success' &&
            currency === 'KES' &&
            paidSubunits >= expectedSubunits
          ) {
            session.status = 'paid';
            session.channel = typeof payload.data?.channel === 'string' ? payload.data.channel : null;
            session.receiptRef = ref.slice(0, 24);
            session.resultDesc =
              payload.data?.gateway_response || 'Payment confirmed via M-Pesa';
          } else if (payload.event === 'charge.failed') {
            session.status = 'failed';
            session.resultDesc =
              payload.data?.gateway_response || 'M-Pesa charge failed or was declined';
          }
        }
      }

      res.json({ received: true });
    }
  );

  // Teacher Assignment File Upload (max 5 MB decoded; scoped 8 MB JSON payload)
  app.post(
    '/api/assignments/upload',
    express.json({ limit: '8mb' }),
    (req: Request, res: Response) => {
      const body = (req.body ?? {}) as {
        fileName?: unknown;
        mimeType?: unknown;
        dataBase64?: unknown;
      };

      const rawName = typeof body.fileName === 'string' ? path.basename(body.fileName.trim()) : '';
      const rawB64 = typeof body.dataBase64 === 'string' ? body.dataBase64.trim() : '';
      if (!rawName || !rawB64) {
        res.status(400).json({ ok: false, error: 'Missing fileName or file payload.' });
        return;
      }

      const ext = path.extname(rawName).toLowerCase();
      const spec = ALLOWED_ASSIGNMENT_MIME[ext];
      if (!spec) {
        res.status(400).json({
          ok: false,
          error:
            'Unsupported file type. Allowed: PDF, PNG, JPG, WEBP, CSV, JSON, TXT, or DOCX.',
        });
        return;
      }

      const cleanBase64 = rawB64.includes(',') ? rawB64.split(',').pop() || '' : rawB64;
      if (!/^[A-Za-z0-9+/=\s]+$/.test(cleanBase64)) {
        res.status(400).json({ ok: false, error: 'Invalid base64 file encoding.' });
        return;
      }

      const buf = Buffer.from(cleanBase64, 'base64');
      if (buf.length === 0 || buf.length > MAX_UPLOAD_BYTES) {
        res.status(400).json({
          ok: false,
          error: 'File must be between 1 byte and 5 MB.',
        });
        return;
      }

      if (!verifyMagicBytes(ext, buf)) {
        res.status(400).json({
          ok: false,
          error: `File contents do not match ${ext.toUpperCase()} format.`,
        });
        return;
      }

      fs.mkdirSync(ASSIGNMENT_UPLOAD_DIR, { recursive: true });
      const safeKey = `asgn_${Date.now()}_${crypto.randomBytes(6).toString('hex')}${spec.ext}`;
      const targetPath = path.resolve(ASSIGNMENT_UPLOAD_DIR, safeKey);
      if (!targetPath.startsWith(ASSIGNMENT_UPLOAD_DIR + path.sep)) {
        res.status(400).json({ ok: false, error: 'Invalid file destination.' });
        return;
      }

      fs.writeFileSync(targetPath, buf);

      const safeDisplayName = rawName.replace(/[^a-zA-Z0-9._ -]/g, '_').slice(0, 120);
      res.json({
        ok: true,
        fileName: safeDisplayName,
        safeKey,
        url: `/api/assignments/files/${safeKey}`,
        mimeType:
          typeof body.mimeType === 'string' && body.mimeType.trim()
            ? body.mimeType.trim().slice(0, 100)
            : spec.contentType,
        sizeBytes: buf.length,
      });
    }
  );

  app.use(express.json({ limit: '256kb' }));

  // 1. Inspect Paystack readiness (never exposes PAYSTACK_SECRET_KEY)
  app.get('/api/paystack/status', (req: Request, res: Response) => {
    const cfg = getPaystackConfig();
    const callbackUrl = resolvePaystackCallbackUrl(req, cfg.explicitCallback, cfg.appUrl);
    const webhookUrl = resolvePaystackWebhookUrl(req, cfg.appUrl);
    res.json({
      configured: cfg.configured,
      environment: cfg.environment,
      monthlyKes: MONTHLY_LICENSE_KES,
      channels: PAYSTACK_CHANNELS,
      hasWebhookUrl: Boolean(webhookUrl),
      callbackUrl,
      webhookUrl,
      missingVars: cfg.missingVars,
    });
  });

  // 2. Trigger Paystack M-Pesa STK Push directly to user's phone (KES 30/child/mo)
  app.post('/api/paystack/initialize', async (req: Request, res: Response) => {
    pruneOldSessions();
    const body = (req.body ?? {}) as {
      email?: unknown;
      phoneNumber?: unknown;
      childIds?: unknown;
    };

    const normalizedPhone = normalizeKenyaPhoneE164(body.phoneNumber);
    if (!normalizedPhone) {
      res.status(400).json({
        ok: false,
        error: 'Enter a valid Kenyan M-Pesa phone number (e.g. 0712 345 678 or +254712345678).',
      });
      return;
    }

    if (!Array.isArray(body.childIds) || body.childIds.length === 0 || body.childIds.length > 10) {
      res.status(400).json({
        ok: false,
        error: 'Select 1 to 10 child profiles to license.',
      });
      return;
    }

    const cleanChildIds = body.childIds
      .filter((id): id is string => typeof id === 'string' && /^[a-zA-Z0-9_-]{1,80}$/.test(id))
      .slice(0, 10);

    if (cleanChildIds.length !== body.childIds.length) {
      res.status(400).json({
        ok: false,
        error: 'Invalid child profile IDs provided.',
      });
      return;
    }

    // Server strictly enforces KES 30 per child per month (3000 subunits in Paystack KES)
    const amountKes = cleanChildIds.length * MONTHLY_LICENSE_KES;
    const amountSubunits = amountKes * 100;
    const phoneDigits = normalizedPhone.slice(1); // 2547XXXXXXXX
    const internalEmail = sanitizeEmail(body.email) || `parent.${phoneDigits}@somatime.ke`;
    const cfg = getPaystackConfig();
    const expiresAt = buildLicenseExpiryIso(30);
    const reference = `SOMA_${Date.now()}_${crypto.randomBytes(4).toString('hex').toUpperCase()}`;

    if (!cfg.configured) {
      const demoRef = `PSK-${phoneDigits.slice(-4)}${reference.slice(-4)}`.slice(0, 24);
      res.json({
        ok: true,
        mode: 'paystack_test_demo',
        configured: false,
        environment: 'test',
        amountKes,
        phoneNumber: normalizedPhone,
        reference: demoRef,
        expiresAt,
        missingVars: cfg.missingVars,
        message: `M-Pesa Test Demo confirmed (KES ${amountKes}) for ${normalizedPhone}.`,
      });
      return;
    }

    try {
      const chargePayload = {
        email: internalEmail,
        amount: amountSubunits,
        currency: 'KES',
        reference,
        mobile_money: {
          phone: normalizedPhone,
          provider: 'mpesa',
        },
        metadata: {
          childIds: cleanChildIds,
          parentPhone: normalizedPhone,
          plan: 'SomaTime Monthly Child Guard (KES 30/child/mo)',
        },
      };

      const pRes = await fetch('https://api.paystack.co/charge', {
        method: 'POST',
        headers: {
          Authorization: `Bearer ${cfg.secretKey}`,
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(chargePayload),
      });

      const pJson = (await pRes.json()) as {
        status?: boolean;
        message?: string;
        data?: {
          reference?: string;
          status?: string;
          display_text?: string;
          gateway_response?: string;
        };
      };

      if (!pRes.ok || !pJson.status || !pJson.data) {
        res.status(400).json({
          ok: false,
          error: pJson.message || 'Could not trigger the M-Pesa phone prompt.',
        });
        return;
      }

      const activeRef =
        pJson.data.reference && /^[a-zA-Z0-9_-]{6,80}$/.test(pJson.data.reference)
          ? pJson.data.reference
          : reference;
      const chargeStatus = String(pJson.data.status || '').toLowerCase();

      if (chargeStatus === 'failed') {
        res.status(400).json({
          ok: false,
          error: pJson.data.gateway_response || pJson.message || 'M-Pesa request failed.',
        });
        return;
      }

      if (chargeStatus === 'success') {
        const receiptRef = `PSK-${activeRef.slice(-8).toUpperCase()}`.slice(0, 24);
        paystackSessions.set(activeRef, {
          reference: activeRef,
          childIds: cleanChildIds,
          amountKes,
          email: internalEmail,
          phoneNumber: normalizedPhone,
          status: 'paid',
          channel: 'mobile_money',
          receiptRef,
          resultDesc: pJson.data.gateway_response || 'Payment confirmed via M-Pesa',
          createdAtMs: Date.now(),
        });
        res.json({
          ok: true,
          mode: 'paystack_stk_instant',
          configured: true,
          environment: cfg.environment,
          amountKes,
          phoneNumber: normalizedPhone,
          reference: activeRef,
          receiptRef,
          expiresAt,
          message: pJson.data.gateway_response || `KES ${amountKes} confirmed on ${normalizedPhone}.`,
        });
        return;
      }

      const promptMsg =
        pJson.data.display_text ||
        `M-Pesa PIN prompt for KES ${amountKes} sent to ${normalizedPhone}. Enter your M-Pesa PIN on your phone.`;

      paystackSessions.set(activeRef, {
        reference: activeRef,
        childIds: cleanChildIds,
        amountKes,
        email: internalEmail,
        phoneNumber: normalizedPhone,
        status: 'pending',
        channel: 'mobile_money',
        receiptRef: null,
        resultDesc: promptMsg,
        createdAtMs: Date.now(),
      });

      res.json({
        ok: true,
        mode: 'paystack_stk',
        configured: true,
        environment: cfg.environment,
        amountKes,
        phoneNumber: normalizedPhone,
        reference: activeRef,
        expiresAt,
        message: promptMsg,
      });
    } catch (err) {
      res.status(502).json({
        ok: false,
        error: err instanceof Error ? err.message : 'Unable to reach M-Pesa payment service right now.',
      });
    }
  });

  async function verifyPaystackTransaction(rawRef: string): Promise<{
    httpStatus: number;
    ok: boolean;
    status: 'pending' | 'paid' | 'failed';
    receiptRef: string | null;
    amountKes?: number;
    channel?: string | null;
    childIds?: string[];
    expiresAt?: string;
    message: string;
    error?: string;
  }> {
    const existing = paystackSessions.get(rawRef);
    if (existing && existing.status !== 'pending') {
      return {
        httpStatus: 200,
        ok: true,
        status: existing.status,
        receiptRef: existing.receiptRef || `PSK-${rawRef.slice(-8).toUpperCase()}`,
        amountKes: existing.amountKes,
        channel: existing.channel,
        childIds: existing.childIds,
        expiresAt: buildLicenseExpiryIso(30),
        message: existing.resultDesc,
      };
    }

    const cfg = getPaystackConfig();
    if (!cfg.configured) {
      return {
        httpStatus: 404,
        ok: false,
        status: 'failed',
        receiptRef: null,
        message: 'M-Pesa payment gateway is not configured on this server.',
        error: 'M-Pesa payment gateway is not configured on this server.',
      };
    }

    try {
      const verifyRes = await fetch(
        `https://api.paystack.co/transaction/verify/${encodeURIComponent(rawRef)}`,
        {
          method: 'GET',
          headers: {
            Authorization: `Bearer ${cfg.secretKey}`,
            Accept: 'application/json',
          },
        }
      );

      const vJson = (await verifyRes.json()) as {
        status?: boolean;
        message?: string;
        data?: {
          status?: string;
          reference?: string;
          amount?: number;
          currency?: string;
          channel?: string;
          gateway_response?: string;
          metadata?: {
            childIds?: unknown;
          };
        };
      };

      const txStatus = String(vJson.data?.status || '').toLowerCase();
      const currency = String(vJson.data?.currency || '').toUpperCase();
      const paidSubunits = Number(vJson.data?.amount || 0);
      const metaChildIds = Array.isArray(vJson.data?.metadata?.childIds)
        ? vJson.data.metadata.childIds.filter(
            (id): id is string => typeof id === 'string' && /^[a-zA-Z0-9_-]{1,80}$/.test(id)
          )
        : [];
      const expectedChildCount = Math.max(
        1,
        existing?.childIds.length || metaChildIds.length || 1
      );
      const expectedSubunits = existing
        ? existing.amountKes * 100
        : expectedChildCount * MONTHLY_LICENSE_KES * 100;

      if (vJson.status && txStatus === 'success') {
        if (currency !== 'KES' || paidSubunits < expectedSubunits) {
          if (existing) {
            existing.status = 'failed';
            existing.resultDesc = 'M-Pesa payment currency or amount mismatch.';
          }
          return {
            httpStatus: 200,
            ok: true,
            status: 'failed',
            receiptRef: null,
            message: 'M-Pesa transaction currency or amount did not match KES 30/child.',
          };
        }

        const receiptRef = `PSK-${rawRef.slice(-8).toUpperCase()}`.slice(0, 24);
        const channel = typeof vJson.data?.channel === 'string' ? vJson.data.channel : null;
        if (existing) {
          existing.status = 'paid';
          existing.channel = channel;
          existing.receiptRef = receiptRef;
          existing.resultDesc = vJson.data?.gateway_response || 'Payment verified via M-Pesa';
        }
        return {
          httpStatus: 200,
          ok: true,
          status: 'paid',
          receiptRef,
          amountKes: Math.round(paidSubunits / 100),
          channel,
          childIds: existing?.childIds || metaChildIds,
          expiresAt: buildLicenseExpiryIso(30),
          message: existing?.resultDesc || 'Payment verified via M-Pesa.',
        };
      }

      if (txStatus === 'failed' || txStatus === 'reversed') {
        const desc = vJson.data?.gateway_response || 'M-Pesa payment failed or was declined.';
        if (existing) {
          existing.status = 'failed';
          existing.resultDesc = desc;
        }
        return {
          httpStatus: 200,
          ok: true,
          status: 'failed',
          receiptRef: null,
          message: desc,
        };
      }

      // Note: Gateway reports status='abandoned' immediately after initialize until the customer
      // completes M-Pesa payment, so treat it as 'pending'.
      return {
        httpStatus: 200,
        ok: true,
        status: 'pending',
        receiptRef: null,
        message: 'Awaiting M-Pesa PIN confirmation on your phone...',
      };
    } catch {
      return {
        httpStatus: 200,
        ok: true,
        status: 'pending',
        receiptRef: null,
        message: 'Checking M-Pesa payment status...',
      };
    }
  }

  // 3. Verify Paystack Transaction status (JSON API)
  app.get('/api/paystack/verify/:reference', async (req: Request, res: Response) => {
    const rawRef = String(req.params.reference || '').trim();
    if (!/^[a-zA-Z0-9_-]{6,80}$/.test(rawRef)) {
      res.status(400).json({ ok: false, error: 'Invalid M-Pesa transaction reference.' });
      return;
    }

    const result = await verifyPaystackTransaction(rawRef);
    const { httpStatus, ...payload } = result;
    res.status(httpStatus).json(payload);
  });

  // 4. Backend-built Paystack Browser Callback Handler (used automatically when PAYSTACK_CALLBACK_URL is blank)
  app.get('/api/paystack/callback', async (req: Request, res: Response) => {
    const rawRef = String(req.query.reference || req.query.trxref || '').trim();
    const client = req.query.client === 'android' ? 'android' : 'web';

    if (!/^[a-zA-Z0-9_-]{6,80}$/.test(rawRef)) {
      res
        .status(400)
        .setHeader('Content-Type', 'text/html; charset=utf-8')
        .send(
          '<!doctype html><html><body style="font-family:system-ui;padding:32px;background:#f8fafc;color:#0f172a"><h2>Invalid M-Pesa reference</h2><p>Return to SomaTime and click Verify Payment Now.</p></body></html>'
        );
      return;
    }

    const result = await verifyPaystackTransaction(rawRef);
    const isPaid = result.status === 'paid';
    const isFailed = result.status === 'failed';
    const safeRef = escapeHtml(result.receiptRef || rawRef);
    const safeMsg = escapeHtml(result.message);
    const safeAmount = escapeHtml(String(result.amountKes ?? MONTHLY_LICENSE_KES));
    const safeReturnHref = `/?paystack_callback=1&reference=${encodeURIComponent(rawRef)}`;
    const safeRecheckHref = `/api/paystack/callback?client=${client}&reference=${encodeURIComponent(rawRef)}`;

    const heading = isPaid
      ? 'M-Pesa Payment Confirmed'
      : isFailed
        ? 'M-Pesa Payment Not Completed'
        : 'Waiting for M-Pesa Confirmation';
    const badgeColor = isPaid ? '#059669' : isFailed ? '#dc2626' : '#0284c7';
    const detailLine = isPaid
      ? `KES ${safeAmount}/mo verified · Receipt <strong>${safeRef}</strong>`
      : safeMsg;
    const actionBlock =
      client === 'android'
        ? isPaid
          ? '<p style="margin-top:16px;font-size:14px;color:#334155;">Switch back to the <strong>SomaTime</strong> Android app now — your child license activates automatically on return.</p>'
          : `<p style="margin-top:16px;font-size:14px;color:#334155;">If you just entered your M-Pesa PIN, wait a few seconds and tap below or switch back to SomaTime and tap Verify Now.</p><a href="${safeRecheckHref}" style="display:inline-block;margin-top:12px;padding:12px 18px;border-radius:12px;background:#0284c7;color:#fff;font-weight:700;text-decoration:none;">Re-check Payment Status</a>`
        : isPaid
          ? `<p style="margin-top:16px;font-size:14px;color:#334155;">Your original SomaTime tab is now unlocked. You can close this tab or continue below.</p><a href="${safeReturnHref}" style="display:inline-block;margin-top:12px;padding:12px 18px;border-radius:12px;background:#0f766e;color:#fff;font-weight:700;text-decoration:none;">Return to SomaTime Dashboard →</a>`
          : `<p style="margin-top:16px;font-size:14px;color:#334155;">If you just completed M-Pesa payment, tap below to re-check confirmation.</p><a href="${safeRecheckHref}" style="display:inline-block;margin-top:12px;margin-right:8px;padding:12px 18px;border-radius:12px;background:#0284c7;color:#fff;font-weight:700;text-decoration:none;">Re-check Status</a><a href="${safeReturnHref}" style="display:inline-block;margin-top:12px;padding:12px 18px;border-radius:12px;background:#e2e8f0;color:#0f172a;font-weight:700;text-decoration:none;">Back to Dashboard</a>`;

    res
      .status(200)
      .setHeader('Content-Type', 'text/html; charset=utf-8')
      .setHeader('Content-Security-Policy', "default-src 'none'; style-src 'unsafe-inline'")
      .send(
        `<!doctype html><html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>${escapeHtml(heading)} · SomaTime</title></head><body style="margin:0;min-height:100vh;display:flex;align-items:center;justify-content:center;background:#f8fafc;font-family:system-ui,-apple-system,sans-serif;padding:20px;color:#0f172a;"><div style="max-width:460px;width:100%;background:#fff;border:1px solid #e2e8f0;border-radius:20px;padding:28px;box-shadow:0 10px 30px rgba(15,23,42,.06);"><div style="display:inline-block;padding:4px 10px;border-radius:8px;background:${badgeColor};color:#fff;font-size:12px;font-weight:700;">SomaTime · Lipa na M-Pesa</div><h1 style="margin:14px 0 8px;font-size:22px;line-height:1.25;">${escapeHtml(heading)}</h1><p style="margin:0;font-size:14px;color:#334155;">${detailLine}</p>${actionBlock}</div></body></html>`
      );
  });

  // 5. Serve uploaded teacher assignment files safely
  app.get('/api/assignments/files/:fileKey', (req: Request, res: Response) => {
    const fileKey = String(req.params.fileKey || '').trim();
    if (!/^asgn_[0-9]{10,16}_[a-f0-9]{12}\.(pdf|png|jpe?g|webp|txt|csv|json|docx)$/.test(fileKey)) {
      res.status(400).send('Invalid file key.');
      return;
    }
    const ext = path.extname(fileKey).toLowerCase();
    const spec = ALLOWED_ASSIGNMENT_MIME[ext];
    if (!spec) {
      res.status(404).send('Unsupported file.');
      return;
    }
    const targetPath = path.resolve(ASSIGNMENT_UPLOAD_DIR, fileKey);
    if (!targetPath.startsWith(ASSIGNMENT_UPLOAD_DIR + path.sep) || !fs.existsSync(targetPath)) {
      res.status(404).send('File not found.');
      return;
    }

    res.setHeader('X-Content-Type-Options', 'nosniff');
    res.setHeader('Content-Security-Policy', "default-src 'none'; img-src 'self'; style-src 'unsafe-inline'");
    res.setHeader('Content-Type', spec.contentType);
    res.setHeader(
      'Content-Disposition',
      spec.inlineSafe ? `inline; filename="${fileKey}"` : `attachment; filename="${fileKey}"`
    );
    fs.createReadStream(targetPath).pipe(res);
  });

  const isProd = process.env.NODE_ENV === 'production';
  if (!isProd) {
    const { createServer: createViteServer } = await import('vite');
    const vite = await createViteServer({
      server: { middlewareMode: true },
      appType: 'spa',
    });
    app.use(vite.middlewares);
  } else {
    const distPath = path.resolve(__dirname, 'dist');
    app.use(express.static(distPath));
    app.get('*', (_req: Request, res: Response) => {
      const indexFile = path.join(distPath, 'index.html');
      if (fs.existsSync(indexFile)) {
        res.sendFile(indexFile);
      } else {
        res.status(404).send('Build output not found. Run npm run build.');
      }
    });
  }

  app.listen(PORT, '0.0.0.0', () => {
    console.log(`SomaTime server listening on http://0.0.0.0:${PORT}`);
  });
}

void startServer();
