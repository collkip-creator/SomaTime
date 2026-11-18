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
  | 'Science & Technology'
  | 'Integrated Science'
  | 'Social Studies'
  | 'Agriculture & Nutrition'
  | 'Creative Arts';

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
  completedLessonIds: string[];
  pendingApprovals: PendingApproval[];
  activityLogs: ChildActivityLog[];
  rubricByArea: Partial<Record<LearningArea, CBCRubric>>;
  streakDays: number;
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
  questions: ActivityQuestion[];
}

export interface TeacherAssignment {
  id: string;
  classStream: string;
  grade: number;
  learningArea: LearningArea;
  strand: string;
  subStrand: string;
  lessonId: string;
  title: string;
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
