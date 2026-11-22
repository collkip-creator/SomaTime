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
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.somatime.app.data.CbcCurriculumRepository
import com.somatime.app.data.PairingResult
import com.somatime.app.data.SetupWizardTab
import com.somatime.app.data.SomaTimeUiState
import com.somatime.app.data.TeacherLoginResult
import com.somatime.app.guard.ScreenTimeGuardService
import com.somatime.app.guard.UsageAccessHelper
import com.somatime.app.model.ChildProfile
import com.somatime.app.model.LearningArea
import com.somatime.app.model.TeacherAccount
import com.somatime.app.ui.theme.SomaAmber
import com.somatime.app.ui.theme.SomaBlue
import com.somatime.app.ui.theme.SomaEmerald
import com.somatime.app.ui.theme.SomaTeal
import kotlinx.coroutines.launch

private enum class WizardStep {
    CHOICE,
    PARENT_FORM,
    PARENT_DONE,
    LEARNER_PAIR,
    TEACHER_AUTH,
    TEACHER_DONE
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
    onRegisterTeacher: suspend (
        fullName: String,
        school: String,
        tscNumber: String,
        pin: String,
        subjects: List<LearningArea>,
        grade: Int,
        classStream: String,
        preserveRoleView: Boolean
    ) -> TeacherAccount,
    onLoginTeacher: suspend (teacherCode: String, pin: String) -> TeacherLoginResult,
    onRetryCloudSync: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    var step by remember(uiState.setupInitialTab) {
        mutableStateOf(
            when (uiState.setupInitialTab) {
                SetupWizardTab.PARENT -> WizardStep.PARENT_FORM
                SetupWizardTab.LEARNER -> WizardStep.LEARNER_PAIR
                SetupWizardTab.TEACHER -> WizardStep.TEACHER_AUTH
                SetupWizardTab.CHOICE -> WizardStep.CHOICE
            }
        )
    }

    var parentName by remember { mutableStateOf(uiState.parentName) }
    var parentContact by remember { mutableStateOf(uiState.parentContact) }
    var parentPin by remember {
        mutableStateOf(if (uiState.parentPin == CbcCurriculumRepository.PARENT_PIN) "" else uiState.parentPin)
    }
    var confirmParentPin by remember {
        mutableStateOf(if (uiState.parentPin == CbcCurriculumRepository.PARENT_PIN) "" else uiState.parentPin)
    }
    var showParentPin by remember { mutableStateOf(false) }
    var childName by remember { mutableStateOf("") }
    var childGrade by remember { mutableIntStateOf(5) }
    var childSchool by remember { mutableStateOf("") }
    var dailyCapMins by remember { mutableIntStateOf(90) }
    var initialWatchMins by remember { mutableIntStateOf(5) }
    var alsoTeacher by remember { mutableStateOf(false) }
    var parentTeacherSchool by remember { mutableStateOf("") }
    var parentTeacherGrade by remember { mutableIntStateOf(5) }
    var parentTeacherSubjects by remember { mutableStateOf(listOf(LearningArea.MATHEMATICS)) }
    var formError by remember { mutableStateOf("") }
    var isSavingParent by remember { mutableStateOf(false) }
    var generatedChild by remember { mutableStateOf<ChildProfile?>(null) }
    var linkedTeacherAccount by remember { mutableStateOf<TeacherAccount?>(null) }

    var pairingInput by remember { mutableStateOf("") }
    var pairingFeedback by remember { mutableStateOf("") }
    var isPairingLearner by remember { mutableStateOf(false) }

    // Teacher registration & login states
    var teacherIsRegisterTab by remember { mutableStateOf(true) }
    var teacherName by remember { mutableStateOf("") }
    var teacherSchool by remember { mutableStateOf("") }
    var teacherTscNo by remember { mutableStateOf("") }
    var teacherPin by remember { mutableStateOf("") }
    var teacherGrade by remember { mutableIntStateOf(6) }
    var teacherStream by remember { mutableStateOf("Grade 6 East") }
    var teacherSubjects by remember { mutableStateOf(listOf(LearningArea.MATHEMATICS)) }
    var teacherError by remember { mutableStateOf("") }
    var isSavingTeacher by remember { mutableStateOf(false) }
    var createdTeacher by remember { mutableStateOf<TeacherAccount?>(null) }

    var teacherLoginCode by remember { mutableStateOf("") }
    var teacherLoginPin by remember { mutableStateOf("") }
    var teacherLoginError by remember { mutableStateOf("") }
    var isLoggingTeacher by remember { mutableStateOf(false) }

    var hasUsageStats by remember { mutableStateOf(UsageAccessHelper.hasUsageStatsPermission(context)) }
    var hasOverlay by remember { mutableStateOf(UsageAccessHelper.canDrawOverlays(context)) }
    var hasAccessibility by remember { mutableStateOf(UsageAccessHelper.isAccessibilityServiceEnabled(context)) }
    var hasDeviceAdmin by remember { mutableStateOf(UsageAccessHelper.isDeviceAdminActive(context)) }

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasUsageStats = UsageAccessHelper.hasUsageStatsPermission(context)
                hasOverlay = UsageAccessHelper.canDrawOverlays(context)
                hasAccessibility = UsageAccessHelper.isAccessibilityServiceEnabled(context)
                hasDeviceAdmin = UsageAccessHelper.isDeviceAdminActive(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

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
                        "Register as Parent, Learner, or School Teacher",
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
                                "On the child's phone: enter the pairing code generated by the parent. SomaTime will monitor watch minutes in the background and take over when time runs out.",
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
                                Icon(Icons.Rounded.School, contentDescription = null, tint = SomaAmber)
                                Text(
                                    "3. Register / Sign In as Teacher",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                "Register with your school, primary grade, and CBC subject areas to push strand assignments directly to your students.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF475569)
                            )
                            Button(
                                onClick = { step = WizardStep.TEACHER_AUTH },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = SomaAmber)
                            ) {
                                Text("Register or Sign In as Teacher")
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
                                    parentPin = input.filter { it.isDigit() }.take(4)
                                },
                                label = { Text("Set Your Own 4-Digit Parent PIN") },
                                visualTransformation = if (showParentPin) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = { showParentPin = !showParentPin }) {
                                        Icon(
                                            if (showParentPin) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                            contentDescription = if (showParentPin) "Hide PIN" else "Show PIN"
                                        )
                                    }
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = confirmParentPin,
                                onValueChange = { input ->
                                    confirmParentPin = input.filter { it.isDigit() }.take(4)
                                },
                                label = { Text("Confirm 4-Digit Parent PIN") },
                                visualTransformation = if (showParentPin) VisualTransformation.None else PasswordVisualTransformation(),
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
                                label = { Text("School Name (e.g. Nairobi Primary School)") },
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

                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                "I am also a Teacher (3-in-1 phone)",
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                "Enable Teacher Portal on this same phone with your Parent Name & PIN.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color(0xFF475569)
                                            )
                                        }
                                        Switch(
                                            checked = alsoTeacher,
                                            onCheckedChange = { checked ->
                                                alsoTeacher = checked
                                                if (checked) {
                                                    if (parentTeacherSchool.isBlank() && childSchool.isNotBlank()) {
                                                        parentTeacherSchool = childSchool
                                                    }
                                                    parentTeacherGrade = childGrade
                                                }
                                            }
                                        )
                                    }

                                    if (alsoTeacher) {
                                        OutlinedTextField(
                                            value = parentTeacherSchool,
                                            onValueChange = { parentTeacherSchool = it },
                                            label = { Text("School Where You Teach") },
                                            placeholder = { Text(childSchool.ifBlank { "e.g. Nairobi Primary School" }) },
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true
                                        )
                                        Text(
                                            "Grade Taught: $parentTeacherGrade (${CbcCurriculumRepository.levelGroupForGrade(parentTeacherGrade)})",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Slider(
                                            value = parentTeacherGrade.toFloat(),
                                            onValueChange = { parentTeacherGrade = it.toInt() },
                                            valueRange = 1f..12f,
                                            steps = 10
                                        )
                                        Text(
                                            "Subjects (${parentTeacherSubjects.size}/8):",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            LearningArea.entries.take(10).forEach { area ->
                                                val selected = area in parentTeacherSubjects
                                                FilterChip(
                                                    selected = selected,
                                                    onClick = {
                                                        parentTeacherSubjects = if (selected) {
                                                            if (parentTeacherSubjects.size > 1) parentTeacherSubjects - area else parentTeacherSubjects
                                                        } else if (parentTeacherSubjects.size < 8) {
                                                            parentTeacherSubjects + area
                                                        } else {
                                                            parentTeacherSubjects
                                                        }
                                                    },
                                                    label = { Text(area.displayName) }
                                                )
                                            }
                                        }
                                    }
                                }
                            }

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
                                            formError = "Set your own 4-digit Parent PIN."
                                            return@Button
                                        }
                                        if (parentPin != confirmParentPin) {
                                            formError = "Parent PIN and Confirm PIN do not match."
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
                                            if (alsoTeacher) {
                                                val tSchool = parentTeacherSchool.trim()
                                                    .ifEmpty { childSchool.trim().ifEmpty { "Nairobi Primary School" } }
                                                val tGrade = parentTeacherGrade
                                                linkedTeacherAccount = onRegisterTeacher(
                                                    parentName.trim().ifEmpty { "Teacher" },
                                                    tSchool,
                                                    "",
                                                    parentPin,
                                                    parentTeacherSubjects,
                                                    tGrade,
                                                    "Grade $tGrade East - $tSchool",
                                                    true
                                                )
                                            } else {
                                                linkedTeacherAccount = null
                                            }
                                            generatedChild = created
                                            pairingInput = if (created.licenseActive) created.pairingCode else ""
                                            isSavingParent = false
                                            step = WizardStep.PARENT_DONE
                                        }
                                    },
                                    enabled = !isSavingParent,
                                    modifier = Modifier.weight(2f),
                                    colors = ButtonDefaults.buttonColors(containerColor = SomaTeal)
                                ) {
                                    Text(
                                        when {
                                            isSavingParent -> "Syncing to Cloud..."
                                            alsoTeacher -> "Save Parent + Teacher & Child"
                                            else -> "Save Parent Setup"
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            WizardStep.PARENT_DONE -> {
                val created = generatedChild
                val licensed = created?.licenseActive == true
                item {
                    Card(
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (licensed) Color(0xFFECFDF5) else Color(0xFFFFFBEB)
                        ),
                        border = BorderStroke(1.dp, if (licensed) SomaEmerald else SomaAmber)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = if (licensed) SomaEmerald else SomaAmber
                            )
                            Text(
                                "Parent Account & Child Profile Saved!",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (linkedTeacherAccount != null) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color.White,
                                    border = BorderStroke(1.dp, SomaTeal)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(
                                            "Teacher Profile Also Active: ${linkedTeacherAccount!!.teacherCode}",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = SomaTeal
                                        )
                                        Text(
                                            "${linkedTeacherAccount!!.school} · Grade ${linkedTeacherAccount!!.grade} · Uses same Parent PIN",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF334155)
                                        )
                                    }
                                }
                            }
                            Text(
                                if (licensed) {
                                    "Enter this Pairing Code on ${created?.name ?: "your child"}'s phone under \"Register as a Learner\":"
                                } else {
                                    "Monthly Child License required (KES ${CbcCurriculumRepository.MONTHLY_LICENSE_KES}/child/mo) before generating ${created?.name ?: "your child"}'s pairing code or activating Child Lock."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF334155)
                            )
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color.White,
                                border = BorderStroke(1.dp, if (licensed) SomaEmerald else Color(0xFFFCD34D))
                            ) {
                                Text(
                                    text = if (licensed) (created?.pairingCode ?: "SOMA-0000") else "SOMA-•••• (Locked)",
                                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (licensed) SomaTeal else Color(0xFF94A3B8)
                                )
                            }
                            Text(
                                "Custom Parent PIN secured (••••)",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFF065F46)
                            )
                            Text(
                                text = uiState.cloudSyncMessage,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (uiState.cloudSyncOk) SomaEmerald else Color(0xFFDC2626),
                                fontWeight = FontWeight.Bold
                            )
                            if (!uiState.cloudSyncOk) {
                                OutlinedButton(
                                    onClick = onRetryCloudSync,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Retry Cloud Sync")
                                }
                            }
                            if (licensed) {
                                Button(
                                    onClick = { step = WizardStep.LEARNER_PAIR },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = SomaBlue)
                                ) {
                                    Text("Pair Learner Phone Now")
                                }
                            }
                            Button(
                                onClick = onDismiss,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = SomaTeal)
                            ) {
                                Text(
                                    if (licensed) {
                                        "Go to Parent Dashboard"
                                    } else {
                                        "Go to Parents Tab & Pay License (KES ${CbcCurriculumRepository.MONTHLY_LICENSE_KES}/mo) →"
                                    }
                                )
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
                                        selected = child.licenseActive && pairingInput.trim() == child.pairingCode,
                                        enabled = child.licenseActive,
                                        onClick = {
                                            pairingInput = child.pairingCode
                                            pairingFeedback = ""
                                        },
                                        label = {
                                            val firstName = child.name.split(" ").first()
                                            Text(
                                                if (child.licenseActive) {
                                                    "$firstName: ${child.pairingCode}"
                                                } else {
                                                    "Locked · $firstName (Pay License)"
                                                }
                                            )
                                        }
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
                                    "Phone Lockdown & Swipe-Down Guard Permissions",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                "Enable all 4 toggles on the learner's phone so Home, Recents, Back, Quick Settings swipe-down, and uninstall are blocked during lock.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF475569)
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { UsageAccessHelper.openUsageAccessSettings(context) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(if (hasUsageStats) "Usage ✓" else "1. Usage Access")
                                }
                                OutlinedButton(
                                    onClick = { UsageAccessHelper.openOverlaySettings(context) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(if (hasOverlay) "Overlay ✓" else "2. Overlay")
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { UsageAccessHelper.openAccessibilitySettings(context) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(if (hasAccessibility) "Swipe Guard ✓" else "3. Lockdown Guard")
                                }
                                OutlinedButton(
                                    onClick = { UsageAccessHelper.openDeviceAdminSettings(context) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(if (hasDeviceAdmin) "Uninstall Lock ✓" else "4. Anti-Uninstall")
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

            WizardStep.TEACHER_AUTH -> {
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
                                Icon(Icons.Rounded.School, contentDescription = null, tint = SomaAmber)
                                Text(
                                    "Teacher Registration & School Login",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = teacherIsRegisterTab,
                                    onClick = {
                                        teacherIsRegisterTab = true
                                        teacherLoginError = ""
                                    },
                                    label = { Text("Register Teacher") },
                                    modifier = Modifier.weight(1f)
                                )
                                FilterChip(
                                    selected = !teacherIsRegisterTab,
                                    onClick = {
                                        teacherIsRegisterTab = false
                                        teacherError = ""
                                    },
                                    label = { Text("Sign In (Code + PIN)") },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            if (teacherIsRegisterTab) {
                                OutlinedTextField(
                                    value = teacherName,
                                    onValueChange = { teacherName = it },
                                    label = { Text("Teacher Full Name (e.g. Ms. Faith Chebet)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = teacherSchool,
                                    onValueChange = { teacherSchool = it },
                                    label = { Text("School Name (e.g. Nairobi Primary School)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = teacherTscNo,
                                        onValueChange = { teacherTscNo = it },
                                        label = { Text("TSC / Staff No") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                    OutlinedTextField(
                                        value = teacherPin,
                                        onValueChange = { teacherPin = it.filter(Char::isDigit).take(4) },
                                        label = { Text("4-Digit PIN") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                }

                                Text(
                                    "Primary Grade Taught: Grade $teacherGrade (${CbcCurriculumRepository.levelGroupForGrade(teacherGrade)})",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Slider(
                                    value = teacherGrade.toFloat(),
                                    onValueChange = {
                                        val g = it.toInt()
                                        teacherGrade = g
                                        teacherStream = "Grade $g East"
                                    },
                                    valueRange = 1f..12f,
                                    steps = 10
                                )

                                OutlinedTextField(
                                    value = teacherStream,
                                    onValueChange = { teacherStream = it },
                                    label = { Text("Class Stream Label") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )

                                Text(
                                    "CBC Subject Area(s) You Teach (${teacherSubjects.size}/8 selected · ${LearningArea.entries.size} areas):",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    LearningArea.entries.forEach { area ->
                                        val selected = area in teacherSubjects
                                        FilterChip(
                                            selected = selected,
                                            onClick = {
                                                teacherSubjects = if (selected) {
                                                    if (teacherSubjects.size > 1) teacherSubjects - area else teacherSubjects
                                                } else {
                                                    (teacherSubjects + area).take(8)
                                                }
                                            },
                                            label = { Text(area.displayName) }
                                        )
                                    }
                                }

                                if (teacherError.isNotEmpty()) {
                                    Text(
                                        teacherError,
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
                                            if (isSavingTeacher) return@Button
                                            if (teacherName.trim().length < 2) {
                                                teacherError = "Please enter your full name."
                                                return@Button
                                            }
                                            if (teacherSchool.trim().length < 2) {
                                                teacherError = "Please enter your school name."
                                                return@Button
                                            }
                                            if (teacherPin.length != 4) {
                                                teacherError = "Choose a 4-digit Teacher PIN."
                                                return@Button
                                            }
                                            teacherError = ""
                                            isSavingTeacher = true
                                            scope.launch {
                                                val fullStream = if (teacherStream.trim().isNotEmpty()) {
                                                    "${teacherStream.trim()} - ${teacherSchool.trim()}"
                                                } else {
                                                    "Grade $teacherGrade East - ${teacherSchool.trim()}"
                                                }
                                                val created = onRegisterTeacher(
                                                    teacherName,
                                                    teacherSchool,
                                                    teacherTscNo,
                                                    teacherPin,
                                                    teacherSubjects,
                                                    teacherGrade,
                                                    fullStream,
                                                    false
                                                )
                                                createdTeacher = created
                                                isSavingTeacher = false
                                                step = WizardStep.TEACHER_DONE
                                            }
                                        },
                                        enabled = !isSavingTeacher,
                                        modifier = Modifier.weight(2f),
                                        colors = ButtonDefaults.buttonColors(containerColor = SomaAmber)
                                    ) {
                                        Text(
                                            if (isSavingTeacher) "Registering..." else "Register Teacher & Get Code"
                                        )
                                    }
                                }
                            } else {
                                Text(
                                    "Enter your Teacher Code (e.g. TCH-4821) and 4-digit PIN to load your school, taught subjects, and assignment tools.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF475569)
                                )
                                OutlinedTextField(
                                    value = teacherLoginCode,
                                    onValueChange = {
                                        teacherLoginCode = it.uppercase()
                                        teacherLoginError = ""
                                    },
                                    label = { Text("Teacher Code (e.g. TCH-4821)") },
                                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = teacherLoginPin,
                                    onValueChange = {
                                        teacherLoginPin = it.filter(Char::isDigit).take(4)
                                        teacherLoginError = ""
                                    },
                                    label = { Text("4-Digit Teacher PIN") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )

                                if (uiState.registeredTeachers.isNotEmpty()) {
                                    Text(
                                        "Registered Teachers on Device:",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color(0xFF64748B)
                                    )
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        uiState.registeredTeachers.forEach { t ->
                                            FilterChip(
                                                selected = teacherLoginCode.trim() == t.teacherCode,
                                                onClick = {
                                                    teacherLoginCode = t.teacherCode
                                                    teacherLoginPin = t.pin
                                                    teacherLoginError = ""
                                                },
                                                label = { Text("${t.teacherCode} · ${t.fullName}") }
                                            )
                                        }
                                    }
                                }

                                if (teacherLoginError.isNotEmpty()) {
                                    Text(
                                        teacherLoginError,
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
                                            if (isLoggingTeacher) return@Button
                                            isLoggingTeacher = true
                                            teacherLoginError = ""
                                            scope.launch {
                                                val res = onLoginTeacher(teacherLoginCode, teacherLoginPin)
                                                isLoggingTeacher = false
                                                if (!res.success) {
                                                    teacherLoginError = res.message
                                                }
                                            }
                                        },
                                        enabled = !isLoggingTeacher,
                                        modifier = Modifier.weight(2f),
                                        colors = ButtonDefaults.buttonColors(containerColor = SomaTeal)
                                    ) {
                                        Text(if (isLoggingTeacher) "Signing In..." else "Sign In to Teacher Portal")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            WizardStep.TEACHER_DONE -> {
                val created = createdTeacher
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
                                "Teacher Registered & Synced!",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "${created?.fullName ?: "Teacher"} · ${created?.school ?: ""}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color.White,
                                border = BorderStroke(1.dp, SomaEmerald)
                            ) {
                                Text(
                                    text = created?.teacherCode ?: "TCH-0000",
                                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = SomaTeal
                                )
                            }
                            Text(
                                "Use Teacher Code ${created?.teacherCode ?: ""} and PIN ${created?.pin ?: ""} to sign in on any device.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF334155)
                            )
                            Text(
                                "Subjects: ${created?.subjects?.joinToString(", ") { it.displayName } ?: ""}",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFF065F46),
                                fontWeight = FontWeight.Bold
                            )
                            Button(
                                onClick = onDismiss,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = SomaTeal)
                            ) {
                                Text("Open Teacher Portal & Push Assignments →")
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}
