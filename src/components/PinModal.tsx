import { useEffect, useState } from 'react';
import { Delete, LockKeyhole, X } from 'lucide-react';
import { PARENT_PIN } from '../data/cbcCurriculum';

interface PinModalProps {
  open: boolean;
  title: string;
  subtitle: string;
  confirmLabel?: string;
  onSuccess: () => void;
  onCancel: () => void;
}

export function PinModal({
  open,
  title,
  subtitle,
  confirmLabel = 'Verify PIN',
  onSuccess,
  onCancel,
}: PinModalProps) {
  const [pin, setPin] = useState('');
  const [error, setError] = useState('');

  useEffect(() => {
    if (open) {
      setPin('');
      setError('');
    }
  }, [open]);

  if (!open) return null;

  const handleDigit = (digit: string) => {
    if (pin.length >= 4) return;
    const next = pin + digit;
    setPin(next);
    setError('');
    if (next.length === 4) {
      if (next === PARENT_PIN) {
        onSuccess();
      } else {
        setError('Incorrect Parent PIN. Demo PIN is 1234.');
        window.setTimeout(() => setPin(''), 350);
      }
    }
  };

  const handleBackspace = () => {
    setPin((p) => p.slice(0, -1));
    setError('');
  };

  const handleManualSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (pin === PARENT_PIN) {
      onSuccess();
    } else {
      setError('Incorrect Parent PIN. Demo PIN is 1234.');
      setPin('');
    }
  };

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/60 p-4 backdrop-blur-sm"
      role="dialog"
      aria-modal="true"
      aria-labelledby="pin-modal-title"
    >
      <div className="w-full max-w-sm rounded-3xl border border-slate-200 bg-white p-6 shadow-2xl">
        <div className="flex items-start justify-between gap-4">
          <div className="flex items-center gap-3">
            <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-2xl bg-teal-50 text-teal-700">
              <LockKeyhole className="h-5 w-5" />
            </div>
            <div>
              <h2 id="pin-modal-title" className="font-display text-lg font-bold text-slate-900">
                {title}
              </h2>
              <p className="text-xs text-slate-600">{subtitle}</p>
            </div>
          </div>
          <button
            type="button"
            onClick={onCancel}
            aria-label="Close PIN prompt"
            className="flex min-h-[44px] min-w-[44px] items-center justify-center rounded-xl text-slate-500 transition-colors hover:bg-slate-100 hover:text-slate-900"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        <form onSubmit={handleManualSubmit} className="mt-5">
          <div className="flex justify-center gap-3">
            {[0, 1, 2, 3].map((idx) => {
              const filled = idx < pin.length;
              return (
                <div
                  key={idx}
                  className={`flex h-12 w-12 items-center justify-center rounded-2xl border text-lg font-bold transition-colors ${
                    filled
                      ? 'border-teal-600 bg-teal-50 text-teal-900'
                      : 'border-slate-200 bg-slate-50 text-slate-400'
                  }`}
                >
                  {filled ? '•' : ''}
                </div>
              );
            })}
          </div>

          <p className="mt-2.5 text-center text-xs text-slate-500">
            Demo Parent PIN: <span className="font-mono font-semibold text-slate-800">1234</span>
          </p>

          {error && (
            <p className="mt-2 text-center text-xs font-medium text-rose-600" role="alert">
              {error}
            </p>
          )}

          <div className="mt-5 grid grid-cols-3 gap-2.5">
            {['1', '2', '3', '4', '5', '6', '7', '8', '9'].map((num) => (
              <button
                key={num}
                type="button"
                onClick={() => handleDigit(num)}
                className="flex min-h-[48px] items-center justify-center rounded-2xl border border-slate-200 bg-slate-50 font-mono text-lg font-semibold text-slate-900 transition-colors hover:bg-slate-100 active:scale-95"
              >
                {num}
              </button>
            ))}
            <button
              type="button"
              onClick={onCancel}
              className="flex min-h-[48px] items-center justify-center rounded-2xl text-xs font-semibold text-slate-600 transition-colors hover:bg-slate-100"
            >
              Cancel
            </button>
            <button
              type="button"
              onClick={() => handleDigit('0')}
              className="flex min-h-[48px] items-center justify-center rounded-2xl border border-slate-200 bg-slate-50 font-mono text-lg font-semibold text-slate-900 transition-colors hover:bg-slate-100 active:scale-95"
            >
              0
            </button>
            <button
              type="button"
              onClick={handleBackspace}
              aria-label="Delete digit"
              className="flex min-h-[48px] items-center justify-center rounded-2xl border border-slate-200 bg-slate-50 text-slate-700 transition-colors hover:bg-slate-100 active:scale-95"
            >
              <Delete className="h-5 w-5" />
            </button>
          </div>

          <button
            type="submit"
            className="mt-4 flex min-h-[46px] w-full items-center justify-center rounded-2xl bg-teal-700 px-4 py-2.5 text-sm font-semibold text-white transition-colors hover:bg-teal-800"
          >
            {confirmLabel}
          </button>
        </form>
      </div>
    </div>
  );
}
