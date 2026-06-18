package com.undy.startrobot3.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "announcements",
    foreignKeys = [ForeignKey(
        entity = AnnouncementChainEntity::class,
        parentColumns = ["id"],
        childColumns = ["chain_id"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("chain_id")]
)
data class AnnouncementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "chain_id") val chainId: Long,
    @ColumnInfo(name = "sort_order") val sortOrder: Int,
    @ColumnInfo(name = "is_anchor") val isAnchor: Boolean,
    @ColumnInfo(name = "anchor_offset_seconds") val anchorOffsetSeconds: Int,
    val type: String,
    @ColumnInfo(name = "time_offset_seconds") val timeOffsetSeconds: Int,
    val text: String,
    @ColumnInfo(name = "audio_file_path") val audioFilePath: String,
    @ColumnInfo(name = "beep_count") val beepCount: Int,
    @ColumnInfo(name = "estimated_duration_ms") val estimatedDurationMs: Long
)
