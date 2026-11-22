package com.somatime.app.ui.screens

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.somatime.app.data.AssignmentUploadParser
import com.somatime.app.data.CbcCurriculumCatalog
import com.somatime.app.data.CbcCurriculumRepository
import com.somatime.app.data.PushAssignmentRequest
import com.somatime.app.data.SomaTimeUiState
import com.somatime.app.model.CBCRubric
import com.somatime.app.model.LearningArea
import com.somatime.app.model.QuestionType
import com.somatime.app.model.QuizQuestion
import com.somatime.app.ui.theme.SomaAmber
import com.somatime.app.ui.theme.SomaEmerald
import com.somatime.app.ui.theme.SomaTeal
import java.util.Locale
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TeacherPortalScreen(
    uiState: SomaTimeUiState,
    onOpenTeacherAuth: () -> Unit,
    onQuickLinkParentAsTeacher: suspend (school: String, grade: Int, subjects: List<LearningArea>) -> Unit,
    onLogoutTeacher: () -> Unit,
    onPushAssignment: (PushAssignmentRequest) -> Unit,
    onPreviewChildMode: () -> Unit
) {
    val activeTeacher = uiState.teacherAccount
    val childFallback = uiState.selectedChild
    val initialArea = activeTeacher?.subjects?.firstOrNull() ?: LearningArea.MATHEMATICS
    val initialGroupedStrands = CbcCurriculumRepository.groupedStrandsForArea(initialArea)

    var schoolName by remember { mutableStateOf(activeTeacher?.school ?: childFallback?.school ?: "") }
    var targetGrade by remember { mutableIntStateOf(activeTeacher?.grade ?: childFallback?.grade ?: 6) }
    var classStream by remember {
        mutableStateOf(activeTeacher?.classStream ?: CbcCurriculumRepository.classStreams.first())
    }
    var isQuickLinking by remember { mutableStateOf(false) }
    var filterStrandsByGrade by remember { mutableStateOf(false) }
    var selectedArea by remember { mutableStateOf(initialArea) }
    var selectedStrand by remember {
        mutableStateOf(initialGroupedStrands.firstOrNull()?.strand ?: "Numbers")
    }
    var selectedSubStrand by remember {
        mutableStateOf(
            initialGroupedStrands.firstOrNull()?.subStrands?.firstOrNull()?.subStrand
                ?: "Fractions in Daily Life"
        )
    }
    var useCustomStrand by remember { mutableStateOf(false) }
    var customStrand by remember { mutableStateOf("") }
    var customSubStrand by remember { mutableStateOf("") }
    var composerMode by remember { mutableStateOf("cbc") } // "cbc" | "custom"
    var customTitle by remember { mutableStateOf("") }
    var customInstructions by remember { mutableStateOf("") }
    var customQuestions by remember { mutableStateOf<List<QuizQuestion>>(emptyList()) }
    var draftQuestionType by remember { mutableStateOf(QuestionType.MULTIPLE_CHOICE) }
    var draftPrompt by remember { mutableStateOf("") }
    var draftOptA by remember { mutableStateOf("") }
    var draftOptB by remember { mutableStateOf("") }
    var draftOptC by remember { mutableStateOf("") }
    var draftOptD by remember { mutableStateOf("") }
    var draftCorrectIdx by remember { mutableIntStateOf(0) }
    var draftBlankAnswer by remember { mutableStateOf("") }
    var draftHint by remember { mutableStateOf("") }
    var attachmentName by remember { mutableStateOf("") }
    var attachmentUrl by remember { mutableStateOf("") }
    var attachmentMime by remember { mutableStateOf("") }
    var attachmentSize by remember { mutableStateOf(0L) }
    var isUploadingFile by remember { mutableStateOf(false) }
    var uploadFeedback by remember { mutableStateOf("") }
    var pushedNotice by remember { mutableStateOf("") }

    var catalogSearch by remember { mutableStateOf("") }
    var expandedSubject by remember { mutableStateOf<LearningArea?>(initialArea) }

    var rosterStreamFilter by remember { mutableStateOf(CbcCurriculumRepository.classStreams.first()) }
    var rubricFilter by remember { mutableStateOf<CBCRubric?>(null) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        var resolvedName = "worksheet_upload"
        var resolvedSize = 0L
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (cursor.moveToFirst()) {
                if (nameIdx >= 0) resolvedName = cursor.getString(nameIdx) ?: resolvedName
                if (sizeIdx >= 0) resolvedSize = cursor.getLong(sizeIdx)
            }
        }
        val mime = context.contentResolver.getType(uri) ?: "application/octet-stream"
        val lower = resolvedName.lowercase(Locale.US)
        val allowedExts = listOf(".csv", ".json", ".txt", ".pdf", ".png", ".jpg", ".jpeg", ".webp", ".docx")
        if (allowedExts.none { lower.endsWith(it) }) {
            uploadFeedback = "Unsupported file type. Use CSV, JSON, TXT, PDF, DOCX, PNG, JPG, or WEBP."
            return@rememberLauncherForActivityResult
        }

        val bytes = runCatching {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        }.getOrNull()
        if (bytes == null || bytes.isEmpty()) {
            uploadFeedback = "Could not read selected file."
            return@rememberLauncherForActivityResult
        }
        if (bytes.size > 5 * 1024 * 1024) {
            uploadFeedback = "File is too large (${AssignmentUploadParser.formatFileSize(bytes.size.toLong())}). Max 5 MB."
            return@rememberLauncherForActivityResult
        }

        attachmentName = resolvedName
        attachmentMime = mime
        attachmentSize = if (resolvedSize > 0L) resolvedSize else bytes.size.toLong()
        attachmentUrl = ""

        val isStructuredText = lower.endsWith(".csv") || lower.endsWith(".json") || lower.endsWith(".txt")
        if (isStructuredText) {
            val textContent = bytes.toString(Charsets.UTF_8)
            val parsed = AssignmentUploadParser.parseUploadedQuestionFile(resolvedName, textContent)
            if (parsed.questions.isNotEmpty()) {
                customQuestions = (customQuestions + parsed.questions).take(15)
                uploadFeedback = "Imported ${parsed.questions.size} interactive question(s) from $resolvedName."
            } else {
                uploadFeedback = parsed.warnings.firstOrNull() ?: "Attached $resolvedName."
            }
        } else {
            uploadFeedback = "Uploading $resolvedName (${AssignmentUploadParser.formatFileSize(attachmentSize)})..."
        }

        isUploadingFile = true
        coroutineScope.launch {
            val uploadRes = AssignmentUploadParser.uploadAttachmentToServer(resolvedName, mime, bytes)
            isUploadingFile = false
            if (uploadRes.ok && uploadRes.data != null) {
                attachmentUrl = uploadRes.data.url
                if (!isStructuredText) {
                    uploadFeedback = "Uploaded $resolvedName (${AssignmentUploadParser.formatFileSize(uploadRes.data.sizeBytes)}) ready for learners."
                }
            } else if (!isStructuredText) {
                uploadFeedback = "Attached $resolvedName locally (${uploadRes.errorMessage})."
            }
        }
    }

    LaunchedEffect(activeTeacher?.teacherCode) {
        if (activeTeacher != null) {
            schoolName = activeTeacher.school
            targetGrade = activeTeacher.grade
            classStream = activeTeacher.classStream
            val firstSubj = activeTeacher.subjects.firstOrNull() ?: LearningArea.MATHEMATICS
            selectedArea = firstSubj
            expandedSubject = firstSubj
            val grouped = CbcCurriculumRepository.groupedStrandsForArea(firstSubj)
            selectedStrand = grouped.firstOrNull()?.strand ?: "Core Strand"
            selectedSubStrand = grouped.firstOrNull()?.subStrands?.firstOrNull()?.subStrand ?: "Priority Topic"
            useCustomStrand = false
        }
    }

    val mySubjects = activeTeacher?.subjects.orEmpty()
    val orderedAreas = remember(mySubjects) {
        mySubjects + LearningArea.entries.filterNot { it in mySubjects }
    }
    val groupedStrands = remember(selectedArea, filterStrandsByGrade, targetGrade) {
        val filtered = CbcCurriculumRepository.groupedStrandsForArea(
            selectedArea,
            if (filterStrandsByGrade) targetGrade else null
        )
        filtered.ifEmpty { CbcCurriculumRepository.groupedStrandsForArea(selectedArea) }
    }
    val currentStrandNode = groupedStrands.firstOrNull { it.strand == selectedStrand }
        ?: groupedStrands.firstOrNull()
    val subStrandOptions = currentStrandNode?.subStrands.orEmpty()

    LaunchedEffect(groupedStrands, selectedStrand, selectedSubStrand) {
        if (groupedStrands.isNotEmpty()) {
            val match = groupedStrands.firstOrNull { it.strand == selectedStrand }
            if (match == null) {
                selectedStrand = groupedStrands.first().strand
                selectedSubStrand = groupedStrands.first().subStrands.firstOrNull()?.subStrand ?: ""
            } else if (match.subStrands.none { it.subStrand == selectedSubStrand }) {
                selectedSubStrand = match.subStrands.firstOrNull()?.subStrand ?: ""
            }
        }
    }

    val (subjectCount, strandCount, subStrandCount) = remember {
        CbcCurriculumRepository.curriculumStats()
    }

    val streamStudents = uiState.classRoster.filter { it.classStream == rosterStreamFilter }
    val filteredStudents = streamStudents.filter { rubricFilter == null || it.rubric == rubricFilter }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Teacher & School Admin Portal",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Register with your school & subjects, sign in, and push CBC strand assignments to learners",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF475569)
            )
        }

        // Teacher Profile Banner OR Register/Login Prompt
        item {
            if (activeTeacher != null) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDFA)),
                    border = BorderStroke(1.dp, SomaTeal)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "${activeTeacher.fullName} · ${activeTeacher.teacherCode}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = SomaTeal
                                )
                                Text(
                                    "${activeTeacher.school} · Grade ${activeTeacher.grade}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF334155)
                                )
                                if (activeTeacher.tscNumber.isNotEmpty()) {
                                    Text(
                                        "Staff / TSC No: ${activeTeacher.tscNumber}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF475569)
                                    )
                                }
                            }
                            Icon(Icons.Rounded.School, contentDescription = null, tint = SomaTeal)
                        }

                        Text(
                            "Registered Subjects: ${activeTeacher.subjects.joinToString(", ") { it.displayName }}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF0F172A)
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = onOpenTeacherAuth,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Switch / Register")
                            }
                            OutlinedButton(
                                onClick = onLogoutTeacher,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Sign Out")
                            }
                        }
                    }
                }
            } else {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
                    border = BorderStroke(1.dp, SomaAmber)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Rounded.Key, contentDescription = null, tint = SomaAmber)
                            Text(
                                "Sign In or Register as a Teacher",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            "Register your name, school, grade, and CBC subject areas to receive a Teacher Code (TCH-XXXX) so your strand assignments sync automatically to learners in your school.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF475569)
                        )
                        if (uiState.isParentRegistered) {
                            Button(
                                onClick = {
                                    if (isQuickLinking) return@Button
                                    isQuickLinking = true
                                    coroutineScope.launch {
                                        onQuickLinkParentAsTeacher(
                                            schoolName.trim().ifEmpty { childFallback?.school ?: "Nairobi Primary School" },
                                            targetGrade,
                                            listOf(selectedArea)
                                        )
                                        isQuickLinking = false
                                    }
                                },
                                enabled = !isQuickLinking,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = SomaTeal)
                            ) {
                                Text(
                                    if (isQuickLinking) {
                                        "Linking Parent Account..."
                                    } else {
                                        "Quick-Link as ${uiState.parentName.split(" ").first()} (Same PIN)"
                                    }
                                )
                            }
                        }
                        Button(
                            onClick = onOpenTeacherAuth,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = SomaAmber)
                        ) {
                            Text("Register / Sign In as Teacher")
                        }
                    }
                }
            }
        }

        // Homework Push Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "Push CBC Priority Assignment",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = schoolName,
                        onValueChange = { schoolName = it },
                        label = { Text("School Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Text(
                        "Target Grade: Grade $targetGrade (${CbcCurriculumRepository.levelGroupForGrade(targetGrade)})",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Slider(
                        value = targetGrade.toFloat(),
                        onValueChange = {
                            val g = it.toInt()
                            targetGrade = g
                            classStream = "Grade $g East - $schoolName"
                        },
                        valueRange = 1f..12f,
                        steps = 10
                    )

                    OutlinedTextField(
                        value = classStream,
                        onValueChange = { classStream = it },
                        label = { Text("Class Stream") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            if (mySubjects.isNotEmpty()) {
                                "1. Subject (${orderedAreas.size} loaded, ★ = Yours)"
                            } else {
                                "1. Subject / CBC Learning Area (${orderedAreas.size})"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        FilterChip(
                            selected = filterStrandsByGrade,
                            onClick = { filterStrandsByGrade = !filterStrandsByGrade },
                            label = { Text("G$targetGrade only") }
                        )
                    }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        orderedAreas.forEach { area ->
                            val isMine = area in mySubjects
                            FilterChip(
                                selected = selectedArea == area,
                                onClick = {
                                    selectedArea = area
                                    expandedSubject = area
                                    val opts = CbcCurriculumRepository.groupedStrandsForArea(
                                        area,
                                        if (filterStrandsByGrade) targetGrade else null
                                    ).ifEmpty { CbcCurriculumRepository.groupedStrandsForArea(area) }
                                    selectedStrand = opts.firstOrNull()?.strand ?: "Core Strand"
                                    selectedSubStrand = opts.firstOrNull()?.subStrands?.firstOrNull()?.subStrand ?: "Priority Topic"
                                },
                                label = {
                                    Text(if (isMine) "★ ${area.displayName}" else area.displayName)
                                }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = composerMode == "cbc",
                            onClick = { composerMode = "cbc" },
                            label = { Text("CBC Strand Pick") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = composerMode == "custom",
                            onClick = { composerMode = "custom" },
                            label = { Text("Custom & Upload") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (composerMode == "cbc") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "2. CBC Strand (${groupedStrands.size} strands)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            TextButton(onClick = { useCustomStrand = !useCustomStrand }) {
                                Text(if (useCustomStrand) "Pick from List" else "+ Custom Strand")
                            }
                        }

                        if (!useCustomStrand) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                groupedStrands.forEach { st ->
                                    val active = selectedStrand == st.strand
                                    FilterChip(
                                        selected = active,
                                        onClick = {
                                            selectedStrand = st.strand
                                            selectedSubStrand = st.subStrands.firstOrNull()?.subStrand ?: ""
                                        },
                                        label = { Text("${st.strand} (${st.subStrands.size})") }
                                    )
                                }
                            }

                            Text(
                                "3. CBC Sub-strand (${subStrandOptions.size} topics)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                subStrandOptions.forEach { sub ->
                                    val active = selectedSubStrand == sub.subStrand
                                    FilterChip(
                                        selected = active,
                                        onClick = { selectedSubStrand = sub.subStrand },
                                        label = { Text("${sub.subStrand} (${sub.levelHint})") }
                                    )
                                }
                            }
                        } else {
                            OutlinedTextField(
                                value = customStrand,
                                onValueChange = { customStrand = it },
                                label = { Text("Strand (e.g. Numbers)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = customSubStrand,
                                onValueChange = { customSubStrand = it },
                                label = { Text("Sub-strand / Topic") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                    } else {
                        OutlinedTextField(
                            value = customTitle,
                            onValueChange = { customTitle = it },
                            label = { Text("Custom Assignment Title (e.g. Week 4 Fractions Drill)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = customInstructions,
                            onValueChange = { customInstructions = it },
                            label = { Text("Instructions for Learners") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2
                        )

                        // File Upload Zone
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    "Upload Worksheet or Question File",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "CSV, JSON, or TXT Q&A files auto-import into interactive questions. PDF, DOCX, PNG, JPG attach as study worksheets (max 5 MB).",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF64748B)
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = { filePickerLauncher.launch(arrayOf("*/*")) },
                                        enabled = !isUploadingFile,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Rounded.UploadFile, contentDescription = null)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(if (isUploadingFile) "Uploading..." else "Choose File")
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            val sample = AssignmentUploadParser.parseUploadedQuestionFile(
                                                "sample_template.csv",
                                                AssignmentUploadParser.SAMPLE_CSV_TEMPLATE
                                            )
                                            customQuestions = (customQuestions + sample.questions).take(15)
                                            uploadFeedback = "Loaded ${sample.questions.size} sample CBC questions from template."
                                        }
                                    ) {
                                        Text("Load Sample Qs")
                                    }
                                }
                                if (attachmentName.isNotEmpty()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(Icons.Rounded.AttachFile, contentDescription = null, tint = SomaTeal)
                                            Text(
                                                "$attachmentName (${AssignmentUploadParser.formatFileSize(attachmentSize)})",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                        TextButton(
                                            onClick = {
                                                attachmentName = ""
                                                attachmentUrl = ""
                                                attachmentMime = ""
                                                attachmentSize = 0L
                                            }
                                        ) {
                                            Text("Remove")
                                        }
                                    }
                                }
                                if (uploadFeedback.isNotEmpty()) {
                                    Text(
                                        uploadFeedback,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SomaTeal,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        // Custom Question Builder
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    "Add Custom Question (${customQuestions.size}/15)",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FilterChip(
                                        selected = draftQuestionType == QuestionType.MULTIPLE_CHOICE,
                                        onClick = { draftQuestionType = QuestionType.MULTIPLE_CHOICE },
                                        label = { Text("Multiple Choice") }
                                    )
                                    FilterChip(
                                        selected = draftQuestionType == QuestionType.FILL_BLANK,
                                        onClick = { draftQuestionType = QuestionType.FILL_BLANK },
                                        label = { Text("Fill in Blank") }
                                    )
                                }
                                OutlinedTextField(
                                    value = draftPrompt,
                                    onValueChange = { draftPrompt = it },
                                    label = { Text("Question Prompt") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                if (draftQuestionType == QuestionType.MULTIPLE_CHOICE) {
                                    val opts = listOf(draftOptA, draftOptB, draftOptC, draftOptD)
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        OutlinedTextField(
                                            value = draftOptA,
                                            onValueChange = { draftOptA = it },
                                            label = { Text("Option A") },
                                            modifier = Modifier.weight(1f),
                                            singleLine = true
                                        )
                                        OutlinedTextField(
                                            value = draftOptB,
                                            onValueChange = { draftOptB = it },
                                            label = { Text("Option B") },
                                            modifier = Modifier.weight(1f),
                                            singleLine = true
                                        )
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        OutlinedTextField(
                                            value = draftOptC,
                                            onValueChange = { draftOptC = it },
                                            label = { Text("Option C") },
                                            modifier = Modifier.weight(1f),
                                            singleLine = true
                                        )
                                        OutlinedTextField(
                                            value = draftOptD,
                                            onValueChange = { draftOptD = it },
                                            label = { Text("Option D") },
                                            modifier = Modifier.weight(1f),
                                            singleLine = true
                                        )
                                    }
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        listOf("A", "B", "C", "D").forEachIndexed { idx, label ->
                                            val hasVal = opts[idx].isNotBlank()
                                            FilterChip(
                                                selected = draftCorrectIdx == idx,
                                                onClick = { draftCorrectIdx = idx },
                                                enabled = hasVal,
                                                label = { Text("Correct: $label") }
                                            )
                                        }
                                    }
                                } else {
                                    OutlinedTextField(
                                        value = draftBlankAnswer,
                                        onValueChange = { draftBlankAnswer = it },
                                        label = { Text("Exact Expected Answer") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )
                                }
                                OutlinedTextField(
                                    value = draftHint,
                                    onValueChange = { draftHint = it },
                                    label = { Text("Study Hint (Optional)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                                OutlinedButton(
                                    onClick = {
                                        val cleanPrompt = draftPrompt.trim()
                                        if (cleanPrompt.isEmpty() || customQuestions.size >= 15) return@OutlinedButton
                                        val builtQ = if (draftQuestionType == QuestionType.MULTIPLE_CHOICE) {
                                            val allOpts = listOf(draftOptA, draftOptB, draftOptC, draftOptD)
                                                .map { it.trim() }
                                                .filter { it.isNotEmpty() }
                                            if (allOpts.size < 2) {
                                                uploadFeedback = "Enter at least 2 answer options for multiple choice."
                                                return@OutlinedButton
                                            }
                                            val picked = listOf(draftOptA, draftOptB, draftOptC, draftOptD)
                                                .getOrNull(draftCorrectIdx)?.trim()
                                                ?.takeIf { it.isNotEmpty() } ?: allOpts.first()
                                            QuizQuestion(
                                                id = "tq-${System.currentTimeMillis()}",
                                                type = QuestionType.MULTIPLE_CHOICE,
                                                prompt = cleanPrompt,
                                                hint = draftHint.trim().ifEmpty { "Read carefully before selecting your answer." },
                                                options = allOpts,
                                                correctAnswer = picked,
                                                explanation = "Correct answer: $picked"
                                            )
                                        } else {
                                            val ans = draftBlankAnswer.trim()
                                            if (ans.isEmpty()) {
                                                uploadFeedback = "Enter the expected answer for the fill-in-the-blank question."
                                                return@OutlinedButton
                                            }
                                            QuizQuestion(
                                                id = "tq-${System.currentTimeMillis()}",
                                                type = QuestionType.FILL_BLANK,
                                                prompt = cleanPrompt,
                                                hint = draftHint.trim().ifEmpty { "Check spelling and units before checking." },
                                                blankPrefix = "Answer:",
                                                blankSuffix = "",
                                                correctAnswer = ans,
                                                acceptableAnswers = listOf(ans.lowercase(Locale.US)),
                                                explanation = "Expected answer: $ans"
                                            )
                                        }
                                        customQuestions = customQuestions + builtQ
                                        draftPrompt = ""
                                        draftOptA = ""
                                        draftOptB = ""
                                        draftOptC = ""
                                        draftOptD = ""
                                        draftBlankAnswer = ""
                                        draftHint = ""
                                        draftCorrectIdx = 0
                                        uploadFeedback = ""
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Rounded.Add, contentDescription = null)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Add Question to Assignment")
                                }
                            }
                        }

                        if (customQuestions.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    "Custom Questions Queue (${customQuestions.size})",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                customQuestions.forEachIndexed { idx, q ->
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0xFFF1F5F9)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 10.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    "${idx + 1}. ${q.prompt}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                Text(
                                                    "Answer: ${q.correctAnswer}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = SomaEmerald
                                                )
                                            }
                                            TextButton(
                                                onClick = {
                                                    customQuestions = customQuestions.filterNot { it.id == q.id }
                                                }
                                            ) {
                                                Icon(Icons.Rounded.Delete, contentDescription = "Remove question")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Button(
                        onClick = {
                            val isCustomMode = composerMode == "custom"
                            if (isCustomMode && customQuestions.isEmpty() && attachmentName.isEmpty() && customTitle.isBlank()) {
                                uploadFeedback = "Enter a title, upload a worksheet, or add at least 1 question."
                                return@Button
                            }
                            val finalStrand = if (isCustomMode) {
                                customStrand.trim().ifEmpty { "Teacher Custom Task" }
                            } else if (useCustomStrand) {
                                customStrand.trim().ifEmpty { selectedStrand }
                            } else {
                                selectedStrand
                            }
                            val finalSub = if (isCustomMode) {
                                customTitle.trim().ifEmpty { customSubStrand.trim().ifEmpty { "Custom Worksheet" } }
                            } else if (useCustomStrand) {
                                customSubStrand.trim().ifEmpty { selectedSubStrand }
                            } else {
                                selectedSubStrand
                            }
                            onPushAssignment(
                                PushAssignmentRequest(
                                    classStream = classStream,
                                    school = schoolName,
                                    grade = targetGrade,
                                    learningArea = selectedArea,
                                    strand = finalStrand,
                                    subStrand = finalSub,
                                    isCustom = isCustomMode,
                                    customTitle = if (isCustomMode) customTitle.trim() else "",
                                    instructions = if (isCustomMode) customInstructions.trim() else "",
                                    attachmentName = if (isCustomMode) attachmentName else "",
                                    attachmentUrl = if (isCustomMode) attachmentUrl else "",
                                    attachmentMimeType = if (isCustomMode) attachmentMime else "",
                                    attachmentSizeBytes = if (isCustomMode) attachmentSize else 0L,
                                    customQuestions = if (isCustomMode) customQuestions else emptyList()
                                )
                            )
                            pushedNotice = if (isCustomMode) {
                                "${customTitle.trim().ifEmpty { finalSub }} (${customQuestions.size} Qs" +
                                    (if (attachmentName.isNotEmpty()) " + $attachmentName" else "") +
                                    ") pushed to Grade $targetGrade at $schoolName"
                            } else {
                                "${selectedArea.displayName} · $finalStrand ($finalSub) pushed to Grade $targetGrade at $schoolName"
                            }
                            if (isCustomMode) {
                                customTitle = ""
                                customInstructions = ""
                                customQuestions = emptyList()
                                attachmentName = ""
                                attachmentUrl = ""
                                attachmentMime = ""
                                attachmentSize = 0L
                                uploadFeedback = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = SomaTeal)
                    ) {
                        Icon(Icons.Rounded.Send, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (composerMode == "custom") "Publish Custom Assignment" else "Push Assignment to Students")
                    }

                    if (pushedNotice.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFECFDF5),
                            border = BorderStroke(1.dp, SomaEmerald)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Synced to Cloud & Learner Devices!", fontWeight = FontWeight.Bold)
                                Text(pushedNotice, style = MaterialTheme.typography.bodySmall)
                                Button(
                                    onClick = onPreviewChildMode,
                                    modifier = Modifier.padding(top = 8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = SomaEmerald)
                                ) {
                                    Text("Preview in Child Mode")
                                }
                            }
                        }
                    }
                }
            }
        }

        // Active Pushed Assignments Feed
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Active Priority Assignments (${uiState.assignments.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (uiState.assignments.isEmpty()) {
                        Text(
                            "No assignments pushed yet. Use the composer above to push a CBC strand or custom assignment.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF64748B)
                        )
                    }
                    uiState.assignments.forEach { assign ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        assign.title,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        assign.pushedAt,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SomaTeal,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    "${assign.school} · Grade ${assign.grade} · ${assign.learningArea.displayName} · ${assign.strand}" +
                                        (if (assign.isCustom) " · Custom (${assign.customQuestions.size.coerceAtLeast(1)} Qs)" else ""),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF475569)
                                )
                                if (assign.instructions.isNotBlank()) {
                                    Text(
                                        assign.instructions,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF334155)
                                    )
                                }
                                if (assign.attachmentName.isNotBlank()) {
                                    Text(
                                        "Attached: ${assign.attachmentName}" +
                                            if (assign.attachmentSizeBytes > 0L) " (${AssignmentUploadParser.formatFileSize(assign.attachmentSizeBytes)})" else "",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SomaTeal,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                if (assign.teacherName.isNotEmpty()) {
                                    Text(
                                        "By ${assign.teacherName}" +
                                            if (assign.teacherCode.isNotEmpty()) " (${assign.teacherCode})" else "",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SomaTeal
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Class Competency Heatmap
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "Class Competency Heatmap · $rosterStreamFilter",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CbcCurriculumRepository.classStreams.forEach { stream ->
                            FilterChip(
                                selected = rosterStreamFilter == stream,
                                onClick = { rosterStreamFilter = stream },
                                label = { Text(stream) }
                            )
                        }
                    }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CBCRubric.entries.forEach { code ->
                            val count = streamStudents.count { it.rubric == code }
                            FilterChip(
                                selected = rubricFilter == code,
                                onClick = { rubricFilter = if (rubricFilter == code) null else code },
                                label = { Text("${code.name} ($count)") }
                            )
                        }
                    }
                }
            }
        }

        items(filteredStudents) { stu ->
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${stu.name} (${stu.admissionNo})", fontWeight = FontWeight.Bold)
                        Text(
                            "${stu.rubric.name} · ${stu.rubric.label}",
                            style = MaterialTheme.typography.bodySmall,
                            color = SomaTeal
                        )
                    }
                    Column {
                        Text("${stu.scorePercent}% · ${stu.learningMins}m", fontWeight = FontWeight.Bold)
                        Text(
                            if (stu.assignmentDone) "Submitted" else "Pending",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (stu.assignmentDone) SomaEmerald else Color(0xFFD97706)
                        )
                    }
                }
            }
        }

        // Full KICD CBC Subjects, Strands & Sub-strands Explorer
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "All CBC Subjects, Strands & Sub-strands",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "$subjectCount Learning Areas · $strandCount Strands · $subStrandCount Sub-strands (Grades 1–12). Tap any sub-strand to load it into the form above.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF475569)
                    )
                    OutlinedTextField(
                        value = catalogSearch,
                        onValueChange = { catalogSearch = it },
                        label = { Text("Search subject, strand or topic...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    val query = catalogSearch.trim().lowercase()
                    CbcCurriculumCatalog.curriculumTree.forEach { subj ->
                        val areaMatch = subj.learningArea.displayName.lowercase().contains(query)
                        val matchingStrands = subj.strands.mapNotNull { st ->
                            val stMatch = st.strand.lowercase().contains(query)
                            val subs = st.subStrands.filter { sub ->
                                query.isEmpty() || areaMatch || stMatch || sub.subStrand.lowercase().contains(query)
                            }
                            if (subs.isEmpty()) null else st.copy(subStrands = subs)
                        }
                        if (matchingStrands.isNotEmpty()) {
                            val isOpen = query.isNotEmpty() || expandedSubject == subj.learningArea
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (selectedArea == subj.learningArea) Color(0xFFF0FDFA) else Color(0xFFF8FAFC),
                                border = BorderStroke(
                                    1.dp,
                                    if (selectedArea == subj.learningArea) SomaTeal else Color(0xFFE2E8F0)
                                ),
                                onClick = {
                                    expandedSubject = if (expandedSubject == subj.learningArea) null else subj.learningArea
                                }
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            if (subj.learningArea in mySubjects) {
                                                "★ ${subj.learningArea.displayName}"
                                            } else {
                                                subj.learningArea.displayName
                                            },
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            "${subj.strands.size} strands · G${subj.minGrade}–${subj.maxGrade}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = SomaTeal,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    if (isOpen) {
                                        matchingStrands.forEach { st ->
                                            Text(
                                                st.strand,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF1E293B)
                                            )
                                            FlowRow(
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                st.subStrands.forEach { sub ->
                                                    val active = !useCustomStrand &&
                                                        selectedArea == subj.learningArea &&
                                                        selectedStrand == st.strand &&
                                                        selectedSubStrand == sub.subStrand
                                                    FilterChip(
                                                        selected = active,
                                                        onClick = {
                                                            useCustomStrand = false
                                                            selectedArea = subj.learningArea
                                                            selectedStrand = st.strand
                                                            selectedSubStrand = sub.subStrand
                                                        },
                                                        label = {
                                                            Text("${sub.subStrand} (G${sub.minGrade}–${sub.maxGrade})")
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            HorizontalDivider()
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
