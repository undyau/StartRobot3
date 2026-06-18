package com.undy.startrobot3.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "announcement_chains")
data class AnnouncementChainEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "sort_order") val sortOrder: Int = 0
)
