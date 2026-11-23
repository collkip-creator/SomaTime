package com.somatime.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.somatime.app.data.AssignmentUploadParser
import com.somatime.app.data.CbcCurriculumRepository
import com.somatime.app.data.FirestoreSyncClient
import com.somatime.app.data.LessonCompletionResult
import com.somatime.app.data.SomaTimeStore
import com.somatime.app.data.SomaTimeUiState
import com.somatime.app.guard.UsageAccessHelper
import com.somatime.app.model.CBCLesson
import com.somatime.app.model.LearningArea
import com.somatime.app.model.QuestionType
import com.somatime.app.ui.theme.SomaAmber
import com.somatime.app.ui.theme.SomaBlue
import com.somatime.app.ui.theme.SomaEmerald
import kotlinx.coroutines.delay

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChildModeScreen(
    uiState: SomaTimeUiState,
    onSimulateTimeExpired: (String) -> Unit,
    onOpenPinUnlock: () -> Unit,
    onOpenExitHandOff: () -> Unit,
    onOpenSetupWizard: () -> Unit,
    onOpenParentSetup: () -> Unit,
    onRecordDisplayedAssignment: (String, String, String, Boolean) -> Unit,
    onMinimizeAndResumeApp: () -> Unit,
    onCompleteLesson: (String, CBCLesson, Int) -> LessonCompletionResult
) {
    val child = uiState.selectedChild
    if (child == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "No Learner Profile Yet",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Pair this device with a parent's SOMA pairing code or register as a parent first to set your child's grade and CBC learning rules.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF475569)
                    )
                    Button(
                        onClick = onOpenSetupWizard,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E))
                    ) {
                        Text("Pair with Parent Code")
                    }
                    OutlinedButton(
                        onClick = onOpenParentSetup,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Register Parent & Child")
                    }
                }
            }
        }
        return
    }
    val learnerGrade = child.grade
    val isLocked = child.isLocked || child.availableScreenSeconds <= 0
    val linkedTeacherCodes = child.linkedTeacherCodes
        .map { FirestoreSyncClient.normalizeTeacherCode(it) }
        .filter { FirestoreSyncClient.isValidTeacherCode(it) }
        .toSet()
    val pendingAssignments = uiState.assignments.filter {
        val assignCode = FirestoreSyncClient.normalizeTeacherCode(it.teacherCode)
        it.grade == learnerGrade &&
            child.id !in it.completedByChildIds &&
            assignCode.isNotEmpty() &&
            assignCode in linkedTeacherCodes
    }
    val gradeLessons = CbcCurriculumRepository.lessons.filter { it.grade == learnerGrade }
    val hasMandatoryLessonForGrade = child.mandatorySubjects.isNotEmpty() &&
        gradeLessons.any { it.learningArea in child.mandatorySubjects }
    val mandatoryDone = !hasMandatoryLessonForGrade ||
        child.activityLogs.any { it.learningArea in child.mandatorySubjects }

    val context = LocalContext.current
    var activeLesson by remember { mutableStateOf<CBCLesson?>(null) }
    var activeIsPriority by remember { mutableStateOf(false) }
    var wasLocked by remember(child.id) { mutableStateOf(isLocked) }
    var ruleNotice by remember { mutableStateOf("") }
    var selectedArea by remember { mutableStateOf<LearningArea?>(null) }
    var curriculumSearch by remember { mutableStateOf("") }

    val displayedKeys = uiState.displayedKeysByChild[child.id].orEmpty()
    val displayedQuizIds = uiState.displayedQuizIdsByChild[child.id].orEmpty()
    val interruptedAppLabel = remember(uiState.lastInterruptedAppPackage) {
        UsageAccessHelper.getAppDisplayLabel(context, uiState.lastInterruptedAppPackage)
    }

    val gradeSubjects = remember(learnerGrade) {
        CbcCurriculumRepository.subjectsForGrade(learnerGrade)
    }
    val gradeQuestionCount = remember(learnerGrade) {
        CbcCurriculumRepository.questionCountForGrade(learnerGrade)
    }
    val effectiveSelectedArea = if (selectedArea == null || selectedArea in gradeSubjects) {
        selectedArea
    } else {
        null
    }

    fun launchLesson(
        lesson: CBCLesson,
        isPriority: Boolean,
        trackingKey: String = CbcCurriculumRepository.catalogTrackingKey(
            lesson.grade,
            lesson.learningArea,
            lesson.strand,
            lesson.subStrand
        ),
        quizId: String = lesson.id,
        resetCatCycle: Boolean = false
    ) {
        if (pendingAssignments.isNotEmpty() && !isPriority && pendingAssignments.first().lessonId != lesson.id) {
            ruleNotice = "Complete your Teacher Assignment (Priority): \"${pendingAssignments.first().title}\" first."
            return
        }
        if (!isPriority && !mandatoryDone && lesson.learningArea !in child.mandatorySubjects) {
            val requiredList = child.mandatorySubjects.joinToString(" or ") { it.displayName }
            ruleNotice = "Mandatory Focus Rule: Complete a $requiredList activity before free-choice ${lesson.learningArea.displayName}."
            return
        }
        ruleNotice = ""
        val scaledLesson = if (lesson.isCustom) {
            lesson
        } else {
            CbcCurriculumRepository.scaleLessonQuestionsForGrade(lesson, learnerGrade)
        }
        onRecordDisplayedAssignment(child.id, trackingKey, quizId, resetCatCycle)
        activeIsPriority = isPriority
        activeLesson = scaledLesson
    }

    // Immediately auto-launch a random unseen assignment whenever the app is pinned or transitions to locked
    val newlyLocked = isLocked && !wasLocked
    LaunchedEffect(uiState.isTakeoverLocked, isLocked, child.id, activeLesson) {
        val shouldAutoLaunch = (uiState.isTakeoverLocked || newlyLocked) && activeLesson == null
        wasLocked = isLocked
        if (shouldAutoLaunch) {
            val pick = CbcCurriculumRepository.pickNextUnseenAssignment(
                child = child,
                pendingAssignments = pendingAssignments,
                displayedKeys = displayedKeys,
                displayedQuizIds = displayedQuizIds
            )
            onRecordDisplayedAssignment(child.id, pick.trackingKey, pick.quizId, pick.wrappedCycle)
            ruleNotice = ""
            activeIsPriority = pick.isPriority
            activeLesson = pick.lesson
        }
    }

    if (activeLesson != null) {
        val currentLesson = activeLesson!!
        key("${currentLesson.id}:${currentLesson.strand}:${currentLesson.subStrand}") {
            NativeLessonPlayer(
                lesson = currentLesson,
                exchangeRateMins = child.exchangeRateMins,
                isPinnedTakeover = uiState.isTakeoverLocked,
                isPriority = activeIsPriority,
                resumedAppLabel = interruptedAppLabel,
                onOpenPinUnlock = onOpenPinUnlock,
                onOpenExitHandOff = if (uiState.isChildLockEnforced) onOpenExitHandOff else null,
                onMinimizeAndResumeApp = onMinimizeAndResumeApp,
                onComplete = { score -> onCompleteLesson(child.id, currentLesson, score) },
                onExit = { activeLesson = null }
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Welcome back, ${child.name.split(" ").first()}!",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Grade ${child.grade} · ${CbcCurriculumRepository.levelGroupForGrade(child.grade)} · ${child.school}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF475569)
                    )
                    Text(
                        when {
                            uiState.handOffActive -> "Shared-Phone Child Lock active · PIN required to exit"
                            child.isPaired && child.deviceMode == com.somatime.app.model.DeviceMode.OWN_PHONE ->
                                "Paired (${child.pairingCode}) · Background watch-minute guard active"
                            child.deviceMode == com.somatime.app.model.DeviceMode.HAND_OFF ->
                                "Shared Phone Ready · Tap Activate Child Lock on Parent tab to lock"
                            else -> "Unpaired device · Tap Pair to enter parent code"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (uiState.isChildLockEnforced) SomaEmerald else Color(0xFF64748B),
                        fontWeight = FontWeight.SemiBold
                    )
                }
                if (uiState.isChildLockEnforced) {
                    OutlinedButton(onClick = onOpenExitHandOff) {
                        Icon(Icons.Rounded.Key, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Exit")
                    }
                } else if (child.deviceMode != com.somatime.app.model.DeviceMode.HAND_OFF) {
                    OutlinedButton(onClick = onOpenSetupWizard) {
                        Icon(Icons.Rounded.Key, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Pair")
                    }
                }
            }
        }

        // Screen Time Guard Status & Lock Interception Card
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isLocked) Color(0xFF0F172A) else Color(0xFF065F46)
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                if (isLocked) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
                                contentDescription = null,
                                tint = if (isLocked) SomaAmber else Color(0xFF6EE7B7)
                            )
                            Text(
                                if (isLocked) "Screen Time Expired!" else "Entertainment Apps Unlocked",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            SomaTimeStore.formatClock(child.availableScreenSeconds),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isLocked) SomaAmber else Color.White
                        )
                    }

                    if (uiState.lastDetectedForegroundApp != null) {
                        Text(
                            "Last detected foreground app: ${uiState.lastDetectedForegroundApp}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFCBD5E1)
                        )
                    }

                    Text(
                        if (isLocked) {
                            "Uninterruptible screen takeover active. YouTube, TikTok & Games are blocked until you complete one 5-minute Kenyan CBC activity or a parent enters their PIN."
                        } else {
                            "Daily usage: ${child.usedScreenTodayMins}/${child.dailyCapMins} mins · Background watch-minute guard checking."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFE2E8F0)
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (isLocked) {
                            Button(
                                onClick = {
                                    val pick = CbcCurriculumRepository.pickNextUnseenAssignment(
                                        child = child,
                                        pendingAssignments = pendingAssignments,
                                        displayedKeys = displayedKeys,
                                        displayedQuizIds = displayedQuizIds
                                    )
                                    launchLesson(
                                        lesson = pick.lesson,
                                        isPriority = pick.isPriority,
                                        trackingKey = pick.trackingKey,
                                        quizId = pick.quizId,
                                        resetCatCycle = pick.wrappedCycle
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = SomaEmerald)
                            ) {
                                Text("Start CBC Lesson")
                            }
                            OutlinedButton(
                                onClick = onOpenPinUnlock,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                            ) {
                                Text("Parent PIN")
                            }
                        } else {
                            Button(
                                onClick = { onSimulateTimeExpired(child.id) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SomaAmber,
                                    contentColor = Color(0xFF0F172A)
                                )
                            ) {
                                Icon(Icons.Rounded.Timer, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Simulate Time Expired (00:00)")
                            }
                            if (!child.isPaired) {
                                OutlinedButton(
                                    onClick = onOpenSetupWizard,
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                                ) {
                                    Text("Pair Code")
                                }
                            }
                        }
                    }
                }
            }
        }

        // Teacher Priority Assignment Banner
        if (pendingAssignments.isNotEmpty()) {
            val priority = pendingAssignments.first()
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
                    border = BorderStroke(1.dp, Color(0xFFFCD34D))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "Teacher Assignment (Priority) · Must Complete First",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF92400E)
                        )
                        Text(priority.title, fontWeight = FontWeight.Bold)
                        Text(
                            "${priority.classStream} · ${priority.learningArea.displayName}" +
                                (if (priority.isCustom) " · Custom Task (${priority.customQuestions.size.coerceAtLeast(1)} Qs)" else ""),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF475569)
                        )
                        if (priority.instructions.isNotBlank()) {
                            Text(
                                priority.instructions,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF334155)
                            )
                        }
                        if (priority.attachmentName.isNotBlank()) {
                            Text(
                                "Attached: ${priority.attachmentName}" +
                                    if (priority.attachmentSizeBytes > 0) " (${AssignmentUploadParser.formatFileSize(priority.attachmentSizeBytes)})" else "",
                                style = MaterialTheme.typography.labelSmall,
                                color = SomaBlue,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Button(
                            onClick = {
                                val built = CbcCurriculumRepository.buildLessonFromTeacherAssignment(priority, learnerGrade)
                                launchLesson(
                                    lesson = built,
                                    isPriority = true,
                                    trackingKey = "assign:${priority.id}",
                                    quizId = priority.lessonId
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A))
                        ) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Start Priority Task")
                        }
                    }
                }
            }
        }

        if (ruleNotice.isNotEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFEFF6FF),
                    border = BorderStroke(1.dp, Color(0xFFBFDBFE))
                ) {
                    Text(
                        text = ruleNotice,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF1E3A8A),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Playable Micro-Lessons for Learner's Grade Only (Question count scales per grade)
        item {
            Text(
                "Grade $learnerGrade Interactive Activities ($gradeQuestionCount Questions)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Showing only Grade $learnerGrade (${CbcCurriculumRepository.levelGroupForGrade(learnerGrade)}) content · $gradeQuestionCount questions per session (scales from 3 in Grade 1)",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF64748B)
            )
        }

        val filteredGradeLessons = gradeLessons.filter {
            effectiveSelectedArea == null || it.learningArea == effectiveSelectedArea
        }
        items(filteredGradeLessons) { lesson ->
            val done = lesson.id in child.completedLessonIds
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Grade ${lesson.grade} · ${lesson.learningArea.displayName}",
                            style = MaterialTheme.typography.labelMedium,
                            color = SomaBlue,
                            fontWeight = FontWeight.Bold
                        )
                        if (done) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.CheckCircle,
                                    contentDescription = null,
                                    tint = SomaEmerald
                                )
                                Text("Done", style = MaterialTheme.typography.labelSmall, color = SomaEmerald)
                            }
                        }
                    }
                    Text(lesson.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "${lesson.strand} · ${lesson.subStrand}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF64748B)
                    )
                    Button(
                        onClick = { launchLesson(lesson, false) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = SomaBlue)
                    ) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Play 5-Min Activity (+${child.exchangeRateMins}m)")
                    }
                }
            }
        }

        // Grade-Locked CBC Curriculum Browser
        val gradeCatalogTotal = CbcCurriculumRepository.catalog.filter { it.grade == learnerGrade }
        val q = curriculumSearch.trim().lowercase()
        val filteredCatalog = gradeCatalogTotal.filter { entry ->
            val areaOk = effectiveSelectedArea == null || entry.learningArea == effectiveSelectedArea
            val queryOk = q.isEmpty() ||
                entry.learningArea.displayName.lowercase().contains(q) ||
                entry.strand.lowercase().contains(q) ||
                entry.subStrand.lowercase().contains(q)
            areaOk && queryOk
        }

        item {
            Text(
                "Grade $learnerGrade CBC Subjects, Strands & Sub-strands",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                "${CbcCurriculumRepository.levelGroupForGrade(learnerGrade)} · ${gradeSubjects.size} subjects · ${gradeCatalogTotal.size} sub-strands",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF64748B)
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = curriculumSearch,
                onValueChange = { curriculumSearch = it },
                label = { Text("Search strand or topic...") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = effectiveSelectedArea == null,
                    onClick = { selectedArea = null },
                    label = { Text("All Subjects (${gradeSubjects.size})") }
                )
                gradeSubjects.forEach { area ->
                    FilterChip(
                        selected = effectiveSelectedArea == area,
                        onClick = { selectedArea = area },
                        label = { Text(area.displayName) }
                    )
                }
            }
        }

        items(filteredCatalog) { entry ->
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "${entry.learningArea.displayName} · ${entry.levelGroup}" +
                                (entry.pathway?.let { " · $it" } ?: ""),
                            style = MaterialTheme.typography.labelMedium,
                            color = SomaEmerald,
                            fontWeight = FontWeight.Bold
                        )
                        Text("Strand: ${entry.strand}", fontWeight = FontWeight.SemiBold)
                        Text(
                            "Sub-strand: ${entry.subStrand}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF64748B)
                        )
                    }
                    OutlinedButton(onClick = {
                        val base = CbcCurriculumRepository.lessons.firstOrNull {
                            it.id == entry.linkedLessonId
                        } ?: gradeLessons.firstOrNull {
                            it.learningArea == entry.learningArea
                        } ?: CbcCurriculumRepository.lessons.firstOrNull {
                            it.learningArea == entry.learningArea
                        } ?: gradeLessons.firstOrNull() ?: CbcCurriculumRepository.lessons.first()

                        val contextual = if (
                            base.learningArea == entry.learningArea &&
                            base.strand == entry.strand &&
                            base.subStrand == entry.subStrand
                        ) {
                            base
                        } else {
                            base.copy(
                                title = "${entry.subStrand} Practice",
                                grade = learnerGrade,
                                levelGroup = entry.levelGroup,
                                learningArea = entry.learningArea,
                                strand = entry.strand,
                                subStrand = entry.subStrand
                            )
                        }
                        launchLesson(
                            lesson = contextual,
                            isPriority = false,
                            trackingKey = CbcCurriculumRepository.catalogTrackingKey(
                                learnerGrade,
                                entry.learningArea,
                                entry.strand,
                                entry.subStrand
                            ),
                            quizId = base.id
                        )
                    }) {
                        Text("Practice")
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
private fun NativeLessonPlayer(
    lesson: CBCLesson,
    exchangeRateMins: Int,
    isPinnedTakeover: Boolean,
    isPriority: Boolean,
    resumedAppLabel: String?,
    onOpenPinUnlock: () -> Unit,
    onOpenExitHandOff: (() -> Unit)? = null,
    onMinimizeAndResumeApp: () -> Unit,
    onComplete: (Int) -> LessonCompletionResult,
    onExit: () -> Unit
) {
    var stepIndex by remember { mutableIntStateOf(0) }
    var showHint by remember { mutableStateOf(false) }
    var selectedMcq by remember { mutableStateOf("") }
    var blankInput by remember { mutableStateOf("") }
    var selectedLeft by remember { mutableStateOf<String?>(null) }
    val matchedMap = remember { mutableStateMapOf<String, String>() }
    var submitted by remember { mutableStateOf(false) }
    var isCorrect by remember { mutableStateOf(false) }
    var correctCount by remember { mutableIntStateOf(0) }
    var result by remember { mutableStateOf<LessonCompletionResult?>(null) }
    var finalScore by remember { mutableIntStateOf(0) }

    if (result != null) {
        val outcome = result!!
        val shouldAutoMinimize = !outcome.queuedForApproval && !outcome.cappedOut
        val resumeName = resumedAppLabel ?: "Previous App"

        LaunchedEffect(result) {
            if (shouldAutoMinimize) {
                delay(1200L)
                onExit()
                onMinimizeAndResumeApp()
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Great Job! Lesson Complete",
                style = MaterialTheme.typography.labelLarge,
                color = SomaEmerald,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = when {
                    outcome.cappedOut -> "Daily Screen Cap Reached"
                    outcome.queuedForApproval -> "+${outcome.minutesEarned} Mins Sent for Parent PIN"
                    else -> "+${outcome.minutesEarned} Mins Earned!"
                },
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Score: $finalScore% · CBC Rubric: ${outcome.rubric.name} (${outcome.rubric.label})",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF475569)
            )
            if (shouldAutoMinimize) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    "Unpinning SomaTime & resuming $resumeName where it was interrupted...",
                    style = MaterialTheme.typography.bodySmall,
                    color = SomaEmerald,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            if (shouldAutoMinimize) {
                Button(
                    onClick = {
                        onExit()
                        onMinimizeAndResumeApp()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = SomaEmerald)
                ) {
                    Text("Minimize & Resume $resumeName")
                }
            } else {
                if (outcome.queuedForApproval) {
                    Button(
                        onClick = onOpenPinUnlock,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = SomaBlue)
                    ) {
                        Text("Enter Parent PIN to Unlock")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                OutlinedButton(
                    onClick = onExit,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Return to Child Mode")
                }
            }
        }
        return
    }

    val q = lesson.questions[stepIndex]
    val progress = (stepIndex + if (submitted) 1 else 0).toFloat() / lesson.questions.size

    val playerContext = LocalContext.current
    val resolvedAttachmentUrl = remember(lesson.attachmentUrl) {
        AssignmentUploadParser.resolveOpenUrl(lesson.attachmentUrl)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isPinnedTakeover) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(onClick = onOpenPinUnlock) {
                            Icon(Icons.Rounded.Key, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("+15m PIN")
                        }
                        if (onOpenExitHandOff != null) {
                            OutlinedButton(onClick = onOpenExitHandOff) {
                                Text("Exit Lock")
                            }
                        }
                    }
                } else {
                    OutlinedButton(onClick = onExit) { Text("Back") }
                }
                Text(
                    "Question ${stepIndex + 1} of ${lesson.questions.size} · +${exchangeRateMins}m",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF475569)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                (if (isPriority) "Teacher Priority · " else "") +
                    "Grade ${lesson.grade} · ${lesson.learningArea.displayName} · ${lesson.strand}",
                style = MaterialTheme.typography.labelSmall,
                color = if (isPriority) Color(0xFFB45309) else SomaEmerald,
                fontWeight = FontWeight.Bold
            )
            Text(
                lesson.subStrand,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF475569),
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(progress = { progress.coerceAtLeast(0.1f) }, modifier = Modifier.fillMaxWidth())
        }

        if (lesson.instructions.isNotBlank()) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF0FDF4),
                    border = BorderStroke(1.dp, Color(0xFFBBF7D0))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "Teacher Instructions",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = SomaEmerald
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            lesson.instructions,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF1E293B)
                        )
                    }
                }
            }
        }

        if (lesson.attachmentName.isNotBlank() || resolvedAttachmentUrl != null) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Rounded.AttachFile, contentDescription = null, tint = SomaBlue)
                            Column {
                                Text(
                                    lesson.attachmentName.ifBlank { "Uploaded Teacher Material" },
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                                if (lesson.attachmentSizeBytes > 0L) {
                                    Text(
                                        AssignmentUploadParser.formatFileSize(lesson.attachmentSizeBytes),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }
                        }
                        if (resolvedAttachmentUrl != null) {
                            OutlinedButton(
                                onClick = {
                                    runCatching {
                                        val viewIntent = Intent(Intent.ACTION_VIEW, Uri.parse(resolvedAttachmentUrl))
                                        playerContext.startActivity(viewIntent)
                                    }
                                }
                            ) {
                                Icon(Icons.Rounded.OpenInNew, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Open")
                            }
                        }
                    }
                }
            }
        }

        item {
            Text(
                q.prompt,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedButton(onClick = { showHint = !showHint }) {
                Icon(Icons.Rounded.HelpOutline, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (showHint) "Hide Study Hint" else "Show Study Hint")
            }
            if (showHint) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFFFBEB),
                    border = BorderStroke(1.dp, Color(0xFFFCD34D))
                ) {
                    Text(
                        q.hint,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF78350F)
                    )
                }
            }
        }

        when (q.type) {
            QuestionType.MULTIPLE_CHOICE -> {
                items(q.options) { opt ->
                    val selected = selectedMcq == opt
                    OutlinedButton(
                        onClick = { if (!submitted) selectedMcq = opt },
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(
                            if (selected) 2.dp else 1.dp,
                            if (selected) SomaBlue else Color(0xFFCBD5E1)
                        )
                    ) {
                        Text(opt)
                    }
                }
            }
            QuestionType.MATCHING -> {
                item {
                    val rightChoices = remember(q.id) {
                        val list = q.pairs.map { it.right }
                        if (list.size > 1) list.drop(1) + list.first() else list
                    }
                    Text(
                        "Tap an item on the left, then tap its match on the right.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF64748B)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            q.pairs.forEach { pair ->
                                val active = selectedLeft == pair.left
                                val mapped = matchedMap[pair.left]
                                OutlinedButton(
                                    onClick = { if (!submitted) selectedLeft = pair.left },
                                    modifier = Modifier.fillMaxWidth(),
                                    border = BorderStroke(
                                        if (active) 2.dp else 1.dp,
                                        if (active) SomaBlue else Color(0xFFCBD5E1)
                                    )
                                ) {
                                    Column {
                                        Text(pair.left, style = MaterialTheme.typography.labelMedium)
                                        if (mapped != null) {
                                            Text(
                                                "→ $mapped",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = SomaEmerald
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rightChoices.forEach { r ->
                                OutlinedButton(
                                    onClick = {
                                        if (!submitted && selectedLeft != null) {
                                            matchedMap[selectedLeft!!] = r
                                            selectedLeft = null
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(r, style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }
            }
            QuestionType.FILL_BLANK -> {
                item {
                    OutlinedTextField(
                        value = blankInput,
                        onValueChange = { if (!submitted) blankInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("${q.blankPrefix} _____ ${q.blankSuffix}".trim()) },
                        singleLine = true
                    )
                }
            }
        }

        if (submitted) {
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isCorrect) Color(0xFFECFDF5) else Color(0xFFFFFBEB),
                    border = BorderStroke(1.dp, if (isCorrect) SomaEmerald else SomaAmber)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            if (isCorrect) "Well Done! Correct Answer" else "Not quite! Expected: ${q.correctAnswer}",
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(q.explanation, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        item {
            if (!submitted) {
                val ready = when (q.type) {
                    QuestionType.MULTIPLE_CHOICE -> selectedMcq.isNotEmpty()
                    QuestionType.FILL_BLANK -> blankInput.isNotBlank()
                    QuestionType.MATCHING -> matchedMap.size == q.pairs.size
                }
                Button(
                    onClick = {
                        val correct = when (q.type) {
                            QuestionType.MULTIPLE_CHOICE -> selectedMcq == q.correctAnswer
                            QuestionType.FILL_BLANK -> {
                                val normalized = blankInput.trim().lowercase()
                                val accepted = q.acceptableAnswers.ifEmpty { listOf(q.correctAnswer) }
                                accepted.any { it.lowercase() == normalized }
                            }
                            QuestionType.MATCHING -> q.pairs.all { matchedMap[it.left] == it.right }
                        }
                        isCorrect = correct
                        submitted = true
                        if (correct) correctCount += 1
                    },
                    enabled = ready,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = SomaBlue)
                ) {
                    Text("Check Answer")
                }
            } else {
                Button(
                    onClick = {
                        if (stepIndex + 1 < lesson.questions.size) {
                            stepIndex += 1
                            showHint = false
                            selectedMcq = ""
                            blankInput = ""
                            selectedLeft = null
                            matchedMap.clear()
                            submitted = false
                            isCorrect = false
                        } else {
                            val score = (correctCount * 100) / lesson.questions.size
                            finalScore = score
                            result = onComplete(score)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = SomaEmerald)
                ) {
                    Text(if (stepIndex + 1 < lesson.questions.size) "Next Question" else "Claim Screen Time Reward")
                }
            }
        }
    }
}
