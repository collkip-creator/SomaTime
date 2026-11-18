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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.somatime.app.data.CbcCurriculumRepository
import com.somatime.app.data.PairingResult
import com.somatime.app.data.SomaTimeUiState
import com.somatime.app.guard.ScreenTimeGuardService
import com.somatime.app.guard.UsageAccessHelper
import com.somatime.app.model.ChildProfile
import com.somatime.app.ui.theme.SomaBlue
import com.somatime.app.ui.theme.SomaEmerald
import com.somatime.app.ui.theme.SomaTeal
import kotlinx.coroutines.launch

private enum class WizardStep {
    CHOICE,
    PARENT_FORM,
    PARENT_DONE,
    LEARNER_PAIR
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SetupWizardScreen(
    uiState: SomaTimeUiState,
    onRegisterParentAndChild: suspend (
        parentName: String,
        parentContact: String,
        parentPin: String,
        childName: String,
        childGrade: Int,
        childSchool: String,
        dailyCapMins: Int,
        initialWatchMins: Int
    ) -> ChildProfile,
    onPairLearnerCode: suspend (String) -> PairingResult,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var step by remember { mutableStateOf(WizardStep.CHOICE) }

    var parentName by remember { mutableStateOf(uiState.parentName) }
    var parentContact by remember { mutableStateOf(uiState.parentContact) }
    var parentPin by remember { mutableStateOf(uiState.parentPin) }
    var childName by remember { mutableStateOf("") }
    var childGrade by remember { mutableIntStateOf(5) }
    var childSchool by remember { mutableStateOf("Nairobi Primary School") }
    var dailyCapMins by remember { mutableIntStateOf(90) }
    var initialWatchMins by remember { mutableIntStateOf(5) }
    var formError by remember { mutableStateOf("") }
    var isSavingParent by remember { mutableStateOf(false) }
    var generatedChild by remember { mutableStateOf<ChildProfile?>(null) }

    var pairingInput by remember { mutableStateOf("") }
    var pairingFeedback by remember { mutableStateOf("") }
    var isPairingLearner by remember { mutableStateOf(false) }

    val hasUsageStats = UsageAccessHelper.hasUsageStatsPermission(context)
    val hasOverlay = UsageAccessHelper.canDrawOverlays(context)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "SomaTime Setup & Device Pairing",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Pair a parent phone and a child phone using a secure child pairing code",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF475569)
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close Setup")
                }
            }
        }

        when (step) {
            WizardStep.CHOICE -> {
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Rounded.AdminPanelSettings, contentDescription = null, tint = SomaTeal)
                                Text(
                                    "1. Register as Parent",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                "Create your parent profile, set a 4-digit Parent PIN, add your child's details, and generate a pairing code for their phone.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF475569)
                            )
                            Button(
                                onClick = { step = WizardStep.PARENT_FORM },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = SomaTeal)
                            ) {
                                Text("Register Parent & Add Child")
                            }
                        }
                    }
                }

                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Rounded.PhoneAndroid, contentDescription = null, tint = SomaBlue)
                                Text(
                                    "2. Register as a Learner",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                "On the child's phone: enter the pairing code generated by the parent. SomaTime will automatically monitor watch minutes in the background and take over the screen when time runs out.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF475569)
                            )
                            Button(
                                onClick = { step = WizardStep.LEARNER_PAIR },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = SomaBlue)
                            ) {
                                Text("Pair Learner Device with Code")
                            }
                        }
                    }
                }

                item {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Return to Dashboard")
                    }
                }
            }

            WizardStep.PARENT_FORM -> {
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
                                "Parent & Child Registration",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            OutlinedTextField(
                                value = parentName,
                                onValueChange = { parentName = it },
                                label = { Text("Parent / Guardian Full Name") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = parentContact,
                                onValueChange = { parentContact = it },
                                label = { Text("Phone Number (e.g. 0712 345 678)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = parentPin,
                                onValueChange = { input ->
                                    val digits = input.filter { it.isDigit() }.take(4)
                                    parentPin = digits
                                },
                                label = { Text("4-Digit Parent PIN") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            HorizontalDivider()

                            OutlinedTextField(
                                value = childName,
                                onValueChange = { childName = it },
                                label = { Text("Child Full Name (e.g. Faith Njeri)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = childSchool,
                                onValueChange = { childSchool = it },
                                label = { Text("School Name") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            Text(
                                "Grade: $childGrade (${CbcCurriculumRepository.levelGroupForGrade(childGrade)})",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Slider(
                                value = childGrade.toFloat(),
                                onValueChange = { childGrade = it.toInt() },
                                valueRange = 1f..12f,
                                steps = 10
                            )

                            Text(
                                "Daily Screen Cap: $dailyCapMins mins/day",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Slider(
                                value = dailyCapMins.toFloat(),
                                onValueChange = { dailyCapMins = it.toInt() },
                                valueRange = 30f..180f,
                                steps = 9
                            )

                            Text(
                                "Starting Watch Minutes: $initialWatchMins mins",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Slider(
                                value = initialWatchMins.toFloat(),
                                onValueChange = { initialWatchMins = it.toInt() },
                                valueRange = 0f..30f,
                                steps = 5
                            )

                            if (formError.isNotEmpty()) {
                                Text(
                                    formError,
                                    color = Color(0xFFDC2626),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(
                                    onClick = { step = WizardStep.CHOICE },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Back")
                                }
                                Button(
                                    onClick = {
                                        if (isSavingParent) return@Button
                                        if (childName.trim().length < 2) {
                                            formError = "Please enter your child's full name."
                                            return@Button
                                        }
                                        if (parentPin.length != 4) {
                                            formError = "Parent PIN must be exactly 4 digits."
                                            return@Button
                                        }
                                        formError = ""
                                        isSavingParent = true
                                        scope.launch {
                                            val created = onRegisterParentAndChild(
                                                parentName,
                                                parentContact,
                                                parentPin,
                                                childName,
                                                childGrade,
                                                childSchool,
                                                dailyCapMins,
                                                initialWatchMins
                                            )
                                            generatedChild = created
                                            pairingInput = created.pairingCode
                                            isSavingParent = false
                                            step = WizardStep.PARENT_DONE
                                        }
                                    },
                                    enabled = !isSavingParent,
                                    modifier = Modifier.weight(2f),
                                    colors = ButtonDefaults.buttonColors(containerColor = SomaTeal)
                                ) {
                                    Text(
                                        if (isSavingParent) "Syncing to Cloud..." else "Complete & Generate Code"
                                    )
                                }
                            }
                        }
                    }
                }
            }

            WizardStep.PARENT_DONE -> {
                val created = generatedChild
                item {
                    Card(
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF5)),
                        border = BorderStroke(1.dp, SomaEmerald)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = SomaEmerald)
                            Text(
                                "Parent Account & Child Profile Ready!",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Enter this Pairing Code on ${created?.name ?: "your child"}'s phone under \"Register as a Learner\":",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF334155)
                            )
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color.White,
                                border = BorderStroke(1.dp, SomaEmerald)
                            ) {
                                Text(
                                    text = created?.pairingCode ?: "SOMA-0000",
                                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = SomaTeal
                                )
                            }
                            Text(
                                "Parent PIN set to $parentPin",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFF065F46)
                            )
                            Button(
                                onClick = { step = WizardStep.LEARNER_PAIR },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = SomaBlue)
                            ) {
                                Text("Pair Learner Phone Now")
                            }
                            OutlinedButton(
                                onClick = onDismiss,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Go to Parent Dashboard")
                            }
                        }
                    }
                }
            }

            WizardStep.LEARNER_PAIR -> {
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
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Rounded.Key, contentDescription = null, tint = SomaBlue)
                                Text(
                                    "Register as a Learner",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                "Enter the Pairing Code from your parent's phone. SomaTime will start checking available watch minutes in the background and take over the screen immediately when time expires.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF475569)
                            )

                            OutlinedTextField(
                                value = pairingInput,
                                onValueChange = {
                                    pairingInput = it.uppercase()
                                    pairingFeedback = ""
                                },
                                label = { Text("Parent Pairing Code (e.g. SOMA-4012)") },
                                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            Text(
                                "Active Codes on Record (Quick Fill):",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFF64748B)
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                uiState.children.forEach { child ->
                                    FilterChip(
                                        selected = pairingInput.trim() == child.pairingCode,
                                        onClick = {
                                            pairingInput = child.pairingCode
                                            pairingFeedback = ""
                                        },
                                        label = { Text("${child.name.split(" ").first()}: ${child.pairingCode}") }
                                    )
                                }
                            }

                            HorizontalDivider()

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Rounded.Shield, contentDescription = null, tint = SomaTeal)
                                Text(
                                    "Background Screen Guard Permissions",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { UsageAccessHelper.openUsageAccessSettings(context) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(if (hasUsageStats) "Usage ✓" else "Usage Access")
                                }
                                OutlinedButton(
                                    onClick = { UsageAccessHelper.openOverlaySettings(context) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(if (hasOverlay) "Overlay ✓" else "Allow Overlay")
                                }
                            }

                            if (pairingFeedback.isNotEmpty()) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFFEF2F2),
                                    border = BorderStroke(1.dp, Color(0xFFFCA5A5))
                                ) {
                                    Text(
                                        text = pairingFeedback,
                                        modifier = Modifier.padding(10.dp),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF991B1B)
                                    )
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(
                                    onClick = { step = WizardStep.CHOICE },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Back")
                                }
                                Button(
                                    onClick = {
                                        if (isPairingLearner) return@Button
                                        isPairingLearner = true
                                        pairingFeedback = ""
                                        scope.launch {
                                            val res = onPairLearnerCode(pairingInput)
                                            isPairingLearner = false
                                            if (res.success) {
                                                runCatching { ScreenTimeGuardService.start(context) }
                                            } else {
                                                pairingFeedback = res.message
                                            }
                                        }
                                    },
                                    enabled = !isPairingLearner,
                                    modifier = Modifier.weight(2f),
                                    colors = ButtonDefaults.buttonColors(containerColor = SomaEmerald)
                                ) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        if (isPairingLearner) "Verifying Code..." else "Pair & Start Background Guard"
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}
