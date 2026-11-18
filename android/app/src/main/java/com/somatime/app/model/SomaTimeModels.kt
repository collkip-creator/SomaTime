package com.somatime.app.model

enum class RoleView {
    PARENT,
    CHILD,
    TEACHER
}

enum class DeviceMode {
    OWN_PHONE,
    HAND_OFF
}

enum class CBCRubric(val label: String, val shortLabel: String) {
    EE("Exceeding Expectations", "Exceeding"),
    ME("Meeting Expectations", "Meeting"),
    AE("Approaching Expectations", "Approaching"),
    BE("Below Expectations", "Below")
}

enum class LearningArea(val displayName: String) {
    MATHEMATICS("Mathematics"),
    ENGLISH("English"),
    KISWAHILI("Kiswahili"),
    SCIENCE_TECH("Science & Technology"),
    INTEGRATED_SCIENCE("Integrated Science"),
    SOCIAL_STUDIES("Social Studies"),
    AGRICULTURE_NUTRITION("Agriculture & Nutrition"),
    CREATIVE_ARTS("Creative Arts")
}

enum class QuestionType {
    MULTIPLE_CHOICE,
    MATCHING,
    FILL_BLANK
}

data class MatchingPair(
    val left: String,
    val right: String
)

data class QuizQuestion(
    val id: String,
    val type: QuestionType,
    val prompt: String,
    val hint: String,
    val options: List<String> = emptyList(),
    val correctAnswer: String = "",
    val acceptableAnswers: List<String> = emptyList(),
    val blankPrefix: String = "",
    val blankSuffix: String = "",
    val pairs: List<MatchingPair> = emptyList(),
    val explanation: String
)

data class CBCLesson(
    val id: String,
    val title: String,
    val grade: Int,
    val levelGroup: String,
    val learningArea: LearningArea,
    val strand: String,
    val subStrand: String,
    val durationMins: Int,
    val summary: String,
    val questions: List<QuizQuestion>
)

data class CbcCatalogEntry(
    val grade: Int,
    val levelGroup: String,
    val pathway: String?,
    val learningArea: LearningArea,
    val strand: String,
    val subStrand: String,
    val linkedLessonId: String
)

data class PendingApproval(
    val id: String,
    val lessonTitle: String,
    val learningArea: LearningArea,
    val minutesEarned: Int,
    val scorePercent: Int,
    val completedAt: String
)

data class ChildActivityLog(
    val id: String,
    val lessonId: String,
    val lessonTitle: String,
    val learningArea: LearningArea,
    val scorePercent: Int,
    val rubricEarned: CBCRubric,
    val minutesEarned: Int,
    val status: String,
    val timestamp: String
)

data class ChildProfile(
    val id: String,
    val name: String,
    val grade: Int,
    val school: String,
    val classStream: String,
    val deviceMode: DeviceMode,
    val dailyCapMins: Int,
    val exchangeRateMins: Int,
    val mandatorySubjects: List<LearningArea>,
    val requireParentApproval: Boolean,
    val availableScreenSeconds: Int,
    val usedScreenTodayMins: Int,
    val learnedMinutesToday: Int,
    val isLocked: Boolean,
    val completedLessonIds: List<String>,
    val pendingApprovals: List<PendingApproval>,
    val activityLogs: List<ChildActivityLog>,
    val rubricByArea: Map<LearningArea, CBCRubric>,
    val streakDays: Int
)

data class TeacherAssignment(
    val id: String,
    val classStream: String,
    val grade: Int,
    val learningArea: LearningArea,
    val strand: String,
    val subStrand: String,
    val lessonId: String,
    val title: String,
    val dueLabel: String,
    val pushedAt: String,
    val completedByChildIds: List<String>
)

data class ClassStudentRecord(
    val id: String,
    val name: String,
    val admissionNo: String,
    val classStream: String,
    val rubric: CBCRubric,
    val scorePercent: Int,
    val learningMins: Int,
    val assignmentDone: Boolean
)
