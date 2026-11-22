import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import {
  ASSETS,
  CBC_LESSONS,
  INITIAL_ASSIGNMENTS,
  INITIAL_CHILDREN,
  INITIAL_CLASS_ROSTER,
  MONTHLY_LICENSE_KES,
  MONTHLY_LICENSE_PLAN_LABEL,
  PARENT_PIN,
  buildLicenseExpiryIso,
  generatePairingCode,
  generateTeacherCode,
} from '../data/cbcCurriculum';
import {
  fetchAndPairFromCloud,
  loginTeacherFromCloud,
  normalizePairingCode,
  normalizeTeacherCode,
  publishPairingToCloud,
  publishSchoolGradeAssignmentToCloud,
  registerTeacherInCloud,
  subscribePairingSession,
  subscribeSchoolGradeAssignments,
  syncLearnerStateToCloud,
  type CloudPairingDoc,
} from '../lib/firebase';
import type {
  ActivityQuestion,
  CBCLesson,
  CBCRubric,
  ChildProfile,
  ClassStudentRecord,
  LearningArea,
  ParentAccount,
  RoleView,
  TeacherAccount,
  TeacherAssignment,
} from '../types/somatime';

const STORAGE_KEY = 'somatime_prototype_state_v2';

interface PersistedState {
  roleView: RoleView;
  selectedChildId: string;
  handOffActive: boolean;
  parentAccount: ParentAccount;
  teacherAccount: TeacherAccount | null;
  registeredTeachers: TeacherAccount[];
  children: ChildProfile[];
  assignments: TeacherAssignment[];
  classRoster: ClassStudentRecord[];
  displayedKeysByChild: Record<string, string[]>;
  displayedQuizIdsByChild: Record<string, string[]>;
}

function getDefaultState(): PersistedState {
  return {
    roleView: 'parent',
    selectedChildId: INITIAL_CHILDREN[0].id,
    handOffActive: false,
    parentAccount: {
      name: 'Grace Wanjiku',
      contact: '0712 345 678',
      pin: PARENT_PIN,
      registered: true,
    },
    teacherAccount: null,
    registeredTeachers: [],
    children: INITIAL_CHILDREN,
    assignments: INITIAL_ASSIGNMENTS,
    classRoster: INITIAL_CLASS_ROSTER,
    displayedKeysByChild: {},
    displayedQuizIdsByChild: {},
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
      parentAccount: parsed.parentAccount ?? getDefaultState().parentAccount,
      teacherAccount: parsed.teacherAccount ?? null,
      registeredTeachers: Array.isArray(parsed.registeredTeachers) ? parsed.registeredTeachers : [],
      children: parsed.children.map((c) => ({
        ...c,
        licenseActive: c.licenseActive ?? (c.id === 'brian'),
        licensePlanLabel: MONTHLY_LICENSE_PLAN_LABEL,
        licenseExpiresAt:
          c.licenseExpiresAt ?? (c.id === 'brian' ? buildLicenseExpiryIso(30) : null),
        licensePaymentRef: c.licensePaymentRef ?? (c.id === 'brian' ? 'MPESA-SOMA8009' : null),
      })),
      assignments: parsed.assignments ?? INITIAL_ASSIGNMENTS,
      classRoster: parsed.classRoster ?? INITIAL_CLASS_ROSTER,
      displayedKeysByChild: parsed.displayedKeysByChild ?? {},
      displayedQuizIdsByChild: parsed.displayedQuizIdsByChild ?? {},
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

function cloudDocToChildProfile(
  cloud: CloudPairingDoc,
  existing?: ChildProfile
): ChildProfile {
  const isUpper = cloud.grade >= 7;
  return {
    id: cloud.childId,
    name: cloud.childName,
    grade: cloud.grade,
    school: cloud.school,
    classStream: cloud.classStream || `Grade ${cloud.grade} East - ${cloud.school}`,
    avatarUrl: existing?.avatarUrl || (isUpper ? ASSETS.brianAvatar : ASSETS.aminaAvatar),
    deviceMode: cloud.deviceMode,
    pairingCode: cloud.pairingCode,
    isPaired: cloud.isPaired,
    backgroundGuardActive: cloud.isPaired || Boolean(existing?.backgroundGuardActive),
    dailyCapMins: cloud.dailyCapMins,
    exchangeRateMins: cloud.exchangeRateMins,
    mandatorySubjects: (cloud.mandatorySubjects || []) as LearningArea[],
    requireParentApproval: cloud.requireParentApproval,
    availableScreenSeconds: cloud.availableScreenSeconds,
    usedScreenTodayMins: cloud.usedScreenTodayMins,
    learnedMinutesToday: cloud.learnedMinutesToday,
    isLocked: cloud.isLocked || cloud.availableScreenSeconds <= 0,
    activeAppName: existing?.activeAppName ?? null,
    lastInterruptedAppName: existing?.lastInterruptedAppName ?? existing?.activeAppName ?? null,
    completedLessonIds: cloud.completedLessonIds || existing?.completedLessonIds || [],
    pendingApprovals: existing?.pendingApprovals ?? [],
    activityLogs: existing?.activityLogs ?? [],
    rubricByArea: existing?.rubricByArea ?? {
      Mathematics: 'ME',
      English: 'ME',
    },
    streakDays: existing?.streakDays ?? 1,
    licenseActive: cloud.licenseActive ?? existing?.licenseActive ?? false,
    licensePlanLabel: existing?.licensePlanLabel ?? MONTHLY_LICENSE_PLAN_LABEL,
    licenseExpiresAt: cloud.licenseExpiresAt || existing?.licenseExpiresAt || null,
    licensePaymentRef: cloud.licensePaymentRef || existing?.licensePaymentRef || null,
  };
}

export function useSomaTimeStore() {
  const [state, setState] = useState<PersistedState>(() => loadState());
  const stateRef = useRef(state);
  stateRef.current = state;
  const lastCloudSyncRef = useRef<number>(0);

  useEffect(() => {
    try {
      window.localStorage.setItem(STORAGE_KEY, JSON.stringify(state));
    } catch {
      // Ignore storage quota errors
    }
  }, [state]);

  const selectedChild = useMemo(
    () => state.children.find((c) => c.id === state.selectedChildId) ?? state.children[0],
    [state.children, state.selectedChildId]
  );

  const [cloudSyncOk, setCloudSyncOk] = useState(false);
  const [cloudSyncMessage, setCloudSyncMessage] = useState('Syncing pairing codes to Cloud...');

  // On initial mount in Parent mode, ensure existing local child pairing codes are published to Cloud Firestore
  useEffect(() => {
    if (stateRef.current.roleView !== 'parent') return;
    const pin = stateRef.current.parentAccount.pin;
    Promise.all(
      stateRef.current.children.map((child) => publishPairingToCloud(child, pin))
    )
      .then(() => {
        setCloudSyncOk(true);
        setCloudSyncMessage('Synced to Cloud ✓ · Ready for learner phone');
      })
      .catch(() => {
        setCloudSyncOk(false);
        setCloudSyncMessage('Cloud sync failed — check internet and click Sync Cloud');
      });
  }, []);

  const retryCloudSyncNow = useCallback(async () => {
    const child =
      stateRef.current.children.find((c) => c.id === stateRef.current.selectedChildId) ??
      stateRef.current.children[0];
    if (!child) return;
    setCloudSyncMessage(`Syncing ${child.pairingCode} to Cloud...`);
    try {
      await publishPairingToCloud(child, stateRef.current.parentAccount.pin);
      setCloudSyncOk(true);
      setCloudSyncMessage(`Synced to Cloud ✓ · Code ${child.pairingCode} is active`);
    } catch {
      setCloudSyncOk(false);
      setCloudSyncMessage('Cloud sync failed — check internet connection and retry.');
    }
  }, []);

  // Live Firestore real-time subscription for the active child's pairing session
  useEffect(() => {
    const code = selectedChild?.pairingCode;
    if (!code) return;

    const unsubscribe = subscribePairingSession(
      code,
      (cloud) => {
        setState((prev) => {
          const idx = prev.children.findIndex(
            (c) =>
              c.id === cloud.childId ||
              normalizePairingCode(c.pairingCode) === normalizePairingCode(cloud.pairingCode)
          );
          if (idx === -1) return prev;
          const current = prev.children[idx];
          const sameState =
            current.isPaired === cloud.isPaired &&
            current.isLocked === cloud.isLocked &&
            current.dailyCapMins === cloud.dailyCapMins &&
            current.exchangeRateMins === cloud.exchangeRateMins &&
            current.usedScreenTodayMins === cloud.usedScreenTodayMins &&
            current.learnedMinutesToday === cloud.learnedMinutesToday &&
            current.requireParentApproval === cloud.requireParentApproval &&
            current.licenseActive === (cloud.licenseActive ?? current.licenseActive) &&
            Math.abs(current.availableScreenSeconds - cloud.availableScreenSeconds) <= 2 &&
            prev.parentAccount.pin === cloud.parentPin;

          if (sameState) return prev;

          const merged = cloudDocToChildProfile(cloud, current);
          const nextChildren = [...prev.children];
          nextChildren[idx] = merged;
          return {
            ...prev,
            parentAccount: {
              ...prev.parentAccount,
              pin: cloud.parentPin || prev.parentAccount.pin,
            },
            children: nextChildren,
          };
        });
      },
      () => {
        // Silently tolerate transient offline listener errors
      }
    );

    return () => unsubscribe();
  }, [selectedChild?.pairingCode]);

  // Live Firestore real-time subscription for assignments matching the learner's school and grade
  useEffect(() => {
    const school = selectedChild?.school;
    const grade = selectedChild?.grade;
    if (!school || !grade) return;

    const unsubscribe = subscribeSchoolGradeAssignments(
      school,
      grade,
      (cloudAssignments) => {
        if (cloudAssignments.length === 0) return;
        setState((prev) => {
          const existingById = new Map(prev.assignments.map((a) => [a.id, a]));
          const mergedCloud = cloudAssignments.map((ca) => {
            const prevItem = existingById.get(ca.id);
            return prevItem
              ? { ...ca, completedByChildIds: prevItem.completedByChildIds }
              : ca;
          });
          const cloudIds = new Set(mergedCloud.map((a) => a.id));
          const remainingLocal = prev.assignments.filter((a) => !cloudIds.has(a.id));
          return {
            ...prev,
            assignments: [...mergedCloud, ...remainingLocal],
          };
        });
      },
      () => {
        // Silently tolerate transient offline listener errors
      }
    );

    return () => unsubscribe();
  }, [selectedChild?.school, selectedChild?.grade]);

  // Live 1-second background monitor ticker checking available watch minutes
  useEffect(() => {
    const interval = window.setInterval(() => {
      let lockedChildToSync: ChildProfile | null = null;
      let pairedChildToSync: ChildProfile | null = null;

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
          const updated: ChildProfile = {
            ...child,
            availableScreenSeconds: nextSeconds,
            usedScreenTodayMins: nextUsed,
            isLocked: nowLocked,
            lastInterruptedAppName: nowLocked
              ? child.activeAppName || child.lastInterruptedAppName || 'YouTube'
              : child.lastInterruptedAppName,
            activeAppName: nowLocked ? null : child.activeAppName,
          };
          if (nowLocked && updated.isPaired) {
            lockedChildToSync = updated;
          } else if (updated.isPaired && updated.id === prev.selectedChildId) {
            pairedChildToSync = updated;
          }
          return updated;
        });

        return changed ? { ...prev, children: nextChildren } : prev;
      });

      const now = Date.now();
      if (lockedChildToSync) {
        lastCloudSyncRef.current = now;
        void syncLearnerStateToCloud(lockedChildToSync).catch(() => {});
      } else if (pairedChildToSync && now - lastCloudSyncRef.current > 10_000) {
        lastCloudSyncRef.current = now;
        void syncLearnerStateToCloud(pairedChildToSync).catch(() => {});
      }
    }, 1000);

    return () => window.clearInterval(interval);
  }, []);

  const setRoleView = useCallback((roleView: RoleView) => {
    setState((prev) => ({ ...prev, roleView }));
  }, []);

  const setSelectedChildId = useCallback((selectedChildId: string) => {
    setState((prev) => ({ ...prev, selectedChildId }));
  }, []);

  const updateChildSettings = useCallback(
    (childId: string, patch: Partial<ChildProfile>) => {
      let updatedTarget: ChildProfile | null = null;
      setState((prev) => {
        const nextChildren = prev.children.map((c) => {
          if (c.id !== childId) return c;
          updatedTarget = { ...c, ...patch };
          return updatedTarget;
        });
        return { ...prev, children: nextChildren };
      });
      if (updatedTarget) {
        void publishPairingToCloud(updatedTarget, stateRef.current.parentAccount.pin).catch(
          () => {}
        );
      }
    },
    []
  );

  const updateParentPin = useCallback((newPin: string): boolean => {
    const clean = newPin.replace(/\D/g, '').slice(0, 4);
    if (clean.length !== 4) return false;

    const currentChildren = stateRef.current.children;
    setState((prev) => ({
      ...prev,
      parentAccount: {
        ...prev.parentAccount,
        pin: clean,
        registered: true,
      },
    }));

    currentChildren.forEach((child) => {
      void publishPairingToCloud(child, clean).catch(() => {});
    });
    return true;
  }, []);

  const activateChildLicense = useCallback(
    (
      childId: string,
      payerPhone?: string,
      explicitReceiptRef?: string
    ): { ok: boolean; receiptRef: string; expiresAt: string } => {
      const expiresAt = buildLicenseExpiryIso(30);
      const token = Math.random().toString(36).slice(2, 6).toUpperCase();
      const suffix = (payerPhone || '').replace(/\D/g, '').slice(-4) || token;
      const receiptRef = (explicitReceiptRef || `MPESA-${suffix}${token}`).slice(0, 24);

      let updatedTarget: ChildProfile | null = null;
      setState((prev) => ({
        ...prev,
        children: prev.children.map((c) => {
          if (c.id !== childId) return c;
          updatedTarget = {
            ...c,
            licenseActive: true,
            licensePlanLabel: MONTHLY_LICENSE_PLAN_LABEL,
            licenseExpiresAt: expiresAt,
            licensePaymentRef: receiptRef,
          };
          return updatedTarget;
        }),
      }));

      if (updatedTarget) {
        void publishPairingToCloud(updatedTarget, stateRef.current.parentAccount.pin)
          .then(() => {
            setCloudSyncOk(true);
            setCloudSyncMessage(
              `M-Pesa License active (${receiptRef}) ✓ · Code ${(updatedTarget as ChildProfile).pairingCode} ready for learner phone`
            );
          })
          .catch(() => {});
      }

      return { ok: true, receiptRef, expiresAt };
    },
    []
  );

  const activateAllUnpaidLicenses = useCallback(
    (payerPhone?: string, explicitReceiptRef?: string): number => {
      const unpaid = stateRef.current.children.filter((c) => !c.licenseActive);
      if (unpaid.length === 0) return 0;

      const expiresAt = buildLicenseExpiryIso(30);
      const token = Math.random().toString(36).slice(2, 6).toUpperCase();
      const suffix = (payerPhone || '').replace(/\D/g, '').slice(-4) || token;
      const receiptRef = (explicitReceiptRef || `MPESA-FAM${suffix}`).slice(0, 24);

      const updatedList: ChildProfile[] = [];
      setState((prev) => ({
        ...prev,
        children: prev.children.map((c) => {
          if (c.licenseActive) return c;
          const next = {
            ...c,
            licenseActive: true,
            licensePlanLabel: MONTHLY_LICENSE_PLAN_LABEL,
            licenseExpiresAt: expiresAt,
            licensePaymentRef: receiptRef,
          };
          updatedList.push(next);
          return next;
        }),
      }));

      updatedList.forEach((child) => {
        void publishPairingToCloud(child, stateRef.current.parentAccount.pin).catch(() => {});
      });

      return unpaid.length;
    },
    []
  );

  const regeneratePairingCode = useCallback((childId: string) => {
    const target = stateRef.current.children.find((c) => c.id === childId);
    if (!target || !target.licenseActive) return null;

    const nextCode = generatePairingCode();
    let updatedTarget: ChildProfile | null = null;
    setState((prev) => ({
      ...prev,
      children: prev.children.map((c) => {
        if (c.id !== childId) return c;
        updatedTarget = { ...c, pairingCode: nextCode, isPaired: false };
        return updatedTarget;
      }),
    }));
    if (updatedTarget) {
      void publishPairingToCloud(updatedTarget, stateRef.current.parentAccount.pin).catch(
        () => {}
      );
    }
    return nextCode;
  }, []);

  const registerParentAndChild = useCallback(
    async (input: {
      parentName: string;
      parentContact: string;
      parentPin: string;
      childName: string;
      childGrade: number;
      childSchool: string;
      dailyCapMins: number;
      initialWatchMins: number;
    }): Promise<ChildProfile> => {
      const id = `child-${Date.now()}`;
      const isUpper = input.childGrade >= 7;
      const code = generatePairingCode();
      const validPin =
        input.parentPin.trim().length === 4
          ? input.parentPin.trim()
          : stateRef.current.parentAccount.pin;

      const newChild: ChildProfile = {
        id,
        name: input.childName.trim(),
        grade: input.childGrade,
        school: input.childSchool.trim() || 'Nairobi Primary School',
        classStream: `Grade ${input.childGrade} East - ${input.childSchool.trim() || 'Nairobi Primary'}`,
        avatarUrl: input.childGrade >= 7 ? ASSETS.brianAvatar : ASSETS.aminaAvatar,
        deviceMode: 'own_phone',
        pairingCode: code,
        isPaired: false,
        backgroundGuardActive: false,
        dailyCapMins: input.dailyCapMins,
        exchangeRateMins: 15,
        mandatorySubjects: isUpper
          ? ['Mathematics', 'Integrated Science']
          : ['Mathematics', 'Kiswahili'],
        requireParentApproval: false,
        availableScreenSeconds: input.initialWatchMins * 60,
        usedScreenTodayMins: 0,
        learnedMinutesToday: 0,
        isLocked: input.initialWatchMins <= 0,
        activeAppName: input.initialWatchMins > 0 ? 'YouTube' : null,
        completedLessonIds: [],
        pendingApprovals: [],
        activityLogs: [],
        rubricByArea: {
          Mathematics: 'ME',
          English: 'ME',
        },
        streakDays: 1,
        licenseActive: false,
        licensePlanLabel: MONTHLY_LICENSE_PLAN_LABEL,
        licenseExpiresAt: null,
        licensePaymentRef: null,
      };

      setState((prev) => ({
        ...prev,
        roleView: 'parent',
        selectedChildId: id,
        parentAccount: {
          name: input.parentName.trim() || prev.parentAccount.name,
          contact: input.parentContact.trim() || prev.parentAccount.contact,
          pin: validPin,
          registered: true,
        },
        children: [newChild, ...prev.children],
      }));

      await publishPairingToCloud(newChild, validPin);
      setCloudSyncOk(true);
      setCloudSyncMessage(
        `Profile saved ✓ · Pay KES ${MONTHLY_LICENSE_KES}/mo via M-Pesa on Parents tab to unlock pairing for ${newChild.name}`
      );
      return newChild;
    },
    []
  );

  const pairLearnerByCode = useCallback(
    async (
      rawCode: string
    ): Promise<{ success: boolean; child?: ChildProfile; message: string }> => {
      const normalized = normalizePairingCode(rawCode);
      if (!normalized) {
        return { success: false, message: 'Please enter the pairing code from your parent.' };
      }

      // 1. Check Cloud Firestore first (enables pairing across two separate phones!)
      let cloudMatch: CloudPairingDoc | null = null;
      let cloudLookupError = '';
      try {
        cloudMatch = await fetchAndPairFromCloud(normalized);
      } catch (err) {
        cloudLookupError =
          err instanceof Error ? err.message : 'Unable to reach Cloud Firestore.';
        cloudMatch = null;
      }

      if (cloudMatch) {
        if (cloudMatch.licenseActive === false) {
          return {
            success: false,
            message: `Monthly child license for ${cloudMatch.childName} is inactive. Ask your parent to pay KES ${MONTHLY_LICENSE_KES}/mo via M-Pesa in the Parents Licensing Area to activate this pairing code.`,
          };
        }
        const existingLocal = stateRef.current.children.find(
          (c) =>
            c.id === cloudMatch.childId ||
            normalizePairingCode(c.pairingCode) === normalized
        );
        const syncedChild = cloudDocToChildProfile(cloudMatch, existingLocal);

        setState((prev) => {
          const exists = prev.children.some((c) => c.id === syncedChild.id);
          const nextChildren = exists
            ? prev.children.map((c) => (c.id === syncedChild.id ? syncedChild : c))
            : [syncedChild, ...prev.children];
          return {
            ...prev,
            roleView: 'child',
            selectedChildId: syncedChild.id,
            handOffActive: true,
            parentAccount: {
              ...prev.parentAccount,
              pin: cloudMatch.parentPin || prev.parentAccount.pin,
            },
            children: nextChildren,
          };
        });

        return {
          success: true,
          child: syncedChild,
          message: `Paired with ${syncedChild.name}! Background watch-minute guard is now active.`,
        };
      }

      // 2. Fallback: check local state (same-device demo) and sync up to Firestore
      const matched = stateRef.current.children.find(
        (c) => normalizePairingCode(c.pairingCode) === normalized
      );
      if (!matched) {
        return {
          success: false,
          message: cloudLookupError
            ? `Cloud lookup failed for "${normalized}". Check internet connection and retry.`
            : `No child profile matches "${normalized}". Verify the code shown on your parent's phone (make sure the parent phone has synced to cloud).`,
        };
      }
      if (!matched.licenseActive) {
        return {
          success: false,
          message: `Monthly license for ${matched.name} is unpaid. Pay KES ${MONTHLY_LICENSE_KES}/mo via M-Pesa on the Parents tab first to unlock pairing.`,
        };
      }

      const updatedChild: ChildProfile = {
        ...matched,
        deviceMode: 'own_phone',
        isPaired: true,
        backgroundGuardActive: true,
      };

      setState((prev) => ({
        ...prev,
        roleView: 'child',
        selectedChildId: matched.id,
        handOffActive: true,
        children: prev.children.map((c) => (c.id === matched.id ? updatedChild : c)),
      }));

      void publishPairingToCloud(updatedChild, stateRef.current.parentAccount.pin).catch(
        () => {}
      );

      return {
        success: true,
        child: updatedChild,
        message: `Paired with ${matched.name}! Background watch-minute guard is now active.`,
      };
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
        deviceMode: 'own_phone',
        pairingCode: generatePairingCode(),
        isPaired: false,
        backgroundGuardActive: false,
        dailyCapMins: 90,
        exchangeRateMins: 15,
        mandatorySubjects: isUpper
          ? ['Mathematics', 'Integrated Science']
          : ['Mathematics', 'Kiswahili'],
        requireParentApproval: false,
        availableScreenSeconds: 180,
        usedScreenTodayMins: 0,
        learnedMinutesToday: 0,
        isLocked: false,
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
        licenseActive: false,
        licensePlanLabel: MONTHLY_LICENSE_PLAN_LABEL,
        licenseExpiresAt: null,
        licensePaymentRef: null,
      };
      setState((prev) => ({
        ...prev,
        selectedChildId: id,
        children: [...prev.children, newChild],
      }));
      void publishPairingToCloud(newChild, stateRef.current.parentAccount.pin).catch(() => {});
    },
    []
  );

  const activateHandOffMode = useCallback((childId: string): boolean => {
    const target = stateRef.current.children.find((c) => c.id === childId);
    if (!target || !target.licenseActive) return false;

    let updatedTarget: ChildProfile | null = null;
    setState((prev) => ({
      ...prev,
      roleView: 'child',
      selectedChildId: childId,
      handOffActive: true,
      children: prev.children.map((c) => {
        if (c.id !== childId) return c;
        updatedTarget = {
          ...c,
          deviceMode: 'hand_off',
          backgroundGuardActive: true,
          isLocked: true,
          availableScreenSeconds: 0,
          lastInterruptedAppName: c.activeAppName || c.lastInterruptedAppName || null,
          activeAppName: null,
        };
        return updatedTarget;
      }),
    }));
    if (updatedTarget) {
      void publishPairingToCloud(updatedTarget, stateRef.current.parentAccount.pin).catch(
        () => {}
      );
    }
    return true;
  }, []);

  const exitHandOffToParent = useCallback(() => {
    setState((prev) => ({
      ...prev,
      handOffActive: false,
      roleView: 'parent',
    }));
  }, []);

  const simulateTimeExpired = useCallback((childId: string) => {
    let updatedTarget: ChildProfile | null = null;
    setState((prev) => ({
      ...prev,
      children: prev.children.map((c) => {
        if (c.id !== childId) return c;
        updatedTarget = {
          ...c,
          availableScreenSeconds: 0,
          isLocked: true,
          lastInterruptedAppName: c.activeAppName || c.lastInterruptedAppName || 'YouTube',
          activeAppName: null,
        };
        return updatedTarget;
      }),
    }));
    if (updatedTarget) {
      void syncLearnerStateToCloud(updatedTarget).catch(() => {});
    }
  }, []);

  const unlockWithParentPin = useCallback((childId: string, bonusMins = 15) => {
    let updatedTarget: ChildProfile | null = null;
    setState((prev) => ({
      ...prev,
      handOffActive: false,
      children: prev.children.map((c) => {
        if (c.id !== childId) return c;
        const remainingCapMins = Math.max(0, c.dailyCapMins - c.usedScreenTodayMins);
        const grantedMins = Math.max(5, Math.min(bonusMins, remainingCapMins || bonusMins));
        updatedTarget = {
          ...c,
          isLocked: false,
          availableScreenSeconds: c.availableScreenSeconds + grantedMins * 60,
          activeAppName: c.activeAppName || c.lastInterruptedAppName || null,
        };
        return updatedTarget;
      }),
    }));
    if (updatedTarget) {
      void publishPairingToCloud(updatedTarget, stateRef.current.parentAccount.pin).catch(
        () => {}
      );
    }
  }, []);

  const selectActiveApp = useCallback((childId: string, appName: string | null) => {
    setState((prev) => ({
      ...prev,
      children: prev.children.map((c) =>
        c.id === childId
          ? {
              ...c,
              activeAppName: appName,
              lastInterruptedAppName: appName ?? c.lastInterruptedAppName ?? null,
            }
          : c
      ),
    }));
  }, []);

  const recordDisplayedAssignment = useCallback(
    (childId: string, trackingKey: string, quizId: string, resetCatCycle = false) => {
      setState((prev) => {
        const currentKeys = prev.displayedKeysByChild[childId] ?? [];
        const baseKeys = resetCatCycle
          ? currentKeys.filter((k) => !k.startsWith('cat:'))
          : currentKeys.filter((k) => k !== trackingKey);
        const nextKeys = [...baseKeys, trackingKey];

        const currentQuizzes = prev.displayedQuizIdsByChild[childId] ?? [];
        const baseQuizzes = resetCatCycle
          ? []
          : currentQuizzes.filter((q) => q !== quizId);
        const nextQuizzes = [...baseQuizzes, quizId];

        return {
          ...prev,
          displayedKeysByChild: {
            ...prev.displayedKeysByChild,
            [childId]: nextKeys,
          },
          displayedQuizIdsByChild: {
            ...prev.displayedQuizIdsByChild,
            [childId]: nextQuizzes,
          },
        };
      });
    },
    []
  );

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
      let updatedTarget: ChildProfile | null = null;

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

          updatedTarget = {
            ...c,
            learnedMinutesToday: c.learnedMinutesToday + lesson.durationMins,
            completedLessonIds,
            availableScreenSeconds: unlockedNow
              ? c.availableScreenSeconds + earnedMins * 60
              : c.availableScreenSeconds,
            isLocked: unlockedNow ? false : c.isLocked,
            activeAppName: unlockedNow
              ? c.lastInterruptedAppName || c.activeAppName || 'YouTube'
              : c.activeAppName,
            pendingApprovals: newPending,
            activityLogs: [newLog, ...c.activityLogs],
            rubricByArea: {
              ...c.rubricByArea,
              [lesson.learningArea]: rubric,
            },
          };
          return updatedTarget;
        });

        const nextAssignments = prev.assignments.map((assign) => {
          if (assign.id === lesson.assignmentId || assign.lessonId === lesson.id) {
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

      if (updatedTarget) {
        void syncLearnerStateToCloud(updatedTarget).catch(() => {});
      }

      return result;
    },
    []
  );

  const resolveApproval = useCallback(
    (childId: string, approvalId: string, approved: boolean) => {
      let updatedTarget: ChildProfile | null = null;
      setState((prev) => ({
        ...prev,
        children: prev.children.map((c) => {
          if (c.id !== childId) return c;
          const target = c.pendingApprovals.find((p) => p.id === approvalId);
          if (!target) return c;

          updatedTarget = {
            ...c,
            pendingApprovals: c.pendingApprovals.filter((p) => p.id !== approvalId),
            availableScreenSeconds: approved
              ? c.availableScreenSeconds + target.minutesEarned * 60
              : c.availableScreenSeconds,
            isLocked: approved ? false : c.isLocked,
            activeAppName: approved
              ? c.lastInterruptedAppName || c.activeAppName || null
              : c.activeAppName,
            activityLogs: c.activityLogs.map((l) =>
              l.lessonTitle === target.lessonTitle && l.status === 'pending'
                ? { ...l, status: approved ? 'approved' : 'declined' }
                : l
            ),
          };
          return updatedTarget;
        }),
      }));
      if (updatedTarget) {
        void publishPairingToCloud(updatedTarget, stateRef.current.parentAccount.pin).catch(
          () => {}
        );
      }
    },
    []
  );

  const registerTeacher = useCallback(
    async (input: {
      fullName: string;
      school: string;
      tscNumber: string;
      pin: string;
      subjects: LearningArea[];
      grade: number;
      classStream: string;
    }): Promise<TeacherAccount> => {
      const code = generateTeacherCode();
      const cleanSchool = input.school.trim() || 'Nairobi Primary School';
      const safeGrade = Math.max(1, Math.min(12, Math.round(input.grade || 6)));
      const cleanStream =
        input.classStream.trim() || `Grade ${safeGrade} East - ${cleanSchool}`;
      const cleanPin = input.pin.replace(/\D/g, '').slice(0, 4);
      const validPin = cleanPin.length === 4 ? cleanPin : '1234';
      const chosenSubjects: LearningArea[] =
        input.subjects.length > 0 ? input.subjects.slice(0, 8) : ['Mathematics'];

      const draft: TeacherAccount = {
        teacherCode: code,
        fullName: input.fullName.trim() || 'Teacher',
        school: cleanSchool,
        tscNumber: input.tscNumber.trim(),
        pin: validPin,
        subjects: chosenSubjects,
        grade: safeGrade,
        classStream: cleanStream,
        registered: true,
      };

      let saved = draft;
      try {
        saved = await registerTeacherInCloud(draft);
      } catch {
        // Retain local profile if offline; user can still use teacher portal immediately
      }

      setState((prev) => ({
        ...prev,
        roleView: 'teacher',
        teacherAccount: saved,
        registeredTeachers: [
          saved,
          ...prev.registeredTeachers.filter((t) => t.teacherCode !== saved.teacherCode),
        ],
      }));

      return saved;
    },
    []
  );

  const loginTeacher = useCallback(
    async (
      rawCode: string,
      rawPin: string
    ): Promise<{ ok: boolean; teacher?: TeacherAccount; message: string }> => {
      const code = normalizeTeacherCode(rawCode);
      const pin = rawPin.replace(/\D/g, '').slice(0, 4);
      if (!code || pin.length !== 4) {
        return {
          ok: false,
          message: 'Enter both your Teacher Code (e.g. TCH-4821) and 4-digit PIN.',
        };
      }

      try {
        const cloudRes = await loginTeacherFromCloud(code, pin);
        if (cloudRes.ok && cloudRes.teacher) {
          const logged = cloudRes.teacher;
          setState((prev) => ({
            ...prev,
            roleView: 'teacher',
            teacherAccount: logged,
            registeredTeachers: [
              logged,
              ...prev.registeredTeachers.filter((t) => t.teacherCode !== logged.teacherCode),
            ],
          }));
          return cloudRes;
        }
        if (cloudRes.message.includes('Incorrect')) {
          return cloudRes;
        }
      } catch {
        // Fallback to local registered teachers below
      }

      const localMatch = stateRef.current.registeredTeachers.find(
        (t) => normalizeTeacherCode(t.teacherCode) === code
      );
      if (!localMatch) {
        return {
          ok: false,
          message: `No teacher account found for "${code}". Register first to generate a Teacher Code.`,
        };
      }
      if (localMatch.pin !== pin) {
        return {
          ok: false,
          message: 'Incorrect 4-digit Teacher PIN for this Teacher Code.',
        };
      }

      setState((prev) => ({
        ...prev,
        roleView: 'teacher',
        teacherAccount: localMatch,
      }));
      return { ok: true, teacher: localMatch, message: `Signed in as ${localMatch.fullName}` };
    },
    []
  );

  const logoutTeacher = useCallback(() => {
    setState((prev) => ({
      ...prev,
      teacherAccount: null,
    }));
  }, []);

  const pushTeacherAssignment = useCallback(
    (input: {
      classStream: string;
      school?: string;
      grade: number;
      learningArea: LearningArea;
      strand: string;
      subStrand: string;
      isCustom?: boolean;
      customTitle?: string;
      instructions?: string;
      attachmentName?: string;
      attachmentUrl?: string;
      attachmentMimeType?: string;
      attachmentSizeBytes?: number;
      customQuestions?: ActivityQuestion[];
    }) => {
      const activeTeacher = stateRef.current.teacherAccount;
      const targetSchool =
        input.school?.trim() || activeTeacher?.school || 'Nairobi Primary School';
      const matchingLesson =
        CBC_LESSONS.find(
          (l) => l.learningArea === input.learningArea && l.grade === input.grade
        ) ??
        CBC_LESSONS.find(
          (l) =>
            l.learningArea === input.learningArea &&
            (l.strand === input.strand || l.grade === input.grade)
        ) ??
        CBC_LESSONS.find((l) => l.learningArea === input.learningArea) ??
        CBC_LESSONS[0];

      const assignId = `assign-${Date.now()}`;
      const hasCustomQuestions =
        Array.isArray(input.customQuestions) && input.customQuestions.length > 0;
      const isCustom =
        Boolean(input.isCustom) ||
        hasCustomQuestions ||
        Boolean(input.attachmentUrl || input.attachmentName);

      const newAssignment: TeacherAssignment = {
        id: assignId,
        classStream: input.classStream,
        school: targetSchool,
        grade: input.grade,
        learningArea: input.learningArea,
        strand: input.strand,
        subStrand: input.subStrand,
        lessonId: isCustom ? `custom-${assignId}` : matchingLesson.id,
        title: isCustom
          ? input.customTitle?.trim() || `${input.subStrand} · Custom Assignment`
          : `${input.subStrand} (${matchingLesson.title})`,
        isCustom,
        instructions: input.instructions?.trim() || undefined,
        attachmentName: input.attachmentName?.trim() || undefined,
        attachmentUrl: input.attachmentUrl?.trim() || undefined,
        attachmentMimeType: input.attachmentMimeType?.trim() || undefined,
        attachmentSizeBytes: input.attachmentSizeBytes,
        customQuestions: hasCustomQuestions ? input.customQuestions : undefined,
        teacherName: activeTeacher?.fullName || 'Class Teacher',
        teacherCode: activeTeacher?.teacherCode,
        dueLabel: isCustom ? 'Custom Teacher Task · Priority' : 'Priority · Before Screen Time',
        pushedAt: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
        completedByChildIds: [],
      };

      setState((prev) => ({
        ...prev,
        assignments: [newAssignment, ...prev.assignments],
      }));

      void publishSchoolGradeAssignmentToCloud(newAssignment, targetSchool).catch(() => {});
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
    fresh.children.forEach((child) => {
      void publishPairingToCloud(child, fresh.parentAccount.pin).catch(() => {});
    });
  }, []);

  return {
    roleView: state.roleView,
    selectedChildId: state.selectedChildId,
    selectedChild,
    handOffActive: state.handOffActive,
    parentAccount: state.parentAccount,
    teacherAccount: state.teacherAccount,
    registeredTeachers: state.registeredTeachers,
    children: state.children,
    assignments: state.assignments,
    classRoster: state.classRoster,
    displayedKeysByChild: state.displayedKeysByChild,
    displayedQuizIdsByChild: state.displayedQuizIdsByChild,
    cloudSyncOk,
    cloudSyncMessage,
    retryCloudSyncNow,
    setRoleView,
    setSelectedChildId,
    updateParentPin,
    activateChildLicense,
    activateAllUnpaidLicenses,
    updateChildSettings,
    regeneratePairingCode,
    registerParentAndChild,
    pairLearnerByCode,
    addChildProfile,
    activateHandOffMode,
    exitHandOffToParent,
    simulateTimeExpired,
    unlockWithParentPin,
    selectActiveApp,
    recordDisplayedAssignment,
    completeLesson,
    resolveApproval,
    registerTeacher,
    loginTeacher,
    logoutTeacher,
    pushTeacherAssignment,
    resetDemoData,
  };
}
