package com.example.sshclient.data.repository

import com.example.sshclient.data.db.dao.ServerDao
import com.example.sshclient.data.db.entity.ServerEntity
import com.example.sshclient.data.model.AuthType
import com.example.sshclient.data.model.Server
import com.example.sshclient.data.security.CredentialStore
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class ServerRepository @Inject constructor(
    private val dao: ServerDao,
    private val credentialStore: CredentialStore
) {
    fun observeAll(): Flow<List<Server>> =
        dao.observeAll().map { servers -> servers.map { it.toDomain() } }

    suspend fun getById(id: Long): Server? = dao.getById(id)?.toDomain()

    fun getStoredPassword(serverId: Long): String? = credentialStore.getPassword(serverId.toString())

    fun getStoredPrivateKey(alias: String): String? = credentialStore.getPrivateKey(alias)

    suspend fun save(server: Server, password: String?, privateKeyPem: String?): Long {
        val id = dao.upsert(server.toEntity())
        password?.let { credentialStore.savePassword(id.toString(), it) }
        privateKeyPem?.let {
            val alias = server.privateKeyAlias?.ifBlank { null } ?: id.toString()
            credentialStore.savePrivateKey(alias, it)
        }
        return id
    }

    suspend fun delete(server: Server) {
        dao.delete(server.toEntity())
        credentialStore.deleteAlias(server.id.toString())
        server.privateKeyAlias?.takeIf { it.isNotBlank() }?.let(credentialStore::deleteAlias)
    }

    private fun ServerEntity.toDomain(): Server =
        Server(
            id = id,
            name = name,
            host = host,
            port = port,
            username = username,
            authType = if (authType == "PASSWORD") {
                AuthType.Password
            } else {
                AuthType.PrivateKey(privateKeyAlias ?: id.toString())
            },
            privateKeyAlias = privateKeyAlias
        )

    private fun Server.toEntity(): ServerEntity =
        ServerEntity(
            id = id,
            name = name,
            host = host,
            port = port,
            username = username,
            authType = when (authType) {
                is AuthType.Password -> "PASSWORD"
                is AuthType.PrivateKey -> "PRIVATE_KEY"
            },
            privateKeyAlias = privateKeyAlias
        )
}
