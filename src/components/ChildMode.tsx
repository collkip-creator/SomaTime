import { useMemo, useRef, useState } from 'react';
import {
  BatteryCharging,
  BookOpen,
  CheckCircle2,
  Clock,
  Gamepad2,
  GraduationCap,
  KeyRound,
  Lock,
  Play,
  ShieldCheck,
  Smartphone,
  Sparkles,
  Tv,
  Unlock,
  Video,
  Wifi,
} from 'lucide-react';
import {
  ALL_LEARNING_AREAS,
  ASSETS,
  CBC_DIRECTORY_CATALOG,
  CBC_LESSONS,
  getLevelGroupForGrade,
  getSeniorPathwayHint,
} from '../data/cbcCurriculum';
import { formatTimeClock } from '../hooks/useSomaTimeStore';
import type {
  CBCLesson,
  CBCRubric,
  ChildProfile,
  LearningArea,
  TeacherAssignment,
} from '../types/somatime';
import { LessonPlayer } from './LessonPlayer';

interface ChildModeProps {
  child: ChildProfile;
  childrenList: ChildProfile[];
  handOffActive: boolean;
  parentPin: string;
  assignments: TeacherAssignment[];
  onSelectChild: (id: string) => void;
  onSimulateTimeExpired: (id: string) => void;
  onOpenPinUnlock: () => void;
  onOpenExitHandOff: () => void;
  onOpenPairSetup: () => void;
  onSelectActiveApp: (id: string, appName: string | null) => void;
  onCompleteLesson: (
    childId: string,
    lesson: CBCLesson,
    scorePercent: number
  ) => {
    minutesEarned: number;
    rubric: CBCRubric;
    queuedForApproval: boolean;
    cappedOut: boolean;
  };
}

const ENTERTAINMENT_APPS = [
  { id: 'youtube', name: 'YouTube', icon: Video, bg: 'bg-rose-600' },
  { id: 'tiktok', name: 'TikTok', icon: Tv, bg: 'bg-slate-900' },
  { id: 'games', name: 'Safari Rush', icon: Gamepad2, bg: 'bg-amber-500' },
  { id: 'cartoons', name: 'Kids TV', icon: Play, bg: 'bg-blue-600' },
];

export function ChildMode({
  child,
  childrenList,
  handOffActive,
  parentPin,
  assignments,
  onSelectChild,
  onSimulateTimeExpired,
  onOpenPinUnlock,
  onOpenExitHandOff,
  onOpenPairSetup,
  onSelectActiveApp,
  onCompleteLesson,
}: ChildModeProps) {
  const [selectedGrade, setSelectedGrade] = useState<number>(child.grade);
  const [selectedArea, setSelectedArea] = useState<LearningArea | 'All'>('All');
  const [activeLesson, setActiveLesson] = useState<CBCLesson | null>(null);
  const [activeIsPriority, setActiveIsPriority] = useState(false);
  const [lockNotice, setLockNotice] = useState<string>('');
  const learningRef = useRef<HTMLDivElement | null>(null);

  const isPhoneLocked = child.isLocked || child.availableScreenSeconds <= 0;

  // Pending Teacher Assignments (Priority) for this child or school
  const priorityAssignments = useMemo(() => {
    return assignments.filter((a) => !a.completedByChildIds.includes(child.id));
  }, [assignments, child.id]);

  const hasPriorityPending = priorityAssignments.length > 0;

  // Check if child completed at least one Mandatory Focus Subject today
  const mandatoryDoneToday = useMemo(() => {
    if (child.mandatorySubjects.length === 0) return true;
    return child.activityLogs.some((log) =>
      child.mandatorySubjects.includes(log.learningArea)
    );
  }, [child.activityLogs, child.mandatorySubjects]);

  const filteredCatalog = useMemo(() => {
    return CBC_DIRECTORY_CATALOG.filter((item) => {
      const gradeMatch = item.grade === selectedGrade;
      const areaMatch = selectedArea === 'All' || item.learningArea === selectedArea;
      return gradeMatch && areaMatch;
    });
  }, [selectedGrade, selectedArea]);

  const featuredPlayableLessons = useMemo(() => {
    return CBC_LESSONS.filter((lesson) => {
      if (selectedArea !== 'All' && lesson.learningArea !== selectedArea) return false;
      return true;
    });
  }, [selectedArea]);

  const scrollToLearning = () => {
    learningRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  };

  const launchLesson = (lesson: CBCLesson, isPriority = false) => {
    // Enforce Teacher Priority Assignment first if another priority task exists
    if (hasPriorityPending && !isPriority) {
      const firstPriority = priorityAssignments[0];
      if (firstPriority.lessonId !== lesson.id) {
        setLockNotice(
          `Complete your Teacher Priority Assignment ("${firstPriority.title}") first before unlocking regular lessons.`
        );
        return;
      }
    }

    // Enforce Parent Mandatory Focus Subjects before free-choice areas
    if (
      !isPriority &&
      !mandatoryDoneToday &&
      child.mandatorySubjects.length > 0 &&
      !child.mandatorySubjects.includes(lesson.learningArea)
    ) {
      setLockNotice(
        `Mandatory Focus Rule: Complete a ${child.mandatorySubjects.join(
          ' or '
        )} activity first before free-choice ${lesson.learningArea}.`
      );
      return;
    }

    setLockNotice('');
    setActiveIsPriority(isPriority);
    setActiveLesson(lesson);
  };

  const handleLaunchCatalogEntry = (linkedLessonId: string) => {
    const found = CBC_LESSONS.find((l) => l.id === linkedLessonId) ?? CBC_LESSONS[0];
    launchLesson(found, false);
  };

  const handleTapEntertainmentApp = (appName: string) => {
    if (isPhoneLocked) {
      setLockNotice(`${appName} is locked. Complete a 5-min CBC activity or enter Parent PIN (${parentPin}).`);
      return;
    }
    onSelectActiveApp(child.id, appName);
  };

  const currentLevelGroup = getLevelGroupForGrade(selectedGrade);
  const seniorPathway = getSeniorPathwayHint(selectedGrade);

  return (
    <main className="mx-auto max-w-7xl px-4 py-6 sm:px-6 lg:py-8">
      {/* Top Child Context Bar */}
      <div className="flex flex-wrap items-center justify-between gap-4 rounded-3xl border border-emerald-200 bg-gradient-to-r from-emerald-50 via-white to-amber-50 p-4 sm:px-6">
        <div className="flex items-center gap-3.5">
          <div className="h-12 w-12 overflow-hidden rounded-2xl bg-emerald-100 shrink-0">
            <img
              src={child.avatarUrl}
              alt={child.name}
              referrerPolicy="no-referrer"
              className="h-full w-full object-cover"
            />
          </div>
          <div>
            <div className="flex flex-wrap items-center gap-2">
              <h1 className="font-display text-lg font-extrabold text-slate-900 sm:text-xl">
                Welcome back, {child.name.split(' ')[0]}!
              </h1>
              <span className="text-xs font-semibold text-emerald-800">
                Grade {child.grade} · {getLevelGroupForGrade(child.grade)}
              </span>
            </div>
            <p className="text-xs text-slate-600">
              {child.isPaired
                ? `Paired (${child.pairingCode}) · Background watch-minute guard active · Parent PIN ${parentPin} to interrupt`
                : handOffActive
                  ? `Hand-Off Mode active on Parent Phone · PIN ${parentPin} required to exit`
                  : `${child.school} · Not paired yet (Code: ${child.pairingCode})`}
            </p>
          </div>
        </div>

        <div className="flex flex-wrap items-center gap-2">
          {!child.isPaired && (
            <button
              type="button"
              onClick={onOpenPairSetup}
              className="flex min-h-[42px] items-center gap-1.5 rounded-xl bg-emerald-600 px-3.5 py-2 text-xs font-bold text-white hover:bg-emerald-700"
            >
              <span>Register as Learner (Enter Code)</span>
            </button>
          )}
          {!handOffActive && childrenList.length > 1 && (
            <select
              aria-label="Switch active child preview"
              value={child.id}
              onChange={(e) => {
                setActiveLesson(null);
                onSelectChild(e.target.value);
              }}
              className="min-h-[42px] rounded-xl border border-slate-200 bg-white px-3 py-1.5 text-xs font-semibold text-slate-800 focus:border-emerald-600 focus:outline-none"
            >
              {childrenList.map((c) => (
                <option key={c.id} value={c.id}>
                  Preview: {c.name} (G{c.grade})
                </option>
              ))}
            </select>
          )}
          {handOffActive && (
            <button
              type="button"
              onClick={onOpenExitHandOff}
              className="flex min-h-[42px] items-center gap-1.5 rounded-xl border border-slate-300 bg-white px-3.5 py-2 text-xs font-bold text-slate-800 hover:bg-slate-50"
            >
              <KeyRound className="h-3.5 w-3.5 text-teal-700" />
              <span>Parent Exit (PIN)</span>
            </button>
          )}
        </div>
      </div>

      {/* Main 2-Column Responsive Layout: Left = Android Device Simulation, Right = CBC Curriculum & Activity */}
      <div className="mt-6 grid grid-cols-1 gap-6 lg:grid-cols-12 lg:items-start">
        {/* LEFT: Android Material 3 Home Screen & Lock Overlay Simulation */}
        <section
          aria-label="Simulated Android Phone Home Screen"
          className="lg:col-span-5"
        >
          <div className="mx-auto max-w-[390px] rounded-[2.5rem] border-[6px] border-slate-900 bg-slate-900 p-2 shadow-2xl">
            <div className="relative min-h-[600px] overflow-hidden rounded-[2rem] bg-gradient-to-b from-blue-900 via-emerald-800 to-amber-700">
              {/* Wallpaper Image with Fallback Gradient */}
              <img
                src={ASSETS.wallpaper}
                alt="Kenyan highlands Material 3 wallpaper"
                referrerPolicy="no-referrer"
                className="absolute inset-0 h-full w-full object-cover"
              />
              <div className="absolute inset-0 bg-gradient-to-t from-slate-950/80 via-slate-950/35 to-slate-950/55" />

              {/* Android Status Bar */}
              <div className="relative z-10 flex items-center justify-between px-5 pt-3 text-xs font-semibold text-white">
                <span className="font-mono tabular-nums">10:24</span>
                <div className="h-4 w-20 rounded-full bg-slate-950/80" aria-hidden="true" />
                <div className="flex items-center gap-1.5">
                  <Wifi className="h-3.5 w-3.5" />
                  <BatteryCharging className="h-4 w-4" />
                </div>
              </div>

              {/* Phone Screen Body */}
              <div className="relative z-10 flex min-h-[555px] flex-col justify-between p-5">
                {/* Screen Time Widget */}
                <div className="mt-2 rounded-3xl border border-white/20 bg-slate-950/55 p-4 text-white backdrop-blur-md">
                  <div className="flex items-center justify-between">
                    <span className="text-xs font-semibold text-emerald-300">
                      SomaTime Screen Timer
                    </span>
                    <span className="flex items-center gap-1 text-xs font-semibold">
                      {isPhoneLocked ? (
                        <>
                          <Lock className="h-3.5 w-3.5 text-amber-400" />
                          <span className="text-amber-300">Locked</span>
                        </>
                      ) : (
                        <>
                          <Unlock className="h-3.5 w-3.5 text-emerald-400" />
                          <span className="text-emerald-300">Unlocked</span>
                        </>
                      )}
                    </span>
                  </div>

                  <div className="mt-2 flex items-baseline justify-between">
                    <p className="font-mono text-4xl font-bold tracking-tight text-white tabular-nums">
                      {formatTimeClock(child.availableScreenSeconds)}
                    </p>
                    <p className="text-xs text-slate-200 tabular-nums">
                      {child.usedScreenTodayMins}/{child.dailyCapMins}m daily cap
                    </p>
                  </div>

                  {!isPhoneLocked && child.activeAppName && (
                    <p className="mt-2 text-xs text-amber-200">
                      Running: <strong>{child.activeAppName}</strong> · Timer counting down
                    </p>
                  )}

                  {!isPhoneLocked && (
                    <button
                      type="button"
                      onClick={() => onSimulateTimeExpired(child.id)}
                      className="mt-3 flex min-h-[40px] w-full items-center justify-center gap-1.5 rounded-2xl bg-amber-400 px-3 py-2 text-xs font-extrabold text-slate-950 transition-colors hover:bg-amber-300"
                    >
                      <Clock className="h-3.5 w-3.5" />
                      <span>Simulate Time Expired (00:00)</span>
                    </button>
                  )}
                </div>

                {/* Android App Grid */}
                <div className="my-4">
                  <p className="mb-3 text-xs font-semibold text-white/85">
                    Entertainment Apps {isPhoneLocked ? '(Intercepted by SomaTime)' : '(Tap to Use)'}
                  </p>
                  <div className="grid grid-cols-4 gap-3">
                    {ENTERTAINMENT_APPS.map((app) => {
                      const Icon = app.icon;
                      const isActiveApp = !isPhoneLocked && child.activeAppName === app.name;
                      return (
                        <button
                          key={app.id}
                          type="button"
                          onClick={() => handleTapEntertainmentApp(app.name)}
                          className="group flex flex-col items-center gap-1.5 text-center"
                        >
                          <div
                            className={`relative flex h-14 w-14 items-center justify-center rounded-2xl ${app.bg} text-white shadow-lg transition-transform group-active:scale-95 ${
                              isPhoneLocked ? 'opacity-65 grayscale-[30%]' : ''
                            } ${isActiveApp ? 'ring-2 ring-amber-300' : ''}`}
                          >
                            <Icon className="h-6 w-6" />
                            {isPhoneLocked && (
                              <span className="absolute -right-1 -top-1 flex h-6 w-6 items-center justify-center rounded-full bg-amber-400 text-slate-950 shadow">
                                <Lock className="h-3.5 w-3.5" />
                              </span>
                            )}
                          </div>
                          <span className="w-full truncate text-[11px] font-semibold text-white">
                            {app.name}
                          </span>
                        </button>
                      );
                    })}
                  </div>
                </div>

                {/* Dock */}
                <div className="flex items-center justify-between rounded-3xl border border-white/15 bg-slate-950/60 px-4 py-3 backdrop-blur-md">
                  <button
                    type="button"
                    onClick={scrollToLearning}
                    className="flex items-center gap-2.5 text-left"
                  >
                    <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-emerald-500 text-slate-950">
                      <BookOpen className="h-5 w-5" />
                    </div>
                    <div>
                      <p className="text-xs font-bold text-white">SomaTime CBC</p>
                      <p className="text-[11px] text-emerald-300">Always Unlocked</p>
                    </div>
                  </button>
                  <button
                    type="button"
                    onClick={onOpenPinUnlock}
                    className="min-h-[38px] rounded-xl border border-white/25 px-3 py-1.5 text-[11px] font-semibold text-white hover:bg-white/10"
                  >
                    Parent PIN
                  </button>
                </div>
              </div>

              {/* FRIENDLY GAMIFIED SOMATIME LOCK INTERCEPTION OVERLAY */}
              {isPhoneLocked && (
                <div
                  role="region"
                  aria-label="SomaTime Lock Screen Overlay"
                  className="absolute inset-0 z-20 flex flex-col justify-between bg-gradient-to-b from-blue-950/95 via-emerald-950/95 to-slate-950/95 p-5 text-white backdrop-blur-md"
                >
                  <div className="flex items-center justify-between pt-3 text-xs text-emerald-200">
                    <span className="flex items-center gap-1.5 font-semibold">
                      <ShieldCheck className="h-4 w-4 text-emerald-400" />
                      SomaTime Guard Active
                    </span>
                    <span className="font-mono font-bold text-amber-300 tabular-nums">00:00</span>
                  </div>

                  <div className="my-auto text-center">
                    <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-3xl bg-amber-400 text-slate-950 shadow-lg">
                      <Lock className="h-8 w-8" />
                    </div>
                    <h2 className="mt-4 font-display text-2xl font-extrabold text-white">
                      Screen Time Expired!
                    </h2>
                    <p className="mt-1.5 text-sm text-emerald-100">
                      Screen time is paused at <span className="font-mono font-bold">00:00</span>.
                      Complete one 5-minute Kenyan CBC activity to unlock{' '}
                      <strong className="text-amber-300">+{child.exchangeRateMins} mins</strong>.
                    </p>

                    {hasPriorityPending && (
                      <div className="mt-4 rounded-2xl border border-amber-400/50 bg-amber-400/15 p-3 text-left text-xs text-amber-100">
                        <p className="font-bold text-amber-300">
                          Teacher Assignment (Priority)
                        </p>
                        <p className="mt-0.5">{priorityAssignments[0].title}</p>
                      </div>
                    )}

                    <div className="mt-5 space-y-2.5">
                      <button
                        type="button"
                        onClick={() => {
                          if (hasPriorityPending) {
                            const assign = priorityAssignments[0];
                            const lesson =
                              CBC_LESSONS.find((l) => l.id === assign.lessonId) ?? CBC_LESSONS[0];
                            launchLesson(lesson, true);
                          } else {
                            const suggested =
                              CBC_LESSONS.find((l) => l.grade === child.grade) ?? CBC_LESSONS[0];
                            launchLesson(suggested, false);
                          }
                          scrollToLearning();
                        }}
                        className="flex min-h-[48px] w-full items-center justify-center gap-2 rounded-2xl bg-emerald-500 px-4 py-3 text-sm font-extrabold text-slate-950 transition-colors hover:bg-emerald-400"
                      >
                        <Sparkles className="h-4 w-4" />
                        <span>
                          {hasPriorityPending
                            ? 'Start Teacher Assignment (+15m)'
                            : `Start CBC Lesson (+${child.exchangeRateMins}m)`}
                        </span>
                      </button>

                      <button
                        type="button"
                        onClick={onOpenPinUnlock}
                        className="flex min-h-[44px] w-full items-center justify-center gap-2 rounded-2xl border border-white/25 bg-white/10 px-4 py-2.5 text-xs font-semibold text-white transition-colors hover:bg-white/20"
                      >
                        <KeyRound className="h-3.5 w-3.5" />
                        <span>Unlock with Parent PIN (1234)</span>
                      </button>
                    </div>
                  </div>

                  <p className="text-center text-[11px] text-slate-300">
                    Entertainment apps stay locked until learning or Parent PIN
                  </p>
                </div>
              )}
            </div>
          </div>
        </section>

        {/* RIGHT: Teacher Priority Banner + CBC Curriculum Browser / Active Lesson Player */}
        <section
          ref={learningRef}
          aria-label="Kenyan CBC learning zone"
          className="space-y-5 lg:col-span-7"
        >
          {/* Teacher Priority Notification Banner */}
          {hasPriorityPending && !activeLesson && (
            <div className="rounded-3xl border border-amber-300 bg-amber-50 p-5">
              <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
                <div className="flex items-start gap-3">
                  <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-2xl bg-amber-400 text-slate-950">
                    <GraduationCap className="h-5 w-5" />
                  </div>
                  <div>
                    <p className="text-xs font-bold text-amber-900">
                      Teacher Assignment (Priority) · Must Complete First
                    </p>
                    <h2 className="mt-0.5 font-display text-base font-bold text-slate-900">
                      {priorityAssignments[0].title}
                    </h2>
                    <p className="mt-0.5 text-xs text-slate-700">
                      {priorityAssignments[0].classStream} · {priorityAssignments[0].learningArea} ·{' '}
                      {priorityAssignments[0].dueLabel}
                    </p>
                  </div>
                </div>
                <button
                  type="button"
                  onClick={() => {
                    const target =
                      CBC_LESSONS.find((l) => l.id === priorityAssignments[0].lessonId) ??
                      CBC_LESSONS[0];
                    launchLesson(target, true);
                  }}
                  className="flex min-h-[46px] shrink-0 items-center justify-center gap-2 rounded-2xl bg-slate-900 px-4 py-2.5 text-xs font-bold text-white hover:bg-slate-800 whitespace-nowrap"
                >
                  <Play className="h-3.5 w-3.5" />
                  <span>Start Priority Task</span>
                </button>
              </div>
            </div>
          )}

          {/* Policy / Rule Feedback Alert */}
          {lockNotice && (
            <div
              role="alert"
              className="rounded-2xl border border-blue-200 bg-blue-50 px-4 py-3 text-xs font-semibold text-blue-950"
            >
              {lockNotice}
            </div>
          )}

          {activeLesson ? (
            <LessonPlayer
              lesson={activeLesson}
              exchangeRateMins={child.exchangeRateMins}
              requireParentApproval={child.requireParentApproval}
              isTeacherPriority={activeIsPriority}
              onComplete={(score) => onCompleteLesson(child.id, activeLesson, score)}
              onExit={() => setActiveLesson(null)}
            />
          ) : (
            <div className="space-y-5">
              {/* Hero Learning Banner + Mandatory Focus Strip */}
              <div className="overflow-hidden rounded-3xl border border-slate-200 bg-white">
                <div className="relative h-36 w-full overflow-hidden bg-emerald-900 sm:h-40">
                  <img
                    src={ASSETS.learningBanner}
                    alt="Kenyan CBC Mathematics and Science learning items"
                    referrerPolicy="no-referrer"
                    className="h-full w-full object-cover"
                  />
                  <div className="absolute inset-0 bg-gradient-to-r from-slate-950/85 via-slate-950/55 to-transparent" />
                  <div className="absolute inset-0 flex flex-col justify-center p-5 sm:p-6">
                    <p className="text-xs font-semibold text-amber-300">
                      Kenyan CBC Micro-Lessons · Grades 1–12
                    </p>
                    <h2 className="mt-1 max-w-md font-display text-xl font-extrabold text-white sm:text-2xl">
                      Learn for 5 Minutes, Unlock Screen Time
                    </h2>
                  </div>
                </div>

                {/* Mandatory Focus Subject Bar */}
                {child.mandatorySubjects.length > 0 && (
                  <div className="flex flex-wrap items-center justify-between gap-2 border-t border-slate-100 bg-slate-50 px-5 py-3 text-xs">
                    <div className="flex flex-wrap items-center gap-2 text-slate-700">
                      <span className="font-bold text-slate-900">Mandatory Focus Today:</span>
                      <span>{child.mandatorySubjects.join(' · ')}</span>
                    </div>
                    <span
                      className={`inline-flex items-center gap-1 font-semibold ${
                        mandatoryDoneToday ? 'text-emerald-700' : 'text-amber-700'
                      }`}
                    >
                      {mandatoryDoneToday ? (
                        <>
                          <CheckCircle2 className="h-3.5 w-3.5" />
                          Free-Choice Subjects Unlocked
                        </>
                      ) : (
                        <>
                          <Lock className="h-3.5 w-3.5" />
                          Complete 1 mandatory subject first
                        </>
                      )}
                    </span>
                  </div>
                )}
              </div>

              {/* Featured Playable 5-Min Activities */}
              <div className="rounded-3xl border border-slate-200 bg-white p-5 sm:p-6">
                <div className="flex items-center justify-between gap-4">
                  <div>
                    <h3 className="font-display text-lg font-bold text-slate-900">
                      Interactive 5-Minute Activities
                    </h3>
                    <p className="text-xs text-slate-600">
                      Playable Kenyan CBC quizzes with guided study hints
                    </p>
                  </div>
                  <span className="font-mono text-xs font-bold text-emerald-700 tabular-nums">
                    +{child.exchangeRateMins}m / activity
                  </span>
                </div>

                <div className="mt-4 grid grid-cols-1 gap-3 sm:grid-cols-2">
                  {featuredPlayableLessons.map((lesson) => {
                    const completed = child.completedLessonIds.includes(lesson.id);
                    const restrictedByMandatory =
                      !mandatoryDoneToday &&
                      child.mandatorySubjects.length > 0 &&
                      !child.mandatorySubjects.includes(lesson.learningArea);

                    return (
                      <div
                        key={lesson.id}
                        className="flex flex-col justify-between rounded-2xl border border-slate-200 bg-slate-50/70 p-4"
                      >
                        <div>
                          <div className="flex items-center justify-between gap-2 text-xs text-slate-600">
                            <span className="font-semibold text-blue-800">
                              Grade {lesson.grade} · {lesson.learningArea}
                            </span>
                            {completed && (
                              <span className="inline-flex items-center gap-1 font-semibold text-emerald-700">
                                <CheckCircle2 className="h-3.5 w-3.5" /> Done
                              </span>
                            )}
                          </div>
                          <h4 className="mt-1.5 font-display text-base font-bold text-slate-900">
                            {lesson.title}
                          </h4>
                          <p className="mt-0.5 text-xs text-slate-600">
                            {lesson.strand} · {lesson.subStrand}
                          </p>
                        </div>

                        <button
                          type="button"
                          onClick={() => launchLesson(lesson, false)}
                          className={`mt-4 flex min-h-[44px] w-full items-center justify-center gap-2 rounded-xl px-4 py-2 text-xs font-bold transition-colors ${
                            restrictedByMandatory
                              ? 'border border-slate-300 bg-white text-slate-600 hover:bg-slate-100'
                              : 'bg-blue-700 text-white hover:bg-blue-800'
                          }`}
                        >
                          {restrictedByMandatory ? (
                            <>
                              <Lock className="h-3.5 w-3.5" />
                              <span>Focus Subjects Required First</span>
                            </>
                          ) : (
                            <>
                              <Play className="h-3.5 w-3.5" />
                              <span>Play 5-Min Activity (+{child.exchangeRateMins}m)</span>
                            </>
                          )}
                        </button>
                      </div>
                    );
                  })}
                </div>
              </div>

              {/* Full Grade 1–12 CBC Curriculum Browser */}
              <div className="rounded-3xl border border-slate-200 bg-white p-5 sm:p-6">
                <div className="flex flex-wrap items-center justify-between gap-2">
                  <div>
                    <h3 className="font-display text-lg font-bold text-slate-900">
                      Kenyan CBC Curriculum Browser (Grades 1–12)
                    </h3>
                    <p className="text-xs text-slate-600">
                      {currentLevelGroup}
                      {seniorPathway ? ` · ${seniorPathway}` : ''} · Browse Strands &amp; Sub-strands
                    </p>
                  </div>
                  <Smartphone className="h-4 w-4 text-slate-400" />
                </div>

                {/* Grade 1–12 Selector */}
                <div className="mt-4 flex gap-1.5 overflow-x-auto pb-1">
                  {Array.from({ length: 12 }, (_, idx) => idx + 1).map((g) => {
                    const active = selectedGrade === g;
                    return (
                      <button
                        key={g}
                        type="button"
                        onClick={() => setSelectedGrade(g)}
                        className={`min-h-[40px] shrink-0 rounded-xl border px-3 py-1.5 font-mono text-xs font-bold transition-colors ${
                          active
                            ? 'border-emerald-700 bg-emerald-700 text-white'
                            : 'border-slate-200 bg-slate-50 text-slate-700 hover:bg-slate-100'
                        }`}
                      >
                        G{g}
                      </button>
                    );
                  })}
                </div>

                {/* Learning Area Filter */}
                <div className="mt-3 flex flex-wrap gap-1.5">
                  <button
                    type="button"
                    onClick={() => setSelectedArea('All')}
                    className={`min-h-[36px] rounded-xl border px-3 py-1 text-xs font-semibold transition-colors ${
                      selectedArea === 'All'
                        ? 'border-blue-700 bg-blue-50 text-blue-900'
                        : 'border-slate-200 bg-white text-slate-600 hover:bg-slate-50'
                    }`}
                  >
                    All Learning Areas
                  </button>
                  {ALL_LEARNING_AREAS.map((area) => (
                    <button
                      key={area}
                      type="button"
                      onClick={() => setSelectedArea(area)}
                      className={`min-h-[36px] rounded-xl border px-3 py-1 text-xs font-semibold transition-colors ${
                        selectedArea === area
                          ? 'border-blue-700 bg-blue-50 text-blue-900'
                          : 'border-slate-200 bg-white text-slate-600 hover:bg-slate-50'
                      }`}
                    >
                      {area}
                    </button>
                  ))}
                </div>

                {/* Strands & Sub-strands List */}
                <div className="mt-4 divide-y divide-slate-100">
                  {filteredCatalog.length === 0 ? (
                    <p className="py-4 text-xs text-slate-500">
                      No strands match this filter in Grade {selectedGrade}. Select &ldquo;All Learning
                      Areas&rdquo; to view all strands for {currentLevelGroup}.
                    </p>
                  ) : (
                    filteredCatalog.map((entry, idx) => (
                      <div
                        key={`${entry.grade}-${entry.learningArea}-${idx}`}
                        className="flex flex-col gap-3 py-3.5 sm:flex-row sm:items-center sm:justify-between"
                      >
                        <div>
                          <p className="text-xs font-semibold text-emerald-800">
                            {entry.learningArea} · {entry.levelGroup}
                            {entry.pathway ? ` · ${entry.pathway}` : ''}
                          </p>
                          <p className="mt-0.5 text-sm font-bold text-slate-900">
                            Strand: {entry.strand}
                          </p>
                          <p className="text-xs text-slate-600">Sub-strand: {entry.subStrand}</p>
                        </div>
                        <button
                          type="button"
                          onClick={() => handleLaunchCatalogEntry(entry.linkedLessonId)}
                          className="flex min-h-[42px] shrink-0 items-center justify-center gap-1.5 rounded-xl border border-blue-200 bg-blue-50 px-3.5 py-2 text-xs font-bold text-blue-900 transition-colors hover:bg-blue-100 whitespace-nowrap"
                        >
                          <Play className="h-3.5 w-3.5" />
                          <span>Practice Strand</span>
                        </button>
                      </div>
                    ))
                  )}
                </div>
              </div>
            </div>
          )}
        </section>
      </div>
    </main>
  );
}
