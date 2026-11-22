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
import com.somatime.app.model.PendingApproval
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

enum class SetupWizardTab {
    CHOICE,
    PARENT,
    LEARNER,
    TEACHER
}

data class SomaTimeUiState(
    val roleView: RoleView = RoleView.PARENT,
    val selectedChildId: String = "amina",
    val handOffActive: Boolean = false,
    val parentName: String = "Grace Wanjiku",
    val parentContact: String = "0712 345 678",
    val parentPin: String = CbcCurriculumRepository.PARENT_PIN,
    val teacherAccount: TeacherAccount? = null,
    val registeredTeachers: List<TeacherAccount> = emptyList(),
    val showSetupWizard: Boolean = false,
    val setupInitialTab: SetupWizardTab = SetupWizardTab.CHOICE,
    val guardServiceRunning: Boolean = false,
    val cloudSyncOk: Boolean = false,
    val cloudSyncMessage: String = "Syncing pairing codes to Cloud...",
    val lastDetectedForegroundApp: String? = null,
    val lastInterruptedAppPackage: String? = null,
    val displayedKeysByChild: Map<String, List<String>> = emptyMap(),
    val displayedQuizIdsByChild: Map<String, List<String>> = emptyMap(),
    val children: List<ChildProfile> = CbcCurriculumRepository.initialChildren(),
    val assignments: List<TeacherAssignment> = CbcCurriculumRepository.initialAssignments(),
    val classRoster: List<ClassStudentRecord> = CbcCurriculumRepository.initialClassRoster()
) {
    val selectedChild: ChildProfile
        get() = children.firstOrNull { it.id == selectedChildId } ?: children.first()

    val isTakeoverLocked: Boolean
        get() = roleView == RoleView.CHILD &&
            !showSetupWizard &&
            (selectedChild.isLocked || selectedChild.availableScreenSeconds <= 0) &&
            (handOffActive || selectedChild.isPaired)
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
        scope.launch {
            var allOk = true
            var lastErr = ""
            current.children.forEach { child ->
                val res = FirestoreSyncClient.publishPairingSession(child, current.parentPin)
                if (!res.ok) {
                    allOk = false
                    lastErr = res.errorMessage
                }
            }
            _state.update {
                it.copy(
                    cloudSyncOk = allOk,
                    cloudSyncMessage = if (allOk) {
                        "Synced to Cloud ✓ · Ready for daughter's phone"
                    } else {
                        lastErr.ifEmpty { "Cloud sync pending — check internet and tap Retry Sync" }
                    }
                )
            }
        }
    }

    private fun loadInitialState(): SomaTimeUiState {
        val base = SomaTimeUiState()
        val savedChildId = prefs.getString("selected_child_id", null) ?: base.selectedChildId
        val savedRole = prefs.getString("role_view", null)?.let {
            runCatching { RoleView.valueOf(it) }.getOrNull()
        } ?: base.roleView
        val handOff = prefs.getBoolean("hand_off_active", false)
        val parentName = prefs.getString("parent_name", base.parentName) ?: base.parentName
        val parentContact = prefs.getString("parent_contact", base.parentContact) ?: base.parentContact
        val parentPin = prefs.getString("parent_pin", base.parentPin) ?: base.parentPin

        val customIds = prefs.getStringSet("custom_child_ids", emptySet()) ?: emptySet()
        val baseIds = base.children.map { it.id }.toSet()

        val restoredBase = base.children.map { child ->
            val prefix = "child_${child.id}_"
            if (!prefs.contains("${prefix}secs")) return@map child
            child.copy(
                availableScreenSeconds = prefs.getInt("${prefix}secs", child.availableScreenSeconds),
                usedScreenTodayMins = prefs.getInt("${prefix}used", child.usedScreenTodayMins),
                learnedMinutesToday = prefs.getInt("${prefix}learned", child.learnedMinutesToday),
                dailyCapMins = prefs.getInt("${prefix}cap", child.dailyCapMins),
                exchangeRateMins = prefs.getInt("${prefix}rate", child.exchangeRateMins),
                requireParentApproval = prefs.getBoolean("${prefix}approval", child.requireParentApproval),
                isLocked = prefs.getBoolean("${prefix}locked", child.isLocked),
                isPaired = prefs.getBoolean("${prefix}paired", child.isPaired),
                pairingCode = prefs.getString("${prefix}code", child.pairingCode) ?: child.pairingCode
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
                streakDays = 1
            )
        }

        val allChildren = restoredCustom + restoredBase
        val registeredTeachers = decodeTeachers(prefs.getString("registered_teachers_json", null))
        val activeTeacherCode = prefs.getString("active_teacher_code", null)
        val activeTeacher = registeredTeachers.firstOrNull { it.teacherCode == activeTeacherCode }
        val lastInterrupted = prefs.getString("last_interrupted_app_package", null)
        val displayedKeys = decodeStringListMap(prefs.getString("displayed_keys_json", null))
        val displayedQuizzes = decodeStringListMap(prefs.getString("displayed_quiz_ids_json", null))

        return base.copy(
            roleView = savedRole,
            selectedChildId = savedChildId,
            handOffActive = handOff,
            parentName = parentName,
            parentContact = parentContact,
            parentPin = parentPin,
            teacherAccount = activeTeacher,
            registeredTeachers = registeredTeachers,
            lastInterruptedAppPackage = lastInterrupted,
            displayedKeysByChild = displayedKeys,
            displayedQuizIdsByChild = displayedQuizzes,
            children = allChildren
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

    private fun persistState(s: SomaTimeUiState) {
        val editor = prefs.edit()
        val defaultIds = setOf("amina", "brian")
        val customIds = s.children.map { it.id }.filter { it !in defaultIds }.toSet()
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
        val child = current.selectedChild
        _state.update { it.copy(cloudSyncMessage = "Syncing ${child.pairingCode} to Cloud...") }
        scope.launch {
            val res = FirestoreSyncClient.publishPairingSession(child, current.parentPin)
            _state.update {
                it.copy(
                    cloudSyncOk = res.ok,
                    cloudSyncMessage = if (res.ok) {
                        "Synced to Cloud ✓ · Code ${child.pairingCode} is active"
                    } else {
                        res.errorMessage
                    }
                )
            }
        }
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
                            "Synced to Cloud ✓ · Code ${target.pairingCode} is active"
                        } else {
                            res.errorMessage
                        }
                    )
                }
            }
        }
    }

    fun regeneratePairingCode(childId: String): String {
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
            streakDays = 1
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
                    "Synced to Cloud ✓ · Enter $code on your daughter's phone"
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
                streakDays = existingLocal?.streakDays ?: 1
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

    suspend fun syncPairedChildWithCloud() {
        val current = _state.value
        val child = current.selectedChild
        if (!FirestoreSyncClient.isValidCode(FirestoreSyncClient.normalizeCode(child.pairingCode))) return

        if (current.roleView == RoleView.CHILD && child.isPaired) {
            val cloudRes = FirestoreSyncClient.fetchPairingSession(child.pairingCode)
            val cloud = cloudRes.data
            if (cloudRes.ok && cloud != null) {
                val remoteUnlocked =
                    !cloud.isLocked && child.isLocked && cloud.availableScreenSeconds > child.availableScreenSeconds
                val settingsChanged =
                    cloud.dailyCapMins != child.dailyCapMins ||
                        cloud.exchangeRateMins != child.exchangeRateMins ||
                        cloud.requireParentApproval != child.requireParentApproval ||
                        cloud.parentPin != current.parentPin
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
                    return
                }
            }
            FirestoreSyncClient.patchLearnerSync(_state.value.selectedChild)
        } else if (current.roleView == RoleView.PARENT) {
            val cloudRes = FirestoreSyncClient.fetchPairingSession(child.pairingCode)
            val cloud = cloudRes.data ?: return
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
                    } else {
                        "Synced to Cloud ✓ · Ready for daughter's phone"
                    },
                    children = nextChildren
                ).also(::persistState)
            }
        }

        // Also fetch teacher assignments pushed for this learner's school and grade
        val assignRes = FirestoreSyncClient.fetchSchoolGradeAssignments(child.school, child.grade)
        val cloudAssignments = assignRes.data
        if (assignRes.ok && !cloudAssignments.isNullOrEmpty()) {
            _state.update { s ->
                val existingMap = s.assignments.associateBy { it.id }
                val mergedCloud = cloudAssignments.map { ca ->
                    val prev = existingMap[ca.id]
                    if (prev != null) ca.copy(completedByChildIds = prev.completedByChildIds) else ca
                }
                val cloudIds = mergedCloud.map { it.id }.toSet()
                val remaining = s.assignments.filterNot { it.id in cloudIds }
                s.copy(assignments = mergedCloud + remaining).also(::persistState)
            }
        }
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
            streakDays = 1
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
                        "Synced to Cloud ✓ · Code ${created.pairingCode} is active"
                    } else {
                        res.errorMessage
                    }
                )
            }
        }
    }

    fun activateHandOffMode(childId: String) {
        var updatedTarget: ChildProfile? = null
        _state.update { current ->
            val nextChildren = current.children.map {
                if (it.id == childId) {
                    it.copy(
                        deviceMode = DeviceMode.HAND_OFF,
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
                if (assign.lessonId == lesson.id && childId !in assign.completedByChildIds) {
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

    suspend fun registerTeacherAsync(
        fullName: String,
        school: String,
        tscNumber: String,
        pin: String,
        subjects: List<LearningArea>,
        grade: Int,
        classStream: String
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

        _state.update { s ->
            val nextList = listOf(saved) + s.registeredTeachers.filterNot { it.teacherCode == saved.teacherCode }
            s.copy(
                roleView = RoleView.TEACHER,
                teacherAccount = saved,
                registeredTeachers = nextList
            ).also(::persistState)
        }
        return saved
    }

    suspend fun loginTeacherAsync(rawCode: String, rawPin: String): TeacherLoginResult {
        val code = FirestoreSyncClient.normalizeTeacherCode(rawCode)
        val pin = rawPin.filter(Char::isDigit).take(4)
        if (code.isEmpty() || pin.length != 4) {
            return TeacherLoginResult(false, message = "Enter both your Teacher Code (TCH-XXXX) and 4-digit PIN.")
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

    fun pushAssignment(
        classStream: String,
        school: String,
        grade: Int,
        learningArea: LearningArea,
        strand: String,
        subStrand: String
    ) {
        val activeTeacher = _state.value.teacherAccount
        val targetSchool = school.trim().ifEmpty { activeTeacher?.school ?: "Nairobi Primary School" }
        val matchingLesson = CbcCurriculumRepository.lessons.firstOrNull {
            it.learningArea == learningArea && it.grade == grade
        } ?: CbcCurriculumRepository.lessons.firstOrNull {
            it.learningArea == learningArea
        } ?: CbcCurriculumRepository.lessons.first()

        val nowTime = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
        val created = TeacherAssignment(
            id = "assign-${System.currentTimeMillis()}",
            classStream = classStream.trim().ifEmpty { "Grade $grade East - $targetSchool" },
            school = targetSchool,
            grade = grade,
            learningArea = learningArea,
            strand = strand,
            subStrand = subStrand,
            lessonId = matchingLesson.id,
            title = "$subStrand (${matchingLesson.title})",
            teacherName = activeTeacher?.fullName ?: "Class Teacher",
            teacherCode = activeTeacher?.teacherCode ?: "",
            dueLabel = "Priority · Complete First",
            pushedAt = nowTime,
            completedByChildIds = emptyList()
        )
        _state.update {
            it.copy(assignments = listOf(created) + it.assignments).also(::persistState)
        }
        scope.launch {
            FirestoreSyncClient.publishSchoolGradeAssignment(created, targetSchool)
        }
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
        val fresh = SomaTimeUiState().also(::persistState)
        _state.value = fresh
        publishExistingChildrenIfParent()
    }

    companion object {
        private const val PREFS_NAME = "somatime_prefs_v2"

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
