package com.example.sshclient.di

import android.content.Context
import androidx.room.Room
import com.example.sshclient.data.db.AppDatabase
import com.example.sshclient.data.db.dao.KnownHostDao
import com.example.sshclient.data.db.dao.ServerDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "ssh_client.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideServerDao(db: AppDatabase): ServerDao = db.serverDao()

    @Provides
    fun provideKnownHostDao(db: AppDatabase): KnownHostDao = db.knownHostDao()
}
