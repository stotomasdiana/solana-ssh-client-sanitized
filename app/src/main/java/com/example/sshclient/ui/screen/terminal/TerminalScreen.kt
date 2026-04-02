package com.example.sshclient.ui.screen.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.sshclient.data.model.HostKeyAction
import com.example.sshclient.ui.component.LoadingOverlay
import com.example.sshclient.ui.screen.hostkey.HostKeyDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalScreen(
    onBack: () -> Unit,
    viewModel: TerminalViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val clipboardManager = LocalClipboardManager.current
    var inputText by remember { mutableStateOf("") }

    state.pendingHostKey?.let { pending ->
        HostKeyDialog(
            host = pending.host,
            port = pending.port,
            keyType = pending.keyType,
            fingerprint = pending.fingerprint,
            onAccept = { viewModel.resolveHostKey(HostKeyAction.ACCEPT) },
            onAcceptOnce = { viewModel.resolveHostKey(HostKeyAction.ACCEPT_ONCE) },
            onReject = { viewModel.resolveHostKey(HostKeyAction.REJECT) }
        )
    }

    state.hostKeyConflictMessage?.let { message ->
        AlertDialog(
            onDismissRequest = viewModel::dismissHostKeyConflict,
            title = { Text("Host Fingerprint Mismatch") },
            text = { Text(message) },
            confirmButton = {
                Button(onClick = viewModel::dismissHostKeyConflict) {
                    Text("OK")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Terminal")
                        Text(
                            text = when {
                                state.isConnecting -> "Connecting…"
                                state.isConnected -> "Connected"
                                else -> "Disconnected"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (state.isConnected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.error
                            }
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            viewModel.disconnect()
                            onBack()
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Disconnect and go back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.clearBuffer() }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear terminal")
                    }
                }
            )
        },
        bottomBar = {
            Column {
                QuickKeyBar(
                    onTab = { viewModel.send("\t") },
                    onCtrlC = { viewModel.sendCtrl('C') },
                    onCtrlD = { viewModel.sendCtrl('D') },
                    onCtrlZ = { viewModel.sendCtrl('Z') },
                    onCtrlL = { viewModel.sendCtrl('L') },
                    onArrowUp = { viewModel.send("\u001B[A") },
                    onArrowDown = { viewModel.send("\u001B[B") },
                    onEscape = { viewModel.send("\u001B") },
                    onPaste = {
                        clipboardManager.getText()?.text?.let(viewModel::send)
                    }
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier.weight(1f),
                        textStyle = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (inputText.isNotBlank()) {
                                    viewModel.sendLine(inputText)
                                    inputText = ""
                                }
                            }
                        ),
                        decorationBox = { innerTextField ->
                            Box(modifier = Modifier.padding(4.dp)) {
                                if (inputText.isEmpty()) {
                                    Text(
                                        text = "Enter command…",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 14.sp
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )

                    IconButton(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                viewModel.sendLine(inputText)
                                inputText = ""
                            }
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            TerminalOutputView(
                lines = state.lines
            )

            if (state.isConnecting) {
                LoadingOverlay()
            }

            if (!state.isConnecting && !state.isConnected && state.error != null) {
                Button(
                    onClick = viewModel::reconnect,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                ) {
                    Text("Reconnect")
                }
            }

            state.error?.let { message ->
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(12.dp)
                ) {
                    Text(message, color = Color.White)
                }
            }
        }
    }
}
