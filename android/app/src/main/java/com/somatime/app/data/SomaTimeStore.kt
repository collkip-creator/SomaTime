package com.somatime.app.data

import android.content.Context
import android.content.SharedPreferences
import android.os.SystemClock
import com.somatime.app.guard.UsageAccessHelper
import com.somatime.app.model.CBCLesson
import com.somatime.app.model.CBCRubric
import com.somatime.app.model.ChildActivityLog
import com.somatime.app.model.ChildProfile
import com.somatime.app.model.ClassStudentRecord
import com.somatime.app.model.DeviceMode
import com.somatime.app.model.LearningArea
import com.somatime.app.model.LinkedTeacherSummary
import com.somatime.app.model.PendingApproval
import com.somatime.app.model.QuestionType
import com.somatime.app.model.QuizQuestion
import com.somatime.app.model.RoleView
import com.somatime.app.model.TeacherAccount
import com.somatime.app.model.TeacherAssignment
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

data class LessonCompletionResult(
    val minutesEarned: Int,
    val rubric: CBCRubric,
    val queuedForApproval: Boolean,
    val cappedOut: Boolean
)

data class PushAssignmentRequest(
    val classStream: String,
    val school: String,
    val grade: Int,
    val learningArea: LearningArea,
    val strand: String,
    val subStrand: String,
    val isCustom: Boolean = false,
    val customTitle: String = "",
    val instructions: String = "",
    val attachmentName: String = "",
    val attachmentUrl: String = "",
    val attachmentMimeType: String = "",
    val attachmentSizeBytes: Long = 0L,
    val customQuestions: List<QuizQuestion> = emptyList()
)

data class PairingResult(
    val success: Boolean,
    val child: ChildProfile? = null,
    val message: String
)

data class TeacherLoginResult(
    val success: Boolean,
    val teacher: TeacherAccount? = null,
    val message: String
)

data class TeacherVerifyResult(
    val ok: Boolean,
    val teacher: LinkedTeacherSummary? = null,
    val message: String
)

enum class SetupWizardTab {
    CHOICE,
    PARENT,
    LEARNER,
    TEACHER
}

data class SomaTimeUiState(
    val roleView: RoleView = RoleView.PARENT,
    val selectedChildId: String = "",
    val handOffActive: Boolean = false,
    val demoSeeded: Boolean = false,
    val parentName: String = "",
    val parentContact: String = "",
    val parentPin: String = "",
    val teacherAccount: TeacherAccount? = null,
    val registeredTeachers: List<TeacherAccount> = emptyList(),
    val showSetupWizard: Boolean = false,
    val setupInitialTab: SetupWizardTab = SetupWizardTab.CHOICE,
    val guardServiceRunning: Boolean = false,
    val cloudSyncOk: Boolean = true,
    val cloudSyncMessage: String = "Fresh start ready · Register to sync",
    val lastDetectedForegroundApp: String? = null,
    val lastInterruptedAppPackage: String? = null,
    val displayedKeysByChild: Map<String, List<String>> = emptyMap(),
    val displayedQuizIdsByChild: Map<String, List<String>> = emptyMap(),
    val children: List<ChildProfile> = emptyList(),
    val assignments: List<TeacherAssignment> = emptyList(),
    val classRoster: List<ClassStudentRecord> = emptyList()
) {
    val selectedChild: ChildProfile?
        get() = children.firstOrNull { it.id == selectedChildId } ?: children.firstOrNull()

    val isParentRegistered: Boolean
        get() = parentName.isNotBlank() && parentPin.length == 4

    val isChildLockEnforced: Boolean
        get() {
            val child = selectedChild ?: return false
            return handOffActive || (child.isPaired && child.deviceMode == DeviceMode.OWN_PHONE)
        }

    val isTakeoverLocked: Boolean
        get() {
            val child = selectedChild ?: return false
            return roleView == RoleView.CHILD &&
                !showSetupWizard &&
                isChildLockEnforced &&
                (child.isLocked || child.availableScreenSeconds <= 0)
        }
}

class SomaTimeStore private constructor(context: Context) {

    private val appContext: Context = context.applicationContext
    private val prefs: SharedPreferences =
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var tickerJob: Job? = null

    @Volatile
    private var guardSuppressedUntilMs: Long = 0L

    @Volatile
    private var mainActivityForeground: Boolean = false

    private val _state = MutableStateFlow(loadInitialState())
    val state: StateFlow<SomaTimeUiState> = _state.asStateFlow()

    fun setMainActivityForeground(foreground: Boolean) {
        mainActivityForeground = foreground
    }

    fun isMainActivityForeground(): Boolean = mainActivityForeground

    fun suppressGuardBriefly(durationMs: Long = 2500L) {
        guardSuppressedUntilMs = System.currentTimeMillis() + durationMs
    }

    fun isGuardSuppressed(): Boolean = System.currentTimeMillis() < guardSuppressedUntilMs

    init {
        startCountdownTicker()
        publishExistingChildrenIfParent()
    }

    private fun publishExistingChildrenIfParent() {
        val current = _state.value
        if (current.roleView != RoleView.PARENT) return
        if (current.children.isEmpty()) {
            _state.update {
                it.copy(
                    cloudSyncOk = true,
                    cloudSyncMessage = "Fresh start ready · Register parent & child to sync to Cloud"
                )
            }
            return
        }
        val safePin = current.parentPin.ifEmpty { CbcCurriculumRepository.PARENT_PIN }
        scope.launch {
            var allOk = true
            var lastErr = ""
            current.children.forEach { child ->
                val res = FirestoreSyncClient.publishPairingSession(child, safePin)
                if (!res.ok) {
                    allOk = false
                    lastErr = res.errorMessage
                }
            }
            _state.update {
                it.copy(
                    cloudSyncOk = allOk,
                    cloudSyncMessage = if (allOk) {
                        "Synced to Cloud ✓ · Ready for learner phone"
                    } else {
                        lastErr.ifEmpty { "Cloud sync pending — check internet and tap Retry Sync" }
                    }
                )
            }
        }
    }

    private fun loadInitialState(): SomaTimeUiState {
        // Clear legacy v2 prefs once so all old test accounts/data are wiped on upgrade
        runCatching {
            appContext.getSharedPreferences(LEGACY_PREFS_V2, Context.MODE_PRIVATE).edit().clear().apply()
        }

        val demoSeeded = prefs.getBoolean("demo_seeded", false)
        val baseChildren = if (demoSeeded) CbcCurriculumRepository.initialChildren() else emptyList()
        val baseAssignments = if (demoSeeded) CbcCurriculumRepository.initialAssignments() else emptyList()
        val baseRoster = if (demoSeeded) CbcCurriculumRepository.initialClassRoster() else emptyList()

        val base = SomaTimeUiState(
            demoSeeded = demoSeeded,
            selectedChildId = baseChildren.firstOrNull()?.id ?: "",
            parentName = if (demoSeeded) "Grace Wanjiku" else "",
            parentContact = if (demoSeeded) "0712 345 678" else "",
            parentPin = if (demoSeeded) CbcCurriculumRepository.PARENT_PIN else "",
            children = baseChildren,
            assignments = baseAssignments,
            classRoster = baseRoster
        )

        val savedChildId = prefs.getString("selected_child_id", null) ?: base.selectedChildId
        val savedRole = prefs.getString("role_view", null)?.let {
            runCatching { RoleView.valueOf(it) }.getOrNull()
        } ?: base.roleView
        val handOff = prefs.getBoolean("hand_off_active", false)
        val parentName = prefs.getString("parent_name", base.parentName) ?: base.parentName
        val parentContact = prefs.getString("parent_contact", base.parentContact) ?: base.parentContact
        val parentPin = prefs.getString("parent_pin", base.parentPin) ?: base.parentPin

        val customIds = prefs.getStringSet("custom_child_ids", emptySet()) ?: emptySet()
        val baseIds = baseChildren.map { it.id }.toSet()

        val restoredBase = baseChildren.map { child ->
            val prefix = "child_${child.id}_"
            if (!prefs.contains("${prefix}secs")) return@map child
            val savedCodesJson = prefs.getString("${prefix}linked_teacher_codes_json", null)
            val savedTeachersJson = prefs.getString("${prefix}linked_teachers_json", null)
            child.copy(
                availableScreenSeconds = prefs.getInt("${prefix}secs", child.availableScreenSeconds),
                usedScreenTodayMins = prefs.getInt("${prefix}used", child.usedScreenTodayMins),
                learnedMinutesToday = prefs.getInt("${prefix}learned", child.learnedMinutesToday),
                dailyCapMins = prefs.getInt("${prefix}cap", child.dailyCapMins),
                exchangeRateMins = prefs.getInt("${prefix}rate", child.exchangeRateMins),
                requireParentApproval = prefs.getBoolean("${prefix}approval", child.requireParentApproval),
                isLocked = prefs.getBoolean("${prefix}locked", child.isLocked),
                isPaired = prefs.getBoolean("${prefix}paired", child.isPaired),
                pairingCode = prefs.getString("${prefix}code", child.pairingCode) ?: child.pairingCode,
                licenseActive = prefs.getBoolean("${prefix}license_active", child.licenseActive),
                licensePlanLabel = CbcCurriculumRepository.MONTHLY_LICENSE_PLAN_LABEL,
                licenseExpiresAt = prefs.getString("${prefix}license_expires_at", child.licenseExpiresAt),
                licensePaymentRef = prefs.getString("${prefix}license_payment_ref", child.licensePaymentRef),
                linkedTeacherCodes = if (savedCodesJson != null) decodeStringList(savedCodesJson) else child.linkedTeacherCodes,
                linkedTeachers = if (savedTeachersJson != null) decodeLinkedTeachers(savedTeachersJson) else child.linkedTeachers
            )
        }

        val restoredCustom = customIds.filter { it !in baseIds }.mapNotNull { id ->
            val prefix = "child_${id}_"
            val name = prefs.getString("${prefix}name", null) ?: return@mapNotNull null
            val grade = prefs.getInt("${prefix}grade", 5)
            val school = prefs.getString("${prefix}school", "Nairobi Primary School") ?: "Nairobi Primary School"
            val mandatory = if (grade >= 7) {
                listOf(LearningArea.MATHEMATICS, LearningArea.INTEGRATED_SCIENCE)
            } else {
                listOf(LearningArea.MATHEMATICS, LearningArea.KISWAHILI)
            }
            val savedCodes = decodeStringList(prefs.getString("${prefix}linked_teacher_codes_json", null))
            val savedTeachers = decodeLinkedTeachers(prefs.getString("${prefix}linked_teachers_json", null))
            ChildProfile(
                id = id,
                name = name,
                grade = grade,
                school = school,
                classStream = "Grade $grade East",
                deviceMode = DeviceMode.OWN_PHONE,
                pairingCode = prefs.getString("${prefix}code", CbcCurriculumRepository.generatePairingCode())
                    ?: CbcCurriculumRepository.generatePairingCode(),
                isPaired = prefs.getBoolean("${prefix}paired", false),
                dailyCapMins = prefs.getInt("${prefix}cap", 90),
                exchangeRateMins = prefs.getInt("${prefix}rate", 15),
                mandatorySubjects = mandatory,
                requireParentApproval = prefs.getBoolean("${prefix}approval", false),
                availableScreenSeconds = prefs.getInt("${prefix}secs", 300),
                usedScreenTodayMins = prefs.getInt("${prefix}used", 0),
                learnedMinutesToday = prefs.getInt("${prefix}learned", 0),
                isLocked = prefs.getBoolean("${prefix}locked", false),
                completedLessonIds = emptyList(),
                pendingApprovals = emptyList(),
                activityLogs = emptyList(),
                rubricByArea = mapOf(LearningArea.MATHEMATICS to CBCRubric.ME),
                streakDays = 1,
                licenseActive = prefs.getBoolean("${prefix}license_active", false),
                licensePlanLabel = CbcCurriculumRepository.MONTHLY_LICENSE_PLAN_LABEL,
                licenseExpiresAt = prefs.getString("${prefix}license_expires_at", null),
                licensePaymentRef = prefs.getString("${prefix}license_payment_ref", null),
                linkedTeacherCodes = savedCodes,
                linkedTeachers = savedTeachers
            )
        }

        val allChildren = restoredCustom + restoredBase
        val registeredTeachers = decodeTeachers(prefs.getString("registered_teachers_json", null))
        val activeTeacherCode = prefs.getString("active_teacher_code", null)
        val activeTeacher = registeredTeachers.firstOrNull { it.teacherCode == activeTeacherCode }
        val lastInterrupted = prefs.getString("last_interrupted_app_package", null)
        val displayedKeys = decodeStringListMap(prefs.getString("displayed_keys_json", null))
        val displayedQuizzes = decodeStringListMap(prefs.getString("displayed_quiz_ids_json", null))
        val savedAssignments = decodeAssignments(prefs.getString("custom_assignments_json", null))
        val savedIds = savedAssignments.map { it.id }.toSet()
        val mergedAssignments = savedAssignments + baseAssignments.filterNot { it.id in savedIds }

        return base.copy(
            roleView = savedRole,
            selectedChildId = savedChildId.ifEmpty { allChildren.firstOrNull()?.id ?: "" },
            handOffActive = handOff,
            parentName = parentName,
            parentContact = parentContact,
            parentPin = parentPin,
            teacherAccount = activeTeacher,
            registeredTeachers = registeredTeachers,
            lastInterruptedAppPackage = lastInterrupted,
            displayedKeysByChild = displayedKeys,
            displayedQuizIdsByChild = displayedQuizzes,
            children = allChildren,
            assignments = mergedAssignments
        )
    }

    private fun decodeStringListMap(rawJson: String?): Map<String, List<String>> {
        if (rawJson.isNullOrBlank()) return emptyMap()
        return runCatching {
            val obj = JSONObject(rawJson)
            buildMap {
                val keys = obj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    val arr = obj.optJSONArray(k) ?: continue
                    val list = buildList {
                        for (i in 0 until arr.length()) {
                            val v = arr.optString(i)
                            if (v.isNotEmpty()) add(v)
                        }
                    }
                    put(k, list)
                }
            }
        }.getOrDefault(emptyMap())
    }

    private fun encodeStringListMap(map: Map<String, List<String>>): String {
        val obj = JSONObject()
        map.forEach { (k, v) ->
            val arr = JSONArray()
            v.forEach { arr.put(it) }
            obj.put(k, arr)
        }
        return obj.toString()
    }

    private fun decodeTeachers(rawJson: String?): List<TeacherAccount> {
        if (rawJson.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(rawJson)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    val subjArr = o.optJSONArray("subjects")
                    val subjects = buildList {
                        if (subjArr != null) {
                            for (j in 0 until subjArr.length()) {
                                val name = subjArr.optString(j)
                                LearningArea.entries.firstOrNull { it.displayName == name }?.let(::add)
                            }
                        }
                    }.ifEmpty { listOf(LearningArea.MATHEMATICS) }
                    add(
                        TeacherAccount(
                            teacherCode = o.optString("teacherCode"),
                            fullName = o.optString("fullName"),
                            school = o.optString("school", "Nairobi Primary School"),
                            tscNumber = o.optString("tscNumber", ""),
                            pin = o.optString("pin", "1234"),
                            subjects = subjects,
                            grade = o.optInt("grade", 6).coerceIn(1, 12),
                            classStream = o.optString("classStream", "Grade 6 East")
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun encodeTeachers(list: List<TeacherAccount>): String {
        val arr = JSONArray()
        list.forEach { t ->
            val subjArr = JSONArray().apply { t.subjects.forEach { put(it.displayName) } }
            arr.put(
                JSONObject()
                    .put("teacherCode", t.teacherCode)
                    .put("fullName", t.fullName)
                    .put("school", t.school)
                    .put("tscNumber", t.tscNumber)
                    .put("pin", t.pin)
                    .put("subjects", subjArr)
                    .put("grade", t.grade)
                    .put("classStream", t.classStream)
            )
        }
        return arr.toString()
    }

    private fun decodeStringList(rawJson: String?): List<String> {
        if (rawJson.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(rawJson)
            buildList {
                for (i in 0 until arr.length()) {
                    val v = arr.optString(i)
                    if (v.isNotEmpty()) add(v)
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun encodeStringList(list: List<String>): String =
        JSONArray().apply { list.forEach { put(it) } }.toString()

    private fun decodeLinkedTeachers(rawJson: String?): List<LinkedTeacherSummary> {
        if (rawJson.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(rawJson)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    val subjArr = o.optJSONArray("subjects")
                    val subjects = buildList {
                        if (subjArr != null) {
                            for (j in 0 until subjArr.length()) {
                                val name = subjArr.optString(j)
                                LearningArea.entries.firstOrNull { it.displayName == name }?.let(::add)
                            }
                        }
                    }.ifEmpty { listOf(LearningArea.MATHEMATICS) }
                    val code = o.optString("teacherCode")
                    if (code.isNotEmpty()) {
                        add(
                            LinkedTeacherSummary(
                                teacherCode = code,
                                fullName = o.optString("fullName", "Teacher"),
                                school = o.optString("school", "Nairobi Primary School"),
                                tscNumber = o.optString("tscNumber", ""),
                                grade = o.optInt("grade", 6).coerceIn(1, 12),
                                classStream = o.optString("classStream", "Grade 6 East"),
                                subjects = subjects
                            )
                        )
                    }
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun encodeLinkedTeachers(list: List<LinkedTeacherSummary>): String {
        val arr = JSONArray()
        list.forEach { t ->
            val subjArr = JSONArray().apply { t.subjects.forEach { put(it.displayName) } }
            arr.put(
                JSONObject()
                    .put("teacherCode", t.teacherCode)
                    .put("fullName", t.fullName)
                    .put("school", t.school)
                    .put("tscNumber", t.tscNumber)
                    .put("grade", t.grade)
                    .put("classStream", t.classStream)
                    .put("subjects", subjArr)
            )
        }
        return arr.toString()
    }

    private fun persistState(s: SomaTimeUiState) {
        val editor = prefs.edit()
        val defaultIds = if (s.demoSeeded) setOf("amina", "brian") else emptySet()
        val customIds = s.children.map { it.id }.filter { it !in defaultIds }.toSet()
        editor.putBoolean("demo_seeded", s.demoSeeded)
        editor.putStringSet("custom_child_ids", customIds)
        editor.putString("selected_child_id", s.selectedChildId)
        editor.putString("role_view", s.roleView.name)
        editor.putBoolean("hand_off_active", s.handOffActive)
        editor.putString("parent_name", s.parentName)
        editor.putString("parent_contact", s.parentContact)
        editor.putString("parent_pin", s.parentPin)
        editor.putString("active_teacher_code", s.teacherAccount?.teacherCode)
        editor.putString("registered_teachers_json", encodeTeachers(s.registeredTeachers))
        editor.putString("last_interrupted_app_package", s.lastInterruptedAppPackage)
        editor.putString("displayed_keys_json", encodeStringListMap(s.displayedKeysByChild))
        editor.putString("displayed_quiz_ids_json", encodeStringListMap(s.displayedQuizIdsByChild))
        editor.putString(
            "custom_assignments_json",
            encodeAssignments(s.assignments.filter { it.isCustom || it.id.startsWith("assign-") }.take(20))
        )
        s.children.forEach { c ->
            val prefix = "child_${c.id}_"
            editor.putString("${prefix}name", c.name)
            editor.putInt("${prefix}grade", c.grade)
            editor.putString("${prefix}school", c.school)
            editor.putInt("${prefix}secs", c.availableScreenSeconds)
            editor.putInt("${prefix}used", c.usedScreenTodayMins)
            editor.putInt("${prefix}learned", c.learnedMinutesToday)
            editor.putInt("${prefix}cap", c.dailyCapMins)
            editor.putInt("${prefix}rate", c.exchangeRateMins)
            editor.putBoolean("${prefix}approval", c.requireParentApproval)
            editor.putBoolean("${prefix}locked", c.isLocked)
            editor.putBoolean("${prefix}paired", c.isPaired)
            editor.putString("${prefix}code", c.pairingCode)
            editor.putBoolean("${prefix}license_active", c.licenseActive)
            editor.putString("${prefix}license_expires_at", c.licenseExpiresAt)
            editor.putString("${prefix}license_payment_ref", c.licensePaymentRef)
            editor.putString("${prefix}linked_teacher_codes_json", encodeStringList(c.linkedTeacherCodes))
            editor.putString("${prefix}linked_teachers_json", encodeLinkedTeachers(c.linkedTeachers))
        }
        editor.apply()
    }

    private fun startCountdownTicker() {
        tickerJob?.cancel()
        tickerJob = scope.launch {
            var lastWallMs = SystemClock.elapsedRealtime()
            while (isActive) {
                delay(500L)
                val nowMs = SystemClock.elapsedRealtime()
                val elapsedSecs = ((nowMs - lastWallMs) / 1000L).toInt()
                if (elapsedSecs <= 0) continue
                lastWallMs += elapsedSecs * 1000L

                var newlyLockedChild: ChildProfile? = null
                _state.update { current ->
                    val anyTicking = current.children.any { !it.isLocked && it.availableScreenSeconds > 0 }
                    if (!anyTicking) return@update current
                    val updated = current.children.map { child ->
                        if (child.isLocked || child.availableScreenSeconds <= 0) {
                            child
                        } else {
                            val beforeSecs = child.availableScreenSeconds
                            val nextSecs = (beforeSecs - elapsedSecs).coerceAtLeast(0)
                            val beforeMinsElapsed = (beforeSecs + 59) / 60
                            val afterMinsElapsed = (nextSecs + 59) / 60
                            val minsConsumed = (beforeMinsElapsed - afterMinsElapsed).coerceAtLeast(0)
                            val nextUsed = (child.usedScreenTodayMins + minsConsumed).coerceAtMost(child.dailyCapMins)
                            val nextChild = child.copy(
                                availableScreenSeconds = nextSecs,
                                usedScreenTodayMins = nextUsed,
                                isLocked = nextSecs == 0
                            )
                            if (nextChild.isLocked && nextChild.isPaired) {
                                newlyLockedChild = nextChild
                            }
                            nextChild
                        }
                    }
                    current.copy(children = updated).also(::persistState)
                }
                newlyLockedChild?.let { locked ->
                    scope.launch {
                        FirestoreSyncClient.patchLearnerSync(locked)
                    }
                }
            }
        }
    }

    fun setShowSetupWizard(show: Boolean, initialTab: SetupWizardTab = SetupWizardTab.CHOICE) {
        _state.update {
            it.copy(
                showSetupWizard = show,
                setupInitialTab = if (show) initialTab else SetupWizardTab.CHOICE
            )
        }
    }

    fun setRoleView(role: RoleView) {
        _state.update { it.copy(roleView = role).also(::persistState) }
    }

    fun selectChild(childId: String) {
        _state.update { it.copy(selectedChildId = childId).also(::persistState) }
    }

    fun retryCloudSyncNow() {
        val current = _state.value
        val child = current.selectedChild ?: return
        val safePin = current.parentPin.ifEmpty { CbcCurriculumRepository.PARENT_PIN }
        _state.update { it.copy(cloudSyncMessage = "Syncing ${child.pairingCode} to Cloud...") }
        scope.launch {
            val res = FirestoreSyncClient.publishPairingSession(child, safePin)
            _state.update {
                it.copy(
                    cloudSyncOk = res.ok,
                    cloudSyncMessage = if (res.ok) {
                        if (child.licenseActive) {
                            "Synced to Cloud ✓ · Code ${child.pairingCode} is active"
                        } else {
                            "Synced to Cloud ✓ · Pay KES ${CbcCurriculumRepository.MONTHLY_LICENSE_KES}/mo license to unlock pairing"
                        }
                    } else {
                        res.errorMessage
                    }
                )
            }
        }
    }

    fun updateParentPin(newPin: String): Boolean {
        val clean = newPin.filter(Char::isDigit).take(4)
        if (clean.length != 4) return false
        _state.update { current ->
            current.copy(parentPin = clean).also(::persistState)
        }
        val childrenSnapshot = _state.value.children
        scope.launch {
            var allOk = true
            var lastErr = ""
            childrenSnapshot.forEach { child ->
                val res = FirestoreSyncClient.publishPairingSession(child, clean)
                if (!res.ok) {
                    allOk = false
                    lastErr = res.errorMessage
                }
            }
            _state.update {
                it.copy(
                    cloudSyncOk = allOk,
                    cloudSyncMessage = if (allOk) {
                        "Parent PIN updated & synced to Cloud ✓"
                    } else {
                        lastErr.ifEmpty { "PIN saved locally; Cloud sync failed" }
                    }
                )
            }
        }
        return true
    }

    fun activateChildLicense(
        childId: String,
        payerContact: String,
        explicitReceiptRef: String? = null
    ): ChildProfile? {
        val suffix = ((System.currentTimeMillis() % 900000L) + 100000L).toString()
        val cleanContact = payerContact.filter { it.isLetterOrDigit() }.takeLast(4).uppercase()
        val fallbackRef = "PSK-${if (cleanContact.isNotEmpty()) cleanContact else "SOMA"}$suffix".take(24)
        val paymentRef = explicitReceiptRef?.takeIf { it.isNotBlank() }?.take(24) ?: fallbackRef
        val expiresAt = CbcCurriculumRepository.buildLicenseExpiryIso(30)
        var updatedTarget: ChildProfile? = null

        _state.update { current ->
            val next = current.children.map { child ->
                if (child.id == childId) {
                    child.copy(
                        licenseActive = true,
                        licensePlanLabel = CbcCurriculumRepository.MONTHLY_LICENSE_PLAN_LABEL,
                        licenseExpiresAt = expiresAt,
                        licensePaymentRef = paymentRef
                    ).also { updatedTarget = it }
                } else {
                    child
                }
            }
            current.copy(children = next).also(::persistState)
        }

        val pin = _state.value.parentPin
        updatedTarget?.let { target ->
            scope.launch {
                val res = FirestoreSyncClient.publishPairingSession(target, pin)
                _state.update {
                    it.copy(
                        cloudSyncOk = res.ok,
                        cloudSyncMessage = if (res.ok) {
                            "Paystack License active (${target.licensePaymentRef}) · Code ${target.pairingCode} unlocked ✓"
                        } else {
                            res.errorMessage
                        }
                    )
                }
            }
        }
        return updatedTarget
    }

    fun activateAllUnpaidLicenses(
        payerContact: String,
        explicitReceiptRef: String? = null
    ): Int {
        val unpaidIds = _state.value.children.filter { !it.licenseActive }.map { it.id }.toSet()
        if (unpaidIds.isEmpty()) return 0
        val suffix = ((System.currentTimeMillis() % 900000L) + 100000L).toString()
        val cleanContact = payerContact.filter { it.isLetterOrDigit() }.takeLast(4).uppercase()
        val fallbackRef = "PSK-${if (cleanContact.isNotEmpty()) cleanContact else "SOMA"}$suffix".take(24)
        val paymentRef = explicitReceiptRef?.takeIf { it.isNotBlank() }?.take(24) ?: fallbackRef
        val expiresAt = CbcCurriculumRepository.buildLicenseExpiryIso(30)
        val updatedTargets = mutableListOf<ChildProfile>()

        _state.update { current ->
            val next = current.children.map { child ->
                if (child.id in unpaidIds) {
                    child.copy(
                        licenseActive = true,
                        licensePlanLabel = CbcCurriculumRepository.MONTHLY_LICENSE_PLAN_LABEL,
                        licenseExpiresAt = expiresAt,
                        licensePaymentRef = paymentRef
                    ).also { updatedTargets.add(it) }
                } else {
                    child
                }
            }
            current.copy(children = next).also(::persistState)
        }

        val pin = _state.value.parentPin
        scope.launch {
            var allOk = true
            var lastErr = ""
            updatedTargets.forEach { target ->
                val res = FirestoreSyncClient.publishPairingSession(target, pin)
                if (!res.ok) {
                    allOk = false
                    lastErr = res.errorMessage
                }
            }
            _state.update {
                it.copy(
                    cloudSyncOk = allOk,
                    cloudSyncMessage = if (allOk) {
                        "Activated ${updatedTargets.size} child license(s) via Paystack · Synced to Cloud ✓"
                    } else {
                        lastErr.ifEmpty { "Licenses activated locally; Cloud sync failed" }
                    }
                )
            }
        }
        return updatedTargets.size
    }

    suspend fun fetchPaystackStatusAsync(): PaystackGatewayStatus? = PaystackClient.fetchStatus()

    suspend fun initializePaystackCheckoutAsync(
        phone: String,
        childIds: List<String>
    ): PaystackInitResult {
        val res = PaystackClient.initializeCheckout(phone, childIds)
        if (res.ok && (res.mode == "paystack_test_demo" || res.mode == "paystack_stk_instant")) {
            if (childIds.size == 1) {
                activateChildLicense(childIds.first(), phone, res.reference)
            } else {
                activateAllUnpaidLicenses(phone, res.reference)
            }
        }
        return res
    }

    suspend fun verifyPaystackPaymentAsync(
        reference: String,
        childIds: List<String>,
        payerContact: String
    ): PaystackVerifyResult {
        val res = PaystackClient.verifyTransaction(reference)
        if (res.ok && res.status == "paid") {
            val targetIds = childIds.ifEmpty { res.childIds }
            val receiptRef = res.receiptRef ?: "PSK-${reference.takeLast(8).uppercase()}"
            if (targetIds.size == 1) {
                activateChildLicense(targetIds.first(), payerContact, receiptRef)
            } else if (targetIds.size > 1) {
                activateAllUnpaidLicenses(payerContact, receiptRef)
            }
        }
        return res
    }

    fun updateChild(childId: String, transform: (ChildProfile) -> ChildProfile) {
        var updatedTarget: ChildProfile? = null
        _state.update { current ->
            val next = current.children.map {
                if (it.id == childId) {
                    transform(it).also { u -> updatedTarget = u }
                } else {
                    it
                }
            }
            current.copy(children = next).also(::persistState)
        }
        val pin = _state.value.parentPin
        updatedTarget?.let { target ->
            scope.launch {
                val res = FirestoreSyncClient.publishPairingSession(target, pin)
                _state.update {
                    it.copy(
                        cloudSyncOk = res.ok,
                        cloudSyncMessage = if (res.ok) {
                            if (target.licenseActive) {
                                "Synced to Cloud ✓ · Code ${target.pairingCode} is active"
                            } else {
                                "Synced to Cloud ✓ · Pay KES ${CbcCurriculumRepository.MONTHLY_LICENSE_KES}/mo license to unlock pairing"
                            }
                        } else {
                            res.errorMessage
                        }
                    )
                }
            }
        }
    }

    fun regeneratePairingCode(childId: String): String {
        val currentTarget = _state.value.children.firstOrNull { it.id == childId } ?: return ""
        if (!currentTarget.licenseActive) return currentTarget.pairingCode
        val code = CbcCurriculumRepository.generatePairingCode()
        updateChild(childId) { it.copy(pairingCode = code, isPaired = false) }
        return code
    }

    suspend fun registerParentAndChildAsync(
        parentName: String,
        parentContact: String,
        parentPin: String,
        childName: String,
        childGrade: Int,
        childSchool: String,
        dailyCapMins: Int,
        initialWatchMins: Int
    ): ChildProfile {
        val id = "child-${System.currentTimeMillis()}"
        val mandatory = if (childGrade >= 7) {
            listOf(LearningArea.MATHEMATICS, LearningArea.INTEGRATED_SCIENCE)
        } else {
            listOf(LearningArea.MATHEMATICS, LearningArea.KISWAHILI)
        }
        val code = CbcCurriculumRepository.generatePairingCode()
        val resolvedPin = if (parentPin.length == 4) parentPin else _state.value.parentPin
        val created = ChildProfile(
            id = id,
            name = childName.trim(),
            grade = childGrade,
            school = childSchool.trim().ifEmpty { "Nairobi Primary School" },
            classStream = "Grade $childGrade East",
            deviceMode = DeviceMode.OWN_PHONE,
            pairingCode = code,
            isPaired = false,
            dailyCapMins = dailyCapMins,
            exchangeRateMins = 15,
            mandatorySubjects = mandatory,
            requireParentApproval = false,
            availableScreenSeconds = initialWatchMins * 60,
            usedScreenTodayMins = 0,
            learnedMinutesToday = 0,
            isLocked = initialWatchMins <= 0,
            completedLessonIds = emptyList(),
            pendingApprovals = emptyList(),
            activityLogs = emptyList(),
            rubricByArea = mapOf(LearningArea.MATHEMATICS to CBCRubric.ME),
            streakDays = 1,
            licenseActive = false,
            licensePlanLabel = CbcCurriculumRepository.MONTHLY_LICENSE_PLAN_LABEL,
            licenseExpiresAt = null,
            licensePaymentRef = null
        )
        _state.update { current ->
            current.copy(
                parentName = parentName.trim().ifEmpty { current.parentName },
                parentContact = parentContact.trim().ifEmpty { current.parentContact },
                parentPin = resolvedPin,
                selectedChildId = id,
                roleView = RoleView.PARENT,
                children = listOf(created) + current.children
            ).also(::persistState)
        }
        val syncRes = FirestoreSyncClient.publishPairingSession(created, resolvedPin)
        _state.update {
            it.copy(
                cloudSyncOk = syncRes.ok,
                cloudSyncMessage = if (syncRes.ok) {
                    "Synced to Cloud ✓ · Pay KES ${CbcCurriculumRepository.MONTHLY_LICENSE_KES}/mo license in Parents tab to unlock ${created.pairingCode}"
                } else {
                    syncRes.errorMessage
                }
            )
        }
        return created
    }

    suspend fun pairLearnerByCodeAsync(rawCode: String): PairingResult {
        val normalized = FirestoreSyncClient.normalizeCode(rawCode)
        if (normalized.isEmpty()) {
            return PairingResult(false, message = "Enter the pairing code generated on your parent's phone.")
        }

        // 1. Query Cloud Firestore so two separate phones pair across devices
        val cloudRes = FirestoreSyncClient.fetchAndPairSession(normalized)
        val cloud = cloudRes.data
        if (cloudRes.ok && cloud != null) {
            if (!cloud.licenseActive) {
                return PairingResult(
                    false,
                    message = "Monthly child license (KES ${CbcCurriculumRepository.MONTHLY_LICENSE_KES}/mo) is not active for ${cloud.childName}. Ask a parent to activate it in the Parents tab first."
                )
            }
            val existingLocal = _state.value.children.firstOrNull {
                it.id == cloud.childId || FirestoreSyncClient.normalizeCode(it.pairingCode) == normalized
            }
            val syncedChild = ChildProfile(
                id = cloud.childId,
                name = cloud.childName,
                grade = cloud.grade,
                school = cloud.school,
                classStream = cloud.classStream,
                deviceMode = DeviceMode.OWN_PHONE,
                pairingCode = cloud.pairingCode,
                isPaired = true,
                dailyCapMins = cloud.dailyCapMins,
                exchangeRateMins = cloud.exchangeRateMins,
                mandatorySubjects = cloud.mandatorySubjects.ifEmpty {
                    existingLocal?.mandatorySubjects ?: listOf(LearningArea.MATHEMATICS)
                },
                requireParentApproval = cloud.requireParentApproval,
                availableScreenSeconds = cloud.availableScreenSeconds,
                usedScreenTodayMins = cloud.usedScreenTodayMins,
                learnedMinutesToday = cloud.learnedMinutesToday,
                isLocked = cloud.isLocked || cloud.availableScreenSeconds <= 0,
                completedLessonIds = cloud.completedLessonIds,
                pendingApprovals = existingLocal?.pendingApprovals ?: emptyList(),
                activityLogs = existingLocal?.activityLogs ?: emptyList(),
                rubricByArea = existingLocal?.rubricByArea ?: mapOf(LearningArea.MATHEMATICS to CBCRubric.ME),
                streakDays = existingLocal?.streakDays ?: 1,
                licenseActive = cloud.licenseActive,
                licensePlanLabel = CbcCurriculumRepository.MONTHLY_LICENSE_PLAN_LABEL,
                licenseExpiresAt = cloud.licenseExpiresAt.ifEmpty { null },
                licensePaymentRef = cloud.licensePaymentRef.ifEmpty { null },
                linkedTeacherCodes = cloud.linkedTeacherCodes,
                linkedTeachers = existingLocal?.linkedTeachers ?: emptyList()
            )

            _state.update { current ->
                val exists = current.children.any { it.id == syncedChild.id }
                val updatedList = if (exists) {
                    current.children.map { if (it.id == syncedChild.id) syncedChild else it }
                } else {
                    listOf(syncedChild) + current.children
                }
                current.copy(
                    roleView = RoleView.CHILD,
                    selectedChildId = syncedChild.id,
                    parentPin = cloud.parentPin,
                    handOffActive = true,
                    showSetupWizard = false,
                    cloudSyncOk = true,
                    cloudSyncMessage = "Paired via Cloud ✓",
                    children = updatedList
                ).also(::persistState)
            }

            if (syncedChild.linkedTeacherCodes.isNotEmpty()) {
                scope.launch {
                    fetchLinkedAssignmentsIntoState(syncedChild.linkedTeacherCodes)
                }
            }

            return PairingResult(
                success = true,
                child = syncedChild,
                message = "Paired with ${syncedChild.name}! Background watch-minute guard active."
            )
        }

        // 2. Fallback: local device list (single-device demo)
        val matched = _state.value.children.firstOrNull {
            FirestoreSyncClient.normalizeCode(it.pairingCode) == normalized
        } ?: return PairingResult(
            false,
            message = cloudRes.errorMessage.ifEmpty {
                "No child profile matches \"$normalized\"."
            }
        )

        if (!matched.licenseActive) {
            return PairingResult(
                false,
                message = "Monthly child license (KES ${CbcCurriculumRepository.MONTHLY_LICENSE_KES}/mo) is not active for ${matched.name}. Ask a parent to pay the license in the Parents tab first."
            )
        }

        val pairedLocal = matched.copy(deviceMode = DeviceMode.OWN_PHONE, isPaired = true)
        _state.update { current ->
            val updated = current.children.map { c ->
                if (c.id == matched.id) pairedLocal else c
            }
            current.copy(
                roleView = RoleView.CHILD,
                selectedChildId = matched.id,
                handOffActive = true,
                showSetupWizard = false,
                children = updated
            ).also(::persistState)
        }
        scope.launch {
            FirestoreSyncClient.publishPairingSession(pairedLocal, _state.value.parentPin)
        }

        return PairingResult(
            success = true,
            child = pairedLocal,
            message = "Paired with ${matched.name}! Background watch-minute guard active."
        )
    }

    private suspend fun fetchLinkedAssignmentsIntoState(linkedCodes: List<String>) {
        val normalized = linkedCodes
            .map { FirestoreSyncClient.normalizeTeacherCode(it) }
            .filter { FirestoreSyncClient.isValidTeacherCode(it) }
            .distinct()
            .take(10)
        if (normalized.isEmpty()) return

        val fetchedAll = mutableListOf<TeacherAssignment>()
        normalized.forEach { code ->
            val res = FirestoreSyncClient.fetchTeacherAssignments(code)
            if (res.ok && !res.data.isNullOrEmpty()) {
                fetchedAll.addAll(res.data)
            }
        }
        if (fetchedAll.isEmpty()) return

        _state.update { s ->
            val existingMap = s.assignments.associateBy { it.id }
            val mergedCloud = fetchedAll.map { ca ->
                val prev = existingMap[ca.id]
                if (prev != null) ca.copy(completedByChildIds = prev.completedByChildIds) else ca
            }
            val cloudIds = mergedCloud.map { it.id }.toSet()
            val remaining = s.assignments.filterNot { it.id in cloudIds }
            s.copy(assignments = mergedCloud + remaining).also(::persistState)
        }
    }

    suspend fun syncPairedChildWithCloud() {
        val current = _state.value
        val child = current.selectedChild ?: return
        if (!FirestoreSyncClient.isValidCode(FirestoreSyncClient.normalizeCode(child.pairingCode))) return

        var latestLinkedCodes = child.linkedTeacherCodes

        if (current.roleView == RoleView.CHILD && child.isPaired) {
            val cloudRes = FirestoreSyncClient.fetchPairingSession(child.pairingCode)
            val cloud = cloudRes.data
            if (cloudRes.ok && cloud != null) {
                latestLinkedCodes = cloud.linkedTeacherCodes
                val remoteUnlocked =
                    !cloud.isLocked && child.isLocked && cloud.availableScreenSeconds > child.availableScreenSeconds
                val settingsChanged =
                    cloud.dailyCapMins != child.dailyCapMins ||
                        cloud.exchangeRateMins != child.exchangeRateMins ||
                        cloud.requireParentApproval != child.requireParentApproval ||
                        cloud.licenseActive != child.licenseActive ||
                        cloud.parentPin != current.parentPin ||
                        cloud.linkedTeacherCodes != child.linkedTeacherCodes
                if (remoteUnlocked || settingsChanged) {
                    _state.update { s ->
                        val nextChildren = s.children.map { c ->
                            if (c.id != child.id) {
                                c
                            } else {
                                c.copy(
                                    dailyCapMins = cloud.dailyCapMins,
                                    exchangeRateMins = cloud.exchangeRateMins,
                                    requireParentApproval = cloud.requireParentApproval,
                                    linkedTeacherCodes = cloud.linkedTeacherCodes,
                                    licenseActive = cloud.licenseActive,
                                    licenseExpiresAt = cloud.licenseExpiresAt.ifEmpty { c.licenseExpiresAt },
                                    licensePaymentRef = cloud.licensePaymentRef.ifEmpty { c.licensePaymentRef },
                                    availableScreenSeconds = if (remoteUnlocked) cloud.availableScreenSeconds else c.availableScreenSeconds,
                                    isLocked = if (remoteUnlocked) {
                                        cloud.isLocked || cloud.availableScreenSeconds <= 0
                                    } else {
                                        c.isLocked
                                    }
                                )
                            }
                        }
                        s.copy(parentPin = cloud.parentPin, children = nextChildren).also(::persistState)
                    }
                }
            }
            if (cloudRes.ok) {
                _state.value.selectedChild?.let { latestChild ->
                    FirestoreSyncClient.patchLearnerSync(latestChild)
                }
            }
        } else if (current.roleView == RoleView.PARENT) {
            val cloudRes = FirestoreSyncClient.fetchPairingSession(child.pairingCode)
            val cloud = cloudRes.data
            if (cloud != null) {
                _state.update { s ->
                    val nextChildren = s.children.map { c ->
                        if (c.id != child.id) {
                            c
                        } else {
                            c.copy(
                                isPaired = cloud.isPaired,
                                availableScreenSeconds = cloud.availableScreenSeconds,
                                usedScreenTodayMins = cloud.usedScreenTodayMins,
                                learnedMinutesToday = cloud.learnedMinutesToday,
                                isLocked = cloud.isLocked,
                                completedLessonIds = if (cloud.completedLessonIds.isNotEmpty()) {
                                    cloud.completedLessonIds
                                } else {
                                    c.completedLessonIds
                                }
                            )
                        }
                    }
                    s.copy(
                        cloudSyncOk = true,
                        cloudSyncMessage = if (cloud.isPaired) {
                            "Synced to Cloud ✓ · Learner phone paired"
                        } else if (!child.licenseActive) {
                            "Synced to Cloud ✓ · Pay KES ${CbcCurriculumRepository.MONTHLY_LICENSE_KES}/mo license to unlock pairing"
                        } else {
                            "Synced to Cloud ✓ · Ready for daughter's phone"
                        },
                        children = nextChildren
                    ).also(::persistState)
                }
            }
        }

        // Fetch teacher assignments strictly for Teacher Class Codes linked by the Parent
        fetchLinkedAssignmentsIntoState(latestLinkedCodes)
    }

    fun addChild(name: String, grade: Int, school: String) {
        val id = "child-${System.currentTimeMillis()}"
        val mandatory = if (grade >= 7) {
            listOf(LearningArea.MATHEMATICS, LearningArea.INTEGRATED_SCIENCE)
        } else {
            listOf(LearningArea.MATHEMATICS, LearningArea.KISWAHILI)
        }
        val created = ChildProfile(
            id = id,
            name = name.trim(),
            grade = grade,
            school = school.trim().ifEmpty { "Nairobi Primary School" },
            classStream = "Grade $grade East",
            deviceMode = DeviceMode.OWN_PHONE,
            pairingCode = CbcCurriculumRepository.generatePairingCode(),
            isPaired = false,
            dailyCapMins = 90,
            exchangeRateMins = 15,
            mandatorySubjects = mandatory,
            requireParentApproval = false,
            availableScreenSeconds = 180,
            usedScreenTodayMins = 0,
            learnedMinutesToday = 0,
            isLocked = false,
            completedLessonIds = emptyList(),
            pendingApprovals = emptyList(),
            activityLogs = emptyList(),
            rubricByArea = mapOf(LearningArea.MATHEMATICS to CBCRubric.ME),
            streakDays = 1,
            licenseActive = false,
            licensePlanLabel = CbcCurriculumRepository.MONTHLY_LICENSE_PLAN_LABEL,
            licenseExpiresAt = null,
            licensePaymentRef = null
        )
        _state.update {
            it.copy(
                selectedChildId = id,
                children = it.children + created
            ).also(::persistState)
        }
        scope.launch {
            val res = FirestoreSyncClient.publishPairingSession(created, _state.value.parentPin)
            _state.update {
                it.copy(
                    cloudSyncOk = res.ok,
                    cloudSyncMessage = if (res.ok) {
                        "Synced to Cloud ✓ · Pay KES ${CbcCurriculumRepository.MONTHLY_LICENSE_KES}/mo license to unlock pairing"
                    } else {
                        res.errorMessage
                    }
                )
            }
        }
    }

    fun activateHandOffMode(childId: String) {
        val existing = _state.value.children.firstOrNull { it.id == childId } ?: return
        if (!existing.licenseActive) return
        var updatedTarget: ChildProfile? = null
        _state.update { current ->
            val nextChildren = current.children.map {
                if (it.id == childId) {
                    it.copy(
                        deviceMode = DeviceMode.HAND_OFF,
                        isPaired = false,
                        isLocked = true,
                        availableScreenSeconds = 0
                    ).also { u -> updatedTarget = u }
                } else {
                    it
                }
            }
            current.copy(
                roleView = RoleView.CHILD,
                selectedChildId = childId,
                handOffActive = true,
                children = nextChildren
            ).also(::persistState)
        }
        updatedTarget?.let { target ->
            scope.launch {
                FirestoreSyncClient.publishPairingSession(target, _state.value.parentPin)
            }
        }
    }

    fun exitHandOffTo(role: RoleView) {
        _state.update {
            it.copy(handOffActive = false, roleView = role).also(::persistState)
        }
    }

    fun simulateTimeExpired(childId: String) {
        updateChild(childId) { it.copy(availableScreenSeconds = 0, isLocked = true) }
    }

    fun unlockWithParentPin(childId: String, bonusMins: Int = 15) {
        var updatedTarget: ChildProfile? = null
        _state.update { current ->
            val next = current.children.map { c ->
                if (c.id == childId) {
                    c.copy(
                        isLocked = false,
                        availableScreenSeconds = c.availableScreenSeconds + bonusMins * 60
                    ).also { u -> updatedTarget = u }
                } else {
                    c
                }
            }
            current.copy(children = next).also(::persistState)
        }
        updatedTarget?.let { target ->
            scope.launch {
                FirestoreSyncClient.publishPairingSession(target, _state.value.parentPin)
            }
        }
    }

    fun completeLesson(childId: String, lesson: CBCLesson, scorePercent: Int): LessonCompletionResult {
        val rubric = when {
            scorePercent >= 85 -> CBCRubric.EE
            scorePercent >= 65 -> CBCRubric.ME
            scorePercent >= 45 -> CBCRubric.AE
            else -> CBCRubric.BE
        }

        val currentChild = _state.value.children.first { it.id == childId }
        val remainingCapMins = (currentChild.dailyCapMins - currentChild.usedScreenTodayMins -
            currentChild.availableScreenSeconds / 60).coerceAtLeast(0)
        val earnedMins = currentChild.exchangeRateMins.coerceAtMost(remainingCapMins)
        val cappedOut = remainingCapMins <= 0
        val queued = currentChild.requireParentApproval && earnedMins > 0
        val nowTime = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
        var updatedTarget: ChildProfile? = null

        _state.update { current ->
            val nextChildren = current.children.map { child ->
                if (child.id != childId) return@map child
                val completed = if (lesson.id in child.completedLessonIds) {
                    child.completedLessonIds
                } else {
                    child.completedLessonIds + lesson.id
                }
                val newLog = ChildActivityLog(
                    id = "log-${System.currentTimeMillis()}",
                    lessonId = lesson.id,
                    lessonTitle = lesson.title,
                    learningArea = lesson.learningArea,
                    scorePercent = scorePercent,
                    rubricEarned = rubric,
                    minutesEarned = earnedMins,
                    status = if (queued) "Pending PIN" else "Credited",
                    timestamp = nowTime
                )
                val nextPending = if (queued) {
                    listOf(
                        PendingApproval(
                            id = "pend-${System.currentTimeMillis()}",
                            lessonTitle = lesson.title,
                            learningArea = lesson.learningArea,
                            minutesEarned = earnedMins,
                            scorePercent = scorePercent,
                            completedAt = nowTime
                        )
                    ) + child.pendingApprovals
                } else {
                    child.pendingApprovals
                }
                val unlockedNow = !queued && earnedMins > 0
                child.copy(
                    learnedMinutesToday = child.learnedMinutesToday + lesson.durationMins,
                    completedLessonIds = completed,
                    availableScreenSeconds = if (unlockedNow) {
                        child.availableScreenSeconds + earnedMins * 60
                    } else {
                        child.availableScreenSeconds
                    },
                    isLocked = if (unlockedNow) false else child.isLocked,
                    pendingApprovals = nextPending,
                    activityLogs = listOf(newLog) + child.activityLogs,
                    rubricByArea = child.rubricByArea + (lesson.learningArea to rubric)
                ).also { updatedTarget = it }
            }

            val nextAssignments = current.assignments.map { assign ->
                val isMatch = (lesson.assignmentId != null && assign.id == lesson.assignmentId) ||
                    assign.lessonId == lesson.id
                if (isMatch && childId !in assign.completedByChildIds) {
                    assign.copy(completedByChildIds = assign.completedByChildIds + childId)
                } else {
                    assign
                }
            }

            current.copy(children = nextChildren, assignments = nextAssignments).also(::persistState)
        }

        updatedTarget?.let { target ->
            scope.launch {
                FirestoreSyncClient.patchLearnerSync(target)
            }
        }

        return LessonCompletionResult(earnedMins, rubric, queued, cappedOut)
    }

    fun resolveApproval(childId: String, approvalId: String, approved: Boolean) {
        var updatedTarget: ChildProfile? = null
        _state.update { current ->
            val nextChildren = current.children.map { child ->
                if (child.id != childId) return@map child
                val item = child.pendingApprovals.firstOrNull { it.id == approvalId } ?: return@map child
                child.copy(
                    pendingApprovals = child.pendingApprovals.filterNot { it.id == approvalId },
                    availableScreenSeconds = if (approved) {
                        child.availableScreenSeconds + item.minutesEarned * 60
                    } else {
                        child.availableScreenSeconds
                    },
                    isLocked = if (approved) false else child.isLocked
                ).also { updatedTarget = it }
            }
            current.copy(children = nextChildren).also(::persistState)
        }
        updatedTarget?.let { target ->
            scope.launch {
                FirestoreSyncClient.publishPairingSession(target, _state.value.parentPin)
            }
        }
    }

    suspend fun verifyTeacherClassCodeAsync(rawCode: String): TeacherVerifyResult {
        val code = FirestoreSyncClient.normalizeTeacherCode(rawCode)
        if (!FirestoreSyncClient.isValidTeacherCode(code)) {
            return TeacherVerifyResult(
                ok = false,
                message = "Enter a valid Teacher Class Code like TCH-7K4M9Q."
            )
        }

        val cloudRes = FirestoreSyncClient.lookupTeacherByCode(code)
        if (cloudRes.ok && cloudRes.data != null) {
            return TeacherVerifyResult(
                ok = true,
                teacher = cloudRes.data,
                message = "Verified ${cloudRes.data.fullName} · ${cloudRes.data.school}"
            )
        }

        val localMatch = _state.value.registeredTeachers.firstOrNull {
            FirestoreSyncClient.normalizeTeacherCode(it.teacherCode) == code
        }
        if (localMatch != null) {
            return TeacherVerifyResult(
                ok = true,
                teacher = LinkedTeacherSummary(
                    teacherCode = localMatch.teacherCode,
                    fullName = localMatch.fullName,
                    school = localMatch.school,
                    tscNumber = localMatch.tscNumber,
                    grade = localMatch.grade,
                    classStream = localMatch.classStream,
                    subjects = localMatch.subjects
                ),
                message = "Verified ${localMatch.fullName} (on-device)"
            )
        }

        if (code == "TCH-4821") {
            return TeacherVerifyResult(
                ok = true,
                teacher = LinkedTeacherSummary(
                    teacherCode = "TCH-4821",
                    fullName = "Ms. Grace Njeri",
                    school = "Nairobi Primary School",
                    grade = 6,
                    classStream = "Grade 6 East - Nairobi Primary",
                    subjects = listOf(LearningArea.SOCIAL_STUDIES, LearningArea.MATHEMATICS)
                ),
                message = "Verified demo teacher Ms. Grace Njeri"
            )
        }

        return TeacherVerifyResult(
            ok = false,
            message = cloudRes.errorMessage.ifEmpty {
                "No registered teacher found for \"$code\"."
            }
        )
    }

    fun linkTeacherToChild(childId: String, teacher: LinkedTeacherSummary) {
        val code = FirestoreSyncClient.normalizeTeacherCode(teacher.teacherCode)
        if (!FirestoreSyncClient.isValidTeacherCode(code)) return
        var updatedTarget: ChildProfile? = null
        _state.update { current ->
            val next = current.children.map { c ->
                if (c.id != childId) {
                    c
                } else {
                    val nextCodes = (listOf(code) + c.linkedTeacherCodes.map {
                        FirestoreSyncClient.normalizeTeacherCode(it)
                    }).distinct().take(10)
                    val nextMeta = (listOf(teacher.copy(teacherCode = code)) +
                        c.linkedTeachers.filterNot {
                            FirestoreSyncClient.normalizeTeacherCode(it.teacherCode) == code
                        }).take(10)
                    c.copy(
                        linkedTeacherCodes = nextCodes,
                        linkedTeachers = nextMeta
                    ).also { updatedTarget = it }
                }
            }
            current.copy(children = next).also(::persistState)
        }
        val pin = _state.value.parentPin
        updatedTarget?.let { target ->
            scope.launch {
                FirestoreSyncClient.publishPairingSession(target, pin)
                fetchLinkedAssignmentsIntoState(listOf(code))
            }
        }
    }

    fun unlinkTeacherFromChild(childId: String, teacherCode: String) {
        val code = FirestoreSyncClient.normalizeTeacherCode(teacherCode)
        var updatedTarget: ChildProfile? = null
        _state.update { current ->
            val next = current.children.map { c ->
                if (c.id != childId) {
                    c
                } else {
                    c.copy(
                        linkedTeacherCodes = c.linkedTeacherCodes.filterNot {
                            FirestoreSyncClient.normalizeTeacherCode(it) == code
                        },
                        linkedTeachers = c.linkedTeachers.filterNot {
                            FirestoreSyncClient.normalizeTeacherCode(it.teacherCode) == code
                        }
                    ).also { updatedTarget = it }
                }
            }
            current.copy(children = next).also(::persistState)
        }
        val pin = _state.value.parentPin
        updatedTarget?.let { target ->
            scope.launch {
                FirestoreSyncClient.publishPairingSession(target, pin)
            }
        }
    }

    suspend fun registerTeacherAsync(
        fullName: String,
        school: String,
        tscNumber: String,
        pin: String,
        subjects: List<LearningArea>,
        grade: Int,
        classStream: String,
        preserveRoleView: Boolean = false,
        autoLinkOwnedChildren: Boolean = false
    ): TeacherAccount {
        val code = CbcCurriculumRepository.generateTeacherCode()
        val safeSchool = school.trim().ifEmpty { "Nairobi Primary School" }
        val safeGrade = grade.coerceIn(1, 12)
        val safeStream = classStream.trim().ifEmpty { "Grade $safeGrade East - $safeSchool" }
        val validPin = pin.filter(Char::isDigit).take(4).let { if (it.length == 4) it else "1234" }
        val chosenSubjects = subjects.ifEmpty { listOf(LearningArea.MATHEMATICS) }.take(8)

        val draft = TeacherAccount(
            teacherCode = code,
            fullName = fullName.trim().ifEmpty { "Teacher" },
            school = safeSchool,
            tscNumber = tscNumber.trim(),
            pin = validPin,
            subjects = chosenSubjects,
            grade = safeGrade,
            classStream = safeStream
        )

        val cloudRes = FirestoreSyncClient.registerTeacher(draft)
        val saved = if (cloudRes.ok && cloudRes.data != null) cloudRes.data else draft

        val summary = LinkedTeacherSummary(
            teacherCode = saved.teacherCode,
            fullName = saved.fullName,
            school = saved.school,
            tscNumber = saved.tscNumber,
            grade = saved.grade,
            classStream = saved.classStream,
            subjects = saved.subjects
        )
        val linkedToSync = mutableListOf<ChildProfile>()

        _state.update { s ->
            val nextList = listOf(saved) + s.registeredTeachers.filterNot { it.teacherCode == saved.teacherCode }
            val nextChildren = if (autoLinkOwnedChildren) {
                s.children.map { c ->
                    val nextCodes = (listOf(saved.teacherCode) + c.linkedTeacherCodes).distinct().take(10)
                    val nextMeta = (listOf(summary) + c.linkedTeachers.filterNot { it.teacherCode == saved.teacherCode }).take(10)
                    c.copy(
                        linkedTeacherCodes = nextCodes,
                        linkedTeachers = nextMeta
                    ).also { linkedToSync.add(it) }
                }
            } else {
                s.children
            }
            s.copy(
                roleView = if (preserveRoleView) s.roleView else RoleView.TEACHER,
                teacherAccount = saved,
                registeredTeachers = nextList,
                children = nextChildren
            ).also(::persistState)
        }
        val parentPin = _state.value.parentPin
        if (linkedToSync.isNotEmpty()) {
            scope.launch {
                linkedToSync.forEach { child ->
                    FirestoreSyncClient.publishPairingSession(child, parentPin)
                }
            }
        }
        return saved
    }

    suspend fun loginTeacherAsync(rawCode: String, rawPin: String): TeacherLoginResult {
        val code = FirestoreSyncClient.normalizeTeacherCode(rawCode)
        val pin = rawPin.filter(Char::isDigit).take(4)
        if (code.isEmpty() || pin.length != 4) {
            return TeacherLoginResult(false, message = "Enter both your Teacher Code (TCH-XXXXXX) and 4-digit PIN.")
        }

        val cloudRes = FirestoreSyncClient.loginTeacher(code, pin)
        if (cloudRes.ok && cloudRes.data != null) {
            val logged = cloudRes.data
            _state.update { s ->
                val nextList = listOf(logged) + s.registeredTeachers.filterNot { it.teacherCode == logged.teacherCode }
                s.copy(
                    roleView = RoleView.TEACHER,
                    teacherAccount = logged,
                    registeredTeachers = nextList,
                    showSetupWizard = false
                ).also(::persistState)
            }
            return TeacherLoginResult(true, logged, "Signed in as ${logged.fullName}")
        }

        val localMatch = _state.value.registeredTeachers.firstOrNull {
            FirestoreSyncClient.normalizeTeacherCode(it.teacherCode) == code
        }
        if (localMatch != null) {
            return if (localMatch.pin == pin) {
                _state.update { s ->
                    s.copy(
                        roleView = RoleView.TEACHER,
                        teacherAccount = localMatch,
                        showSetupWizard = false
                    ).also(::persistState)
                }
                TeacherLoginResult(true, localMatch, "Signed in as ${localMatch.fullName}")
            } else {
                TeacherLoginResult(false, message = "Incorrect 4-digit Teacher PIN for $code.")
            }
        }

        return TeacherLoginResult(
            success = false,
            message = cloudRes.errorMessage.ifEmpty { "No teacher account found for \"$code\"." }
        )
    }

    fun logoutTeacher() {
        _state.update { it.copy(teacherAccount = null).also(::persistState) }
    }

    fun pushAssignment(req: PushAssignmentRequest) {
        val activeTeacher = _state.value.teacherAccount ?: return
        val targetSchool = req.school.trim().ifEmpty { activeTeacher.school }
        val matchingLesson = CbcCurriculumRepository.lessons.firstOrNull {
            it.learningArea == req.learningArea && it.grade == req.grade
        } ?: CbcCurriculumRepository.lessons.firstOrNull {
            it.learningArea == req.learningArea
        } ?: CbcCurriculumRepository.lessons.first()

        val assignId = "assign-${System.currentTimeMillis()}"
        val isCustomMode = req.isCustom || req.customQuestions.isNotEmpty() || req.attachmentName.isNotBlank()
        val resolvedStrand = req.strand.trim().ifEmpty {
            if (isCustomMode) "Teacher Custom Task" else "Core Strand"
        }
        val resolvedSubStrand = req.subStrand.trim().ifEmpty {
            if (isCustomMode) req.customTitle.trim().ifEmpty { "Custom Worksheet" } else "Core Topic"
        }
        val resolvedTitle = req.customTitle.trim().ifEmpty {
            if (isCustomMode) {
                "$resolvedSubStrand (Teacher Custom Task)"
            } else {
                "$resolvedSubStrand (${matchingLesson.title})"
            }
        }
        val safeUrl = if (AssignmentUploadParser.isSafeAttachmentUrl(req.attachmentUrl)) {
            req.attachmentUrl.trim()
        } else {
            ""
        }

        val nowTime = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
        val created = TeacherAssignment(
            id = assignId,
            classStream = req.classStream.trim().ifEmpty { "Grade ${req.grade} East - $targetSchool" },
            school = targetSchool,
            grade = req.grade,
            learningArea = req.learningArea,
            strand = resolvedStrand,
            subStrand = resolvedSubStrand,
            lessonId = if (isCustomMode) "custom-$assignId" else matchingLesson.id,
            title = resolvedTitle,
            isCustom = isCustomMode,
            instructions = req.instructions.trim(),
            attachmentName = req.attachmentName.trim(),
            attachmentUrl = safeUrl,
            attachmentMimeType = req.attachmentMimeType.trim(),
            attachmentSizeBytes = req.attachmentSizeBytes.coerceAtLeast(0L),
            customQuestions = req.customQuestions.take(15),
            teacherName = activeTeacher.fullName,
            teacherCode = activeTeacher.teacherCode,
            dueLabel = if (isCustomMode) "Custom Teacher Task · Complete First" else "Priority · Complete First",
            pushedAt = nowTime,
            completedByChildIds = emptyList()
        )
        _state.update {
            it.copy(assignments = listOf(created) + it.assignments).also(::persistState)
        }
        scope.launch {
            FirestoreSyncClient.publishTeacherAssignment(
                assignment = created,
                teacherCode = activeTeacher.teacherCode,
                schoolName = activeTeacher.school
            )
        }
    }

    private fun encodeAssignments(list: List<TeacherAssignment>): String {
        val arr = JSONArray()
        list.forEach { a ->
            val qArr = JSONArray()
            a.customQuestions.take(15).forEach { q ->
                val optsArr = JSONArray().apply { q.options.forEach { put(it) } }
                val accArr = JSONArray().apply { q.acceptableAnswers.forEach { put(it) } }
                qArr.put(
                    JSONObject()
                        .put("id", q.id)
                        .put("type", if (q.type == QuestionType.FILL_BLANK) "fill_blank" else "multiple_choice")
                        .put("prompt", q.prompt)
                        .put("hint", q.hint)
                        .put("options", optsArr)
                        .put("correctAnswer", q.correctAnswer)
                        .put("acceptableAnswers", accArr)
                        .put("blankPrefix", q.blankPrefix)
                        .put("blankSuffix", q.blankSuffix)
                        .put("explanation", q.explanation)
                )
            }
            val doneArr = JSONArray().apply { a.completedByChildIds.forEach { put(it) } }
            arr.put(
                JSONObject()
                    .put("id", a.id)
                    .put("classStream", a.classStream)
                    .put("school", a.school)
                    .put("grade", a.grade)
                    .put("learningArea", a.learningArea.displayName)
                    .put("strand", a.strand)
                    .put("subStrand", a.subStrand)
                    .put("lessonId", a.lessonId)
                    .put("title", a.title)
                    .put("isCustom", a.isCustom)
                    .put("instructions", a.instructions)
                    .put("attachmentName", a.attachmentName)
                    .put("attachmentUrl", a.attachmentUrl)
                    .put("attachmentMimeType", a.attachmentMimeType)
                    .put("attachmentSizeBytes", a.attachmentSizeBytes)
                    .put("customQuestions", qArr)
                    .put("teacherName", a.teacherName)
                    .put("teacherCode", a.teacherCode)
                    .put("dueLabel", a.dueLabel)
                    .put("pushedAt", a.pushedAt)
                    .put("completedByChildIds", doneArr)
            )
        }
        return arr.toString()
    }

    private fun decodeAssignments(rawJson: String?): List<TeacherAssignment> {
        if (rawJson.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(rawJson)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    val areaName = o.optString("learningArea", "Mathematics")
                    val area = LearningArea.entries.firstOrNull { it.displayName == areaName }
                        ?: LearningArea.MATHEMATICS
                    val qArr = o.optJSONArray("customQuestions")
                    val questions = buildList {
                        if (qArr != null) {
                            for (j in 0 until minOf(qArr.length(), 15)) {
                                val qo = qArr.optJSONObject(j) ?: continue
                                val optsArr = qo.optJSONArray("options")
                                val opts = buildList {
                                    if (optsArr != null) {
                                        for (k in 0 until optsArr.length()) {
                                            val v = optsArr.optString(k)
                                            if (v.isNotEmpty()) add(v)
                                        }
                                    }
                                }
                                val accArr = qo.optJSONArray("acceptableAnswers")
                                val acc = buildList {
                                    if (accArr != null) {
                                        for (k in 0 until accArr.length()) {
                                            val v = accArr.optString(k)
                                            if (v.isNotEmpty()) add(v)
                                        }
                                    }
                                }
                                add(
                                    QuizQuestion(
                                        id = qo.optString("id", "tq-$j"),
                                        type = if (qo.optString("type") == "fill_blank") {
                                            QuestionType.FILL_BLANK
                                        } else {
                                            QuestionType.MULTIPLE_CHOICE
                                        },
                                        prompt = qo.optString("prompt"),
                                        hint = qo.optString("hint", "Read carefully before checking."),
                                        options = opts,
                                        correctAnswer = qo.optString("correctAnswer"),
                                        acceptableAnswers = acc,
                                        blankPrefix = qo.optString("blankPrefix", ""),
                                        blankSuffix = qo.optString("blankSuffix", ""),
                                        explanation = qo.optString("explanation", "")
                                    )
                                )
                            }
                        }
                    }
                    val doneArr = o.optJSONArray("completedByChildIds")
                    val doneIds = buildList {
                        if (doneArr != null) {
                            for (j in 0 until doneArr.length()) {
                                val v = doneArr.optString(j)
                                if (v.isNotEmpty()) add(v)
                            }
                        }
                    }
                    val rawUrl = o.optString("attachmentUrl", "")
                    add(
                        TeacherAssignment(
                            id = o.optString("id"),
                            classStream = o.optString("classStream"),
                            school = o.optString("school", "Nairobi Primary School"),
                            grade = o.optInt("grade", 6).coerceIn(1, 12),
                            learningArea = area,
                            strand = o.optString("strand", "Teacher Custom Task"),
                            subStrand = o.optString("subStrand", "Custom Worksheet"),
                            lessonId = o.optString("lessonId"),
                            title = o.optString("title"),
                            isCustom = o.optBoolean("isCustom", false) || questions.isNotEmpty(),
                            instructions = o.optString("instructions", ""),
                            attachmentName = o.optString("attachmentName", ""),
                            attachmentUrl = if (AssignmentUploadParser.isSafeAttachmentUrl(rawUrl)) rawUrl else "",
                            attachmentMimeType = o.optString("attachmentMimeType", ""),
                            attachmentSizeBytes = o.optLong("attachmentSizeBytes", 0L),
                            customQuestions = questions,
                            teacherName = o.optString("teacherName", "Class Teacher"),
                            teacherCode = o.optString("teacherCode", ""),
                            dueLabel = o.optString("dueLabel", "Priority · Complete First"),
                            pushedAt = o.optString("pushedAt", "Today"),
                            completedByChildIds = doneIds
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun setGuardServiceRunning(running: Boolean) {
        _state.update { it.copy(guardServiceRunning = running) }
    }

    fun recordDisplayedAssignment(
        childId: String,
        trackingKey: String,
        quizId: String,
        resetCatCycle: Boolean = false
    ) {
        _state.update { current ->
            val prevKeys = current.displayedKeysByChild[childId].orEmpty()
            val baseKeys = if (resetCatCycle) {
                prevKeys.filterNot { it.startsWith("cat:") }
            } else {
                prevKeys.filterNot { it == trackingKey }
            }
            val nextKeys = baseKeys + trackingKey

            val prevQuizzes = current.displayedQuizIdsByChild[childId].orEmpty()
            val baseQuizzes = if (resetCatCycle) emptyList() else prevQuizzes.filterNot { it == quizId }
            val nextQuizzes = baseQuizzes + quizId

            current.copy(
                displayedKeysByChild = current.displayedKeysByChild + (childId to nextKeys),
                displayedQuizIdsByChild = current.displayedQuizIdsByChild + (childId to nextQuizzes)
            ).also(::persistState)
        }
    }

    fun updateForegroundApp(packageName: String?) {
        val trackable = UsageAccessHelper.isTrackableInterruptedApp(appContext, packageName)
        val launcherPkg = UsageAccessHelper.getDefaultLauncherPackage(appContext)
        _state.update { current ->
            val nextInterrupted = when {
                trackable -> packageName
                !current.isTakeoverLocked && packageName != null && packageName == launcherPkg -> null
                else -> current.lastInterruptedAppPackage
            }
            val changed = current.lastDetectedForegroundApp != packageName ||
                current.lastInterruptedAppPackage != nextInterrupted
            if (!changed) return@update current
            current.copy(
                lastDetectedForegroundApp = packageName,
                lastInterruptedAppPackage = nextInterrupted
            ).also { if (current.lastInterruptedAppPackage != nextInterrupted) persistState(it) }
        }
    }

    fun resetDemo() {
        prefs.edit().clear().apply()
        runCatching {
            appContext.getSharedPreferences(LEGACY_PREFS_V2, Context.MODE_PRIVATE).edit().clear().apply()
        }
        _state.value = SomaTimeUiState()
    }

    fun loadSampleDemo() {
        val demoChildren = CbcCurriculumRepository.initialChildren()
        val demoState = SomaTimeUiState(
            roleView = RoleView.PARENT,
            selectedChildId = demoChildren.firstOrNull()?.id ?: "",
            handOffActive = false,
            demoSeeded = true,
            parentName = "Grace Wanjiku",
            parentContact = "0712 345 678",
            parentPin = CbcCurriculumRepository.PARENT_PIN,
            children = demoChildren,
            assignments = CbcCurriculumRepository.initialAssignments(),
            classRoster = CbcCurriculumRepository.initialClassRoster()
        ).also(::persistState)
        _state.value = demoState
        publishExistingChildrenIfParent()
    }

    companion object {
        private const val PREFS_NAME = "somatime_prefs_v3"
        private const val LEGACY_PREFS_V2 = "somatime_prefs_v2"

        @Volatile
        private var instance: SomaTimeStore? = null

        fun getInstance(context: Context): SomaTimeStore {
            return instance ?: synchronized(this) {
                instance ?: SomaTimeStore(context).also { instance = it }
            }
        }

        fun formatClock(totalSeconds: Int): String {
            val safe = totalSeconds.coerceAtLeast(0)
            val mins = safe / 60
            val secs = safe % 60
            return String.format(Locale.US, "%02d:%02d", mins, secs)
        }
    }
}
