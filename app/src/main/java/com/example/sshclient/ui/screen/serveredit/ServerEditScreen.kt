package com.example.sshclient.ui.screen.serveredit

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.sshclient.ui.component.LoadingOverlay
import com.example.sshclient.ui.component.PasswordField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerEditScreen(
    onBack: () -> Unit,
    viewModel: ServerEditViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        val content = context.contentResolver.openInputStream(uri)
            ?.bufferedReader()
            ?.use { it.readText() }
            ?: return@rememberLauncherForActivityResult

        if (content.contains("PRIVATE KEY")) {
            viewModel.update { copy(privateKeyPem = content, error = null) }
        } else {
            viewModel.update { copy(error = "The selected file is not a valid PEM private key") }
        }
    }

    LaunchedEffect(state.saved) {
        if (state.saved) onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.name.isEmpty()) "New Server" else "Edit Server") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = viewModel::save,
                        enabled = !state.isLoading
                    ) {
                        Text("Save")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = state.name,
                onValueChange = { value -> viewModel.update { copy(name = value) } },
                label = { Text("Name (Optional)") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = state.host,
                onValueChange = { value -> viewModel.update { copy(host = value) } },
                label = { Text("Host *") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
            )
            OutlinedTextField(
                value = state.port,
                onValueChange = { value -> viewModel.update { copy(port = value) } },
                label = { Text("Port") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            OutlinedTextField(
                value = state.username,
                onValueChange = { value -> viewModel.update { copy(username = value) } },
                label = { Text("Username *") },
                modifier = Modifier.fillMaxWidth()
            )

            HorizontalDivider()
            Text("Authentication", style = MaterialTheme.typography.titleSmall)

            androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = state.authType == "PASSWORD",
                    onClick = { viewModel.update { copy(authType = "PASSWORD") } },
                    label = { Text("Password") }
                )
                FilterChip(
                    selected = state.authType == "PRIVATE_KEY",
                    onClick = { viewModel.update { copy(authType = "PRIVATE_KEY") } },
                    label = { Text("Private Key") }
                )
            }

            if (state.authType == "PASSWORD") {
                PasswordField(
                    value = state.password,
                    onValueChange = { value -> viewModel.update { copy(password = value) } },
                    label = "Password"
                )
            } else {
                OutlinedButton(
                    onClick = { launcher.launch("*/*") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Key, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (state.privateKeyPem.isNotEmpty()) "Private key file selected" else "Choose private key file")
                }
                if (state.privateKeyPem.isNotEmpty()) {
                    Text(
                        text = "Private key loaded",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            state.error?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (state.isLoading) {
        LoadingOverlay()
    }
}
