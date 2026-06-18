package com.undy.startrobot3.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.undy.startrobot3.data.db.dao.AnnouncementChainDao
import com.undy.startrobot3.data.db.dao.AnnouncementDao
import com.undy.startrobot3.data.db.entity.AnnouncementChainEntity
import com.undy.startrobot3.data.db.entity.AnnouncementEntity

@Database(
    entities = [AnnouncementChainEntity::class, AnnouncementEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun chainDao(): AnnouncementChainDao
    abstract fun announcementDao(): AnnouncementDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(context, AppDatabase::class.java, "startrobot.db")
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
