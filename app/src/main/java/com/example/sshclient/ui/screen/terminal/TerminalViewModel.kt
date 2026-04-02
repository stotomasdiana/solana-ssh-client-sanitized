package com.example.sshclient.ui.screen.terminal

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sshclient.data.model.HostKeyAction
import com.example.sshclient.data.repository.ServerRepository
import com.example.sshclient.ssh.HostKeyVerifier
import com.example.sshclient.ssh.SshConnectionManager
import com.example.sshclient.ssh.SshSession
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import net.schmizz.sshj.userauth.UserAuthException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class TerminalViewModel @Inject constructor(
    private val serverRepository: ServerRepository,
    private val connectionManager: SshConnectionManager,
    private val hostKeyVerifier: HostKeyVerifier,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    data class PendingHostKey(
        val host: String,
        val port: Int,
        val keyType: String,
        val fingerprint: String,
        val deferred: CompletableDeferred<HostKeyAction>
    )

    data class TerminalState(
        val lines: List<String> = emptyList(),
        val isConnecting: Boolean = false,
        val isConnected: Boolean = false,
        val error: String? = null,
        val hostKeyConflictMessage: String? = null,
        val pendingHostKey: PendingHostKey? = null
    )

    private val serverId: Long = checkNotNull(savedStateHandle["serverId"])

    private val _state = MutableStateFlow(TerminalState())
    val state: StateFlow<TerminalState> = _state

    private var session: SshSession? = null
    private var outputJob: Job? = null

    init {
        connect()
    }

    fun reconnect() {
        disconnect()
        _state.value = TerminalState()
        connect()
    }

    private fun connect() {
        viewModelScope.launch {
            _state.update { it.copy(isConnecting = true, error = null) }

            hostKeyVerifier.onUnknownHost = { host, port, keyType, fingerprint ->
                val deferred = CompletableDeferred<HostKeyAction>()
                _state.update {
                    it.copy(
                        pendingHostKey = PendingHostKey(
                            host = host,
                            port = port,
                            keyType = keyType,
                            fingerprint = fingerprint,
                            deferred = deferred
                        )
                    )
                }
                deferred.await().also {
                    _state.update { current -> current.copy(pendingHostKey = null) }
                }
            }

            val server = serverRepository.getById(serverId)
            if (server == null) {
                _state.update { it.copy(isConnecting = false, error = "Server not found") }
                return@launch
            }

            val activeSession = connectionManager.createSession(server, hostKeyVerifier)
            session = activeSession
            subscribeOutput(activeSession)
            _state.update { it.copy(lines = activeSession.snapshotLines()) }

            runCatching {
                activeSession.connect()
            }.onSuccess {
                _state.update {
                    it.copy(
                        isConnecting = false,
                        isConnected = true,
                        hostKeyConflictMessage = null
                    )
                }
            }.onFailure { error ->
                val message = when (error) {
                    is SecurityException -> "The host fingerprint changed, so the connection was rejected. Verify it out of band before trusting this host."
                    is UserAuthException -> "Authentication failed. Check the username, password, or private key."
                    else -> error.message ?: "Connection failed"
                }
                _state.update {
                    it.copy(
                        isConnecting = false,
                        isConnected = false,
                        error = message,
                        hostKeyConflictMessage = if (error is SecurityException) message else null
                    )
                }
            }
        }
    }

    private fun subscribeOutput(activeSession: SshSession) {
        outputJob?.cancel()
        outputJob = viewModelScope.launch {
            activeSession.outputFlow.collect {
                _state.update {
                    it.copy(
                        lines = activeSession.snapshotLines(),
                        isConnected = activeSession.connected.value
                    )
                }
            }
        }
    }

    fun send(text: String) {
        viewModelScope.launch {
            session?.send(text)
        }
    }

    fun sendLine(command: String) {
        send("$command\n")
    }

    fun sendCtrl(char: Char) {
        val ctrl = (char.uppercaseChar().code - 'A'.code + 1).toChar()
        send(ctrl.toString())
    }

    fun resolveHostKey(action: HostKeyAction) {
        _state.value.pendingHostKey?.deferred?.complete(action)
    }

    fun clearBuffer() {
        session?.clearBuffer()
        _state.update { it.copy(lines = emptyList()) }
    }

    fun dismissHostKeyConflict() {
        _state.update { it.copy(hostKeyConflictMessage = null) }
    }

    fun disconnect() {
        outputJob?.cancel()
        connectionManager.removeSession(serverId)
        session = null
        _state.update { it.copy(isConnected = false, isConnecting = false) }
    }

    override fun onCleared() {
        super.onCleared()
        disconnect()
    }
}
