import { initializeApp } from 'firebase/app';
import {
  GoogleAuthProvider,
  getAuth,
  signInWithPopup,
  signOut,
  type User,
} from 'firebase/auth';
import {
  doc,
  getDocFromServer,
  getFirestore,
  onSnapshot,
  setDoc,
  updateDoc,
  type Unsubscribe,
} from 'firebase/firestore';
import firebaseConfig from '../../firebase-applet-config.json';
import type {
  ActivityQuestion,
  ChildProfile,
  DeviceMode,
  LearningArea,
  TeacherAccount,
  TeacherAssignment,
} from '../types/somatime';

const app = initializeApp(firebaseConfig);
export const db = getFirestore(app, firebaseConfig.firestoreDatabaseId);
export const auth = getAuth(app);
const googleProvider = new GoogleAuthProvider();

export enum OperationType {
  CREATE = 'create',
  UPDATE = 'update',
  DELETE = 'delete',
  LIST = 'list',
  GET = 'get',
  WRITE = 'write',
}

export interface FirestoreErrorInfo {
  error: string;
  operationType: OperationType;
  path: string | null;
  authInfo: {
    userId?: string | null;
    email?: string | null;
    emailVerified?: boolean | null;
    isAnonymous?: boolean | null;
    tenantId?: string | null;
    providerInfo?: {
      providerId?: string | null;
      email?: string | null;
    }[];
  };
}

export function handleFirestoreError(
  error: unknown,
  operationType: OperationType,
  path: string | null
): never {
  const errInfo: FirestoreErrorInfo = {
    error: error instanceof Error ? error.message : String(error),
    authInfo: {
      userId: auth.currentUser?.uid,
      email: auth.currentUser?.email,
      emailVerified: auth.currentUser?.emailVerified,
      isAnonymous: auth.currentUser?.isAnonymous,
      tenantId: auth.currentUser?.tenantId,
      providerInfo:
        auth.currentUser?.providerData?.map((provider) => ({
          providerId: provider.providerId,
          email: provider.email,
        })) || [],
    },
    operationType,
    path,
  };
  console.error('Firestore Error:', JSON.stringify(errInfo));
  throw new Error(JSON.stringify(errInfo));
}

// Validate Firestore connectivity on startup
async function testConnection() {
  try {
    await getDocFromServer(doc(db, 'test', 'connection'));
  } catch (error) {
    if (error instanceof Error && error.message.includes('the client is offline')) {
      console.error('Please check your Firebase configuration: client is offline.');
    }
  }
}
void testConnection();

export interface CloudPairingDoc {
  pairingCode: string;
  childId: string;
  childName: string;
  grade: number;
  school: string;
  classStream?: string;
  deviceMode: DeviceMode;
  parentPin: string;
  dailyCapMins: number;
  exchangeRateMins: number;
  mandatorySubjects: LearningArea[];
  requireParentApproval: boolean;
  availableScreenSeconds: number;
  usedScreenTodayMins: number;
  learnedMinutesToday: number;
  isLocked: boolean;
  isPaired: boolean;
  ownerUid: string;
  completedLessonIds: string[];
  licenseActive?: boolean;
  licenseExpiresAt?: string;
  licensePaymentRef?: string;
  updatedAt: string;
}

function sanitizeId(raw: string, fallback: string): string {
  const cleaned = raw.replace(/[^a-zA-Z0-9_-]/g, '-').slice(0, 80);
  return cleaned.length >= 3 ? cleaned : fallback;
}

export function normalizePairingCode(raw: string): string {
  const cleaned = raw.trim().toUpperCase().replace(/[^A-Z0-9-\s]/g, '');
  if (!cleaned) return '';
  const collapsed = cleaned.replace(/\s+/g, '-').replace(/-+/g, '-');
  if (collapsed.startsWith('SOMA-')) {
    return collapsed.slice(0, 24);
  }
  if (collapsed.startsWith('SOMA') && collapsed.length > 4) {
    const rest = collapsed.slice(4).replace(/^-+/, '');
    return `SOMA-${rest}`.slice(0, 24);
  }
  if (/^[A-Z0-9]{4,12}$/.test(collapsed)) {
    return `SOMA-${collapsed}`.slice(0, 24);
  }
  return collapsed.slice(0, 24);
}

export function isValidPairingCodeFormat(code: string): boolean {
  return /^SOMA-[A-Z0-9]{4,12}$/.test(code);
}

function clampInt(val: number, min: number, max: number): number {
  const rounded = Math.round(Number.isFinite(val) ? val : min);
  return Math.max(min, Math.min(max, rounded));
}

function sanitizePin(raw: string): string {
  const digits = raw.replace(/\D/g, '').slice(0, 4);
  return digits.length === 4 ? digits : '1234';
}

function buildPairingPayload(
  child: ChildProfile,
  parentPin: string,
  ownerUid: string
): CloudPairingDoc {
  const code = normalizePairingCode(child.pairingCode);
  return {
    pairingCode: isValidPairingCodeFormat(code) ? code : 'SOMA-1000',
    childId: sanitizeId(child.id, 'child-default'),
    childName: (child.name.trim() || 'Learner').slice(0, 80),
    grade: clampInt(child.grade, 1, 12),
    school: (child.school.trim() || 'Nairobi Primary School').slice(0, 120),
    classStream: (child.classStream.trim() || `Grade ${clampInt(child.grade, 1, 12)} East`).slice(
      0,
      120
    ),
    deviceMode: child.deviceMode === 'hand_off' ? 'hand_off' : 'own_phone',
    parentPin: sanitizePin(parentPin),
    dailyCapMins: clampInt(child.dailyCapMins, 15, 300),
    exchangeRateMins: clampInt(child.exchangeRateMins, 5, 60),
    mandatorySubjects: (child.mandatorySubjects || []).slice(0, 8) as LearningArea[],
    requireParentApproval: Boolean(child.requireParentApproval),
    availableScreenSeconds: clampInt(child.availableScreenSeconds, 0, 28800),
    usedScreenTodayMins: clampInt(child.usedScreenTodayMins, 0, 600),
    learnedMinutesToday: clampInt(child.learnedMinutesToday, 0, 600),
    isLocked: Boolean(child.isLocked),
    isPaired: Boolean(child.isPaired),
    ownerUid: sanitizeId(ownerUid, 'device_parent'),
    completedLessonIds: (child.completedLessonIds || []).slice(0, 50),
    licenseActive: Boolean(child.licenseActive),
    licenseExpiresAt: (child.licenseExpiresAt || '').slice(0, 64),
    licensePaymentRef: (child.licensePaymentRef || '').slice(0, 64),
    updatedAt: new Date().toISOString().slice(0, 64),
  };
}

export async function publishPairingToCloud(
  child: ChildProfile,
  parentPin: string
): Promise<boolean> {
  const code = normalizePairingCode(child.pairingCode);
  if (!isValidPairingCodeFormat(code)) return false;

  const path = `pairings/${code}`;
  const docRef = doc(db, 'pairings', code);
  const uid = auth.currentUser?.uid || 'device_parent';
  const payload = buildPairingPayload(child, parentPin, uid);

  try {
    const existingSnap = await getDocFromServer(docRef);
    if (!existingSnap.exists()) {
      await setDoc(docRef, payload);
    } else {
      await updateDoc(docRef, {
        childName: payload.childName,
        grade: payload.grade,
        school: payload.school,
        classStream: payload.classStream,
        deviceMode: payload.deviceMode,
        parentPin: payload.parentPin,
        dailyCapMins: payload.dailyCapMins,
        exchangeRateMins: payload.exchangeRateMins,
        mandatorySubjects: payload.mandatorySubjects,
        requireParentApproval: payload.requireParentApproval,
        availableScreenSeconds: payload.availableScreenSeconds,
        usedScreenTodayMins: payload.usedScreenTodayMins,
        learnedMinutesToday: payload.learnedMinutesToday,
        isLocked: payload.isLocked,
        isPaired: payload.isPaired,
        completedLessonIds: payload.completedLessonIds,
        licenseActive: payload.licenseActive,
        licenseExpiresAt: payload.licenseExpiresAt,
        licensePaymentRef: payload.licensePaymentRef,
        updatedAt: payload.updatedAt,
      });
    }
    return true;
  } catch (error) {
    handleFirestoreError(error, OperationType.WRITE, path);
  }
}

export async function fetchAndPairFromCloud(
  rawCode: string
): Promise<CloudPairingDoc | null> {
  const code = normalizePairingCode(rawCode);
  if (!isValidPairingCodeFormat(code)) return null;

  const path = `pairings/${code}`;
  const docRef = doc(db, 'pairings', code);

  let snap;
  try {
    snap = await getDocFromServer(docRef);
  } catch (error) {
    handleFirestoreError(error, OperationType.GET, path);
  }

  if (!snap.exists()) return null;
  const data = snap.data() as CloudPairingDoc;

  if (!data.licenseActive) {
    return data;
  }

  try {
    await updateDoc(docRef, {
      isPaired: true,
      deviceMode: 'own_phone',
      updatedAt: new Date().toISOString().slice(0, 64),
    });
  } catch (error) {
    handleFirestoreError(error, OperationType.UPDATE, path);
  }

  return {
    ...data,
    isPaired: true,
    deviceMode: 'own_phone',
  };
}

export async function syncLearnerStateToCloud(child: ChildProfile): Promise<void> {
  const code = normalizePairingCode(child.pairingCode);
  if (!isValidPairingCodeFormat(code)) return;

  const path = `pairings/${code}`;
  const docRef = doc(db, 'pairings', code);
  try {
    await updateDoc(docRef, {
      isPaired: Boolean(child.isPaired),
      deviceMode: child.deviceMode === 'hand_off' ? 'hand_off' : 'own_phone',
      availableScreenSeconds: clampInt(child.availableScreenSeconds, 0, 28800),
      usedScreenTodayMins: clampInt(child.usedScreenTodayMins, 0, 600),
      learnedMinutesToday: clampInt(child.learnedMinutesToday, 0, 600),
      isLocked: Boolean(child.isLocked),
      completedLessonIds: (child.completedLessonIds || []).slice(0, 50),
      updatedAt: new Date().toISOString().slice(0, 64),
    });
  } catch (error) {
    handleFirestoreError(error, OperationType.UPDATE, path);
  }
}

export function subscribePairingSession(
  rawCode: string,
  onData: (data: CloudPairingDoc) => void,
  onError?: (err: Error) => void
): Unsubscribe {
  const code = normalizePairingCode(rawCode);
  const path = `pairings/${code}`;
  const docRef = doc(db, 'pairings', code);

  return onSnapshot(
    docRef,
    (snapshot) => {
      if (snapshot.exists()) {
        onData(snapshot.data() as CloudPairingDoc);
      }
    },
    (error) => {
      if (onError) {
        onError(error);
      } else {
        handleFirestoreError(error, OperationType.GET, path);
      }
    }
  );
}

export async function signInParentWithGoogle(): Promise<User | null> {
  const res = await signInWithPopup(auth, googleProvider);
  const user = res.user;
  if (user && user.emailVerified) {
    const now = new Date().toISOString().slice(0, 64);
    try {
      await setDoc(
        doc(db, 'users', user.uid),
        {
          uid: user.uid,
          displayName: (user.displayName || 'Parent').slice(0, 80),
          role: 'parent',
          updatedAt: now,
        },
        { merge: true }
      );
    } catch (error) {
      handleFirestoreError(error, OperationType.WRITE, `users/${user.uid}`);
    }
  }
  return user;
}

export async function savePrivateParentProfileToCloud(
  user: User,
  phoneContact: string,
  parentPin: string
): Promise<void> {
  if (!user.emailVerified) return;
  const path = `users/${user.uid}/private/profile`;
  try {
    await setDoc(doc(db, 'users', user.uid, 'private', 'profile'), {
      uid: user.uid,
      email: (user.email || '').slice(0, 160),
      phoneContact: phoneContact.trim().slice(0, 40),
      parentPin: sanitizePin(parentPin),
      updatedAt: new Date().toISOString().slice(0, 64),
    });
  } catch (error) {
    handleFirestoreError(error, OperationType.WRITE, path);
  }
}

export async function signOutParent(): Promise<void> {
  await signOut(auth);
}

export function normalizeTeacherCode(raw: string): string {
  const cleaned = raw.trim().toUpperCase().replace(/[^A-Z0-9-\s]/g, '');
  if (!cleaned) return '';
  const collapsed = cleaned.replace(/\s+/g, '-').replace(/-+/g, '-');
  if (collapsed.startsWith('TCH-')) {
    return collapsed.slice(0, 24);
  }
  if (collapsed.startsWith('TCH') && collapsed.length > 3) {
    const rest = collapsed.slice(3).replace(/^-+/, '');
    return `TCH-${rest}`.slice(0, 24);
  }
  if (/^[A-Z0-9]{4,12}$/.test(collapsed)) {
    return `TCH-${collapsed}`.slice(0, 24);
  }
  return collapsed.slice(0, 24);
}

export function isValidTeacherCodeFormat(code: string): boolean {
  return /^TCH-[A-Z0-9]{4,12}$/.test(code);
}

export function buildSchoolGradeKey(school: string, grade: number): string {
  const safeGrade = clampInt(grade, 1, 12);
  const slug = school
    .trim()
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '')
    .slice(0, 100);
  const finalSlug = slug.length >= 2 ? slug : 'nairobi-primary';
  return `g${safeGrade}-${finalSlug}`;
}

export interface CloudTeacherDoc {
  teacherCode: string;
  fullName: string;
  school: string;
  tscNumber?: string;
  teacherPin: string;
  subjects: LearningArea[];
  grade: number;
  classStream: string;
  ownerUid: string;
  updatedAt: string;
}

export async function registerTeacherInCloud(
  teacher: TeacherAccount
): Promise<TeacherAccount> {
  const code = normalizeTeacherCode(teacher.teacherCode);
  if (!isValidTeacherCodeFormat(code)) {
    throw new Error('Teacher code must follow format TCH-XXXX.');
  }

  const path = `teachers/${code}`;
  const docRef = doc(db, 'teachers', code);
  const uid = sanitizeId(auth.currentUser?.uid || 'device_teacher', 'device_teacher');
  const safePin = sanitizePin(teacher.pin);
  const safeSchool = (teacher.school.trim() || 'Nairobi Primary School').slice(0, 120);
  const safeGrade = clampInt(teacher.grade, 1, 12);
  const safeSubjects = (
    teacher.subjects.length > 0 ? teacher.subjects : (['Mathematics'] as LearningArea[])
  ).slice(0, 8) as LearningArea[];

  const payload: CloudTeacherDoc = {
    teacherCode: code,
    fullName: (teacher.fullName.trim() || 'Teacher').slice(0, 80),
    school: safeSchool,
    tscNumber: teacher.tscNumber.trim().slice(0, 40),
    teacherPin: safePin,
    subjects: safeSubjects,
    grade: safeGrade,
    classStream: (teacher.classStream.trim() || `Grade ${safeGrade} East - ${safeSchool}`).slice(
      0,
      120
    ),
    ownerUid: uid,
    updatedAt: new Date().toISOString().slice(0, 64),
  };

  try {
    const existingSnap = await getDocFromServer(docRef);
    if (!existingSnap.exists()) {
      await setDoc(docRef, payload);
    } else {
      await updateDoc(docRef, {
        fullName: payload.fullName,
        school: payload.school,
        tscNumber: payload.tscNumber,
        teacherPin: payload.teacherPin,
        subjects: payload.subjects,
        grade: payload.grade,
        classStream: payload.classStream,
        updatedAt: payload.updatedAt,
      });
    }
    return {
      teacherCode: payload.teacherCode,
      fullName: payload.fullName,
      school: payload.school,
      tscNumber: payload.tscNumber || '',
      pin: payload.teacherPin,
      subjects: payload.subjects,
      grade: payload.grade,
      classStream: payload.classStream,
      registered: true,
    };
  } catch (error) {
    handleFirestoreError(error, OperationType.WRITE, path);
  }
}

export async function loginTeacherFromCloud(
  rawCode: string,
  rawPin: string
): Promise<{ ok: boolean; teacher?: TeacherAccount; message: string }> {
  const code = normalizeTeacherCode(rawCode);
  const pin = rawPin.replace(/\D/g, '').slice(0, 4);
  if (!isValidTeacherCodeFormat(code)) {
    return { ok: false, message: 'Enter a valid Teacher Code like TCH-4821.' };
  }
  if (pin.length !== 4) {
    return { ok: false, message: 'Enter your 4-digit Teacher PIN.' };
  }

  const path = `teachers/${code}`;
  const docRef = doc(db, 'teachers', code);
  let snap;
  try {
    snap = await getDocFromServer(docRef);
  } catch (error) {
    handleFirestoreError(error, OperationType.GET, path);
  }

  if (!snap.exists()) {
    return {
      ok: false,
      message: `No teacher account found for "${code}". Check the code or register as a new teacher.`,
    };
  }

  const data = snap.data() as CloudTeacherDoc;
  if (data.teacherPin !== pin) {
    return { ok: false, message: 'Incorrect 4-digit Teacher PIN for this Teacher Code.' };
  }

  return {
    ok: true,
    message: `Signed in as ${data.fullName}`,
    teacher: {
      teacherCode: data.teacherCode,
      fullName: data.fullName,
      school: data.school,
      tscNumber: data.tscNumber || '',
      pin: data.teacherPin,
      subjects: (data.subjects || ['Mathematics']) as LearningArea[],
      grade: clampInt(data.grade, 1, 12),
      classStream: data.classStream,
      registered: true,
    },
  };
}

function sanitizeCustomQuestions(rawList?: ActivityQuestion[]): Array<Record<string, unknown>> {
  if (!Array.isArray(rawList) || rawList.length === 0) return [];
  return rawList.slice(0, 15).map((q, idx) => ({
    id: (q.id || `tq-${idx + 1}`).slice(0, 64),
    type: q.type === 'fill_blank' ? 'fill_blank' : 'multiple_choice',
    prompt: (q.prompt || '').trim().slice(0, 320),
    bilingualHint: (q.bilingualHint || 'Read carefully before checking.').trim().slice(0, 220),
    options: Array.isArray(q.options)
      ? q.options.map((o) => String(o).trim().slice(0, 120)).filter(Boolean).slice(0, 4)
      : [],
    correctAnswer: (q.correctAnswer || '').trim().slice(0, 140),
    acceptableAnswers: Array.isArray(q.acceptableAnswers)
      ? q.acceptableAnswers.map((a) => String(a).trim().slice(0, 140)).filter(Boolean).slice(0, 6)
      : [],
    blankPrefix: (q.blankPrefix || '').slice(0, 40),
    blankSuffix: (q.blankSuffix || '').slice(0, 40),
    explanation: (q.explanation || '').trim().slice(0, 260),
  }));
}

function isSafeCloudAttachmentUrl(url?: string): boolean {
  if (!url) return false;
  const trimmed = url.trim();
  return trimmed.startsWith('/api/assignments/files/') || trimmed.startsWith('https://');
}

export async function publishSchoolGradeAssignmentToCloud(
  assignment: TeacherAssignment,
  schoolName: string
): Promise<void> {
  const safeSchool = (schoolName.trim() || assignment.school || 'Nairobi Primary School').slice(
    0,
    120
  );
  const safeGrade = clampInt(assignment.grade, 1, 12);
  const schoolGradeKey = buildSchoolGradeKey(safeSchool, safeGrade);
  const path = `school_assignments/${schoolGradeKey}`;
  const docRef = doc(db, 'school_assignments', schoolGradeKey);

  const cloudItem: Record<string, unknown> = {
    id: assignment.id.slice(0, 80),
    classStream: assignment.classStream.slice(0, 120),
    school: safeSchool,
    grade: safeGrade,
    learningArea: assignment.learningArea,
    strand: assignment.strand.slice(0, 120),
    subStrand: assignment.subStrand.slice(0, 120),
    lessonId: assignment.lessonId.slice(0, 80),
    title: assignment.title.slice(0, 140),
    isCustom: Boolean(assignment.isCustom),
    instructions: (assignment.instructions || '').trim().slice(0, 600),
    attachmentName: (assignment.attachmentName || '').trim().slice(0, 120),
    attachmentUrl: isSafeCloudAttachmentUrl(assignment.attachmentUrl)
      ? assignment.attachmentUrl!.trim().slice(0, 300)
      : '',
    attachmentMimeType: (assignment.attachmentMimeType || '').trim().slice(0, 100),
    attachmentSizeBytes: clampInt(assignment.attachmentSizeBytes || 0, 0, 10_000_000),
    customQuestions: sanitizeCustomQuestions(assignment.customQuestions),
    teacherName: (assignment.teacherName || 'Teacher').slice(0, 80),
    teacherCode: (assignment.teacherCode || 'TCH-0000').slice(0, 24),
    dueLabel: assignment.dueLabel.slice(0, 80),
    pushedAt: assignment.pushedAt.slice(0, 40),
  };

  try {
    const existingSnap = await getDocFromServer(docRef);
    const prevItems = existingSnap.exists()
      ? ((existingSnap.data().recentAssignments as Array<Record<string, unknown>>) || [])
      : [];
    const nextItems = [
      cloudItem,
      ...prevItems.filter((item) => item.id !== cloudItem.id),
    ].slice(0, 12);

    const payload = {
      schoolGradeKey,
      school: safeSchool,
      grade: safeGrade,
      recentAssignments: nextItems,
      updatedAt: new Date().toISOString().slice(0, 64),
    };

    if (!existingSnap.exists()) {
      await setDoc(docRef, payload);
    } else {
      await updateDoc(docRef, {
        school: payload.school,
        recentAssignments: payload.recentAssignments,
        updatedAt: payload.updatedAt,
      });
    }
  } catch (error) {
    handleFirestoreError(error, OperationType.WRITE, path);
  }
}

export function subscribeSchoolGradeAssignments(
  school: string,
  grade: number,
  onData: (assignments: TeacherAssignment[]) => void,
  onError?: (err: Error) => void
): Unsubscribe {
  const key = buildSchoolGradeKey(school, grade);
  const path = `school_assignments/${key}`;
  const docRef = doc(db, 'school_assignments', key);

  return onSnapshot(
    docRef,
    (snapshot) => {
      if (!snapshot.exists()) return;
      const rawList =
        (snapshot.data().recentAssignments as Array<Record<string, unknown>>) || [];
      const mapped: TeacherAssignment[] = rawList.map((item) => {
        const rawQuestions = Array.isArray(item.customQuestions) ? item.customQuestions : [];
        const customQuestions: ActivityQuestion[] = rawQuestions
          .filter((q): q is Record<string, unknown> => Boolean(q && typeof q === 'object'))
          .map((q, idx) => ({
            id: String(q.id || `tq-${idx + 1}`),
            type: (q.type === 'fill_blank' ? 'fill_blank' : 'multiple_choice') as ActivityQuestion['type'],
            prompt: String(q.prompt || ''),
            bilingualHint: String(q.bilingualHint || 'Read carefully before answering.'),
            options: Array.isArray(q.options) ? q.options.map((o) => String(o)) : undefined,
            correctAnswer: q.correctAnswer ? String(q.correctAnswer) : undefined,
            acceptableAnswers: Array.isArray(q.acceptableAnswers)
              ? q.acceptableAnswers.map((a) => String(a))
              : undefined,
            blankPrefix: q.blankPrefix ? String(q.blankPrefix) : undefined,
            blankSuffix: q.blankSuffix ? String(q.blankSuffix) : undefined,
            explanation: String(q.explanation || 'Well done!'),
          }))
          .filter((q) => q.prompt.length > 0);

        const rawUrl = item.attachmentUrl ? String(item.attachmentUrl) : '';
        return {
          id: String(item.id || `assign-${Date.now()}`),
          classStream: String(item.classStream || `Grade ${grade} East`),
          school: String(item.school || school),
          grade: Number(item.grade || grade),
          learningArea: String(item.learningArea || 'Mathematics') as LearningArea,
          strand: String(item.strand || 'Numbers'),
          subStrand: String(item.subStrand || 'Core Strand'),
          lessonId: String(item.lessonId || 'g4-math-fractions'),
          title: String(item.title || 'Teacher Priority Assignment'),
          isCustom: Boolean(item.isCustom) || customQuestions.length > 0,
          instructions: item.instructions ? String(item.instructions) : undefined,
          attachmentName: item.attachmentName ? String(item.attachmentName) : undefined,
          attachmentUrl: isSafeCloudAttachmentUrl(rawUrl) ? rawUrl : undefined,
          attachmentMimeType: item.attachmentMimeType ? String(item.attachmentMimeType) : undefined,
          attachmentSizeBytes:
            typeof item.attachmentSizeBytes === 'number' ? item.attachmentSizeBytes : undefined,
          customQuestions: customQuestions.length > 0 ? customQuestions : undefined,
          teacherName: item.teacherName ? String(item.teacherName) : undefined,
          teacherCode: item.teacherCode ? String(item.teacherCode) : undefined,
          dueLabel: String(item.dueLabel || 'Priority · Before Screen Time'),
          pushedAt: String(item.pushedAt || 'Today'),
          completedByChildIds: [],
        };
      });
      onData(mapped);
    },
    (error) => {
      if (onError) {
        onError(error);
      } else {
        handleFirestoreError(error, OperationType.GET, path);
      }
    }
  );
}
