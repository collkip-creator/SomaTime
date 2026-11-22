import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
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
  Search,
  ShieldCheck,
  Sparkles,
  Tv,
  Unlock,
  Video,
  Wifi,
} from 'lucide-react';
import {
  ASSETS,
  CBC_DIRECTORY_CATALOG,
  CBC_LESSONS,
  buildLessonFromTeacherAssignment,
  getCatalogTrackingKey,
  getLevelGroupForGrade,
  getQuestionCountForGrade,
  getSeniorPathwayHint,
  getSubjectsForGrade,
  pickNextUnseenAssignment,
  scaleLessonQuestionsForGrade,
} from '../data/cbcCurriculum';
import { formatTimeClock } from '../hooks/useSomaTimeStore';
import type {
  CBCLesson,
  CBCRubric,
  CbcCatalogEntry,
  ChildProfile,
  LearningArea,
  TeacherAssignment,
} from '../types/somatime';
import { LessonPlayer } from './LessonPlayer';

interface ChildModeProps {
  child: ChildProfile;
  childrenList: ChildProfile[];
  handOffActive: boolean;
  assignments: TeacherAssignment[];
  displayedKeys: string[];
  displayedQuizIds: string[];
  onSelectChild: (id: string) => void;
  onSimulateTimeExpired: (id: string) => void;
  onOpenPinUnlock: () => void;
  onOpenExitHandOff: () => void;
  onOpenPairSetup: () => void;
  onSelectActiveApp: (id: string, appName: string | null) => void;
  onRecordDisplayedAssignment: (
    childId: string,
    trackingKey: string,
    quizId: string,
    resetCatCycle?: boolean
  ) => void;
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
  assignments,
  displayedKeys,
  displayedQuizIds,
  onSelectChild,
  onSimulateTimeExpired,
  onOpenPinUnlock,
  onOpenExitHandOff,
  onOpenPairSetup,
  onSelectActiveApp,
  onRecordDisplayedAssignment,
  onCompleteLesson,
}: ChildModeProps) {
  const [selectedArea, setSelectedArea] = useState<LearningArea | 'All'>('All');
  const [curriculumSearch, setCurriculumSearch] = useState('');
  const [activeLesson, setActiveLesson] = useState<CBCLesson | null>(null);
  const [activeIsPriority, setActiveIsPriority] = useState(false);
  const [lockNotice, setLockNotice] = useState<string>('');
  const [resumedBanner, setResumedBanner] = useState<string>('');
  const learningRef = useRef<HTMLDivElement | null>(null);
  const prevChildIdRef = useRef<string>(child.id);
  const wasLockedRef = useRef<boolean>(false);

  const learnerGrade = child.grade;
  const gradeQuestionCount = getQuestionCountForGrade(learnerGrade);
  const isPhoneLocked = child.isLocked || child.availableScreenSeconds <= 0;

  const gradeSubjects = useMemo(() => getSubjectsForGrade(learnerGrade), [learnerGrade]);
  const effectiveSelectedArea =
    selectedArea === 'All' || gradeSubjects.includes(selectedArea) ? selectedArea : 'All';

  // Pending Teacher Assignments (Priority) matching this learner's grade
  const priorityAssignments = useMemo(() => {
    return assignments.filter(
      (a) => a.grade === learnerGrade && !a.completedByChildIds.includes(child.id)
    );
  }, [assignments, child.id, learnerGrade]);

  const hasPriorityPending = priorityAssignments.length > 0;

  const gradePlayableLessons = useMemo(() => {
    return CBC_LESSONS.filter((lesson) => lesson.grade === learnerGrade);
  }, [learnerGrade]);

  // Check if child completed at least one Mandatory Focus Subject today (when available for their grade)
  const hasMandatoryLessonForGrade = useMemo(() => {
    if (child.mandatorySubjects.length === 0) return false;
    return gradePlayableLessons.some((l) => child.mandatorySubjects.includes(l.learningArea));
  }, [child.mandatorySubjects, gradePlayableLessons]);

  const mandatoryDoneToday = useMemo(() => {
    if (!hasMandatoryLessonForGrade) return true;
    return child.activityLogs.some((log) =>
      child.mandatorySubjects.includes(log.learningArea)
    );
  }, [child.activityLogs, child.mandatorySubjects, hasMandatoryLessonForGrade]);

  const gradeCatalogTotal = useMemo(() => {
    return CBC_DIRECTORY_CATALOG.filter((item) => item.grade === learnerGrade);
  }, [learnerGrade]);

  const filteredCatalog = useMemo(() => {
    const q = curriculumSearch.trim().toLowerCase();
    return gradeCatalogTotal.filter((item) => {
      const areaMatch = effectiveSelectedArea === 'All' || item.learningArea === effectiveSelectedArea;
      const queryMatch =
        !q ||
        item.learningArea.toLowerCase().includes(q) ||
        item.strand.toLowerCase().includes(q) ||
        item.subStrand.toLowerCase().includes(q);
      return areaMatch && queryMatch;
    });
  }, [gradeCatalogTotal, effectiveSelectedArea, curriculumSearch]);

  const featuredPlayableLessons = useMemo(() => {
    return gradePlayableLessons.filter((lesson) => {
      if (effectiveSelectedArea !== 'All' && lesson.learningArea !== effectiveSelectedArea) {
        return false;
      }
      return true;
    });
  }, [gradePlayableLessons, effectiveSelectedArea]);

  const scrollToLearning = useCallback(() => {
    learningRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }, []);

  const launchLesson = useCallback(
    (
      lesson: CBCLesson,
      isPriority = false,
      trackingMeta?: { key: string; quizId: string; resetCatCycle?: boolean }
    ) => {
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
      setResumedBanner('');
      if (trackingMeta) {
        onRecordDisplayedAssignment(
          child.id,
          trackingMeta.key,
          trackingMeta.quizId,
          Boolean(trackingMeta.resetCatCycle)
        );
      } else {
        onRecordDisplayedAssignment(
          child.id,
          getCatalogTrackingKey(lesson.grade, lesson.learningArea, lesson.strand, lesson.subStrand),
          lesson.id,
          false
        );
      }
      const finalLesson = lesson.isCustom
        ? lesson
        : scaleLessonQuestionsForGrade(lesson, learnerGrade);
      setActiveIsPriority(isPriority);
      setActiveLesson(finalLesson);
    },
    [
      child.id,
      child.mandatorySubjects,
      hasPriorityPending,
      learnerGrade,
      mandatoryDoneToday,
      onRecordDisplayedAssignment,
      priorityAssignments,
    ]
  );

  // Immediately auto-launch a random unseen assignment whenever the app locks / pins
  useEffect(() => {
    if (prevChildIdRef.current !== child.id) {
      prevChildIdRef.current = child.id;
      wasLockedRef.current = false;
      setActiveLesson(null);
      return;
    }

    if (isPhoneLocked && activeLesson === null) {
      const picked = pickNextUnseenAssignment({
        child,
        pendingAssignments: priorityAssignments,
        displayedKeys,
        displayedQuizIds,
      });
      onRecordDisplayedAssignment(
        child.id,
        picked.trackingKey,
        picked.quizId,
        picked.wrappedCycle
      );
      setLockNotice('');
      setResumedBanner('');
      setActiveIsPriority(picked.isPriority);
      setActiveLesson(picked.lesson);
      if (!wasLockedRef.current) {
        wasLockedRef.current = true;
      }
    } else if (!isPhoneLocked) {
      wasLockedRef.current = false;
    }
  }, [
    activeLesson,
    child,
    displayedKeys,
    displayedQuizIds,
    isPhoneLocked,
    onRecordDisplayedAssignment,
    priorityAssignments,
  ]);

  const handleLaunchCatalogEntry = (entry: CbcCatalogEntry) => {
    const base =
      CBC_LESSONS.find((l) => l.id === entry.linkedLessonId) ??
      gradePlayableLessons.find((l) => l.learningArea === entry.learningArea) ??
      CBC_LESSONS.find((l) => l.learningArea === entry.learningArea) ??
      gradePlayableLessons[0] ??
      CBC_LESSONS[0];

    const contextualLesson: CBCLesson =
      base.learningArea === entry.learningArea &&
      base.strand === entry.strand &&
      base.subStrand === entry.subStrand
        ? base
        : {
            ...base,
            title: `${entry.subStrand} Practice`,
            swahiliTitle: `${entry.subStrand} Practice`,
            grade: learnerGrade,
            levelGroup: entry.levelGroup,
            learningArea: entry.learningArea,
            strand: entry.strand,
            subStrand: entry.subStrand,
          };

    launchLesson(contextualLesson, false, {
      key: getCatalogTrackingKey(learnerGrade, entry.learningArea, entry.strand, entry.subStrand),
      quizId: base.id,
    });
  };

  const handleTapEntertainmentApp = (appName: string) => {
    if (isPhoneLocked) {
      setLockNotice(`${appName} is locked. Complete a 5-min CBC activity or ask a parent to enter their PIN.`);
      return;
    }
    onSelectActiveApp(child.id, appName);
  };

  const currentLevelGroup = getLevelGroupForGrade(learnerGrade);
  const seniorPathway = getSeniorPathwayHint(learnerGrade);

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
              {handOffActive
                ? 'Shared-Phone Child Lock active · Enter Parent PIN to return to Parent or Teacher mode'
                : child.isPaired && child.deviceMode === 'own_phone'
                  ? `Paired (${child.pairingCode}) · Background watch-minute guard active · Parent PIN required to interrupt`
                  : child.deviceMode === 'hand_off'
                    ? `${child.school} · Shared Phone Ready (Unrestricted until Hand-Off Lock activated)`
                    : `${child.school} · Not paired yet (Code: ${child.pairingCode})`}
            </p>
          </div>
        </div>

        <div className="flex flex-wrap items-center gap-2">
          {!handOffActive && !child.isPaired && child.deviceMode !== 'hand_off' && (
            <button
              type="button"
              onClick={onOpenPairSetup}
              className="flex min-h-[42px] items-center gap-1.5 rounded-xl bg-emerald-600 px-3.5 py-2 text-xs font-bold text-white hover:bg-emerald-700"
            >
              <span>Register as Learner (Enter Code)</span>
            </button>
          )}
          {!handOffActive &&
            !(child.isPaired && child.deviceMode === 'own_phone') &&
            childrenList.length > 1 && (
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
          {(handOffActive || (child.isPaired && child.deviceMode === 'own_phone')) && (
            <button
              type="button"
              onClick={onOpenExitHandOff}
              className="flex min-h-[42px] items-center gap-1.5 rounded-xl border border-slate-300 bg-white px-3.5 py-2 text-xs font-bold text-slate-800 hover:bg-slate-50"
            >
              <KeyRound className="h-3.5 w-3.5 text-teal-700" />
              <span>Parent / Teacher Exit (PIN)</span>
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

                  {resumedBanner && !isPhoneLocked && (
                    <div className="mt-2 rounded-xl border border-emerald-400/50 bg-emerald-500/20 px-3 py-1.5 text-[11px] font-semibold text-emerald-100">
                      {resumedBanner}
                    </div>
                  )}

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
                      App Pinned · Guard Active
                    </span>
                    <span className="font-mono font-bold text-amber-300 tabular-nums">00:00</span>
                  </div>

                  <div className="my-auto text-center">
                    <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-3xl bg-amber-400 text-slate-950 shadow-lg">
                      <Lock className="h-8 w-8" />
                    </div>
                    <h2 className="mt-4 font-display text-2xl font-extrabold text-white">
                      {activeIsPriority ? 'Priority Assignment Pinned' : 'Assignment Auto-Loaded'}
                    </h2>
                    <p className="mt-1.5 text-sm text-emerald-100">
                      {child.lastInterruptedAppName ? (
                        <>
                          Paused <strong className="text-amber-300">{child.lastInterruptedAppName}</strong>{' '}
                          at <span className="font-mono font-bold">00:00</span>. Complete the live quiz to
                          unlock <strong className="text-amber-300">+{child.exchangeRateMins} mins</strong>{' '}
                          and auto-resume.
                        </>
                      ) : (
                        <>
                          Screen time paused at <span className="font-mono font-bold">00:00</span>.
                          Complete the live quiz to unlock{' '}
                          <strong className="text-amber-300">+{child.exchangeRateMins} mins</strong>.
                        </>
                      )}
                    </p>

                    {activeLesson && (
                      <div className="mt-4 rounded-2xl border border-emerald-400/50 bg-emerald-400/15 p-3 text-left text-xs text-emerald-100">
                        <p className="font-bold text-emerald-300">
                          {activeIsPriority
                            ? 'Teacher Priority Assignment · Live'
                            : `${activeLesson.learningArea} · ${activeLesson.strand}`}
                        </p>
                        <p className="mt-0.5 font-semibold text-white">{activeLesson.title}</p>
                      </div>
                    )}

                    <div className="mt-5 space-y-2.5">
                      <button
                        type="button"
                        onClick={scrollToLearning}
                        className="flex min-h-[48px] w-full items-center justify-center gap-2 rounded-2xl bg-emerald-500 px-4 py-3 text-sm font-extrabold text-slate-950 transition-colors hover:bg-emerald-400"
                      >
                        <Sparkles className="h-4 w-4" />
                        <span>Answer Pinned Assignment (+{child.exchangeRateMins}m)</span>
                      </button>

                      <button
                        type="button"
                        onClick={onOpenPinUnlock}
                        className="flex min-h-[44px] w-full items-center justify-center gap-2 rounded-2xl border border-white/25 bg-white/10 px-4 py-2.5 text-xs font-semibold text-white transition-colors hover:bg-white/20"
                      >
                        <KeyRound className="h-3.5 w-3.5" />
                        <span>Unlock with Parent PIN</span>
                      </button>
                    </div>
                  </div>

                  <p className="text-center text-[11px] text-slate-300">
                    Unpins &amp; resumes {child.lastInterruptedAppName || 'apps'} automatically on completion
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
          {/* Auto-Minimized & Resumed Banner */}
          {resumedBanner && (
            <div
              role="status"
              className="rounded-2xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-xs font-semibold text-emerald-950"
            >
              {resumedBanner}
            </div>
          )}

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
                    const assign = priorityAssignments[0];
                    const built = buildLessonFromTeacherAssignment(assign, learnerGrade);
                    launchLesson(built, true, {
                      key: `assign:${assign.id}`,
                      quizId: assign.lessonId,
                    });
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
              key={`${activeLesson.id}-${activeLesson.strand}-${activeLesson.subStrand}`}
              lesson={activeLesson}
              exchangeRateMins={child.exchangeRateMins}
              requireParentApproval={child.requireParentApproval}
              isTeacherPriority={activeIsPriority}
              isPinnedLock={isPhoneLocked}
              resumedAppName={child.lastInterruptedAppName || child.activeAppName || 'YouTube'}
              onOpenPinUnlock={onOpenPinUnlock}
              onOpenExitHandOff={
                handOffActive || (child.isPaired && child.deviceMode === 'own_phone')
                  ? onOpenExitHandOff
                  : undefined
              }
              onAutoMinimized={(appName) => {
                setResumedBanner(
                  `SomaTime minimized · ${appName || 'YouTube'} resumed where it was interrupted (+${child.exchangeRateMins} mins unlocked).`
                );
              }}
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
                      Kenyan CBC Micro-Lessons · Grade {learnerGrade} ({currentLevelGroup})
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
                      Grade {learnerGrade} Interactive Activities ({gradeQuestionCount} Questions)
                    </h3>
                    <p className="text-xs text-slate-600">
                      Showing only Grade {learnerGrade} Kenyan CBC quizzes · {gradeQuestionCount}{' '}
                      questions per session (scales from 3 in Grade 1)
                    </p>
                  </div>
                  <span className="font-mono text-xs font-bold text-emerald-700 tabular-nums">
                    +{child.exchangeRateMins}m / session
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

              {/* Grade-Locked CBC Curriculum Browser */}
              <div className="rounded-3xl border border-slate-200 bg-white p-5 sm:p-6">
                <div className="flex flex-wrap items-center justify-between gap-3">
                  <div>
                    <h3 className="font-display text-lg font-bold text-slate-900">
                      Grade {learnerGrade} CBC Subjects, Strands &amp; Sub-strands
                    </h3>
                    <p className="text-xs text-slate-600">
                      {currentLevelGroup}
                      {seniorPathway ? ` · ${seniorPathway}` : ''} · {gradeSubjects.length}{' '}
                      subjects · {gradeCatalogTotal.length} sub-strands loaded
                    </p>
                  </div>
                  <div className="relative w-full sm:w-56">
                    <Search className="pointer-events-none absolute left-3 top-1/2 h-3.5 w-3.5 -translate-y-1/2 text-slate-400" />
                    <input
                      type="text"
                      value={curriculumSearch}
                      onChange={(e) => setCurriculumSearch(e.target.value)}
                      placeholder="Search strand or topic..."
                      className="min-h-[38px] w-full rounded-xl border border-slate-200 bg-slate-50 pl-8 pr-3 text-xs text-slate-900 focus:border-blue-600 focus:bg-white focus:outline-none"
                    />
                  </div>
                </div>

                {/* Grade-Scoped Learning Area Filter */}
                <div className="mt-3 flex flex-wrap gap-1.5">
                  <button
                    type="button"
                    onClick={() => setSelectedArea('All')}
                    className={`min-h-[36px] rounded-xl border px-3 py-1 text-xs font-semibold transition-colors ${
                      effectiveSelectedArea === 'All'
                        ? 'border-blue-700 bg-blue-50 text-blue-900'
                        : 'border-slate-200 bg-white text-slate-600 hover:bg-slate-50'
                    }`}
                  >
                    All Grade {learnerGrade} Subjects ({gradeSubjects.length})
                  </button>
                  {gradeSubjects.map((area) => (
                    <button
                      key={area}
                      type="button"
                      onClick={() => setSelectedArea(area)}
                      className={`min-h-[36px] rounded-xl border px-3 py-1 text-xs font-semibold transition-colors ${
                        effectiveSelectedArea === area
                          ? 'border-blue-700 bg-blue-50 text-blue-900'
                          : 'border-slate-200 bg-white text-slate-600 hover:bg-slate-50'
                      }`}
                    >
                      {area}
                    </button>
                  ))}
                </div>

                {/* Strands & Sub-strands List */}
                <div className="mt-4 max-h-[480px] overflow-y-auto divide-y divide-slate-100 pr-1">
                  {filteredCatalog.length === 0 ? (
                    <p className="py-4 text-xs text-slate-500">
                      No strands match this search in Grade {learnerGrade}. Clear search or select
                      &ldquo;All Grade {learnerGrade} Subjects&rdquo;.
                    </p>
                  ) : (
                    filteredCatalog.map((entry, idx) => (
                      <div
                        key={`${entry.grade}-${entry.learningArea}-${entry.strand}-${entry.subStrand}-${idx}`}
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
                          onClick={() => handleLaunchCatalogEntry(entry)}
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
