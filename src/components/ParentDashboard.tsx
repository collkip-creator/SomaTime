import { useState } from 'react';
import {
  CheckCircle2,
  Clock,
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
  ALL_LEARNING_AREAS,
  CBC_RUBRIC_META,
  getLevelGroupForGrade,
} from '../data/cbcCurriculum';
import { formatTimeClock } from '../hooks/useSomaTimeStore';
import type { ChildProfile, DeviceMode, LearningArea, PendingApproval } from '../types/somatime';

interface ParentDashboardProps {
  childrenList: ChildProfile[];
  selectedChild: ChildProfile;
  parentPin: string;
  cloudSyncOk: boolean;
  cloudSyncMessage: string;
  onRetryCloudSync: () => void;
  onSelectChild: (id: string) => void;
  onUpdateChild: (id: string, patch: Partial<ChildProfile>) => void;
  onRegeneratePairingCode: (id: string) => void;
  onOpenLearnerPair: () => void;
  onOpenParentRegistration: () => void;
  onAddChild: (input: { name: string; grade: number; school: string }) => void;
  onActivateHandOff: (id: string) => void;
  onRequestApprovalPin: (approval: PendingApproval) => void;
  onDeclineApproval: (childId: string, approvalId: string) => void;
}

export function ParentDashboard({
  childrenList,
  selectedChild,
  parentPin,
  cloudSyncOk,
  cloudSyncMessage,
  onRetryCloudSync,
  onSelectChild,
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
              Manage Kenyan CBC learning requirements, screen-time economy, and device lock rules.
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
                onClick={() => onSelectChild(child.id)}
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
                  <p className="mt-1 flex items-center gap-1.5 text-xs font-medium text-slate-700">
                    {child.isLocked ? (
                      <>
                        <Lock className="h-3.5 w-3.5 text-amber-600" />
                        <span>Locked (Learn to Unlock)</span>
                      </>
                    ) : (
                      <>
                        <Unlock className="h-3.5 w-3.5 text-emerald-600" />
                        <span>Unlocked</span>
                      </>
                    )}
                    <span aria-hidden="true">·</span>
                    <span>
                      {child.deviceMode === 'hand_off' ? 'Hand-Off Phone' : "Child's Own Phone"}
                    </span>
                  </p>
                </div>
              </button>
            );
          })}
        </div>
      </section>

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
              Generate a pairing code for {selectedChild.name.split(' ')[0]}&apos;s own phone, or use
              locked Hand-Off Mode on your phone (Parent PIN{' '}
              <span className="font-mono font-semibold text-slate-900">{parentPin}</span> required to exit).
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
              onClick={() => onActivateHandOff(selectedChild.id)}
              className="flex min-h-[48px] items-center justify-center gap-2.5 rounded-2xl bg-teal-700 px-5 py-3 text-sm font-bold text-white transition-colors hover:bg-teal-800 active:scale-[0.99] whitespace-nowrap"
            >
              <Lock className="h-4 w-4" />
              <span>Activate Child Lock &amp; Hand Over Phone</span>
            </button>
            <span className="text-center text-xs text-slate-500 lg:text-right">
              Switches to locked Child Mode · Requires PIN {parentPin} to exit
            </span>
          </div>
        </div>

        {/* Child Phone Pairing Code Strip */}
        <div className="mt-5 flex flex-col gap-4 border-t border-slate-100 pt-5 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <p className="text-xs font-bold text-slate-800">
              Child Phone Pairing Code for {selectedChild.name}
            </p>
            <div className="mt-1.5 flex flex-wrap items-center gap-3">
              <span className="rounded-xl border border-teal-200 bg-teal-50 px-3.5 py-1.5 font-mono text-lg font-extrabold tracking-wider text-teal-900">
                {selectedChild.pairingCode}
              </span>
              <span className="text-xs font-semibold text-slate-600">
                {selectedChild.isPaired
                  ? 'Paired · Background watch-minute guard active'
                  : 'Not yet paired · Enter this code on learner phone'}
              </span>
            </div>
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
              onClick={onRetryCloudSync}
              className="flex min-h-[42px] items-center gap-1.5 rounded-xl border border-slate-200 bg-white px-3.5 py-2 text-xs font-semibold text-slate-800 hover:bg-slate-50"
            >
              <RefreshCw className="h-3.5 w-3.5" />
              <span>Sync Cloud</span>
            </button>
            <button
              type="button"
              onClick={() => onRegeneratePairingCode(selectedChild.id)}
              className="flex min-h-[42px] items-center gap-1.5 rounded-xl border border-slate-200 bg-white px-3.5 py-2 text-xs font-semibold text-slate-800 hover:bg-slate-50"
            >
              <RefreshCw className="h-3.5 w-3.5" />
              <span>Generate New Code</span>
            </button>
            <button
              type="button"
              onClick={onOpenLearnerPair}
              className="flex min-h-[42px] items-center gap-1.5 rounded-xl border border-emerald-200 bg-emerald-50 px-3.5 py-2 text-xs font-bold text-emerald-900 hover:bg-emerald-100"
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
                {ALL_LEARNING_AREAS.map((area) => {
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
