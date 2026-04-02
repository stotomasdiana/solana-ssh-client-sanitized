package com.example.sshclient.ui.screen.serverlist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.solana.mobilewalletadapter.clientlib.ActivityResultSender
import com.example.sshclient.data.model.Server
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerListScreen(
    walletActivityResultSender: ActivityResultSender?,
    onAddServer: () -> Unit,
    onEditServer: (Long) -> Unit,
    onConnectServer: (Long) -> Unit,
    viewModel: ServerListViewModel = hiltViewModel()
) {
    val servers by viewModel.servers.collectAsStateWithLifecycle()
    val walletTransferState by viewModel.walletTransferState.collectAsStateWithLifecycle()
    val isPreview = LocalInspectionMode.current

    LaunchedEffect(walletActivityResultSender, isPreview) {
        if (!isPreview && walletActivityResultSender != null) {
            viewModel.launchWalletTransfer(walletActivityResultSender)
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("SSH Client") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddServer) {
                Icon(Icons.Default.Add, contentDescription = "Add server")
            }
        }
    ) { paddingValues ->
        if (servers.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                WalletTransferCard(
                    state = walletTransferState,
                    onRetry = {
                        if (walletActivityResultSender != null) {
                            viewModel.retryWalletTransfer(walletActivityResultSender)
                        }
                    }
                )
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No servers yet. Tap + to add one.",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = paddingValues,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    WalletTransferCard(
                        state = walletTransferState,
                        onRetry = {
                            if (walletActivityResultSender != null) {
                                viewModel.retryWalletTransfer(walletActivityResultSender)
                            }
                        }
                    )
                }
                items(servers, key = { it.id }) { server ->
                    ServerListItem(
                        server = server,
                        onConnect = { onConnectServer(server.id) },
                        onEdit = { onEditServer(server.id) },
                        onDelete = { viewModel.deleteServer(server) }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun WalletTransferCard(
    state: WalletTransferState,
    onRetry: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Wallet Signing",
                style = MaterialTheme.typography.titleMedium
            )
            AssistChip(
                onClick = {},
                enabled = false,
                label = { Text(state.statusText) },
                colors = AssistChipDefaults.assistChipColors(
                    disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                    disabledLabelColor = MaterialTheme.colorScheme.onSurface
                )
            )
            state.walletAddress?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = "Wallet address: $it",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            state.signature?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = "Transaction signature: $it",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            state.errorMessage?.let {
                Text(
                    text = "Error: $it",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (state.signature.isNullOrBlank()) {
                TextButton(onClick = onRetry, enabled = !state.isRunning) {
                    Icon(Icons.Default.Sync, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Retry")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ServerListItem(
    server: Server,
    onConnect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    ListItem(
        headlineContent = { Text(server.name) },
        supportingContent = { Text("${server.username}@${server.host}:${server.port}") },
        trailingContent = {
            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                }
                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                }
            }
        },
        modifier = Modifier.clickable(onClick = onConnect)
    )

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Server") },
            text = { Text("Delete ${server.name}?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
