package com.example.sshclient.ui.screen.hostkey

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HostKeyDialog(
    host: String,
    port: Int,
    keyType: String,
    fingerprint: String,
    onAccept: () -> Unit,
    onAcceptOnce: () -> Unit,
    onReject: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onReject,
        icon = { Icon(Icons.Default.Security, contentDescription = null) },
        title = { Text("Unknown Host") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("First connection to:")
                Text("$host:$port", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Key type: $keyType", style = MaterialTheme.typography.bodySmall)
                Text("Fingerprint (SHA-256):", style = MaterialTheme.typography.bodySmall)
                Text(
                    text = fingerprint,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Verify the fingerprint out of band before trusting this host.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onAccept) {
                Text("Trust Permanently")
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onAcceptOnce) {
                    Text("This Session Only")
                }
                TextButton(onClick = onReject) {
                    Text("Reject")
                }
            }
        }
    )
}
