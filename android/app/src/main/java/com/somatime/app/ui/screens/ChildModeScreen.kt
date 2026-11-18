package com.somatime.app.ui.screens

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
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.somatime.app.data.CbcCurriculumRepository
import com.somatime.app.data.LessonCompletionResult
import com.somatime.app.data.SomaTimeStore
import com.somatime.app.data.SomaTimeUiState
import com.somatime.app.model.CBCLesson
import com.somatime.app.model.LearningArea
import com.somatime.app.model.QuestionType
import com.somatime.app.ui.theme.SomaAmber
import com.somatime.app.ui.theme.SomaBlue
import com.somatime.app.ui.theme.SomaEmerald

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChildModeScreen(
    uiState: SomaTimeUiState,
    onSimulateTimeExpired: (String) -> Unit,
    onOpenPinUnlock: () -> Unit,
    onOpenExitHandOff: () -> Unit,
    onOpenSetupWizard: () -> Unit,
    onCompleteLesson: (String, CBCLesson, Int) -> LessonCompletionResult
) {
    val child = uiState.selectedChild
    val isLocked = child.isLocked || child.availableScreenSeconds <= 0
    val pendingAssignments = uiState.assignments.filter { child.id !in it.completedByChildIds }
    val mandatoryDone = child.mandatorySubjects.isEmpty() ||
        child.activityLogs.any { it.learningArea in child.mandatorySubjects }

    var activeLesson by remember { mutableStateOf<CBCLesson?>(null) }
    var ruleNotice by remember { mutableStateOf("") }
    var selectedGrade by remember { mutableIntStateOf(child.grade) }
    var selectedArea by remember { mutableStateOf<LearningArea?>(null) }

    fun launchLesson(lesson: CBCLesson, isPriority: Boolean) {
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
        activeLesson = lesson
    }

    if (activeLesson != null) {
        NativeLessonPlayer(
            lesson = activeLesson!!,
            exchangeRateMins = child.exchangeRateMins,
            onComplete = { score -> onCompleteLesson(child.id, activeLesson!!, score) },
            onExit = { activeLesson = null }
        )
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
                        if (child.isPaired) {
                            "Paired (${child.pairingCode}) · Background watch-minute guard active"
                        } else {
                            "Unpaired device · Tap Register as Learner to enter parent code"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (child.isPaired) SomaEmerald else Color(0xFF64748B),
                        fontWeight = FontWeight.SemiBold
                    )
                }
                if (uiState.handOffActive || child.isPaired) {
                    OutlinedButton(onClick = onOpenExitHandOff) {
                        Icon(Icons.Rounded.Key, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Exit")
                    }
                } else {
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
                            "Uninterruptible screen takeover active. YouTube, TikTok & Games are blocked until you complete one 5-minute Kenyan CBC activity or a parent enters PIN (${uiState.parentPin})."
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
                                    val target = if (pendingAssignments.isNotEmpty()) {
                                        CbcCurriculumRepository.lessons.firstOrNull {
                                            it.id == pendingAssignments.first().lessonId
                                        } ?: CbcCurriculumRepository.lessons.first()
                                    } else {
                                        CbcCurriculumRepository.lessons.firstOrNull {
                                            it.grade == child.grade
                                        } ?: CbcCurriculumRepository.lessons.first()
                                    }
                                    launchLesson(target, pendingAssignments.isNotEmpty())
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
                            "${priority.classStream} · ${priority.learningArea.displayName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF475569)
                        )
                        Button(
                            onClick = {
                                val target = CbcCurriculumRepository.lessons.firstOrNull {
                                    it.id == priority.lessonId
                                } ?: CbcCurriculumRepository.lessons.first()
                                launchLesson(target, true)
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

        // Playable 5-Minute Micro-Lessons
        item {
            Text(
                "Interactive 5-Minute CBC Activities",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        items(CbcCurriculumRepository.lessons) { lesson ->
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

        // Grade 1-12 CBC Curriculum Browser
        item {
            Text(
                "Kenyan CBC Curriculum Browser (Grades 1–12)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                (1..12).forEach { g ->
                    FilterChip(
                        selected = selectedGrade == g,
                        onClick = { selectedGrade = g },
                        label = { Text("G$g") }
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = selectedArea == null,
                    onClick = { selectedArea = null },
                    label = { Text("All Areas") }
                )
                LearningArea.entries.forEach { area ->
                    FilterChip(
                        selected = selectedArea == area,
                        onClick = { selectedArea = area },
                        label = { Text(area.displayName) }
                    )
                }
            }
        }

        val filteredCatalog = CbcCurriculumRepository.catalog.filter {
            it.grade == selectedGrade && (selectedArea == null || it.learningArea == selectedArea)
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
                            "${entry.learningArea.displayName} · ${entry.levelGroup}",
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
                        val linked = CbcCurriculumRepository.lessons.firstOrNull {
                            it.id == entry.linkedLessonId
                        } ?: CbcCurriculumRepository.lessons.first()
                        launchLesson(linked, false)
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
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onExit,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = SomaEmerald)
            ) {
                Text("Return to Child Mode")
            }
        }
        return
    }

    val q = lesson.questions[stepIndex]
    val progress = (stepIndex + if (submitted) 1 else 0).toFloat() / lesson.questions.size

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
                OutlinedButton(onClick = onExit) { Text("Back") }
                Text(
                    "Question ${stepIndex + 1} of ${lesson.questions.size} · +${exchangeRateMins}m",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF475569)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(progress = { progress.coerceAtLeast(0.1f) }, modifier = Modifier.fillMaxWidth())
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
