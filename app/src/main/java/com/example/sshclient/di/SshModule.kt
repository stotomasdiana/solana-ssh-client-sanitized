package com.example.sshclient.di

import com.example.sshclient.data.repository.KnownHostRepository
import com.example.sshclient.data.security.CredentialStore
import com.example.sshclient.ssh.HostKeyVerifier
import com.example.sshclient.ssh.SshConnectionManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SshModule {

    @Provides
    @Singleton
    fun provideHostKeyVerifier(repo: KnownHostRepository): HostKeyVerifier = HostKeyVerifier(repo)

    @Provides
    @Singleton
    fun provideConnectionManager(
        credentialStore: CredentialStore
    ): SshConnectionManager = SshConnectionManager(credentialStore)
}
