import { useEffect, useState } from 'react';
import {
  BadgeCheck,
  CheckCircle2,
  Clock,
  Eye,
  EyeOff,
  KeyRound,
  Link2,
  Lock,
  Plus,
  RefreshCw,
  ShieldAlert,
  Smartphone,
  TabletSmartphone,
  Unlock,
  UserPlus,
  X,
} from 'lucide-react';
import {
  CBC_RUBRIC_META,
  MONTHLY_LICENSE_KES,
  getLevelGroupForGrade,
  getSubjectsForGrade,
} from '../data/cbcCurriculum';
import { formatTimeClock } from '../hooks/useSomaTimeStore';
import type { ChildProfile, DeviceMode, LearningArea, PendingApproval } from '../types/somatime';

interface ParentDashboardProps {
  childrenList: ChildProfile[];
  selectedChild: ChildProfile;
  parentPin: string;
  parentContact: string;
  cloudSyncOk: boolean;
  cloudSyncMessage: string;
  onRetryCloudSync: () => void;
  onSelectChild: (id: string) => void;
  onUpdateParentPin: (newPin: string) => boolean;
  onActivateChildLicense: (
    childId: string,
    payerPhone?: string,
    explicitReceiptRef?: string
  ) => { ok: boolean; receiptRef: string; expiresAt: string };
  onActivateAllUnpaidLicenses: (payerPhone?: string, explicitReceiptRef?: string) => number;
  onUpdateChild: (id: string, patch: Partial<ChildProfile>) => void;
  onRegeneratePairingCode: (id: string) => string | null;
  onOpenLearnerPair: () => void;
  onOpenParentRegistration: () => void;
  onAddChild: (input: { name: string; grade: number; school: string }) => void;
  onActivateHandOff: (id: string) => boolean;
  onRequestApprovalPin: (approval: PendingApproval) => void;
  onDeclineApproval: (childId: string, approvalId: string) => void;
}

export function ParentDashboard({
  childrenList,
  selectedChild,
  parentPin,
  parentContact,
  cloudSyncOk,
  cloudSyncMessage,
  onRetryCloudSync,
  onSelectChild,
  onUpdateParentPin,
  onActivateChildLicense,
  onActivateAllUnpaidLicenses,
  onUpdateChild,
  onRegeneratePairingCode,
  onOpenLearnerPair,
  onOpenParentRegistration,
  onAddChild,
  onActivateHandOff,
  onRequestApprovalPin,
  onDeclineApproval,
}: ParentDashboardProps) {
  const [showAddChild, setShowAddChild] = useState(false);
  const [newName, setNewName] = useState('');
  const [newGrade, setNewGrade] = useState(5);
  const [newSchool, setNewSchool] = useState('Nairobi Primary School');

  // Parent PIN Setup / Update state
  const [showCurrentPin, setShowCurrentPin] = useState(false);
  const [draftPin, setDraftPin] = useState('');
  const [confirmDraftPin, setConfirmDraftPin] = useState('');
  const [pinSaveMsg, setPinSaveMsg] = useState<{ ok: boolean; text: string } | null>(null);

  // Per-child M-Pesa monthly licensing state (KES 30/child/mo)
  const [payerPhone, setPayerPhone] = useState(parentContact || '0712 345 678');
  const [licenseBanner, setLicenseBanner] = useState('');
  const [licenseError, setLicenseError] = useState('');
  const [isPayingMpesa, setIsPayingMpesa] = useState(false);
  const [mpesaStatus, setMpesaStatus] = useState<{
    configured: boolean;
    environment: 'sandbox' | 'production';
    shortcode: string;
    hasCallbackUrl: boolean;
    missingVars: string[];
  } | null>(null);

  useEffect(() => {
    let active = true;
    fetch('/api/mpesa/status')
      .then((res) => (res.ok ? res.json() : null))
      .then((data) => {
        if (active && data) setMpesaStatus(data);
      })
      .catch(() => {});
    return () => {
      active = false;
    };
  }, []);

  const remainingCapMins = Math.max(
    0,
    selectedChild.dailyCapMins - selectedChild.usedScreenTodayMins
  );
  const usedRatio = Math.min(
    100,
    Math.round((selectedChild.usedScreenTodayMins / Math.max(1, selectedChild.dailyCapMins)) * 100)
  );
  const totalActivityMins =
    selectedChild.learnedMinutesToday + selectedChild.usedScreenTodayMins || 1;
  const learnSharePct = Math.round(
    (selectedChild.learnedMinutesToday / totalActivityMins) * 100
  );

  const activeLicenseCount = childrenList.filter((c) => c.licenseActive).length;
  const unpaidChildren = childrenList.filter((c) => !c.licenseActive);

  const handleToggleMandatory = (area: LearningArea) => {
    const exists = selectedChild.mandatorySubjects.includes(area);
    const next = exists
      ? selectedChild.mandatorySubjects.filter((s) => s !== area)
      : [...selectedChild.mandatorySubjects, area];
    onUpdateChild(selectedChild.id, { mandatorySubjects: next });
  };

  const handleCreateChild = (e: React.FormEvent) => {
    e.preventDefault();
    if (!newName.trim()) return;
    onAddChild({ name: newName, grade: newGrade, school: newSchool });
    setNewName('');
    setNewGrade(5);
    setShowAddChild(false);
  };

  const handleDeviceMode = (mode: DeviceMode) => {
    onUpdateChild(selectedChild.id, { deviceMode: mode });
  };

  const handleSaveCustomPin = (e: React.FormEvent) => {
    e.preventDefault();
    if (draftPin.length !== 4) {
      setPinSaveMsg({ ok: false, text: 'Parent PIN must be exactly 4 digits.' });
      return;
    }
    if (draftPin !== confirmDraftPin) {
      setPinSaveMsg({ ok: false, text: 'New PIN and Confirm PIN do not match.' });
      return;
    }
    const ok = onUpdateParentPin(draftPin);
    if (ok) {
      setDraftPin('');
      setConfirmDraftPin('');
      setPinSaveMsg({ ok: true, text: 'Parent PIN updated and synced to paired devices.' });
    }
  };

  const runMpesaStkCheckout = async (targetChildIds: string[], singleChildName?: string) => {
    if (isPayingMpesa || targetChildIds.length === 0) return;
    setIsPayingMpesa(true);
    setLicenseError('');
    setLicenseBanner('');

    const totalAmount = targetChildIds.length * MONTHLY_LICENSE_KES;
    try {
      const initRes = await fetch('/api/mpesa/stkpush', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          phoneNumber: payerPhone,
          childIds: targetChildIds,
          accountReference:
            targetChildIds.length === 1 ? `SOMA-${targetChildIds[0].slice(0, 6)}` : 'SOMA-FAMILY',
        }),
      });
      const initData = (await initRes.json()) as {
        ok?: boolean;
        mode?: 'sandbox_demo' | 'daraja_stk';
        checkoutRequestId?: string;
        receiptRef?: string;
        message?: string;
        error?: string;
      };

      if (!initRes.ok || !initData.ok) {
        setLicenseError(initData.error || 'Could not initiate M-Pesa STK Push.');
        setIsPayingMpesa(false);
        return;
      }

      if (initData.mode === 'sandbox_demo') {
        if (targetChildIds.length === 1) {
          const res = onActivateChildLicense(targetChildIds[0], payerPhone, initData.receiptRef);
          setLicenseBanner(
            `M-Pesa Sandbox confirmed KES ${totalAmount}/mo for ${singleChildName || selectedChild.name} · Ref ${res.receiptRef} · Valid until ${res.expiresAt}. Pairing & Child Lock unlocked.`
          );
        } else {
          const count = onActivateAllUnpaidLicenses(payerPhone, initData.receiptRef);
          setLicenseBanner(
            `M-Pesa Sandbox confirmed KES ${count * MONTHLY_LICENSE_KES}/mo for ${count} child profile(s) · Ref ${initData.receiptRef}. All pairing codes & Child Locks unlocked.`
          );
        }
        setIsPayingMpesa(false);
        return;
      }

      const checkoutId = initData.checkoutRequestId;
      if (!checkoutId) {
        setLicenseError('Daraja did not return a CheckoutRequestID.');
        setIsPayingMpesa(false);
        return;
      }

      setLicenseBanner(
        initData.message ||
          `M-Pesa STK prompt for KES ${totalAmount} sent to ${payerPhone}. Enter your M-Pesa PIN on your phone...`
      );

      for (let attempt = 0; attempt < 20; attempt += 1) {
        await new Promise((r) => window.setTimeout(r, 3000));
        const qRes = await fetch(`/api/mpesa/query/${encodeURIComponent(checkoutId)}`);
        const qData = (await qRes.json()) as {
          status?: 'pending' | 'paid' | 'failed';
          receiptRef?: string | null;
          message?: string;
        };

        if (qData.status === 'paid') {
          const confirmedRef = qData.receiptRef || `MPESA-${checkoutId.slice(-8).toUpperCase()}`;
          if (targetChildIds.length === 1) {
            const res = onActivateChildLicense(targetChildIds[0], payerPhone, confirmedRef);
            setLicenseBanner(
              `M-Pesa confirmed KES ${totalAmount}/mo for ${singleChildName || selectedChild.name} · Receipt ${res.receiptRef} · Valid until ${res.expiresAt}.`
            );
          } else {
            const count = onActivateAllUnpaidLicenses(payerPhone, confirmedRef);
            setLicenseBanner(
              `M-Pesa confirmed KES ${count * MONTHLY_LICENSE_KES}/mo for ${count} child profile(s) · Receipt ${confirmedRef}.`
            );
          }
          setIsPayingMpesa(false);
          return;
        }

        if (qData.status === 'failed') {
          setLicenseBanner('');
          setLicenseError(qData.message || 'M-Pesa payment was cancelled or failed.');
          setIsPayingMpesa(false);
          return;
        }
      }

      setLicenseBanner('');
      setLicenseError(
        'Still waiting on M-Pesa confirmation. If you completed the PIN prompt, retry in a moment.'
      );
    } catch {
      setLicenseError('Unable to reach M-Pesa server endpoint. Check your connection.');
    } finally {
      setIsPayingMpesa(false);
    }
  };

  const handlePaySelectedLicense = (e: React.FormEvent) => {
    e.preventDefault();
    void runMpesaStkCheckout([selectedChild.id], selectedChild.name);
  };

  const handlePayAllUnpaid = () => {
    void runMpesaStkCheckout(unpaidChildren.map((c) => c.id));
  };

  return (
    <main className="mx-auto max-w-7xl px-4 py-6 sm:px-6 lg:py-8">
      {/* Multi-Child Profile Manager */}
      <section aria-label="Child profiles">
        <div className="flex flex-wrap items-center justify-between gap-4">
          <div>
            <h1 className="font-display text-2xl font-bold tracking-tight text-slate-900 sm:text-3xl">
              Parent & Guardian Control Center
            </h1>
            <p className="mt-1 text-sm text-slate-600">
              Manage Kenyan CBC learning requirements, Parent PIN, monthly child licenses, and device lock rules.
            </p>
          </div>
          <button
            type="button"
            onClick={() => setShowAddChild((v) => !v)}
            className="flex min-h-[44px] items-center gap-2 rounded-2xl border border-slate-200 bg-white px-4 py-2 text-sm font-semibold text-slate-800 transition-colors hover:bg-slate-50 whitespace-nowrap shrink-0"
          >
            {showAddChild ? <X className="h-4 w-4" /> : <UserPlus className="h-4 w-4 text-teal-700" />}
            <span>{showAddChild ? 'Close Form' : 'Add Child Profile'}</span>
          </button>
        </div>

        {showAddChild && (
          <form
            onSubmit={handleCreateChild}
            className="mt-4 grid grid-cols-1 gap-4 rounded-2xl border border-slate-200 bg-white p-5 sm:grid-cols-4 sm:items-end"
          >
            <div>
              <label htmlFor="child-name" className="block text-xs font-semibold text-slate-700">
                Child Full Name
              </label>
              <input
                id="child-name"
                type="text"
                required
                placeholder="e.g. Kamau Njoroge"
                value={newName}
                onChange={(e) => setNewName(e.target.value)}
                className="mt-1.5 w-full rounded-xl border border-slate-200 bg-slate-50 px-3 py-2.5 text-sm text-slate-900 focus:border-teal-600 focus:bg-white focus:outline-none"
              />
            </div>
            <div>
              <label htmlFor="child-grade" className="block text-xs font-semibold text-slate-700">
                CBC Grade (1–12)
              </label>
              <select
                id="child-grade"
                value={newGrade}
                onChange={(e) => setNewGrade(Number(e.target.value))}
                className="mt-1.5 w-full rounded-xl border border-slate-200 bg-slate-50 px-3 py-2.5 text-sm text-slate-900 focus:border-teal-600 focus:bg-white focus:outline-none"
              >
                {Array.from({ length: 12 }, (_, idx) => idx + 1).map((g) => (
                  <option key={g} value={g}>
                    Grade {g} ({getLevelGroupForGrade(g)})
                  </option>
                ))}
              </select>
            </div>
            <div>
              <label htmlFor="child-school" className="block text-xs font-semibold text-slate-700">
                School Name
              </label>
              <input
                id="child-school"
                type="text"
                value={newSchool}
                onChange={(e) => setNewSchool(e.target.value)}
                className="mt-1.5 w-full rounded-xl border border-slate-200 bg-slate-50 px-3 py-2.5 text-sm text-slate-900 focus:border-teal-600 focus:bg-white focus:outline-none"
              />
            </div>
            <button
              type="submit"
              className="flex min-h-[44px] items-center justify-center gap-2 rounded-xl bg-teal-700 px-4 py-2.5 text-sm font-semibold text-white transition-colors hover:bg-teal-800"
            >
              <Plus className="h-4 w-4" />
              <span>Save Child</span>
            </button>
          </form>
        )}

        {/* Child Switcher Strip */}
        <div className="mt-5 grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3">
          {childrenList.map((child) => {
            const isSelected = child.id === selectedChild.id;
            return (
              <button
                key={child.id}
                type="button"
                onClick={() => {
                  onSelectChild(child.id);
                  setLicenseBanner('');
                }}
                className={`flex items-center gap-3.5 rounded-2xl border p-4 text-left transition-colors ${
                  isSelected
                    ? 'border-teal-600 bg-teal-50/60'
                    : 'border-slate-200 bg-white hover:bg-slate-50'
                }`}
              >
                <div className="relative h-12 w-12 shrink-0 overflow-hidden rounded-2xl bg-slate-200">
                  <img
                    src={child.avatarUrl}
                    alt={child.name}
                    referrerPolicy="no-referrer"
                    className="h-full w-full object-cover"
                  />
                </div>
                <div className="min-w-0 flex-1">
                  <div className="flex items-center justify-between gap-2">
                    <p className="truncate font-display text-base font-bold text-slate-900">
                      {child.name}
                    </p>
                    <span className="font-mono text-xs font-semibold text-slate-700 tabular-nums shrink-0">
                      {formatTimeClock(child.availableScreenSeconds)}
                    </span>
                  </div>
                  <p className="mt-0.5 truncate text-xs text-slate-600">
                    Grade {child.grade} · {getLevelGroupForGrade(child.grade)} · {child.school}
                  </p>
                  <p className="mt-1 flex flex-wrap items-center gap-1.5 text-xs font-medium text-slate-700">
                    {child.isLocked ? (
                      <>
                        <Lock className="h-3.5 w-3.5 text-amber-600" />
                        <span>Locked</span>
                      </>
                    ) : (
                      <>
                        <Unlock className="h-3.5 w-3.5 text-emerald-600" />
                        <span>Unlocked</span>
                      </>
                    )}
                    <span aria-hidden="true">·</span>
                    <span
                      className={
                        child.licenseActive
                          ? 'font-bold text-emerald-700'
                          : 'font-bold text-rose-600'
                      }
                    >
                      {child.licenseActive ? 'Licensed ✓' : 'License Unpaid'}
                    </span>
                  </p>
                </div>
              </button>
            );
          })}
        </div>
      </section>

      {/* Parent PIN Setup & Per-Child Monthly Licensing Row */}
      <div className="mt-6 grid grid-cols-1 gap-6 lg:grid-cols-12">
        {/* Parent PIN Security Card */}
        <section
          aria-label="Parent PIN setup"
          className="rounded-3xl border border-slate-200 bg-white p-5 sm:p-6 lg:col-span-5"
        >
          <div className="flex items-start justify-between gap-3">
            <div>
              <p className="text-xs font-semibold text-teal-700">Parent Access Security</p>
              <h2 className="font-display text-lg font-bold text-slate-900">
                Set Your Own Parent PIN
              </h2>
            </div>
            <button
              type="button"
              onClick={() => setShowCurrentPin((v) => !v)}
              className="flex min-h-[38px] items-center gap-1.5 rounded-xl border border-slate-200 bg-slate-50 px-3 py-1.5 text-xs font-semibold text-slate-700 hover:bg-slate-100"
            >
              {showCurrentPin ? <EyeOff className="h-3.5 w-3.5" /> : <Eye className="h-3.5 w-3.5" />}
              <span className="font-mono font-bold">
                {showCurrentPin ? parentPin : '••••'}
              </span>
            </button>
          </div>
          <p className="mt-1.5 text-xs text-slate-600">
            Your private 4-digit PIN protects Child Hand-Off exits, screen unlocks, and minute approvals. Never shown on the learner&apos;s screen.
          </p>

          <form onSubmit={handleSaveCustomPin} className="mt-4 space-y-3">
            <div className="grid grid-cols-2 gap-2.5">
              <div>
                <label htmlFor="new-parent-pin" className="block text-xs font-semibold text-slate-700">
                  New 4-Digit PIN
                </label>
                <input
                  id="new-parent-pin"
                  type={showCurrentPin ? 'text' : 'password'}
                  inputMode="numeric"
                  maxLength={4}
                  placeholder="••••"
                  value={draftPin}
                  onChange={(e) => {
                    setDraftPin(e.target.value.replace(/\D/g, '').slice(0, 4));
                    setPinSaveMsg(null);
                  }}
                  className="mt-1 w-full rounded-xl border border-slate-200 bg-slate-50 px-3 py-2.5 font-mono text-sm font-bold text-slate-900 focus:border-teal-600 focus:bg-white focus:outline-none"
                />
              </div>
              <div>
                <label htmlFor="confirm-parent-pin" className="block text-xs font-semibold text-slate-700">
                  Confirm PIN
                </label>
                <input
                  id="confirm-parent-pin"
                  type={showCurrentPin ? 'text' : 'password'}
                  inputMode="numeric"
                  maxLength={4}
                  placeholder="••••"
                  value={confirmDraftPin}
                  onChange={(e) => {
                    setConfirmDraftPin(e.target.value.replace(/\D/g, '').slice(0, 4));
                    setPinSaveMsg(null);
                  }}
                  className="mt-1 w-full rounded-xl border border-slate-200 bg-slate-50 px-3 py-2.5 font-mono text-sm font-bold text-slate-900 focus:border-teal-600 focus:bg-white focus:outline-none"
                />
              </div>
            </div>

            {pinSaveMsg && (
              <p
                role="status"
                className={`text-xs font-semibold ${
                  pinSaveMsg.ok ? 'text-emerald-700' : 'text-rose-600'
                }`}
              >
                {pinSaveMsg.text}
              </p>
            )}

            <button
              type="submit"
              className="flex min-h-[44px] w-full items-center justify-center gap-2 rounded-2xl bg-slate-900 px-4 py-2.5 text-xs font-bold text-white transition-colors hover:bg-slate-800"
            >
              <KeyRound className="h-4 w-4" />
              <span>Save Custom Parent PIN</span>
            </button>
          </form>
        </section>

        {/* Per-Child Monthly Licensing Area */}
        <section
          aria-label="Child monthly licensing area"
          className="rounded-3xl border border-slate-200 bg-white p-5 sm:p-6 lg:col-span-7"
        >
          <div className="flex flex-wrap items-start justify-between gap-3">
            <div>
              <p className="text-xs font-semibold text-teal-700">
                Child Licensing · Charged Per Child Per Month (KES {MONTHLY_LICENSE_KES}/child/mo)
              </p>
              <h2 className="font-display text-lg font-bold text-slate-900">
                Licensing Area · {selectedChild.name}
              </h2>
            </div>
            <div className="text-right">
              <span className="font-mono text-xs font-bold text-slate-700">
                {activeLicenseCount}/{childrenList.length} Children Licensed
              </span>
              <p className="text-[11px] text-slate-500">
                Monthly Total: KES {childrenList.length * MONTHLY_LICENSE_KES}/mo
              </p>
            </div>
          </div>

          {/* Per-child status breakdown */}
          <div className="mt-4 grid grid-cols-1 gap-2.5 sm:grid-cols-2">
            {childrenList.map((child) => {
              const isSelected = child.id === selectedChild.id;
              return (
                <div
                  key={child.id}
                  className={`flex items-center justify-between rounded-2xl border px-3.5 py-2.5 text-xs ${
                    isSelected
                      ? 'border-teal-500 bg-teal-50/50'
                      : 'border-slate-200 bg-slate-50'
                  }`}
                >
                  <div className="min-w-0">
                    <p className="truncate font-bold text-slate-900">
                      {child.name} (G{child.grade})
                    </p>
                    <p className="text-[11px] text-slate-600">
                      {child.licenseActive
                        ? `Active until ${child.licenseExpiresAt || '30 days'} · ${child.licensePaymentRef || 'Paid'}`
                        : `Unpaid · KES ${MONTHLY_LICENSE_KES}/mo due`}
                    </p>
                  </div>
                  {child.licenseActive ? (
                    <BadgeCheck className="h-5 w-5 shrink-0 text-emerald-600" />
                  ) : (
                    <button
                      type="button"
                      onClick={() => onSelectChild(child.id)}
                      className="shrink-0 rounded-lg bg-amber-100 px-2.5 py-1 text-[11px] font-bold text-amber-900 hover:bg-amber-200"
                    >
                      Select to Pay
                    </button>
                  )}
                </div>
              );
            })}
          </div>

          {licenseBanner && (
            <div
              role="status"
              className="mt-4 rounded-2xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-xs font-semibold text-emerald-900"
            >
              {licenseBanner}
            </div>
          )}

          {licenseError && (
            <div
              role="alert"
              className="mt-4 rounded-2xl border border-rose-200 bg-rose-50 px-4 py-3 text-xs font-semibold text-rose-800"
            >
              {licenseError}
            </div>
          )}

          {selectedChild.licenseActive ? (
            <div className="mt-4 flex flex-col gap-3 rounded-2xl border border-emerald-200 bg-emerald-50/70 p-4 sm:flex-row sm:items-center sm:justify-between">
              <div>
                <p className="flex items-center gap-1.5 text-sm font-bold text-emerald-900">
                  <BadgeCheck className="h-4 w-4 text-emerald-700" />
                  <span>{selectedChild.name}&apos;s Monthly M-Pesa License is Active</span>
                </p>
                <p className="mt-0.5 text-xs text-emerald-800">
                  KES {MONTHLY_LICENSE_KES}/mo · Valid until{' '}
                  <span className="font-mono font-bold">{selectedChild.licenseExpiresAt}</span> ·
                  M-Pesa Receipt{' '}
                  <span className="font-mono font-bold">{selectedChild.licensePaymentRef}</span>
                </p>
                <p className="mt-1 text-xs text-slate-700">
                  Pairing Code generation and &ldquo;Activate Child Lock &amp; Hand Over Phone&rdquo; are unlocked below.
                </p>
              </div>
              {unpaidChildren.length > 0 && (
                <button
                  type="button"
                  disabled={isPayingMpesa}
                  onClick={handlePayAllUnpaid}
                  className="shrink-0 rounded-xl border border-emerald-300 bg-white px-3.5 py-2 text-xs font-bold text-emerald-900 hover:bg-emerald-100 disabled:opacity-50"
                >
                  Pay Remaining via M-Pesa ({unpaidChildren.length} × KES {MONTHLY_LICENSE_KES})
                </button>
              )}
            </div>
          ) : (
            <form
              onSubmit={handlePaySelectedLicense}
              className="mt-4 rounded-2xl border border-amber-200 bg-amber-50/60 p-4 space-y-3"
            >
              <div className="flex flex-wrap items-center justify-between gap-2">
                <div>
                  <p className="text-xs font-bold text-amber-950">
                    M-Pesa Lipa Na SomaTime · {selectedChild.name} · KES {MONTHLY_LICENSE_KES}/mo
                  </p>
                  <p className="text-[11px] text-slate-700">
                    M-Pesa only. Required before generating a child phone pairing code or activating &ldquo;Activate Child Lock &amp; Hand Over Phone&rdquo;.
                  </p>
                </div>
                <span className="rounded-xl bg-emerald-700 px-3 py-1 text-[11px] font-extrabold text-white">
                  M-Pesa Only · Paybill {mpesaStatus?.shortcode || '174379'}
                </span>
              </div>

              <div className="grid grid-cols-1 gap-2.5 sm:grid-cols-12 sm:items-end">
                <div className="sm:col-span-5">
                  <label htmlFor="mpesa-phone-input" className="block text-[11px] font-semibold text-slate-700">
                    M-Pesa Phone Number (Safaricom)
                  </label>
                  <input
                    id="mpesa-phone-input"
                    type="tel"
                    required
                    placeholder="0712 345 678 or 2547..."
                    value={payerPhone}
                    onChange={(e) => {
                      setPayerPhone(e.target.value);
                      setLicenseError('');
                    }}
                    className="mt-1 w-full rounded-xl border border-slate-300 bg-white px-3 py-2 font-mono text-xs font-semibold text-slate-900 focus:border-emerald-600 focus:outline-none"
                  />
                </div>
                <div className="flex flex-wrap gap-2 sm:col-span-7 sm:justify-end">
                  <button
                    type="submit"
                    disabled={isPayingMpesa}
                    className="flex min-h-[42px] items-center justify-center gap-2 rounded-xl bg-emerald-600 px-4 py-2 text-xs font-extrabold text-white transition-colors hover:bg-emerald-700 disabled:opacity-60"
                  >
                    <Smartphone className="h-4 w-4" />
                    <span>
                      {isPayingMpesa
                        ? 'Sending M-Pesa STK...'
                        : `Pay KES ${MONTHLY_LICENSE_KES}/mo via M-Pesa (${selectedChild.name.split(' ')[0]})`}
                    </span>
                  </button>
                  {unpaidChildren.length > 1 && (
                    <button
                      type="button"
                      disabled={isPayingMpesa}
                      onClick={handlePayAllUnpaid}
                      className="min-h-[42px] rounded-xl border border-emerald-700 bg-white px-3 py-2 text-xs font-bold text-emerald-900 hover:bg-emerald-50 disabled:opacity-60"
                    >
                      Pay All ({unpaidChildren.length} × KES {MONTHLY_LICENSE_KES})
                    </button>
                  )}
                </div>
              </div>
            </form>
          )}

          {/* Daraja M-Pesa Sandbox / Live Readiness Box */}
          <div className="mt-3 rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 text-[11px] text-slate-700">
            <div className="flex flex-wrap items-center justify-between gap-2">
              <span className="font-bold text-slate-900">
                Safaricom Daraja STK Status:{' '}
                <span
                  className={
                    mpesaStatus?.configured ? 'text-emerald-700' : 'text-amber-800'
                  }
                >
                  {mpesaStatus?.configured
                    ? `${mpesaStatus.environment === 'production' ? 'Production Live' : 'Daraja Sandbox Active'} (Shortcode ${mpesaStatus.shortcode})`
                    : 'Local Sandbox Simulation (Add Daraja Secrets for Real STK Phone Popups)'}
                </span>
              </span>
            </div>
            {!mpesaStatus?.configured && (
              <p className="mt-1 leading-relaxed text-slate-600">
                To send real M-Pesa Sandbox PIN prompts to a phone: create a Sandbox app on{' '}
                <span className="font-semibold text-slate-800">developer.safaricom.co.ke</span>{' '}
                (Lipa Na M-Pesa Sandbox), and add{' '}
                <span className="font-mono font-semibold text-slate-800">
                  {mpesaStatus?.missingVars?.join(', ') ||
                    'MPESA_CONSUMER_KEY, MPESA_CONSUMER_SECRET, MPESA_PASSKEY, MPESA_SHORTCODE=174379'}
                </span>{' '}
                plus HTTPS <span className="font-mono font-semibold text-slate-800">APP_URL</span> in
                AI Studio Secrets.
              </p>
            )}
          </div>
        </section>
      </div>

      {/* Hand-Off Mode & Child Phone Pairing Card */}
      <section
        aria-label="Device hand-off and pairing controls"
        className="mt-6 rounded-3xl border border-slate-200 bg-white p-5 sm:p-6"
      >
        <div className="flex flex-col gap-5 lg:flex-row lg:items-center lg:justify-between">
          <div className="space-y-2">
            <p className="text-xs font-semibold text-teal-700">
              Active Device Policy · {selectedChild.name} (Grade {selectedChild.grade})
            </p>
            <h2 className="font-display text-xl font-bold text-slate-900">
              Device Mode, Child Phone Pairing &amp; Hand-Off Lock
            </h2>
            <p className="max-w-2xl text-sm text-slate-600">
              {selectedChild.licenseActive
                ? `Generate a pairing code for ${selectedChild.name.split(' ')[0]}'s own phone, or use locked Hand-Off Mode on your shared phone (your custom Parent PIN is required to exit).`
                : `Pay ${selectedChild.name.split(' ')[0]}'s monthly license (KES ${MONTHLY_LICENSE_KES}/mo) above to unlock pairing code generation or shared-phone Hand-Off Lock.`}
            </p>

            <div className="flex flex-wrap gap-2 pt-1">
              <button
                type="button"
                onClick={() => handleDeviceMode('own_phone')}
                className={`flex min-h-[44px] items-center gap-2 rounded-xl border px-3.5 py-2 text-xs font-semibold transition-colors ${
                  selectedChild.deviceMode === 'own_phone'
                    ? 'border-teal-600 bg-teal-50 text-teal-900'
                    : 'border-slate-200 bg-slate-50 text-slate-700 hover:bg-slate-100'
                }`}
              >
                <Smartphone className="h-4 w-4" />
                <span>Mode A: Child&apos;s Own Phone</span>
              </button>
              <button
                type="button"
                onClick={() => handleDeviceMode('hand_off')}
                className={`flex min-h-[44px] items-center gap-2 rounded-xl border px-3.5 py-2 text-xs font-semibold transition-colors ${
                  selectedChild.deviceMode === 'hand_off'
                    ? 'border-teal-600 bg-teal-50 text-teal-900'
                    : 'border-slate-200 bg-slate-50 text-slate-700 hover:bg-slate-100'
                }`}
              >
                <TabletSmartphone className="h-4 w-4" />
                <span>Mode B: Shared Parent Phone (Hand-Off)</span>
              </button>
            </div>
          </div>

          <div className="flex flex-col items-stretch gap-2.5 sm:flex-row sm:items-center lg:flex-col lg:items-end shrink-0">
            <button
              type="button"
              disabled={!selectedChild.licenseActive}
              onClick={() => onActivateHandOff(selectedChild.id)}
              className={`flex min-h-[48px] items-center justify-center gap-2.5 rounded-2xl px-5 py-3 text-sm font-bold transition-colors whitespace-nowrap ${
                selectedChild.licenseActive
                  ? 'bg-teal-700 text-white hover:bg-teal-800 active:scale-[0.99]'
                  : 'cursor-not-allowed bg-slate-200 text-slate-500'
              }`}
            >
              <Lock className="h-4 w-4" />
              <span>Activate Child Lock &amp; Hand Over Phone</span>
            </button>
            <span className="text-center text-xs text-slate-500 lg:text-right">
              {selectedChild.licenseActive
                ? 'Switches to locked Child Mode · Requires your Parent PIN to exit'
                : `Pay KES ${MONTHLY_LICENSE_KES}/mo license above to activate this button`}
            </span>
          </div>
        </div>

        {/* Child Phone Pairing Code Strip */}
        <div className="mt-5 flex flex-col gap-4 border-t border-slate-100 pt-5 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <p className="text-xs font-bold text-slate-800">
              Child Phone Pairing Code for {selectedChild.name}
            </p>
            {selectedChild.licenseActive ? (
              <div className="mt-1.5 flex flex-wrap items-center gap-3">
                <span className="rounded-xl border border-teal-200 bg-teal-50 px-3.5 py-1.5 font-mono text-lg font-extrabold tracking-wider text-teal-900">
                  {selectedChild.pairingCode}
                </span>
                <span className="text-xs font-semibold text-slate-600">
                  {selectedChild.isPaired
                    ? 'Paired · Background watch-minute guard active'
                    : 'Licensed & Ready · Enter this code on learner phone'}
                </span>
              </div>
            ) : (
              <div className="mt-1.5 flex flex-wrap items-center gap-3">
                <span className="inline-flex items-center gap-1.5 rounded-xl border border-amber-200 bg-amber-50 px-3.5 py-1.5 font-mono text-sm font-extrabold tracking-wider text-amber-900">
                  <Lock className="h-3.5 w-3.5" />
                  SOMA-•••• (Locked)
                </span>
                <span className="text-xs font-semibold text-amber-800">
                  Pay KES {MONTHLY_LICENSE_KES}/mo monthly license above to generate &amp; unlock pairing code
                </span>
              </div>
            )}
            <p
              className={`mt-1.5 text-xs font-semibold ${
                cloudSyncOk ? 'text-emerald-700' : 'text-rose-600'
              }`}
            >
              {cloudSyncMessage}
            </p>
          </div>
          <div className="flex flex-wrap gap-2">
            <button
              type="button"
              disabled={!selectedChild.licenseActive}
              onClick={onRetryCloudSync}
              className="flex min-h-[42px] items-center gap-1.5 rounded-xl border border-slate-200 bg-white px-3.5 py-2 text-xs font-semibold text-slate-800 hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-50"
            >
              <RefreshCw className="h-3.5 w-3.5" />
              <span>Sync Cloud</span>
            </button>
            <button
              type="button"
              disabled={!selectedChild.licenseActive}
              onClick={() => onRegeneratePairingCode(selectedChild.id)}
              className="flex min-h-[42px] items-center gap-1.5 rounded-xl border border-slate-200 bg-white px-3.5 py-2 text-xs font-semibold text-slate-800 hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-50"
            >
              <RefreshCw className="h-3.5 w-3.5" />
              <span>Generate New Code</span>
            </button>
            <button
              type="button"
              disabled={!selectedChild.licenseActive}
              onClick={onOpenLearnerPair}
              className="flex min-h-[42px] items-center gap-1.5 rounded-xl border border-emerald-200 bg-emerald-50 px-3.5 py-2 text-xs font-bold text-emerald-900 hover:bg-emerald-100 disabled:cursor-not-allowed disabled:opacity-50"
            >
              <Link2 className="h-3.5 w-3.5" />
              <span>Pair Learner Phone</span>
            </button>
            <button
              type="button"
              onClick={onOpenParentRegistration}
              className="flex min-h-[42px] items-center gap-1.5 rounded-xl border border-slate-200 bg-slate-50 px-3.5 py-2 text-xs font-semibold text-slate-700 hover:bg-slate-100"
            >
              <span>Parent Setup Wizard</span>
            </button>
          </div>
        </div>
      </section>

      {/* Main Two-Column Controls & Analytics */}
      <div className="mt-6 grid grid-cols-1 gap-6 lg:grid-cols-12">
        {/* Left Column: Screen Time Economy & Rules Engine */}
        <section
          aria-label="Screen time economy and rules"
          className="space-y-6 lg:col-span-6"
        >
          <div className="rounded-3xl border border-slate-200 bg-white p-5 sm:p-6">
            <h2 className="font-display text-lg font-bold text-slate-900">
              Screen Time Economy &amp; Rules Engine
            </h2>
            <p className="mt-1 text-xs text-slate-600">
              Changes sync immediately to Child Mode via local device policy.
            </p>

            {/* Daily Max Cap */}
            <div className="mt-5 border-t border-slate-100 pt-5">
              <div className="flex items-center justify-between gap-4">
                <label
                  htmlFor="daily-cap-slider"
                  className="text-sm font-semibold text-slate-800"
                >
                  Daily Maximum Screen Time Cap
                </label>
                <span className="font-mono text-sm font-bold text-teal-800 tabular-nums">
                  {selectedChild.dailyCapMins} mins/day
                </span>
              </div>
              <input
                id="daily-cap-slider"
                type="range"
                min={30}
                max={180}
                step={15}
                value={selectedChild.dailyCapMins}
                onChange={(e) =>
                  onUpdateChild(selectedChild.id, { dailyCapMins: Number(e.target.value) })
                }
                className="mt-3 h-2 w-full cursor-pointer accent-teal-700"
              />
              <div className="mt-1.5 flex justify-between text-xs text-slate-500 tabular-nums">
                <span>30m</span>
                <span>Used today: {selectedChild.usedScreenTodayMins}m ({remainingCapMins}m left)</span>
                <span>180m</span>
              </div>
            </div>

            {/* Exchange Rate */}
            <div className="mt-5 border-t border-slate-100 pt-5">
              <div className="flex items-center justify-between gap-2">
                <span className="text-sm font-semibold text-slate-800">
                  CBC Lesson Exchange Rate
                </span>
                <span className="text-xs text-slate-600">1 completed activity =</span>
              </div>
              <div className="mt-3 grid grid-cols-4 gap-2">
                {[10, 15, 20, 30].map((mins) => {
                  const active = selectedChild.exchangeRateMins === mins;
                  return (
                    <button
                      key={mins}
                      type="button"
                      onClick={() => onUpdateChild(selectedChild.id, { exchangeRateMins: mins })}
                      className={`min-h-[44px] rounded-xl border font-mono text-xs font-bold tabular-nums transition-colors ${
                        active
                          ? 'border-teal-700 bg-teal-700 text-white'
                          : 'border-slate-200 bg-slate-50 text-slate-700 hover:bg-slate-100'
                      }`}
                    >
                      {mins} mins
                    </button>
                  );
                })}
              </div>
            </div>

            {/* Mandatory Focus Subjects */}
            <div className="mt-5 border-t border-slate-100 pt-5">
              <span className="block text-sm font-semibold text-slate-800">
                Mandatory Focus Subjects
              </span>
              <p className="mt-1 text-xs text-slate-600">
                Child must complete at least one selected subject before unlocking free-choice CBC areas.
              </p>
              <div className="mt-3 flex flex-wrap gap-2">
                {Array.from(
                  new Set([
                    ...getSubjectsForGrade(selectedChild.grade),
                    ...selectedChild.mandatorySubjects,
                  ])
                ).map((area) => {
                  const selected = selectedChild.mandatorySubjects.includes(area);
                  return (
                    <button
                      key={area}
                      type="button"
                      onClick={() => handleToggleMandatory(area)}
                      className={`min-h-[40px] rounded-xl border px-3 py-1.5 text-xs font-semibold transition-colors ${
                        selected
                          ? 'border-teal-700 bg-teal-50 text-teal-900'
                          : 'border-slate-200 bg-white text-slate-600 hover:bg-slate-50'
                      }`}
                    >
                      {selected ? `✓ ${area}` : area}
                    </button>
                  );
                })}
              </div>
            </div>

            {/* Approval Queue Mode */}
            <div className="mt-5 border-t border-slate-100 pt-5">
              <span className="block text-sm font-semibold text-slate-800">
                Earned Screen Time Approval Rule
              </span>
              <div className="mt-3 grid grid-cols-1 gap-2 sm:grid-cols-2">
                <button
                  type="button"
                  onClick={() =>
                    onUpdateChild(selectedChild.id, { requireParentApproval: false })
                  }
                  className={`min-h-[48px] rounded-xl border p-3 text-left transition-colors ${
                    !selectedChild.requireParentApproval
                      ? 'border-teal-600 bg-teal-50 text-teal-950'
                      : 'border-slate-200 bg-slate-50 text-slate-700 hover:bg-slate-100'
                  }`}
                >
                  <p className="text-xs font-bold">Auto-Approve Earned Minutes</p>
                  <p className="mt-0.5 text-xs text-slate-600">
                    Unlocks apps right after lesson completion
                  </p>
                </button>
                <button
                  type="button"
                  onClick={() =>
                    onUpdateChild(selectedChild.id, { requireParentApproval: true })
                  }
                  className={`min-h-[48px] rounded-xl border p-3 text-left transition-colors ${
                    selectedChild.requireParentApproval
                      ? 'border-teal-600 bg-teal-50 text-teal-950'
                      : 'border-slate-200 bg-slate-50 text-slate-700 hover:bg-slate-100'
                  }`}
                >
                  <p className="text-xs font-bold">Require Parent PIN / Approval</p>
                  <p className="mt-0.5 text-xs text-slate-600">
                    Holds earned time in queue until PIN {parentPin}
                  </p>
                </button>
              </div>
            </div>
          </div>

          {/* Pending Approvals Queue */}
          <div className="rounded-3xl border border-slate-200 bg-white p-5 sm:p-6">
            <div className="flex items-center justify-between gap-4">
              <div>
                <h3 className="font-display text-base font-bold text-slate-900">
                  Parent Approval Queue
                </h3>
                <p className="text-xs text-slate-600">
                  Review completed lessons awaiting Parent PIN sign-off
                </p>
              </div>
              <span className="font-mono text-xs font-bold text-slate-700 tabular-nums">
                {selectedChild.pendingApprovals.length} pending
              </span>
            </div>

            {selectedChild.pendingApprovals.length === 0 ? (
              <div className="mt-4 rounded-2xl border border-dashed border-slate-200 bg-slate-50 p-4 text-center text-xs text-slate-600">
                No pending minute requests for {selectedChild.name}.
              </div>
            ) : (
              <div className="mt-4 divide-y divide-slate-100">
                {selectedChild.pendingApprovals.map((item) => (
                  <div
                    key={item.id}
                    className="flex flex-col gap-3 py-3.5 first:pt-0 last:pb-0 sm:flex-row sm:items-center sm:justify-between"
                  >
                    <div>
                      <p className="text-sm font-semibold text-slate-900">{item.lessonTitle}</p>
                      <p className="mt-0.5 text-xs text-slate-600 tabular-nums">
                        {item.learningArea} · Score {item.scorePercent}% · +{item.minutesEarned} mins ·{' '}
                        {item.completedAt}
                      </p>
                    </div>
                    <div className="flex items-center gap-2 shrink-0">
                      <button
                        type="button"
                        onClick={() => onDeclineApproval(selectedChild.id, item.id)}
                        className="min-h-[40px] rounded-xl border border-slate-200 px-3 py-1.5 text-xs font-semibold text-slate-700 hover:bg-slate-50"
                      >
                        Decline
                      </button>
                      <button
                        type="button"
                        onClick={() => onRequestApprovalPin(item)}
                        className="flex min-h-[40px] items-center gap-1.5 rounded-xl bg-teal-700 px-3.5 py-1.5 text-xs font-semibold text-white hover:bg-teal-800"
                      >
                        <CheckCircle2 className="h-3.5 w-3.5" />
                        <span>Approve (+{item.minutesEarned}m)</span>
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </section>

        {/* Right Column: CBC Progress & Competency Analytics */}
        <section
          aria-label="Kenyan CBC progress and analytics"
          className="space-y-6 lg:col-span-6"
        >
          <div className="rounded-3xl border border-slate-200 bg-white p-5 sm:p-6">
            <h2 className="font-display text-lg font-bold text-slate-900">
              CBC Progress &amp; Competency Analytics
            </h2>
            <p className="mt-1 text-xs text-slate-600">
              Official Kenyan CBC Mastery Rubrics (EE, ME, AE, BE) and screen-time balance.
            </p>

            {/* Tabular KPI Row */}
            <div className="mt-5 grid grid-cols-3 gap-3 border-y border-slate-100 py-4">
              <div>
                <p className="text-xs text-slate-500">Lessons Done</p>
                <p className="mt-1 font-mono text-2xl font-bold text-slate-900 tabular-nums">
                  {selectedChild.completedLessonIds.length}
                </p>
                <p className="text-xs text-slate-500">{selectedChild.streakDays}-day streak</p>
              </div>
              <div>
                <p className="text-xs text-slate-500">Learning Time</p>
                <p className="mt-1 font-mono text-2xl font-bold text-teal-700 tabular-nums">
                  {selectedChild.learnedMinutesToday}m
                </p>
                <p className="text-xs text-slate-500">CBC today</p>
              </div>
              <div>
                <p className="text-xs text-slate-500">Entertainment</p>
                <p className="mt-1 font-mono text-2xl font-bold text-slate-900 tabular-nums">
                  {selectedChild.usedScreenTodayMins}m
                </p>
                <p className="text-xs text-slate-500">of {selectedChild.dailyCapMins}m cap</p>
              </div>
            </div>

            {/* Learning vs Entertainment Balance Bar */}
            <div className="mt-5 space-y-3">
              <div>
                <div className="flex justify-between text-xs font-medium text-slate-700">
                  <span>Learning vs. Entertainment Balance</span>
                  <span className="font-mono tabular-nums">
                    {learnSharePct}% Learning · {100 - learnSharePct}% Play
                  </span>
                </div>
                <div className="mt-2 flex h-3 w-full overflow-hidden rounded-full bg-slate-100">
                  <div
                    className="bg-teal-600 transition-all"
                    style={{ width: `${learnSharePct}%` }}
                  />
                  <div
                    className="bg-amber-400 transition-all"
                    style={{ width: `${100 - learnSharePct}%` }}
                  />
                </div>
              </div>

              <div>
                <div className="flex justify-between text-xs font-medium text-slate-700">
                  <span>Daily Cap Utilization</span>
                  <span className="font-mono tabular-nums">
                    {selectedChild.usedScreenTodayMins} / {selectedChild.dailyCapMins} mins ({usedRatio}%)
                  </span>
                </div>
                <div className="mt-2 h-2.5 w-full overflow-hidden rounded-full bg-slate-100">
                  <div
                    className={`h-full transition-all ${
                      usedRatio >= 90 ? 'bg-rose-600' : 'bg-slate-800'
                    }`}
                    style={{ width: `${usedRatio}%` }}
                  />
                </div>
              </div>
            </div>

            {/* Official CBC Rubric Mastery Grid */}
            <div className="mt-6">
              <h3 className="text-sm font-semibold text-slate-800">
                Learning Area Competency Levels
              </h3>
              <div className="mt-3 divide-y divide-slate-100">
                {Object.entries(selectedChild.rubricByArea).map(([area, code]) => {
                  if (!code) return null;
                  const meta = CBC_RUBRIC_META[code];
                  return (
                    <div
                      key={area}
                      className="flex items-center justify-between gap-3 py-2.5 text-sm"
                    >
                      <span className="font-medium text-slate-800">{area}</span>
                      <span
                        className={`inline-flex items-center gap-1.5 rounded-lg border px-2.5 py-1 text-xs font-bold ${meta.badgeBg} ${meta.badgeText} ${meta.borderColor}`}
                      >
                        <span className="font-mono">{meta.code}</span>
                        <span>·</span>
                        <span>{meta.label}</span>
                      </span>
                    </div>
                  );
                })}
              </div>
            </div>
          </div>

          {/* Activity History */}
          <div className="rounded-3xl border border-slate-200 bg-white p-5 sm:p-6">
            <div className="flex items-center justify-between">
              <h3 className="font-display text-base font-bold text-slate-900">
                Recent Activity &amp; Reward Log
              </h3>
              <Clock className="h-4 w-4 text-slate-400" />
            </div>

            {selectedChild.activityLogs.length === 0 ? (
              <p className="mt-4 text-xs text-slate-500">
                No activities completed yet today.
              </p>
            ) : (
              <div className="mt-3 divide-y divide-slate-100">
                {selectedChild.activityLogs.slice(0, 6).map((log) => (
                  <div
                    key={log.id}
                    className="flex items-center justify-between gap-3 py-3 text-xs"
                  >
                    <div className="min-w-0">
                      <p className="truncate font-semibold text-slate-900">{log.lessonTitle}</p>
                      <p className="mt-0.5 text-slate-500 tabular-nums">
                        {log.learningArea} · {log.timestamp} · Rubric {log.rubricEarned} ({log.scorePercent}%)
                      </p>
                    </div>
                    <div className="text-right shrink-0">
                      <p className="font-mono font-bold text-teal-800 tabular-nums">
                        +{log.minutesEarned}m
                      </p>
                      <p className="text-slate-500 capitalize">
                        {log.status === 'pending' && (
                          <span className="inline-flex items-center gap-1 text-amber-700">
                            <ShieldAlert className="h-3 w-3" /> Pending PIN
                          </span>
                        )}
                        {log.status === 'approved' && 'Credited'}
                        {log.status === 'declined' && 'Declined'}
                      </p>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </section>
      </div>
    </main>
  );
}
