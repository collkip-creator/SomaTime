package com.somatime.app.data

import android.content.Context
import android.content.SharedPreferences
import com.somatime.app.model.CBCLesson
import com.somatime.app.model.CBCRubric
import com.somatime.app.model.ChildActivityLog
import com.somatime.app.model.ChildProfile
import com.somatime.app.model.ClassStudentRecord
import com.somatime.app.model.DeviceMode
import com.somatime.app.model.LearningArea
import com.somatime.app.model.PendingApproval
import com.somatime.app.model.RoleView
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

data class SomaTimeUiState(
    val roleView: RoleView = RoleView.PARENT,
    val selectedChildId: String = "amina",
    val handOffActive: Boolean = false,
    val parentName: String = "Grace Wanjiku",
    val parentContact: String = "0712 345 678",
    val parentPin: String = CbcCurriculumRepository.PARENT_PIN,
    val showSetupWizard: Boolean = false,
    val guardServiceRunning: Boolean = false,
    val cloudSyncOk: Boolean = false,
    val cloudSyncMessage: String = "Syncing pairing codes to Cloud...",
    val lastDetectedForegroundApp: String? = null,
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

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var tickerJob: Job? = null

    private val _state = MutableStateFlow(loadInitialState())
    val state: StateFlow<SomaTimeUiState> = _state.asStateFlow()

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

        return base.copy(
            roleView = savedRole,
            selectedChildId = savedChildId,
            handOffActive = handOff,
            parentName = parentName,
            parentContact = parentContact,
            parentPin = parentPin,
            children = allChildren
        )
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
            while (isActive) {
                delay(1000L)
                var newlyLockedChild: ChildProfile? = null
                _state.update { current ->
                    val anyTicking = current.children.any { !it.isLocked && it.availableScreenSeconds > 0 }
                    if (!anyTicking) return@update current
                    val updated = current.children.map { child ->
                        if (child.isLocked || child.availableScreenSeconds <= 0) {
                            child
                        } else {
                            val nextSecs = (child.availableScreenSeconds - 1).coerceAtLeast(0)
                            val minuteTicked = nextSecs % 60 == 59 && child.availableScreenSeconds > 0
                            val nextUsed = if (minuteTicked) {
                                (child.usedScreenTodayMins + 1).coerceAtMost(child.dailyCapMins)
                            } else {
                                child.usedScreenTodayMins
                            }
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

    fun setShowSetupWizard(show: Boolean) {
        _state.update { it.copy(showSetupWizard = show) }
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
                val remoteUnlockedOrChanged =
                    (!cloud.isLocked && child.isLocked && cloud.availableScreenSeconds > child.availableScreenSeconds) ||
                        cloud.dailyCapMins != child.dailyCapMins ||
                        cloud.exchangeRateMins != child.exchangeRateMins ||
                        cloud.parentPin != current.parentPin
                if (remoteUnlockedOrChanged) {
                    _state.update { s ->
                        val nextChildren = s.children.map { c ->
                            if (c.id != child.id) {
                                c
                            } else {
                                c.copy(
                                    dailyCapMins = cloud.dailyCapMins,
                                    exchangeRateMins = cloud.exchangeRateMins,
                                    requireParentApproval = cloud.requireParentApproval,
                                    availableScreenSeconds = cloud.availableScreenSeconds,
                                    isLocked = cloud.isLocked || cloud.availableScreenSeconds <= 0
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
            current.copy(handOffActive = false, children = next).also(::persistState)
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

    fun pushAssignment(
        classStream: String,
        grade: Int,
        learningArea: LearningArea,
        strand: String,
        subStrand: String
    ) {
        val matchingLesson = CbcCurriculumRepository.lessons.firstOrNull {
            it.learningArea == learningArea
        } ?: CbcCurriculumRepository.lessons.first()
        val nowTime = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
        val created = TeacherAssignment(
            id = "assign-${System.currentTimeMillis()}",
            classStream = classStream,
            grade = grade,
            learningArea = learningArea,
            strand = strand,
            subStrand = subStrand,
            lessonId = matchingLesson.id,
            title = "$subStrand (${matchingLesson.title})",
            dueLabel = "Priority · Complete First",
            pushedAt = nowTime,
            completedByChildIds = emptyList()
        )
        _state.update {
            it.copy(assignments = listOf(created) + it.assignments).also(::persistState)
        }
    }

    fun setGuardServiceRunning(running: Boolean) {
        _state.update { it.copy(guardServiceRunning = running) }
    }

    fun updateForegroundApp(packageName: String?) {
        _state.update { it.copy(lastDetectedForegroundApp = packageName) }
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
