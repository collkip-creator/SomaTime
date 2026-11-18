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
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.somatime.app.data.CbcCurriculumRepository
import com.somatime.app.data.SomaTimeUiState
import com.somatime.app.model.CBCRubric
import com.somatime.app.model.LearningArea
import com.somatime.app.ui.theme.SomaEmerald
import com.somatime.app.ui.theme.SomaTeal

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TeacherPortalScreen(
    uiState: SomaTimeUiState,
    onPushAssignment: (String, Int, LearningArea, String, String) -> Unit,
    onPreviewChildMode: () -> Unit
) {
    var selectedStream by remember { mutableStateOf(CbcCurriculumRepository.classStreams.first()) }
    var selectedArea by remember { mutableStateOf(LearningArea.SOCIAL_STUDIES) }
    var pushedNotice by remember { mutableStateOf("") }
    var rubricFilter by remember { mutableStateOf<CBCRubric?>(null) }

    val matchingCatalog = CbcCurriculumRepository.catalog.firstOrNull {
        it.learningArea == selectedArea
    } ?: CbcCurriculumRepository.catalog.first()

    val streamStudents = uiState.classRoster.filter { it.classStream == selectedStream }
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
                "Push Kenyan CBC priority assignments to learner devices and track class mastery",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF475569)
            )
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
                    Text("Class Stream", style = MaterialTheme.typography.labelMedium)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CbcCurriculumRepository.classStreams.forEach { stream ->
                            FilterChip(
                                selected = selectedStream == stream,
                                onClick = { selectedStream = stream },
                                label = { Text(stream) }
                            )
                        }
                    }

                    Text("CBC Learning Area", style = MaterialTheme.typography.labelMedium)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        LearningArea.entries.forEach { area ->
                            FilterChip(
                                selected = selectedArea == area,
                                onClick = { selectedArea = area },
                                label = { Text(area.displayName) }
                            )
                        }
                    }

                    Text(
                        "Selected Strand: ${matchingCatalog.strand} · ${matchingCatalog.subStrand}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF475569)
                    )

                    Button(
                        onClick = {
                            val grade = when {
                                selectedStream.contains("Grade 4") -> 4
                                selectedStream.contains("Grade 8") -> 8
                                else -> 6
                            }
                            onPushAssignment(
                                selectedStream,
                                grade,
                                selectedArea,
                                matchingCatalog.strand,
                                matchingCatalog.subStrand
                            )
                            pushedNotice = "${matchingCatalog.subStrand} pushed to $selectedStream"
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
                                Text("Synced to Child Mode!", fontWeight = FontWeight.Bold)
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
                        "Class Competency Heatmap · $selectedStream",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
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

        item {
            HorizontalDivider()
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
