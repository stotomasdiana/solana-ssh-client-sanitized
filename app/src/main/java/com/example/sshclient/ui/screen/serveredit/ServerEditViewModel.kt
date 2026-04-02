package com.example.sshclient.ui.screen.serveredit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sshclient.data.model.AuthType
import com.example.sshclient.data.model.Server
import com.example.sshclient.data.repository.ServerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ServerEditViewModel @Inject constructor(
    private val serverRepository: ServerRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    data class EditState(
        val name: String = "",
        val host: String = "",
        val port: String = "22",
        val username: String = "",
        val authType: String = "PASSWORD",
        val password: String = "",
        val privateKeyPem: String = "",
        val isLoading: Boolean = false,
        val error: String? = null,
        val saved: Boolean = false
    )

    private val serverId: Long = savedStateHandle["serverId"] ?: -1L

    private val _state = MutableStateFlow(EditState())
    val state: StateFlow<EditState> = _state

    init {
        if (serverId != -1L) {
            viewModelScope.launch {
                serverRepository.getById(serverId)?.let { server ->
                    _state.update {
                        it.copy(
                            name = server.name,
                            host = server.host,
                            port = server.port.toString(),
                            username = server.username,
                            authType = when (server.authType) {
                                is AuthType.Password -> "PASSWORD"
                                is AuthType.PrivateKey -> "PRIVATE_KEY"
                            },
                            password = serverRepository.getStoredPassword(server.id).orEmpty(),
                            privateKeyPem = server.privateKeyAlias
                                ?.let(serverRepository::getStoredPrivateKey)
                                .orEmpty()
                        )
                    }
                }
            }
        }
    }

    fun update(block: EditState.() -> EditState) {
        _state.update(block)
    }

    fun save() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            runCatching {
                val current = _state.value
                val port = current.port.toIntOrNull() ?: throw IllegalArgumentException("Port is invalid")
                require(current.host.isNotBlank()) { "Host is required" }
                require(current.username.isNotBlank()) { "Username is required" }
                if (current.authType == "PASSWORD") {
                    require(current.password.isNotBlank()) { "Password is required" }
                } else {
                    require(current.privateKeyPem.contains("PRIVATE KEY")) { "Private key content is invalid" }
                }

                val authType: AuthType = if (current.authType == "PASSWORD") {
                    AuthType.Password
                } else {
                    AuthType.PrivateKey(
                        alias = if (serverId == -1L) "" else serverId.toString()
                    )
                }

                val server = Server(
                    id = if (serverId == -1L) 0L else serverId,
                    name = current.name.ifBlank { current.host.trim() },
                    host = current.host.trim(),
                    port = port,
                    username = current.username.trim(),
                    authType = authType,
                    privateKeyAlias = if (authType is AuthType.PrivateKey && serverId != -1L) {
                        serverId.toString()
                    } else {
                        null
                    }
                )

                serverRepository.save(
                    server = server,
                    password = if (current.authType == "PASSWORD") current.password else null,
                    privateKeyPem = if (current.authType == "PRIVATE_KEY") current.privateKeyPem else null
                )
            }.onSuccess {
                _state.update { it.copy(isLoading = false, saved = true) }
            }.onFailure { error ->
                _state.update { it.copy(isLoading = false, error = error.message) }
            }
        }
    }
}
