package com.example.sshclient.ssh

import com.example.sshclient.data.model.AuthType
import com.example.sshclient.data.model.Server
import com.example.sshclient.data.security.CredentialStore
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import net.schmizz.sshj.AndroidConfig
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.connection.channel.direct.PTYMode
import net.schmizz.sshj.connection.channel.direct.Session

class SshSession(
    private val server: Server,
    private val credentialStore: CredentialStore,
    private val hostKeyVerifier: HostKeyVerifier
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val terminalBuffer = TerminalBuffer(maxLines = 3000)

    private lateinit var client: SSHClient
    private lateinit var shell: Session.Shell
    private lateinit var outputReader: BufferedReader

    private val _outputFlow = MutableSharedFlow<String>(extraBufferCapacity = 512)
    val outputFlow: SharedFlow<String> = _outputFlow

    private val _connected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected

    suspend fun connect() = withContext(Dispatchers.IO) {
        if (_connected.value) return@withContext

        val config = AndroidConfig().apply {
            setKeyExchangeFactories(
                getKeyExchangeFactories().filterNot { factory ->
                    val name = factory.name.lowercase()
                    name.contains("curve25519") ||
                        name.contains("x25519")
                }
            )
        }

        client = SSHClient(config).apply {
            addHostKeyVerifier(hostKeyVerifier)
        }

        client.connect(server.host, server.port)

        when (val authType = server.authType) {
            is AuthType.Password -> {
                val password = credentialStore.getPassword(server.id.toString())
                    ?: error("No password stored for server ${server.id}")
                client.authPassword(server.username, password)
            }

            is AuthType.PrivateKey -> {
                val alias = authType.alias.ifBlank {
                    server.privateKeyAlias?.ifBlank { null } ?: server.id.toString()
                }
                val pem = credentialStore.getPrivateKey(alias)
                    ?: error("No private key stored for alias $alias")
                val keyProvider = client.loadKeys(pem, null, null)
                client.authPublickey(server.username, keyProvider)
            }
        }

        val session = client.startSession()
        session.allocatePTY("xterm-256color", 220, 50, 0, 0, emptyMap<PTYMode, Int>())
        shell = session.startShell()
        outputReader = BufferedReader(InputStreamReader(shell.inputStream, Charsets.UTF_8))
        _connected.value = true

        scope.launch {
            readOutput()
        }
    }

    private suspend fun readOutput() = withContext(Dispatchers.IO) {
        try {
            val buffer = CharArray(4096)
            while (isActive && !shell.isEOF) {
                val count = outputReader.read(buffer)
                if (count > 0) {
                    val chunk = String(buffer, 0, count)
                    terminalBuffer.append(chunk)
                    _outputFlow.emit(chunk)
                } else if (count < 0) {
                    break
                }
            }
        } catch (e: Exception) {
            val message = "\r\n[Session closed: ${e.message ?: "unknown error"}]\r\n"
            terminalBuffer.append(message)
            _outputFlow.emit(message)
        } finally {
            _connected.value = false
        }
    }

    suspend fun send(text: String) = withContext(Dispatchers.IO) {
        if (::shell.isInitialized && !shell.isEOF) {
            shell.outputStream.write(text.toByteArray(Charsets.UTF_8))
            shell.outputStream.flush()
        }
    }

    suspend fun resize(cols: Int, rows: Int) = withContext(Dispatchers.IO) {
        if (::shell.isInitialized) {
            shell.changeWindowDimensions(cols, rows, 0, 0)
        }
    }

    fun snapshotLines(): List<String> = terminalBuffer.snapshot()

    fun clearBuffer() {
        terminalBuffer.clear()
    }

    fun disconnect() {
        runCatching { shell.close() }
        runCatching { client.disconnect() }
        scope.cancel()
        _connected.value = false
    }
}
