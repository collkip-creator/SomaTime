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
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Send
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.somatime.app.data.CbcCurriculumCatalog
import com.somatime.app.data.CbcCurriculumRepository
import com.somatime.app.data.SomaTimeUiState
import com.somatime.app.model.CBCRubric
import com.somatime.app.model.LearningArea
import com.somatime.app.ui.theme.SomaAmber
import com.somatime.app.ui.theme.SomaEmerald
import com.somatime.app.ui.theme.SomaTeal

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TeacherPortalScreen(
    uiState: SomaTimeUiState,
    onOpenTeacherAuth: () -> Unit,
    onLogoutTeacher: () -> Unit,
    onPushAssignment: (String, String, Int, LearningArea, String, String) -> Unit,
    onPreviewChildMode: () -> Unit
) {
    val activeTeacher = uiState.teacherAccount
    val initialArea = activeTeacher?.subjects?.firstOrNull() ?: LearningArea.MATHEMATICS
    val initialGroupedStrands = CbcCurriculumRepository.groupedStrandsForArea(initialArea)

    var schoolName by remember { mutableStateOf(activeTeacher?.school ?: "Nairobi Primary School") }
    var targetGrade by remember { mutableIntStateOf(activeTeacher?.grade ?: 6) }
    var classStream by remember {
        mutableStateOf(activeTeacher?.classStream ?: CbcCurriculumRepository.classStreams.first())
    }
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
    var pushedNotice by remember { mutableStateOf("") }

    var catalogSearch by remember { mutableStateOf("") }
    var expandedSubject by remember { mutableStateOf<LearningArea?>(initialArea) }

    var rosterStreamFilter by remember { mutableStateOf(CbcCurriculumRepository.classStreams.first()) }
    var rubricFilter by remember { mutableStateOf<CBCRubric?>(null) }

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

                    Button(
                        onClick = {
                            val finalStrand = if (useCustomStrand) {
                                customStrand.trim().ifEmpty { selectedStrand }
                            } else {
                                selectedStrand
                            }
                            val finalSub = if (useCustomStrand) {
                                customSubStrand.trim().ifEmpty { selectedSubStrand }
                            } else {
                                selectedSubStrand
                            }
                            onPushAssignment(
                                classStream,
                                schoolName,
                                targetGrade,
                                selectedArea,
                                finalStrand,
                                finalSub
                            )
                            pushedNotice = "${selectedArea.displayName} · $finalStrand ($finalSub) pushed to Grade $targetGrade at $schoolName"
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = SomaTeal)
                    ) {
                        Icon(Icons.Rounded.Send, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Push Assignment to Students")
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
                                    "${assign.school} · Grade ${assign.grade} · ${assign.learningArea.displayName} · ${assign.strand}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF475569)
                                )
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
