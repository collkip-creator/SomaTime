import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  ASSETS,
  CBC_LESSONS,
  INITIAL_ASSIGNMENTS,
  INITIAL_CHILDREN,
  INITIAL_CLASS_ROSTER,
} from '../data/cbcCurriculum';
import type {
  CBCLesson,
  CBCRubric,
  ChildProfile,
  ClassStudentRecord,
  LearningArea,
  RoleView,
  TeacherAssignment,
} from '../types/somatime';

const STORAGE_KEY = 'somatime_prototype_state_v1';

interface PersistedState {
  roleView: RoleView;
  selectedChildId: string;
  handOffActive: boolean;
  children: ChildProfile[];
  assignments: TeacherAssignment[];
  classRoster: ClassStudentRecord[];
}

function getDefaultState(): PersistedState {
  return {
    roleView: 'parent',
    selectedChildId: INITIAL_CHILDREN[0].id,
    handOffActive: false,
    children: INITIAL_CHILDREN,
    assignments: INITIAL_ASSIGNMENTS,
    classRoster: INITIAL_CLASS_ROSTER,
  };
}

function loadState(): PersistedState {
  try {
    const raw = window.localStorage.getItem(STORAGE_KEY);
    if (!raw) return getDefaultState();
    const parsed = JSON.parse(raw) as Partial<PersistedState>;
    if (!parsed.children || !Array.isArray(parsed.children) || parsed.children.length === 0) {
      return getDefaultState();
    }
    return {
      roleView: parsed.roleView ?? 'parent',
      selectedChildId: parsed.selectedChildId ?? parsed.children[0].id,
      handOffActive: Boolean(parsed.handOffActive),
      children: parsed.children,
      assignments: parsed.assignments ?? INITIAL_ASSIGNMENTS,
      classRoster: parsed.classRoster ?? INITIAL_CLASS_ROSTER,
    };
  } catch {
    return getDefaultState();
  }
}

export function scoreToRubric(scorePercent: number): CBCRubric {
  if (scorePercent >= 85) return 'EE';
  if (scorePercent >= 65) return 'ME';
  if (scorePercent >= 45) return 'AE';
  return 'BE';
}

export function formatTimeClock(totalSeconds: number): string {
  const safe = Math.max(0, Math.floor(totalSeconds));
  const mins = Math.floor(safe / 60);
  const secs = safe % 60;
  return `${String(mins).padStart(2, '0')}:${String(secs).padStart(2, '0')}`;
}

export function useSomaTimeStore() {
  const [state, setState] = useState<PersistedState>(() => loadState());

  useEffect(() => {
    try {
      window.localStorage.setItem(STORAGE_KEY, JSON.stringify(state));
    } catch {
      // Ignore storage quota errors in private browsing
    }
  }, [state]);

  // Live 1-second screen-time countdown when Child Mode is unlocked & active
  useEffect(() => {
    const interval = window.setInterval(() => {
      setState((prev) => {
        const hasTickingChild = prev.children.some(
          (c) => !c.isLocked && c.availableScreenSeconds > 0
        );
        if (!hasTickingChild) return prev;

        let changed = false;
        const nextChildren = prev.children.map((child) => {
          if (child.isLocked || child.availableScreenSeconds <= 0) return child;
          changed = true;
          const nextSeconds = Math.max(0, child.availableScreenSeconds - 1);
          const minuteRolled = nextSeconds % 60 === 59 && child.availableScreenSeconds > 0;
          const nextUsed = minuteRolled
            ? Math.min(child.dailyCapMins, child.usedScreenTodayMins + 1)
            : child.usedScreenTodayMins;
          const nowLocked = nextSeconds === 0;
          return {
            ...child,
            availableScreenSeconds: nextSeconds,
            usedScreenTodayMins: nextUsed,
            isLocked: nowLocked,
            activeAppName: nowLocked ? null : child.activeAppName,
          };
        });

        return changed ? { ...prev, children: nextChildren } : prev;
      });
    }, 1000);

    return () => window.clearInterval(interval);
  }, []);

  const selectedChild = useMemo(
    () => state.children.find((c) => c.id === state.selectedChildId) ?? state.children[0],
    [state.children, state.selectedChildId]
  );

  const setRoleView = useCallback((roleView: RoleView) => {
    setState((prev) => ({ ...prev, roleView }));
  }, []);

  const setSelectedChildId = useCallback((selectedChildId: string) => {
    setState((prev) => ({ ...prev, selectedChildId }));
  }, []);

  const updateChildSettings = useCallback(
    (childId: string, patch: Partial<ChildProfile>) => {
      setState((prev) => ({
        ...prev,
        children: prev.children.map((c) => (c.id === childId ? { ...c, ...patch } : c)),
      }));
    },
    []
  );

  const addChildProfile = useCallback(
    (input: { name: string; grade: number; school: string }) => {
      const id = `child-${Date.now()}`;
      const isUpper = input.grade >= 7;
      const newChild: ChildProfile = {
        id,
        name: input.name.trim(),
        grade: input.grade,
        school: input.school.trim() || 'Nairobi Primary School',
        classStream: `Grade ${input.grade} East - ${input.school.trim() || 'Nairobi Primary'}`,
        avatarUrl: input.grade >= 7 ? ASSETS.brianAvatar : ASSETS.aminaAvatar,
        deviceMode: 'hand_off',
        dailyCapMins: 90,
        exchangeRateMins: 15,
        mandatorySubjects: isUpper
          ? ['Mathematics', 'Integrated Science']
          : ['Mathematics', 'Kiswahili'],
        requireParentApproval: false,
        availableScreenSeconds: 0,
        usedScreenTodayMins: 0,
        learnedMinutesToday: 0,
        isLocked: true,
        activeAppName: null,
        completedLessonIds: [],
        pendingApprovals: [],
        activityLogs: [],
        rubricByArea: {
          Mathematics: 'ME',
          English: 'ME',
          Kiswahili: 'ME',
        },
        streakDays: 1,
      };
      setState((prev) => ({
        ...prev,
        selectedChildId: id,
        children: [...prev.children, newChild],
      }));
    },
    []
  );

  const activateHandOffMode = useCallback((childId: string) => {
    setState((prev) => ({
      ...prev,
      roleView: 'child',
      selectedChildId: childId,
      handOffActive: true,
      children: prev.children.map((c) =>
        c.id === childId
          ? {
              ...c,
              deviceMode: 'hand_off',
              isLocked: true,
              availableScreenSeconds: 0,
              activeAppName: null,
            }
          : c
      ),
    }));
  }, []);

  const exitHandOffToParent = useCallback(() => {
    setState((prev) => ({
      ...prev,
      handOffActive: false,
      roleView: 'parent',
    }));
  }, []);

  const simulateTimeExpired = useCallback((childId: string) => {
    setState((prev) => ({
      ...prev,
      children: prev.children.map((c) =>
        c.id === childId
          ? {
              ...c,
              availableScreenSeconds: 0,
              isLocked: true,
              activeAppName: null,
            }
          : c
      ),
    }));
  }, []);

  const unlockWithParentPin = useCallback((childId: string, bonusMins = 15) => {
    setState((prev) => ({
      ...prev,
      handOffActive: false,
      children: prev.children.map((c) => {
        if (c.id !== childId) return c;
        const remainingCapMins = Math.max(0, c.dailyCapMins - c.usedScreenTodayMins);
        const grantedMins = Math.max(5, Math.min(bonusMins, remainingCapMins || bonusMins));
        return {
          ...c,
          isLocked: false,
          availableScreenSeconds: c.availableScreenSeconds + grantedMins * 60,
        };
      }),
    }));
  }, []);

  const selectActiveApp = useCallback((childId: string, appName: string | null) => {
    setState((prev) => ({
      ...prev,
      children: prev.children.map((c) =>
        c.id === childId ? { ...c, activeAppName: appName } : c
      ),
    }));
  }, []);

  const completeLesson = useCallback(
    (
      childId: string,
      lesson: CBCLesson,
      scorePercent: number
    ): {
      minutesEarned: number;
      rubric: CBCRubric;
      queuedForApproval: boolean;
      cappedOut: boolean;
    } => {
      const rubric = scoreToRubric(scorePercent);
      let result = {
        minutesEarned: 0,
        rubric,
        queuedForApproval: false,
        cappedOut: false,
      };

      setState((prev) => {
        const targetChild = prev.children.find((c) => c.id === childId);
        if (!targetChild) return prev;

        const remainingCapMins = Math.max(
          0,
          targetChild.dailyCapMins -
            targetChild.usedScreenTodayMins -
            Math.floor(targetChild.availableScreenSeconds / 60)
        );
        const earnedMins = Math.min(targetChild.exchangeRateMins, remainingCapMins);
        const cappedOut = remainingCapMins <= 0;
        const queuedForApproval = targetChild.requireParentApproval && earnedMins > 0;

        result = {
          minutesEarned: earnedMins,
          rubric,
          queuedForApproval,
          cappedOut,
        };

        const nowTime = new Date().toLocaleTimeString([], {
          hour: '2-digit',
          minute: '2-digit',
        });

        const nextChildren = prev.children.map((c) => {
          if (c.id !== childId) return c;
          const completedLessonIds = c.completedLessonIds.includes(lesson.id)
            ? c.completedLessonIds
            : [...c.completedLessonIds, lesson.id];

          const newLog = {
            id: `log-${Date.now()}`,
            lessonId: lesson.id,
            lessonTitle: lesson.title,
            learningArea: lesson.learningArea,
            scorePercent,
            rubricEarned: rubric,
            minutesEarned: earnedMins,
            status: queuedForApproval ? ('pending' as const) : ('approved' as const),
            timestamp: nowTime,
          };

          const newPending = queuedForApproval
            ? [
                {
                  id: `pend-${Date.now()}`,
                  lessonTitle: lesson.title,
                  learningArea: lesson.learningArea,
                  minutesEarned: earnedMins,
                  scorePercent,
                  completedAt: nowTime,
                },
                ...c.pendingApprovals,
              ]
            : c.pendingApprovals;

          const unlockedNow = !queuedForApproval && earnedMins > 0;

          return {
            ...c,
            learnedMinutesToday: c.learnedMinutesToday + lesson.durationMins,
            completedLessonIds,
            availableScreenSeconds: unlockedNow
              ? c.availableScreenSeconds + earnedMins * 60
              : c.availableScreenSeconds,
            isLocked: unlockedNow ? false : c.isLocked,
            pendingApprovals: newPending,
            activityLogs: [newLog, ...c.activityLogs],
            rubricByArea: {
              ...c.rubricByArea,
              [lesson.learningArea]: rubric,
            },
          };
        });

        const nextAssignments = prev.assignments.map((assign) => {
          if (assign.lessonId !== lesson.id && assign.learningArea !== lesson.learningArea) {
            return assign;
          }
          if (assign.lessonId === lesson.id) {
            return assign.completedByChildIds.includes(childId)
              ? assign
              : { ...assign, completedByChildIds: [...assign.completedByChildIds, childId] };
          }
          return assign;
        });

        const nextRoster = prev.classRoster.map((student) =>
          student.name.toLowerCase() === targetChild.name.toLowerCase()
            ? {
                ...student,
                rubric,
                scorePercent: Math.round((student.scorePercent + scorePercent) / 2),
                learningMins: student.learningMins + lesson.durationMins,
                assignmentDone: true,
              }
            : student
        );

        return {
          ...prev,
          children: nextChildren,
          assignments: nextAssignments,
          classRoster: nextRoster,
        };
      });

      return result;
    },
    []
  );

  const resolveApproval = useCallback(
    (childId: string, approvalId: string, approved: boolean) => {
      setState((prev) => ({
        ...prev,
        children: prev.children.map((c) => {
          if (c.id !== childId) return c;
          const target = c.pendingApprovals.find((p) => p.id === approvalId);
          if (!target) return c;

          return {
            ...c,
            pendingApprovals: c.pendingApprovals.filter((p) => p.id !== approvalId),
            availableScreenSeconds: approved
              ? c.availableScreenSeconds + target.minutesEarned * 60
              : c.availableScreenSeconds,
            isLocked: approved ? false : c.isLocked,
            activityLogs: c.activityLogs.map((l) =>
              l.lessonTitle === target.lessonTitle && l.status === 'pending'
                ? { ...l, status: approved ? 'approved' : 'declined' }
                : l
            ),
          };
        }),
      }));
    },
    []
  );

  const pushTeacherAssignment = useCallback(
    (input: {
      classStream: string;
      grade: number;
      learningArea: LearningArea;
      strand: string;
      subStrand: string;
    }) => {
      const matchingLesson =
        CBC_LESSONS.find(
          (l) =>
            l.learningArea === input.learningArea &&
            (l.strand === input.strand || l.grade === input.grade)
        ) ??
        CBC_LESSONS.find((l) => l.learningArea === input.learningArea) ??
        CBC_LESSONS[0];

      const newAssignment: TeacherAssignment = {
        id: `assign-${Date.now()}`,
        classStream: input.classStream,
        grade: input.grade,
        learningArea: input.learningArea,
        strand: input.strand,
        subStrand: input.subStrand,
        lessonId: matchingLesson.id,
        title: `${input.subStrand} (${matchingLesson.title})`,
        dueLabel: 'Priority · Before Screen Time',
        pushedAt: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
        completedByChildIds: [],
      };

      setState((prev) => ({
        ...prev,
        assignments: [newAssignment, ...prev.assignments],
      }));
    },
    []
  );

  const resetDemoData = useCallback(() => {
    const fresh = getDefaultState();
    setState(fresh);
    try {
      window.localStorage.setItem(STORAGE_KEY, JSON.stringify(fresh));
    } catch {
      // Ignore storage errors
    }
  }, []);

  return {
    roleView: state.roleView,
    selectedChildId: state.selectedChildId,
    selectedChild,
    handOffActive: state.handOffActive,
    children: state.children,
    assignments: state.assignments,
    classRoster: state.classRoster,
    setRoleView,
    setSelectedChildId,
    updateChildSettings,
    addChildProfile,
    activateHandOffMode,
    exitHandOffToParent,
    simulateTimeExpired,
    unlockWithParentPin,
    selectActiveApp,
    completeLesson,
    resolveApproval,
    pushTeacherAssignment,
    resetDemoData,
  };
}
