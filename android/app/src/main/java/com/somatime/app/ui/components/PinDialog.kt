package com.somatime.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.somatime.app.data.CbcCurriculumRepository
import com.somatime.app.model.RoleView
import com.somatime.app.ui.theme.SomaTeal

@Composable
fun PinDialog(
    title: String,
    subtitle: String,
    expectedPin: String = CbcCurriculumRepository.PARENT_PIN,
    confirmLabel: String = "Verify PIN",
    exitTargetRole: RoleView = RoleView.PARENT,
    onChangeExitTargetRole: ((RoleView) -> Unit)? = null,
    onSuccess: () -> Unit,
    onDismiss: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }

    fun handleDigit(digit: String) {
        if (pin.length >= 4) return
        val next = pin + digit
        pin = next
        error = ""
        if (next.length == 4) {
            if (next == expectedPin) {
                onSuccess()
            } else {
                error = "Incorrect Parent PIN. Please try again."
                pin = ""
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Rounded.Lock, contentDescription = null, tint = SomaTeal)
                Column {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Color(0xFF64748B))
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (onChangeExitTargetRole != null) {
                    Text(
                        text = "Return after PIN check:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF475569)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = exitTargetRole == RoleView.PARENT,
                            onClick = { onChangeExitTargetRole(RoleView.PARENT) },
                            label = { Text("Parent") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = exitTargetRole == RoleView.TEACHER,
                            onClick = { onChangeExitTargetRole(RoleView.TEACHER) },
                            label = { Text("Teacher") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    repeat(4) { idx ->
                        val filled = idx < pin.length
                        Surface(
                            modifier = Modifier.size(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            color = if (filled) Color(0xFFCCFBF1) else Color(0xFFF8FAFC),
                            border = BorderStroke(
                                1.dp,
                                if (filled) SomaTeal else Color(0xFFCBD5E1)
                            )
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = if (filled) "•" else "",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = SomaTeal
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Enter your 4-digit Parent PIN",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF64748B),
                    textAlign = TextAlign.Center
                )
                if (error.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = error,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFFDC2626),
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                val rows = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9")
                )
                rows.forEach { row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row.forEach { digit ->
                            OutlinedButton(
                                onClick = { handleDigit(digit) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text(digit, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text("Cancel")
                    }
                    OutlinedButton(
                        onClick = { handleDigit("0") },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("0", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = {
                            if (pin.isNotEmpty()) pin = pin.dropLast(1)
                            error = ""
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Rounded.Backspace,
                            contentDescription = "Delete",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (pin == expectedPin) {
                        onSuccess()
                    } else {
                        error = "Incorrect Parent PIN. Please try again."
                        pin = ""
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = SomaTeal)
            ) {
                Text(confirmLabel)
            }
        }
    )
}
