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

interface MpesaCheckoutRecord {
  checkoutRequestId: string;
  merchantRequestId: string;
  childIds: string[];
  amountKes: number;
  phoneNumber: string;
  status: 'pending' | 'paid' | 'failed';
  receiptNumber: string | null;
  resultDesc: string;
  createdAtMs: number;
}

const checkoutSessions = new Map<string, MpesaCheckoutRecord>();

function pruneOldSessions(): void {
  const cutoff = Date.now() - 30 * 60 * 1000;
  for (const [key, session] of checkoutSessions.entries()) {
    if (session.createdAtMs < cutoff) {
      checkoutSessions.delete(key);
    }
  }
}

function getDarajaConfig() {
  const env =
    process.env.MPESA_ENV?.trim().toLowerCase() === 'production' ? 'production' : 'sandbox';
  const consumerKey = process.env.MPESA_CONSUMER_KEY?.trim() || '';
  const consumerSecret = process.env.MPESA_CONSUMER_SECRET?.trim() || '';
  const shortcode = process.env.MPESA_SHORTCODE?.trim() || (env === 'sandbox' ? '174379' : '');
  const passkey = process.env.MPESA_PASSKEY?.trim() || '';
  const transactionType =
    process.env.MPESA_TRANSACTION_TYPE?.trim() === 'CustomerBuyGoodsOnline'
      ? 'CustomerBuyGoodsOnline'
      : 'CustomerPayBillOnline';
  const explicitCallback = process.env.MPESA_CALLBACK_URL?.trim() || '';
  const appUrl = process.env.APP_URL?.trim().replace(/\/+$/, '') || '';

  const missingVars: string[] = [];
  if (!consumerKey) missingVars.push('MPESA_CONSUMER_KEY');
  if (!consumerSecret) missingVars.push('MPESA_CONSUMER_SECRET');
  if (!shortcode) missingVars.push('MPESA_SHORTCODE');
  if (!passkey) missingVars.push('MPESA_PASSKEY');

  const baseUrl =
    env === 'production' ? 'https://api.safaricom.co.ke' : 'https://sandbox.safaricom.co.ke';

  return {
    env,
    baseUrl,
    consumerKey,
    consumerSecret,
    shortcode,
    passkey,
    transactionType,
    explicitCallback,
    appUrl,
    missingVars,
    configured: missingVars.length === 0,
  };
}

function normalizeKenyaPhone(raw: unknown): string | null {
  if (typeof raw !== 'string') return null;
  const digits = raw.replace(/\D/g, '');
  if (/^254[71]\d{8}$/.test(digits)) return digits;
  if (/^0[71]\d{8}$/.test(digits)) return `254${digits.slice(1)}`;
  if (/^[71]\d{8}$/.test(digits)) return `254${digits}`;
  return null;
}

function buildDarajaTimestamp(date = new Date()): string {
  const pad = (n: number) => String(n).padStart(2, '0');
  const year = date.getUTCFullYear();
  const month = pad(date.getUTCMonth() + 1);
  const day = pad(date.getUTCDate());
  // EAT is UTC+3
  const eatDate = new Date(date.getTime() + 3 * 60 * 60 * 1000);
  const eatYear = eatDate.getUTCFullYear();
  const eatMonth = pad(eatDate.getUTCMonth() + 1);
  const eatDay = pad(eatDate.getUTCDate());
  const hours = pad(eatDate.getUTCHours());
  const mins = pad(eatDate.getUTCMinutes());
  const secs = pad(eatDate.getUTCSeconds());
  return `${eatYear || year}${eatMonth || month}${eatDay || day}${hours}${mins}${secs}`;
}

function buildLicenseExpiryIso(daysFromNow = 30): string {
  const dt = new Date(Date.now() + daysFromNow * 24 * 60 * 60 * 1000);
  return dt.toISOString().slice(0, 10);
}

async function fetchDarajaAccessToken(
  baseUrl: string,
  consumerKey: string,
  consumerSecret: string
): Promise<string> {
  const creds = Buffer.from(`${consumerKey}:${consumerSecret}`, 'utf8').toString('base64');
  const response = await fetch(`${baseUrl}/oauth/v1/generate?grant_type=client_credentials`, {
    method: 'GET',
    headers: {
      Authorization: `Basic ${creds}`,
      Accept: 'application/json',
    },
  });
  if (!response.ok) {
    throw new Error(`Daraja OAuth failed (HTTP ${response.status})`);
  }
  const data = (await response.json()) as { access_token?: string };
  if (!data.access_token) {
    throw new Error('Daraja OAuth did not return access_token');
  }
  return data.access_token;
}

function resolveCallbackUrl(req: Request, explicitCallback: string, appUrl: string): string | null {
  if (explicitCallback.startsWith('https://')) {
    return explicitCallback;
  }
  if (appUrl.startsWith('https://')) {
    return `${appUrl}/api/mpesa/callback`;
  }
  const protoHeader = req.headers['x-forwarded-proto'];
  const proto = Array.isArray(protoHeader) ? protoHeader[0] : protoHeader || req.protocol;
  const hostHeader = req.headers['x-forwarded-host'] || req.headers.host;
  const host = Array.isArray(hostHeader) ? hostHeader[0] : hostHeader;
  if (proto === 'https' && host) {
    return `https://${host}/api/mpesa/callback`;
  }
  return null;
}

async function startServer(): Promise<void> {
  const app = express();
  app.set('trust proxy', true);

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

  // 1. Inspect Safaricom Daraja readiness (never exposes credentials)
  app.get('/api/mpesa/status', (req: Request, res: Response) => {
    const cfg = getDarajaConfig();
    const callbackUrl = resolveCallbackUrl(req, cfg.explicitCallback, cfg.appUrl);
    res.json({
      configured: cfg.configured,
      environment: cfg.env,
      shortcode: cfg.shortcode || '174379',
      monthlyKes: MONTHLY_LICENSE_KES,
      hasCallbackUrl: Boolean(callbackUrl),
      missingVars: cfg.missingVars,
    });
  });

  // 2. Initiate M-Pesa Lipa Na M-Pesa Online STK Push (KES 30/child/mo)
  app.post('/api/mpesa/stkpush', async (req: Request, res: Response) => {
    pruneOldSessions();
    const body = (req.body ?? {}) as {
      phoneNumber?: unknown;
      childIds?: unknown;
      accountReference?: unknown;
    };

    const normalizedPhone = normalizeKenyaPhone(body.phoneNumber);
    if (!normalizedPhone) {
      res.status(400).json({
        ok: false,
        error: 'Enter a valid Kenyan M-Pesa phone number (e.g. 0712 345 678 or 254712345678).',
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

    // Server strictly enforces KES 30 per child per month
    const amountKes = cleanChildIds.length * MONTHLY_LICENSE_KES;
    const cfg = getDarajaConfig();
    const expiresAt = buildLicenseExpiryIso(30);

    if (!cfg.configured) {
      const token = Math.random().toString(36).slice(2, 6).toUpperCase();
      const suffix = normalizedPhone.slice(-4);
      const receiptRef = `MPESA-${suffix}${token}`.slice(0, 24);
      res.json({
        ok: true,
        mode: 'sandbox_demo',
        configured: false,
        amountKes,
        phoneNumber: normalizedPhone,
        receiptRef,
        expiresAt,
        missingVars: cfg.missingVars,
        message: `Sandbox Demo M-Pesa STK completed (KES ${amountKes}). Add Daraja secrets in Settings -> Secrets to trigger real phone PIN prompts.`,
      });
      return;
    }

    const callbackUrl = resolveCallbackUrl(req, cfg.explicitCallback, cfg.appUrl);
    if (!callbackUrl) {
      res.status(400).json({
        ok: false,
        error:
          'Daraja requires an HTTPS callback URL. Set MPESA_CALLBACK_URL or APP_URL (https://...) in Secrets.',
      });
      return;
    }

    try {
      const accessToken = await fetchDarajaAccessToken(
        cfg.baseUrl,
        cfg.consumerKey,
        cfg.consumerSecret
      );
      const timestamp = buildDarajaTimestamp();
      const password = Buffer.from(`${cfg.shortcode}${cfg.passkey}${timestamp}`, 'utf8').toString(
        'base64'
      );

      const safeRef =
        typeof body.accountReference === 'string'
          ? body.accountReference.replace(/[^a-zA-Z0-9-]/g, '').slice(0, 12) || 'SomaTime'
          : 'SomaTime';

      const stkPayload = {
        BusinessShortCode: cfg.shortcode,
        Password: password,
        Timestamp: timestamp,
        TransactionType: cfg.transactionType,
        Amount: amountKes,
        PartyA: normalizedPhone,
        PartyB: cfg.shortcode,
        PhoneNumber: normalizedPhone,
        CallBackURL: callbackUrl,
        AccountReference: safeRef,
        TransactionDesc: `SomaTime ${cleanChildIds.length} child license`,
      };

      const stkResponse = await fetch(`${cfg.baseUrl}/mpesa/stkpush/v1/processrequest`, {
        method: 'POST',
        headers: {
          Authorization: `Bearer ${accessToken}`,
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(stkPayload),
      });

      const stkJson = (await stkResponse.json()) as {
        MerchantRequestID?: string;
        CheckoutRequestID?: string;
        ResponseCode?: string;
        ResponseDescription?: string;
        CustomerMessage?: string;
        errorMessage?: string;
      };

      if (!stkResponse.ok || stkJson.ResponseCode !== '0' || !stkJson.CheckoutRequestID) {
        res.status(400).json({
          ok: false,
          error:
            stkJson.errorMessage ||
            stkJson.ResponseDescription ||
            'Safaricom Daraja rejected the STK Push request.',
        });
        return;
      }

      checkoutSessions.set(stkJson.CheckoutRequestID, {
        checkoutRequestId: stkJson.CheckoutRequestID,
        merchantRequestId: stkJson.MerchantRequestID || '',
        childIds: cleanChildIds,
        amountKes,
        phoneNumber: normalizedPhone,
        status: 'pending',
        receiptNumber: null,
        resultDesc: stkJson.CustomerMessage || 'M-Pesa STK prompt sent to phone',
        createdAtMs: Date.now(),
      });

      res.json({
        ok: true,
        mode: 'daraja_stk',
        configured: true,
        environment: cfg.env,
        amountKes,
        phoneNumber: normalizedPhone,
        checkoutRequestId: stkJson.CheckoutRequestID,
        merchantRequestId: stkJson.MerchantRequestID || '',
        expiresAt,
        message:
          stkJson.CustomerMessage ||
          `M-Pesa prompt for KES ${amountKes} sent to ${normalizedPhone}. Enter your M-Pesa PIN.`,
      });
    } catch (err) {
      res.status(502).json({
        ok: false,
        error:
          err instanceof Error
            ? err.message
            : 'Unable to reach Safaricom Daraja M-Pesa API right now.',
      });
    }
  });

  // 3. Query STK Push transaction status
  app.get('/api/mpesa/query/:checkoutRequestId', async (req: Request, res: Response) => {
    const rawId = String(req.params.checkoutRequestId || '').trim();
    if (!/^[a-zA-Z0-9_-]{6,80}$/.test(rawId)) {
      res.status(400).json({ ok: false, error: 'Invalid CheckoutRequestID' });
      return;
    }

    const existing = checkoutSessions.get(rawId);
    if (existing && existing.status !== 'pending') {
      res.json({
        ok: true,
        status: existing.status,
        receiptRef: existing.receiptNumber,
        amountKes: existing.amountKes,
        expiresAt: buildLicenseExpiryIso(30),
        message: existing.resultDesc,
      });
      return;
    }

    const cfg = getDarajaConfig();
    if (!cfg.configured) {
      res.status(404).json({ ok: false, error: 'Daraja not configured' });
      return;
    }

    try {
      const accessToken = await fetchDarajaAccessToken(
        cfg.baseUrl,
        cfg.consumerKey,
        cfg.consumerSecret
      );
      const timestamp = buildDarajaTimestamp();
      const password = Buffer.from(`${cfg.shortcode}${cfg.passkey}${timestamp}`, 'utf8').toString(
        'base64'
      );

      const queryRes = await fetch(`${cfg.baseUrl}/mpesa/stkpushquery/v1/query`, {
        method: 'POST',
        headers: {
          Authorization: `Bearer ${accessToken}`,
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          BusinessShortCode: cfg.shortcode,
          Password: password,
          Timestamp: timestamp,
          CheckoutRequestID: rawId,
        }),
      });

      const qJson = (await queryRes.json()) as {
        ResultCode?: string | number;
        ResultDesc?: string;
        errorCode?: string;
        errorMessage?: string;
      };

      const resultCode = qJson.ResultCode !== undefined ? String(qJson.ResultCode) : '';
      if (resultCode === '0') {
        const receipt =
          existing?.receiptNumber ||
          `MPESA-${rawId.replace(/[^a-zA-Z0-9]/g, '').slice(-8).toUpperCase()}`;
        if (existing) {
          existing.status = 'paid';
          existing.receiptNumber = receipt;
          existing.resultDesc = qJson.ResultDesc || 'Payment confirmed by Safaricom M-Pesa';
        }
        res.json({
          ok: true,
          status: 'paid',
          receiptRef: receipt,
          amountKes: existing?.amountKes || MONTHLY_LICENSE_KES,
          expiresAt: buildLicenseExpiryIso(30),
          message: existing?.resultDesc || 'Payment confirmed by Safaricom M-Pesa',
        });
        return;
      }

      if (resultCode && resultCode !== '4999') {
        const desc = qJson.ResultDesc || 'M-Pesa transaction was cancelled or failed.';
        if (existing) {
          existing.status = 'failed';
          existing.resultDesc = desc;
        }
        res.json({
          ok: true,
          status: 'failed',
          receiptRef: null,
          message: desc,
        });
        return;
      }

      res.json({
        ok: true,
        status: 'pending',
        receiptRef: null,
        message: 'Waiting for parent to enter M-Pesa PIN on phone...',
      });
    } catch {
      res.json({
        ok: true,
        status: 'pending',
        receiptRef: null,
        message: 'Waiting for Safaricom M-Pesa confirmation...',
      });
    }
  });

  // 4. Safaricom Daraja Callback Webhook
  app.post('/api/mpesa/callback', (req: Request, res: Response) => {
    const callback = (req.body as {
      Body?: {
        stkCallback?: {
          CheckoutRequestID?: string;
          ResultCode?: number;
          ResultDesc?: string;
          CallbackMetadata?: {
            Item?: Array<{ Name?: string; Value?: unknown }>;
          };
        };
      };
    })?.Body?.stkCallback;

    if (callback?.CheckoutRequestID) {
      const session = checkoutSessions.get(callback.CheckoutRequestID);
      if (session) {
        if (Number(callback.ResultCode) === 0) {
          const items = callback.CallbackMetadata?.Item ?? [];
          const receiptItem = items.find((i) => i.Name === 'MpesaReceiptNumber');
          const receiptStr =
            typeof receiptItem?.Value === 'string' && receiptItem.Value.trim()
              ? receiptItem.Value.trim().slice(0, 24)
              : `MPESA-${callback.CheckoutRequestID.slice(-8).toUpperCase()}`;
          session.status = 'paid';
          session.receiptNumber = receiptStr;
          session.resultDesc = callback.ResultDesc || 'Confirmed via M-Pesa callback';
        } else {
          session.status = 'failed';
          session.resultDesc = callback.ResultDesc || 'M-Pesa payment failed or cancelled';
        }
      }
    }

    res.json({ ResultCode: 0, ResultDesc: 'Accepted' });
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
