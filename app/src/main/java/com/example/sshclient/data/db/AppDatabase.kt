package com.example.sshclient.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.sshclient.data.db.dao.KnownHostDao
import com.example.sshclient.data.db.dao.ServerDao
import com.example.sshclient.data.db.entity.KnownHostEntity
import com.example.sshclient.data.db.entity.ServerEntity

@Database(
    entities = [ServerEntity::class, KnownHostEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun serverDao(): ServerDao
    abstract fun knownHostDao(): KnownHostDao
}
