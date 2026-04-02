package com.example.sshclient.ssh

import android.util.Base64
import com.example.sshclient.data.db.entity.KnownHostEntity
import com.example.sshclient.data.model.HostKeyAction
import com.example.sshclient.data.repository.KnownHostRepository
import java.security.MessageDigest
import java.security.PublicKey
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.runBlocking

@Singleton
class HostKeyVerifier @Inject constructor(
    private val knownHostRepo: KnownHostRepository
) : net.schmizz.sshj.transport.verification.HostKeyVerifier {

    var onUnknownHost: (suspend (host: String, port: Int, keyType: String, fingerprint: String) -> HostKeyAction)? =
        null

    override fun verify(hostname: String, port: Int, key: PublicKey): Boolean {
        return runBlocking { verifyAsync(hostname, port, key) }
    }

    override fun findExistingAlgorithms(hostname: String, port: Int): MutableList<String> =
        runBlocking {
            knownHostRepo.find(hostname, port)
                ?.let { mutableListOf(it.keyType) }
                ?: mutableListOf()
        }

    private suspend fun verifyAsync(host: String, port: Int, key: PublicKey): Boolean {
        val fingerprint = computeFingerprint(key)
        val keyType = key.algorithm
        val stored = knownHostRepo.find(host, port)

        return when {
            stored == null -> {
                val action = onUnknownHost?.invoke(host, port, keyType, fingerprint)
                    ?: HostKeyAction.REJECT
                if (action == HostKeyAction.ACCEPT || action == HostKeyAction.ACCEPT_ONCE) {
                    if (action == HostKeyAction.ACCEPT) {
                        knownHostRepo.upsert(
                            KnownHostEntity(
                                host = host,
                                port = port,
                                keyType = keyType,
                                fingerprint = fingerprint
                            )
                        )
                    }
                    true
                } else {
                    false
                }
            }

            stored.fingerprint == fingerprint -> true

            else -> {
                throw SecurityException(
                    "Host key mismatch for $host:$port. Expected ${stored.fingerprint}, got $fingerprint."
                )
            }
        }
    }

    private fun computeFingerprint(key: PublicKey): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(key.encoded)
        return "SHA256:${Base64.encodeToString(digest, Base64.NO_WRAP)}"
    }
}
