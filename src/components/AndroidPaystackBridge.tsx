import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { CheckCircle2, Loader2, RefreshCw, ShieldCheck, Smartphone } from 'lucide-react';
import { activatePairingLicenseInCloud } from '../lib/firebase';
import { MONTHLY_LICENSE_KES } from '../data/cbcCurriculum';

type BridgeStatus = 'ready' | 'sending' | 'waiting' | 'paid' | 'failed';

export function AndroidPaystackBridge() {
  const params = useMemo(() => new URLSearchParams(window.location.search), []);
  const initialPhone = params.get('phone') || '';
  const childIds = useMemo(
    () =>
      (params.get('childIds') || '')
        .split(',')
        .map((s) => s.trim())
        .filter(Boolean),
    [params]
  );
  const pairingCodes = useMemo(
    () =>
      (params.get('pairingCodes') || '')
        .split(',')
        .map((s) => s.trim())
        .filter(Boolean),
    [params]
  );
  const childNames = params.get('childNames') || 'Selected Child';
  const amountKes = Math.max(1, childIds.length) * MONTHLY_LICENSE_KES;

  const [phoneNumber, setPhoneNumber] = useState(initialPhone);
  const [status, setStatus] = useState<BridgeStatus>('ready');
  const [message, setMessage] = useState(
    'Tap below to send the Safaricom M-Pesa PIN prompt to your phone.'
  );
  const [reference, setReference] = useState('');
  const [receiptRef, setReceiptRef] = useState('');
  const autoTriggeredRef = useRef(false);

  const buildReturnAppUrl = useCallback(
    (refVal: string, rcptVal: string) => {
      const q = new URLSearchParams({
        status: 'paid',
        reference: refVal,
        receiptRef: rcptVal || refVal,
        childIds: childIds.join(','),
        phone: phoneNumber.trim(),
      });
      return `somatime://paystack-return?${q.toString()}`;
    },
    [childIds, phoneNumber]
  );

  const finalizePaidState = useCallback(
    async (refVal: string, rcptVal: string, expiresAt: string, confirmedMsg: string) => {
      setReference(refVal);
      setReceiptRef(rcptVal);
      setStatus('paid');
      setMessage(confirmedMsg);
      await Promise.all(
        pairingCodes.map((code) => activatePairingLicenseInCloud(code, rcptVal, expiresAt))
      );
      const deepLink = buildReturnAppUrl(refVal, rcptVal);
      window.setTimeout(() => {
        window.location.href = deepLink;
      }, 900);
    },
    [buildReturnAppUrl, pairingCodes]
  );

  const safeParseJson = useCallback(async <T,>(res: Response): Promise<T | null> => {
    const contentType = res.headers.get('content-type') || '';
    const text = await res.text();
    const trimmed = text.trimStart();
    if (contentType.toLowerCase().includes('text/html') || trimmed.startsWith('<')) {
      return null;
    }
    try {
      return JSON.parse(trimmed) as T;
    } catch {
      return null;
    }
  }, []);

  const triggerMpesaPrompt = useCallback(async () => {
    const cleanPhone = phoneNumber.trim();
    if (cleanPhone.replace(/\D/g, '').length < 9 || childIds.length === 0) {
      setStatus('failed');
      setMessage('Missing M-Pesa phone number or child profile from Android app.');
      return;
    }

    setStatus('sending');
    setMessage(`Sending KES ${amountKes} M-Pesa STK prompt to ${cleanPhone}...`);

    try {
      const res = await fetch('/api/paystack/initialize', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
        body: JSON.stringify({
          phoneNumber: cleanPhone,
          childIds,
          client: 'android',
        }),
      });
      const json = await safeParseJson<{
        ok?: boolean;
        mode?: string;
        reference?: string;
        receiptRef?: string;
        expiresAt?: string;
        message?: string;
        error?: string;
      }>(res);

      if (!json) {
        setStatus('failed');
        setMessage(
          'Browser session check interrupted the request. Refresh this page while signed into Google, then tap Pay again.'
        );
        return;
      }

      if (!res.ok || !json.ok || !json.reference) {
        setStatus('failed');
        setMessage(json.error || 'Could not send Paystack M-Pesa prompt.');
        return;
      }

      const refVal = json.reference;
      const rcptVal = json.receiptRef || refVal;
      const exp = json.expiresAt || new Date(Date.now() + 30 * 86400_000).toISOString().slice(0, 10);

      if (json.mode === 'paystack_test_demo' || json.mode === 'paystack_stk_instant') {
        await finalizePaidState(
          refVal,
          rcptVal,
          exp,
          json.message || `KES ${amountKes}/mo confirmed (${rcptVal}).`
        );
        return;
      }

      setReference(refVal);
      setStatus('waiting');
      setMessage(
        json.message ||
          `M-Pesa PIN prompt sent to ${cleanPhone}. Enter your M-Pesa PIN on your phone now.`
      );
    } catch {
      setStatus('failed');
      setMessage('Network error contacting SomaTime Paystack backend.');
    }
  }, [amountKes, childIds, finalizePaidState, phoneNumber, safeParseJson]);

  const checkVerificationNow = useCallback(async () => {
    if (!reference) return;
    try {
      const res = await fetch(`/api/paystack/verify/${encodeURIComponent(reference)}`);
      const json = await safeParseJson<{
        status?: 'pending' | 'paid' | 'failed';
        receiptRef?: string | null;
        expiresAt?: string;
        message?: string;
      }>(res);
      if (!json) return;
      if (json.status === 'paid') {
        const rcpt = json.receiptRef || `PSK-${reference.slice(-8).toUpperCase()}`;
        const exp = json.expiresAt || new Date(Date.now() + 30 * 86400_000).toISOString().slice(0, 10);
        await finalizePaidState(
          reference,
          rcpt,
          exp,
          json.message || `KES ${amountKes}/mo verified via M-Pesa.`
        );
      } else if (json.status === 'failed') {
        setStatus('failed');
        setMessage(json.message || 'M-Pesa transaction was cancelled or failed.');
      }
    } catch {
      // Keep waiting cleanly on transient polling error
    }
  }, [amountKes, finalizePaidState, reference, safeParseJson]);

  useEffect(() => {
    if (autoTriggeredRef.current) return;
    autoTriggeredRef.current = true;
    if (initialPhone.replace(/\D/g, '').length >= 9 && childIds.length > 0) {
      void triggerMpesaPrompt();
    }
  }, [childIds.length, initialPhone, triggerMpesaPrompt]);

  useEffect(() => {
    if (status !== 'waiting' || !reference) return;
    const timer = window.setInterval(() => {
      void checkVerificationNow();
    }, 2500);
    return () => window.clearInterval(timer);
  }, [checkVerificationNow, reference, status]);

  const deepLinkUrl = reference ? buildReturnAppUrl(reference, receiptRef || reference) : '';

  return (
    <div className="flex min-h-screen items-center justify-center bg-slate-50 px-4 py-8 text-slate-900">
      <div className="w-full max-w-md rounded-3xl border border-slate-200 bg-white p-6 shadow-sm">
        <div className="flex items-center gap-3">
          <div className="flex h-11 w-11 items-center justify-center rounded-2xl bg-teal-700 text-white">
            <ShieldCheck className="h-5 w-5" />
          </div>
          <div>
            <p className="text-xs font-bold uppercase tracking-wider text-teal-700">
              SomaTime · Paystack M-Pesa
            </p>
            <h1 className="font-display text-lg font-extrabold text-slate-900">
              Monthly Child Guard Checkout
            </h1>
          </div>
        </div>

        <div className="mt-5 rounded-2xl border border-slate-200 bg-slate-50 p-4 text-sm">
          <p className="font-semibold text-slate-800">{childNames}</p>
          <p className="mt-1 text-slate-600">
            Total Due: <span className="font-bold text-slate-900">KES {amountKes}</span> / month
          </p>
        </div>

        <div className="mt-4">
          <label
            htmlFor="android-mpesa-phone"
            className="block text-xs font-semibold text-slate-700"
          >
            Safaricom M-Pesa Number
          </label>
          <input
            id="android-mpesa-phone"
            type="tel"
            value={phoneNumber}
            onChange={(e) => setPhoneNumber(e.target.value)}
            disabled={status === 'sending' || status === 'waiting' || status === 'paid'}
            placeholder="0712 345 678"
            className="mt-1.5 w-full rounded-xl border border-slate-300 bg-white px-3.5 py-2.5 text-sm font-medium text-slate-900 focus:border-teal-600 focus:outline-none disabled:bg-slate-100"
          />
        </div>

        <div
          className={`mt-4 rounded-2xl border p-4 text-sm ${
            status === 'paid'
              ? 'border-emerald-200 bg-emerald-50 text-emerald-900'
              : status === 'failed'
                ? 'border-rose-200 bg-rose-50 text-rose-900'
                : 'border-sky-200 bg-sky-50 text-sky-900'
          }`}
        >
          <div className="flex items-start gap-2.5">
            {status === 'paid' ? (
              <CheckCircle2 className="mt-0.5 h-5 w-5 shrink-0 text-emerald-600" />
            ) : status === 'sending' || status === 'waiting' ? (
              <Loader2 className="mt-0.5 h-5 w-5 shrink-0 animate-spin text-sky-600" />
            ) : (
              <Smartphone className="mt-0.5 h-5 w-5 shrink-0 text-slate-600" />
            )}
            <div>
              <p className="font-semibold">{message}</p>
              {reference && (
                <p className="mt-1 font-mono text-xs opacity-80">Ref: {receiptRef || reference}</p>
              )}
            </div>
          </div>
        </div>

        <div className="mt-5 space-y-2.5">
          {status === 'paid' ? (
            <a
              href={deepLinkUrl}
              className="flex w-full items-center justify-center rounded-xl bg-emerald-600 px-4 py-3 text-sm font-bold text-white hover:bg-emerald-700"
            >
              Return to SomaTime Android App
            </a>
          ) : status === 'waiting' ? (
            <>
              <button
                type="button"
                onClick={() => void checkVerificationNow()}
                className="flex w-full items-center justify-center gap-2 rounded-xl bg-sky-700 px-4 py-3 text-sm font-bold text-white hover:bg-sky-800"
              >
                <RefreshCw className="h-4 w-4" />
                Check M-Pesa Status Now
              </button>
              <button
                type="button"
                onClick={() => void triggerMpesaPrompt()}
                className="w-full rounded-xl border border-slate-300 bg-white px-4 py-2.5 text-xs font-semibold text-slate-700 hover:bg-slate-50"
              >
                Resend M-Pesa Prompt
              </button>
            </>
          ) : (
            <button
              type="button"
              disabled={status === 'sending'}
              onClick={() => void triggerMpesaPrompt()}
              className="w-full rounded-xl bg-teal-700 px-4 py-3 text-sm font-bold text-white hover:bg-teal-800 disabled:opacity-60"
            >
              {status === 'sending'
                ? 'Sending M-Pesa Prompt...'
                : `Pay KES ${amountKes}/mo via M-Pesa`}
            </button>
          )}
        </div>
      </div>
    </div>
  );
}
