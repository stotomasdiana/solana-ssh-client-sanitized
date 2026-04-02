package com.example.sshclient.di

import com.example.sshclient.data.db.dao.KnownHostDao
import com.example.sshclient.data.db.dao.ServerDao
import com.example.sshclient.data.repository.KnownHostRepository
import com.example.sshclient.data.repository.ServerRepository
import com.example.sshclient.data.security.CredentialStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideServerRepository(
        dao: ServerDao,
        credentialStore: CredentialStore
    ): ServerRepository = ServerRepository(dao, credentialStore)

    @Provides
    @Singleton
    fun provideKnownHostRepository(dao: KnownHostDao): KnownHostRepository =
        KnownHostRepository(dao)
}
