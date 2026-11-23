export type RoleView = 'parent' | 'child' | 'teacher';

export type DeviceMode = 'own_phone' | 'hand_off';

export type CBCRubric = 'EE' | 'ME' | 'AE' | 'BE';

export type CBCLevelGroup =
  | 'Lower Primary'
  | 'Upper Primary'
  | 'Junior School'
  | 'Senior School';

export type LearningArea =
  | 'Mathematics'
  | 'English'
  | 'Kiswahili'
  | 'Environmental Activities'
  | 'Science & Technology'
  | 'Integrated Science'
  | 'Social Studies'
  | 'Religious Education'
  | 'Agriculture & Nutrition'
  | 'Creative Arts'
  | 'Pre-Technical Studies'
  | 'Business Studies'
  | 'Computer Studies'
  | 'Biology'
  | 'Chemistry'
  | 'Physics'
  | 'History & Citizenship'
  | 'Geography';

export interface CbcSubStrandNode {
  subStrand: string;
  minGrade: number;
  maxGrade: number;
  pathway?: string;
  linkedLessonId: string;
}

export interface CbcStrandNode {
  strand: string;
  subStrands: CbcSubStrandNode[];
}

export interface CbcSubjectCurriculum {
  learningArea: LearningArea;
  minGrade: number;
  maxGrade: number;
  pathway?: string;
  strands: CbcStrandNode[];
}

export interface CbcCatalogEntry {
  grade: number;
  levelGroup: CBCLevelGroup;
  pathway?: string;
  learningArea: LearningArea;
  strand: string;
  subStrand: string;
  linkedLessonId: string;
}

export interface PendingApproval {
  id: string;
  lessonTitle: string;
  learningArea: LearningArea;
  minutesEarned: number;
  scorePercent: number;
  completedAt: string;
}

export interface ChildActivityLog {
  id: string;
  lessonId: string;
  lessonTitle: string;
  learningArea: LearningArea;
  scorePercent: number;
  rubricEarned: CBCRubric;
  minutesEarned: number;
  status: 'approved' | 'pending' | 'declined';
  timestamp: string;
}

export interface ParentAccount {
  name: string;
  contact: string;
  pin: string;
  registered: boolean;
}

export interface ChildProfile {
  id: string;
  name: string;
  grade: number; // 1 to 12
  school: string;
  classStream: string;
  avatarUrl: string;
  deviceMode: DeviceMode;
  pairingCode: string;
  isPaired: boolean;
  backgroundGuardActive: boolean;
  dailyCapMins: number;
  exchangeRateMins: number;
  mandatorySubjects: LearningArea[];
  requireParentApproval: boolean;
  availableScreenSeconds: number;
  usedScreenTodayMins: number;
  learnedMinutesToday: number;
  isLocked: boolean;
  activeAppName: string | null;
  lastInterruptedAppName?: string | null;
  completedLessonIds: string[];
  pendingApprovals: PendingApproval[];
  activityLogs: ChildActivityLog[];
  rubricByArea: Partial<Record<LearningArea, CBCRubric>>;
  streakDays: number;
  licenseActive: boolean;
  licensePlanLabel: string;
  licenseExpiresAt: string | null;
  licensePaymentRef: string | null;
  linkedTeacherCodes: string[];
  linkedTeachers?: LinkedTeacherSummary[];
}

export interface LinkedTeacherSummary {
  teacherCode: string;
  fullName: string;
  school: string;
  tscNumber?: string;
  grade: number;
  classStream: string;
  subjects: LearningArea[];
}

export interface MatchingPair {
  id: string;
  left: string;
  right: string;
}

export interface ActivityQuestion {
  id: string;
  type: 'multiple_choice' | 'matching' | 'fill_blank';
  prompt: string;
  bilingualHint: string;
  visualBadge?: string;
  visualFraction?: {
    numerator: number;
    denominator: number;
    label: string;
  };
  options?: string[];
  correctAnswer?: string;
  acceptableAnswers?: string[];
  blankPrefix?: string;
  blankSuffix?: string;
  pairs?: MatchingPair[];
  explanation: string;
}

export interface CBCLesson {
  id: string;
  assignmentId?: string;
  isCustom?: boolean;
  title: string;
  swahiliTitle: string;
  grade: number;
  levelGroup: CBCLevelGroup;
  pathway?: string;
  learningArea: LearningArea;
  strand: string;
  subStrand: string;
  durationMins: number;
  summary: string;
  instructions?: string;
  attachmentName?: string;
  attachmentUrl?: string;
  attachmentMimeType?: string;
  attachmentSizeBytes?: number;
  questions: ActivityQuestion[];
}

export interface TeacherAccount {
  teacherCode: string;
  fullName: string;
  school: string;
  tscNumber: string;
  pin: string;
  subjects: LearningArea[];
  grade: number;
  classStream: string;
  registered: boolean;
}

export interface TeacherAssignment {
  id: string;
  classStream: string;
  school?: string;
  grade: number;
  learningArea: LearningArea;
  strand: string;
  subStrand: string;
  lessonId: string;
  title: string;
  isCustom?: boolean;
  instructions?: string;
  attachmentName?: string;
  attachmentUrl?: string;
  attachmentMimeType?: string;
  attachmentSizeBytes?: number;
  customQuestions?: ActivityQuestion[];
  teacherName?: string;
  teacherCode?: string;
  dueLabel: string;
  pushedAt: string;
  completedByChildIds: string[];
}

export interface ClassStudentRecord {
  id: string;
  name: string;
  admissionNo: string;
  classStream: string;
  rubric: CBCRubric;
  scorePercent: number;
  learningMins: number;
  assignmentDone: boolean;
}
