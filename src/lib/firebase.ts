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
import type { ChildProfile, DeviceMode, LearningArea } from '../types/somatime';

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
