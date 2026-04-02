package com.example.sshclient.data.repository

import com.example.sshclient.data.db.dao.KnownHostDao
import com.example.sshclient.data.db.entity.KnownHostEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KnownHostRepository @Inject constructor(
    private val dao: KnownHostDao
) {
    suspend fun find(host: String, port: Int): KnownHostEntity? = dao.find(host, port)

    suspend fun upsert(entity: KnownHostEntity) = dao.upsert(entity)

    suspend fun delete(host: String, port: Int) = dao.delete(host, port)
}
