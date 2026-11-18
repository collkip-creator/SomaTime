package com.somatime.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.School
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.somatime.app.data.SomaTimeStore
import com.somatime.app.guard.ScreenTimeGuardService
import com.somatime.app.model.PendingApproval
import com.somatime.app.model.RoleView
import com.somatime.app.ui.components.PinDialog
import com.somatime.app.ui.screens.ChildModeScreen
import com.somatime.app.ui.screens.ParentDashboardScreen
import com.somatime.app.ui.screens.TeacherPortalScreen
import com.somatime.app.ui.theme.SomaTeal
import com.somatime.app.ui.theme.SomaTimeTheme

private sealed interface PinIntent {
    data class ExitHandOff(val targetRole: RoleView) : PinIntent
    data object UnlockChild : PinIntent
    data class ApproveMinutes(val approval: PendingApproval) : PinIntent
}

class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Permission result handled gracefully */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        val store = SomaTimeStore.getInstance(this)
        handleIncomingIntent(intent, store)

        setContent {
            SomaTimeTheme {
                SomaTimeRoot(store = store)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingIntent(intent, SomaTimeStore.getInstance(this))
    }

    private fun handleIncomingIntent(intent: Intent?, store: SomaTimeStore) {
        val interceptedPkg = intent?.getStringExtra(ScreenTimeGuardService.EXTRA_INTERCEPTED_APP)
        if (!interceptedPkg.isNullOrBlank()) {
            store.updateForegroundApp(interceptedPkg)
            store.setRoleView(RoleView.CHILD)
        }
    }
}

@Composable
private fun SomaTimeRoot(store: SomaTimeStore) {
    val uiState by store.state.collectAsState()
    var pinIntent by remember { mutableStateOf<PinIntent?>(null) }

    fun handleRoleSelect(nextRole: RoleView) {
        if (uiState.handOffActive && uiState.roleView == RoleView.CHILD && nextRole != RoleView.CHILD) {
            pinIntent = PinIntent.ExitHandOff(nextRole)
        } else {
            store.setRoleView(nextRole)
        }
    }

    Scaffold(
        containerColor = Color(0xFFF8FAFC),
        topBar = {
            Surface(color = Color.White) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "SomaTime",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = SomaTeal
                        )
                        IconButton(onClick = { store.resetDemo() }) {
                            Icon(Icons.Rounded.Refresh, contentDescription = "Reset Demo")
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 12.dp, end = 12.dp, bottom = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = uiState.roleView == RoleView.PARENT,
                            onClick = { handleRoleSelect(RoleView.PARENT) },
                            label = { Text("Parent") },
                            leadingIcon = { Icon(Icons.Rounded.AdminPanelSettings, contentDescription = null) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = uiState.roleView == RoleView.CHILD,
                            onClick = { handleRoleSelect(RoleView.CHILD) },
                            label = { Text("Child") },
                            leadingIcon = { Icon(Icons.Rounded.PhoneAndroid, contentDescription = null) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = uiState.roleView == RoleView.TEACHER,
                            onClick = { handleRoleSelect(RoleView.TEACHER) },
                            label = { Text("Teacher") },
                            leadingIcon = { Icon(Icons.Rounded.School, contentDescription = null) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    HorizontalDivider()
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (uiState.roleView) {
                RoleView.PARENT -> ParentDashboardScreen(
                    uiState = uiState,
                    onSelectChild = store::selectChild,
                    onUpdateChildCap = { id, cap ->
                        store.updateChild(id) { it.copy(dailyCapMins = cap) }
                    },
                    onUpdateExchangeRate = { id, rate ->
                        store.updateChild(id) { it.copy(exchangeRateMins = rate) }
                    },
                    onToggleMandatory = { id, area ->
                        store.updateChild(id) { c ->
                            val next = if (area in c.mandatorySubjects) {
                                c.mandatorySubjects - area
                            } else {
                                c.mandatorySubjects + area
                            }
                            c.copy(mandatorySubjects = next)
                        }
                    },
                    onSetApprovalRequired = { id, req ->
                        store.updateChild(id) { it.copy(requireParentApproval = req) }
                    },
                    onSetDeviceMode = { id, mode ->
                        store.updateChild(id) { it.copy(deviceMode = mode) }
                    },
                    onActivateHandOff = store::activateHandOffMode,
                    onAddChild = store::addChild,
                    onRequestApprovalPin = { approval ->
                        pinIntent = PinIntent.ApproveMinutes(approval)
                    },
                    onDeclineApproval = { childId, approvalId ->
                        store.resolveApproval(childId, approvalId, approved = false)
                    }
                )

                RoleView.CHILD -> ChildModeScreen(
                    uiState = uiState,
                    onSimulateTimeExpired = store::simulateTimeExpired,
                    onOpenPinUnlock = { pinIntent = PinIntent.UnlockChild },
                    onOpenExitHandOff = { pinIntent = PinIntent.ExitHandOff(RoleView.PARENT) },
                    onCompleteLesson = store::completeLesson
                )

                RoleView.TEACHER -> TeacherPortalScreen(
                    uiState = uiState,
                    onPushAssignment = store::pushAssignment,
                    onPreviewChildMode = { store.setRoleView(RoleView.CHILD) }
                )
            }
        }
    }

    when (val intent = pinIntent) {
        is PinIntent.ExitHandOff -> PinDialog(
            title = "Exit Hand-Off Lock",
            subtitle = "Enter Parent PIN (1234) to exit ${uiState.selectedChild.name}'s locked mode.",
            confirmLabel = "Exit to Parent",
            onSuccess = {
                store.exitHandOffTo(intent.targetRole)
                pinIntent = null
            },
            onDismiss = { pinIntent = null }
        )

        PinIntent.UnlockChild -> PinDialog(
            title = "Parent PIN Screen Unlock",
            subtitle = "Enter Parent PIN (1234) to grant +15 mins to ${uiState.selectedChild.name}.",
            confirmLabel = "Unlock +15m",
            onSuccess = {
                store.unlockWithParentPin(uiState.selectedChild.id, 15)
                pinIntent = null
            },
            onDismiss = { pinIntent = null }
        )

        is PinIntent.ApproveMinutes -> PinDialog(
            title = "Approve Earned Minutes",
            subtitle = "Confirm +${intent.approval.minutesEarned} mins for ${intent.approval.lessonTitle}.",
            confirmLabel = "Approve",
            onSuccess = {
                store.resolveApproval(uiState.selectedChild.id, intent.approval.id, approved = true)
                pinIntent = null
            },
            onDismiss = { pinIntent = null }
        )

        null -> Unit
    }
}
