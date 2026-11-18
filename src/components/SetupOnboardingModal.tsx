import { useState } from 'react';
import {
  CheckCircle2,
  Copy,
  KeyRound,
  Link2,
  ShieldCheck,
  Smartphone,
  UserCheck,
  X,
} from 'lucide-react';
import { getLevelGroupForGrade } from '../data/cbcCurriculum';
import type { ChildProfile } from '../types/somatime';

interface SetupOnboardingModalProps {
  open: boolean;
  initialTab?: 'choice' | 'parent' | 'learner';
  defaultParentName: string;
  defaultParentContact: string;
  defaultParentPin: string;
  childrenList: ChildProfile[];
  onRegisterParentAndChild: (input: {
    parentName: string;
    parentContact: string;
    parentPin: string;
    childName: string;
    childGrade: number;
    childSchool: string;
    dailyCapMins: number;
    initialWatchMins: number;
  }) => Promise<ChildProfile>;
  onPairLearnerCode: (code: string) => Promise<{
    success: boolean;
    child?: ChildProfile;
    message: string;
  }>;
  onClose: () => void;
}

export function SetupOnboardingModal({
  open,
  initialTab = 'choice',
  defaultParentName,
  defaultParentContact,
  defaultParentPin,
  childrenList,
  onRegisterParentAndChild,
  onPairLearnerCode,
  onClose,
}: SetupOnboardingModalProps) {
  const [tab, setTab] = useState<'choice' | 'parent' | 'learner'>(initialTab);

  // Parent registration state
  const [parentName, setParentName] = useState(defaultParentName);
  const [parentContact, setParentContact] = useState(defaultParentContact);
  const [parentPin, setParentPin] = useState(defaultParentPin);
  const [childName, setChildName] = useState('');
  const [childGrade, setChildGrade] = useState(6);
  const [childSchool, setChildSchool] = useState('Nairobi Primary School');
  const [dailyCapMins, setDailyCapMins] = useState(90);
  const [initialWatchMins, setInitialWatchMins] = useState(1);
  const [createdChild, setCreatedChild] = useState<ChildProfile | null>(null);
  const [copied, setCopied] = useState(false);
  const [isSavingParent, setIsSavingParent] = useState(false);
  const [parentError, setParentError] = useState('');

  // Learner pairing state
  const [pairingInput, setPairingInput] = useState('');
  const [pairingError, setPairingError] = useState('');
  const [isPairing, setIsPairing] = useState(false);

  if (!open) return null;

  const handleParentSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!childName.trim() || parentPin.trim().length !== 4 || isSavingParent) return;
    setIsSavingParent(true);
    setParentError('');
    try {
      const child = await onRegisterParentAndChild({
        parentName,
        parentContact,
        parentPin,
        childName,
        childGrade,
        childSchool,
        dailyCapMins,
        initialWatchMins,
      });
      setCreatedChild(child);
      setPairingInput(child.pairingCode);
    } catch (err) {
      setParentError(
        err instanceof Error
          ? 'Could not sync pairing code to cloud. Check your internet connection and retry.'
          : 'Cloud sync failed. Please retry.'
      );
    } finally {
      setIsSavingParent(false);
    }
  };

  const handleCopyCode = (code: string) => {
    navigator.clipboard?.writeText(code).catch(() => {});
    setCopied(true);
    window.setTimeout(() => setCopied(false), 1800);
  };

  const handleLearnerSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (isPairing) return;
    setIsPairing(true);
    setPairingError('');
    try {
      const res = await onPairLearnerCode(pairingInput);
      if (!res.success) {
        setPairingError(res.message);
      } else {
        onClose();
      }
    } catch {
      setPairingError('Unable to reach cloud pairing service. Check internet connection and retry.');
    } finally {
      setIsPairing(false);
    }
  };

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/65 p-4 backdrop-blur-sm"
      role="dialog"
      aria-modal="true"
      aria-labelledby="setup-wizard-title"
    >
      <div className="max-h-[92vh] w-full max-w-xl overflow-y-auto rounded-3xl border border-slate-200 bg-white p-6 shadow-2xl">
        <div className="flex items-start justify-between gap-4">
          <div>
            <p className="text-xs font-bold text-teal-700">Initial Device Setup &amp; Pairing</p>
            <h2 id="setup-wizard-title" className="font-display text-xl font-extrabold text-slate-900">
              {tab === 'choice' && 'How are you setting up this phone?'}
              {tab === 'parent' && 'Parent Registration & Child Pairing Code'}
              {tab === 'learner' && 'Register as a Learner (Pair Child Phone)'}
            </h2>
          </div>
          <button
            type="button"
            onClick={onClose}
            aria-label="Close setup wizard"
            className="flex min-h-[44px] min-w-[44px] items-center justify-center rounded-xl text-slate-500 hover:bg-slate-100 hover:text-slate-900"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        {/* Role Choice Screen */}
        {tab === 'choice' && (
          <div className="mt-5 grid grid-cols-1 gap-3 sm:grid-cols-2">
            <button
              type="button"
              onClick={() => {
                setCreatedChild(null);
                setTab('parent');
              }}
              className="flex flex-col items-start rounded-2xl border border-slate-200 bg-slate-50 p-5 text-left transition-colors hover:border-teal-600 hover:bg-teal-50/50"
            >
              <div className="flex h-11 w-11 items-center justify-center rounded-2xl bg-teal-700 text-white">
                <ShieldCheck className="h-5 w-5" />
              </div>
              <h3 className="mt-3 font-display text-base font-bold text-slate-900">
                1. Register as Parent
              </h3>
              <p className="mt-1 text-xs leading-relaxed text-slate-600">
                Set your Parent PIN, add your child&apos;s CBC details, and generate a pairing code for their phone.
              </p>
            </button>

            <button
              type="button"
              onClick={() => {
                setPairingError('');
                setTab('learner');
              }}
              className="flex flex-col items-start rounded-2xl border border-slate-200 bg-slate-50 p-5 text-left transition-colors hover:border-emerald-600 hover:bg-emerald-50/50"
            >
              <div className="flex h-11 w-11 items-center justify-center rounded-2xl bg-emerald-600 text-white">
                <Smartphone className="h-5 w-5" />
              </div>
              <h3 className="mt-3 font-display text-base font-bold text-slate-900">
                2. Register as a Learner
              </h3>
              <p className="mt-1 text-xs leading-relaxed text-slate-600">
                Enter the pairing code generated on your parent&apos;s phone to activate the background watch-time guard.
              </p>
            </button>
          </div>
        )}

        {/* Parent Registration Flow */}
        {tab === 'parent' && !createdChild && (
          <form onSubmit={handleParentSubmit} className="mt-5 space-y-4">
            <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
              <div>
                <label className="block text-xs font-semibold text-slate-700">Parent Full Name</label>
                <input
                  type="text"
                  required
                  value={parentName}
                  onChange={(e) => setParentName(e.target.value)}
                  className="mt-1 w-full rounded-xl border border-slate-200 bg-slate-50 px-3 py-2.5 text-sm text-slate-900 focus:border-teal-600 focus:bg-white focus:outline-none"
                />
              </div>
              <div>
                <label className="block text-xs font-semibold text-slate-700">Phone or Email</label>
                <input
                  type="text"
                  required
                  value={parentContact}
                  onChange={(e) => setParentContact(e.target.value)}
                  className="mt-1 w-full rounded-xl border border-slate-200 bg-slate-50 px-3 py-2.5 text-sm text-slate-900 focus:border-teal-600 focus:bg-white focus:outline-none"
                />
              </div>
            </div>

            <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
              <div>
                <label className="block text-xs font-semibold text-slate-700">4-Digit Parent PIN</label>
                <input
                  type="text"
                  required
                  maxLength={4}
                  pattern="\d{4}"
                  value={parentPin}
                  onChange={(e) => setParentPin(e.target.value.replace(/\D/g, '').slice(0, 4))}
                  className="mt-1 w-full rounded-xl border border-slate-200 bg-slate-50 px-3 py-2.5 font-mono text-sm font-bold text-slate-900 focus:border-teal-600 focus:bg-white focus:outline-none"
                />
              </div>
              <div>
                <label className="block text-xs font-semibold text-slate-700">Child Full Name</label>
                <input
                  type="text"
                  required
                  placeholder="e.g. Faith Muthoni"
                  value={childName}
                  onChange={(e) => setChildName(e.target.value)}
                  className="mt-1 w-full rounded-xl border border-slate-200 bg-slate-50 px-3 py-2.5 text-sm text-slate-900 focus:border-teal-600 focus:bg-white focus:outline-none"
                />
              </div>
            </div>

            <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
              <div>
                <label className="block text-xs font-semibold text-slate-700">Child CBC Grade</label>
                <select
                  value={childGrade}
                  onChange={(e) => setChildGrade(Number(e.target.value))}
                  className="mt-1 w-full rounded-xl border border-slate-200 bg-slate-50 px-3 py-2.5 text-sm text-slate-900 focus:border-teal-600 focus:bg-white focus:outline-none"
                >
                  {Array.from({ length: 12 }, (_, idx) => idx + 1).map((g) => (
                    <option key={g} value={g}>
                      Grade {g} ({getLevelGroupForGrade(g)})
                    </option>
                  ))}
                </select>
              </div>
              <div>
                <label className="block text-xs font-semibold text-slate-700">School Name</label>
                <input
                  type="text"
                  value={childSchool}
                  onChange={(e) => setChildSchool(e.target.value)}
                  className="mt-1 w-full rounded-xl border border-slate-200 bg-slate-50 px-3 py-2.5 text-sm text-slate-900 focus:border-teal-600 focus:bg-white focus:outline-none"
                />
              </div>
            </div>

            <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
              <div>
                <label className="block text-xs font-semibold text-slate-700">
                  Daily Max Cap (mins)
                </label>
                <input
                  type="number"
                  min={30}
                  max={180}
                  step={15}
                  value={dailyCapMins}
                  onChange={(e) => setDailyCapMins(Number(e.target.value))}
                  className="mt-1 w-full rounded-xl border border-slate-200 bg-slate-50 px-3 py-2.5 font-mono text-sm text-slate-900 focus:border-teal-600 focus:bg-white focus:outline-none"
                />
              </div>
              <div>
                <label className="block text-xs font-semibold text-slate-700">
                  Starting Watch Minutes
                </label>
                <input
                  type="number"
                  min={0}
                  max={60}
                  value={initialWatchMins}
                  onChange={(e) => setInitialWatchMins(Number(e.target.value))}
                  className="mt-1 w-full rounded-xl border border-slate-200 bg-slate-50 px-3 py-2.5 font-mono text-sm text-slate-900 focus:border-teal-600 focus:bg-white focus:outline-none"
                />
              </div>
            </div>

            {parentError && (
              <p className="text-xs font-semibold text-rose-600" role="alert">
                {parentError}
              </p>
            )}

            <div className="flex items-center justify-between gap-3 pt-2">
              <button
                type="button"
                onClick={() => setTab('choice')}
                className="min-h-[44px] rounded-xl px-4 py-2 text-xs font-semibold text-slate-600 hover:bg-slate-100"
              >
                Back
              </button>
              <button
                type="submit"
                disabled={isSavingParent}
                className="flex min-h-[46px] items-center gap-2 rounded-2xl bg-teal-700 px-5 py-2.5 text-sm font-bold text-white hover:bg-teal-800 disabled:opacity-60"
              >
                <UserCheck className="h-4 w-4" />
                <span>
                  {isSavingParent
                    ? 'Saving & Syncing Code...'
                    : 'Complete Registration & Generate Code'}
                </span>
              </button>
            </div>
          </form>
        )}

        {/* Parent Pairing Code Generated Confirmation */}
        {tab === 'parent' && createdChild && (
          <div className="mt-5 space-y-4">
            <div className="rounded-2xl border border-emerald-200 bg-emerald-50 p-5 text-center">
              <CheckCircle2 className="mx-auto h-9 w-9 text-emerald-700" />
              <p className="mt-2 text-xs font-bold text-emerald-800">
                Parent Account &amp; Child Profile Ready
              </p>
              <h3 className="mt-1 font-display text-lg font-extrabold text-slate-900">
                Pairing Code for {createdChild.name}
              </h3>
              <div className="mt-3 flex items-center justify-center gap-2">
                <span className="rounded-xl border border-emerald-300 bg-white px-4 py-2 font-mono text-2xl font-extrabold tracking-wider text-slate-900">
                  {createdChild.pairingCode}
                </span>
                <button
                  type="button"
                  onClick={() => handleCopyCode(createdChild.pairingCode)}
                  className="flex min-h-[44px] items-center gap-1.5 rounded-xl border border-emerald-300 bg-white px-3 py-2 text-xs font-bold text-emerald-900 hover:bg-emerald-100"
                >
                  <Copy className="h-4 w-4" />
                  <span>{copied ? 'Copied' : 'Copy'}</span>
                </button>
              </div>
              <p className="mt-3 text-xs text-slate-700">
                Key this code into {createdChild.name.split(' ')[0]}&apos;s phone under{' '}
                <strong>Register as a Learner</strong>. Once paired, the background watch-time guard
                starts automatically.
              </p>
            </div>

            <div className="flex flex-col gap-2 sm:flex-row sm:justify-between">
              <button
                type="button"
                onClick={onClose}
                className="min-h-[44px] rounded-xl border border-slate-200 px-4 py-2 text-xs font-semibold text-slate-700 hover:bg-slate-50"
              >
                Stay in Parent Dashboard
              </button>
              <button
                type="button"
                onClick={() => setTab('learner')}
                className="flex min-h-[46px] items-center justify-center gap-2 rounded-2xl bg-emerald-600 px-5 py-2.5 text-sm font-bold text-white hover:bg-emerald-700"
              >
                <Smartphone className="h-4 w-4" />
                <span>Test Child Phone Pairing Now →</span>
              </button>
            </div>
          </div>
        )}

        {/* Learner Pairing Flow */}
        {tab === 'learner' && (
          <form onSubmit={handleLearnerSubmit} className="mt-5 space-y-4">
            <p className="text-xs leading-relaxed text-slate-600">
              Enter the pairing code generated on your parent&apos;s SomaTime app. Once keyed in,
              SomaTime runs in the background checking your watch minutes and takes over the screen
              automatically when time reaches <span className="font-mono font-bold">00:00</span>.
            </p>

            <div>
              <label htmlFor="learner-code-input" className="block text-xs font-semibold text-slate-700">
                Parent Pairing Code
              </label>
              <input
                id="learner-code-input"
                type="text"
                required
                placeholder="e.g. SOMA-4012"
                value={pairingInput}
                onChange={(e) => {
                  setPairingInput(e.target.value.toUpperCase());
                  setPairingError('');
                }}
                className="mt-1.5 w-full rounded-2xl border border-slate-300 bg-slate-50 px-4 py-3 font-mono text-lg font-bold tracking-wider text-slate-900 uppercase focus:border-emerald-600 focus:bg-white focus:outline-none"
              />
            </div>

            {/* Quick-fill helper for existing demo codes */}
            <div className="rounded-2xl border border-slate-200 bg-slate-50 p-3">
              <p className="text-[11px] font-semibold text-slate-600">
                Active Parent Codes Available to Test:
              </p>
              <div className="mt-2 flex flex-wrap gap-2">
                {childrenList.map((c) => (
                  <button
                    key={c.id}
                    type="button"
                    onClick={() => {
                      setPairingInput(c.pairingCode);
                      setPairingError('');
                    }}
                    className="rounded-xl border border-slate-200 bg-white px-2.5 py-1.5 font-mono text-xs font-bold text-slate-800 hover:border-emerald-600"
                  >
                    {c.pairingCode} · {c.name.split(' ')[0]}
                  </button>
                ))}
              </div>
            </div>

            {pairingError && (
              <p className="text-xs font-semibold text-rose-600" role="alert">
                {pairingError}
              </p>
            )}

            <div className="flex items-center justify-between gap-3 pt-1">
              <button
                type="button"
                onClick={() => setTab('choice')}
                className="min-h-[44px] rounded-xl px-4 py-2 text-xs font-semibold text-slate-600 hover:bg-slate-100"
              >
                Back
              </button>
              <button
                type="submit"
                disabled={isPairing}
                className="flex min-h-[48px] items-center gap-2 rounded-2xl bg-emerald-600 px-5 py-2.5 text-sm font-bold text-white hover:bg-emerald-700 disabled:opacity-60"
              >
                <Link2 className="h-4 w-4" />
                <span>{isPairing ? 'Verifying Pairing Code...' : 'Pair Phone & Start Background Guard'}</span>
              </button>
            </div>

            <p className="flex items-center justify-center gap-1.5 text-[11px] text-slate-500">
              <KeyRound className="h-3.5 w-3.5" />
              <span>Once paired, Parent PIN is required to exit or override lock</span>
            </p>
          </form>
        )}
      </div>
    </div>
  );
}
