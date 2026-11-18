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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.somatime.app.data.CbcCurriculumRepository
import com.somatime.app.data.SomaTimeStore
import com.somatime.app.data.SomaTimeUiState
import com.somatime.app.guard.ScreenTimeGuardService
import com.somatime.app.guard.UsageAccessHelper
import com.somatime.app.model.DeviceMode
import com.somatime.app.model.LearningArea
import com.somatime.app.model.PendingApproval
import com.somatime.app.ui.theme.SomaEmerald
import com.somatime.app.ui.theme.SomaTeal

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ParentDashboardScreen(
    uiState: SomaTimeUiState,
    onSelectChild: (String) -> Unit,
    onUpdateChildCap: (String, Int) -> Unit,
    onUpdateExchangeRate: (String, Int) -> Unit,
    onToggleMandatory: (String, LearningArea) -> Unit,
    onSetApprovalRequired: (String, Boolean) -> Unit,
    onSetDeviceMode: (String, DeviceMode) -> Unit,
    onActivateHandOff: (String) -> Unit,
    onAddChild: (String, Int, String) -> Unit,
    onRequestApprovalPin: (PendingApproval) -> Unit,
    onDeclineApproval: (String, String) -> Unit
) {
    val context = LocalContext.current
    val child = uiState.selectedChild
    var showAddDialog by remember { mutableStateOf(false) }

    val hasUsageStats = UsageAccessHelper.hasUsageStatsPermission(context)
    val hasOverlay = UsageAccessHelper.canDrawOverlays(context)

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
                        "Parent & Guardian Center",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Kenyan CBC rules, screen-time limits & real device guard",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF475569)
                    )
                }
                OutlinedButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Child")
                }
            }
        }

        // Child Profile Switcher
        item {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                uiState.children.forEach { profile ->
                    val selected = profile.id == child.id
                    FilterChip(
                        selected = selected,
                        onClick = { onSelectChild(profile.id) },
                        label = {
                            Text(
                                "${profile.name} · G${profile.grade} (${SomaTimeStore.formatClock(profile.availableScreenSeconds)})"
                            )
                        }
                    )
                }
            }
        }

        // Real Android Guard Permissions Card
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Rounded.Shield, contentDescription = null, tint = SomaTeal)
                        Text(
                            "Real Android App Interception Guard",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        "Grant Usage Access and Overlay permissions on your Android phone so SomaTime can monitor YouTube/TikTok/Games and lock when time hits 00:00.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF475569)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { UsageAccessHelper.openUsageAccessSettings(context) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (hasUsageStats) "Usage Access ✓" else "Grant Usage Access")
                        }
                        OutlinedButton(
                            onClick = { UsageAccessHelper.openOverlaySettings(context) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (hasOverlay) "Overlay ✓" else "Allow Overlay")
                        }
                    }
                    Button(
                        onClick = {
                            if (uiState.guardServiceRunning) {
                                ScreenTimeGuardService.stop(context)
                            } else {
                                ScreenTimeGuardService.start(context)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (uiState.guardServiceRunning) Color(0xFF334155) else SomaEmerald
                        )
                    ) {
                        Text(
                            if (uiState.guardServiceRunning) {
                                "Stop Background Guard Service"
                            } else {
                                "Start Background Guard Service"
                            }
                        )
                    }
                }
            }
        }

        // Device Mode & Hand-Off Lock Card
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
                        "Device Mode · ${child.name} (Grade ${child.grade})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "${CbcCurriculumRepository.levelGroupForGrade(child.grade)} · ${child.school}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF475569)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = child.deviceMode == DeviceMode.OWN_PHONE,
                            onClick = { onSetDeviceMode(child.id, DeviceMode.OWN_PHONE) },
                            label = { Text("Child's Own Phone") }
                        )
                        FilterChip(
                            selected = child.deviceMode == DeviceMode.HAND_OFF,
                            onClick = { onSetDeviceMode(child.id, DeviceMode.HAND_OFF) },
                            label = { Text("Shared Hand-Off Mode") }
                        )
                    }
                    Button(
                        onClick = { onActivateHandOff(child.id) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = SomaTeal),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Rounded.Lock, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Activate Child Lock & Hand Over Phone")
                    }
                    Text(
                        "Locks immediately into Child Mode. Parent PIN 1234 required to exit.",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF64748B)
                    )
                }
            }
        }

        // Screen Time Economy & Rules Engine
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Screen Time Economy & Rules",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Daily Max Cap", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "${child.dailyCapMins} mins/day",
                            fontWeight = FontWeight.Bold,
                            color = SomaTeal
                        )
                    }
                    Slider(
                        value = child.dailyCapMins.toFloat(),
                        onValueChange = { onUpdateChildCap(child.id, it.toInt()) },
                        valueRange = 30f..180f,
                        steps = 9
                    )

                    Text("CBC Exchange Rate (1 Activity =)", style = MaterialTheme.typography.bodyMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(10, 15, 20, 30).forEach { mins ->
                            FilterChip(
                                selected = child.exchangeRateMins == mins,
                                onClick = { onUpdateExchangeRate(child.id, mins) },
                                label = { Text("${mins}m") }
                            )
                        }
                    }

                    HorizontalDivider()
                    Text("Mandatory Focus Subjects", style = MaterialTheme.typography.bodyMedium)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        LearningArea.entries.forEach { area ->
                            FilterChip(
                                selected = area in child.mandatorySubjects,
                                onClick = { onToggleMandatory(child.id, area) },
                                label = { Text(area.displayName) }
                            )
                        }
                    }

                    HorizontalDivider()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Require Parent PIN Approval", fontWeight = FontWeight.SemiBold)
                            Text(
                                if (child.requireParentApproval) "Earned minutes wait in queue" else "Auto-approve earned minutes",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF64748B)
                            )
                        }
                        Switch(
                            checked = child.requireParentApproval,
                            onCheckedChange = { onSetApprovalRequired(child.id, it) }
                        )
                    }
                }
            }
        }

        // Pending Approvals Queue
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
                        "Parent Approval Queue (${child.pendingApprovals.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (child.pendingApprovals.isEmpty()) {
                        Text(
                            "No pending minute requests.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF64748B)
                        )
                    } else {
                        child.pendingApprovals.forEach { item ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.lessonTitle, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        "${item.learningArea.displayName} · ${item.scorePercent}% · +${item.minutesEarned}m",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF475569)
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    TextButton(onClick = { onDeclineApproval(child.id, item.id) }) {
                                        Text("Decline")
                                    }
                                    Button(
                                        onClick = { onRequestApprovalPin(item) },
                                        colors = ButtonDefaults.buttonColors(containerColor = SomaTeal)
                                    ) {
                                        Icon(Icons.Rounded.CheckCircle, contentDescription = null)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Approve")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // CBC Progress & Competency Analytics
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
                        "CBC Competency Analytics",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Completed: ${child.completedLessonIds.size} lessons · Learning: ${child.learnedMinutesToday}m · Play: ${child.usedScreenTodayMins}/${child.dailyCapMins}m",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF475569)
                    )
                    LinearProgressIndicator(
                        progress = {
                            (child.usedScreenTodayMins.toFloat() / child.dailyCapMins.coerceAtLeast(1)).coerceIn(0f, 1f)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    child.rubricByArea.forEach { (area, rubric) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(area.displayName, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "${rubric.name} · ${rubric.label}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = SomaTeal
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    if (showAddDialog) {
        var newName by remember { mutableStateOf("") }
        var newSchool by remember { mutableStateOf("Nairobi Primary School") }
        var newGrade by remember { mutableIntStateOf(5) }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Child Profile") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Child Full Name") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newSchool,
                        onValueChange = { newSchool = it },
                        label = { Text("School Name") },
                        singleLine = true
                    )
                    Text("Grade: $newGrade (${CbcCurriculumRepository.levelGroupForGrade(newGrade)})")
                    Slider(
                        value = newGrade.toFloat(),
                        onValueChange = { newGrade = it.toInt() },
                        valueRange = 1f..12f,
                        steps = 10
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newName.isNotBlank()) {
                            onAddChild(newName, newGrade, newSchool)
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SomaTeal)
                ) {
                    Text("Save Child")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
