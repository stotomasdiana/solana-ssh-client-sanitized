package com.example.sshclient.ssh

import com.example.sshclient.data.model.Server
import com.example.sshclient.data.security.CredentialStore
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SshConnectionManager @Inject constructor(
    private val credentialStore: CredentialStore
) {
    private val sessions = ConcurrentHashMap<Long, SshSession>()

    fun getSession(serverId: Long): SshSession? = sessions[serverId]

    fun createSession(server: Server, hostKeyVerifier: HostKeyVerifier): SshSession {
        return sessions.getOrPut(server.id) {
            SshSession(server, credentialStore, hostKeyVerifier)
        }
    }

    fun removeSession(serverId: Long) {
        sessions.remove(serverId)?.disconnect()
    }

    fun disconnectAll() {
        sessions.values.forEach(SshSession::disconnect)
        sessions.clear()
    }
}
