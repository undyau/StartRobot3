package com.undy.startrobot3.data.repository

import com.undy.startrobot3.data.db.AppDatabase
import com.undy.startrobot3.data.db.entity.AnnouncementChainEntity
import com.undy.startrobot3.data.db.entity.AnnouncementEntity
import com.undy.startrobot3.data.model.Announcement
import com.undy.startrobot3.data.model.AnnouncementChain
import com.undy.startrobot3.data.model.AnnouncementType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class EventRepository(private val db: AppDatabase) {

    val chains: Flow<List<AnnouncementChain>> = db.chainDao().getAllChains().map { list ->
        list.map { cwa ->
            AnnouncementChain(
                id = cwa.chain.id,
                sortOrder = cwa.chain.sortOrder,
                announcements = cwa.announcements
                    .sortedBy { it.sortOrder }
                    .map { it.toDomain() }
            )
        }.sortedBy { it.sortOrder }
    }

    suspend fun insertChain(sortOrder: Int): Long =
        db.chainDao().insert(AnnouncementChainEntity(sortOrder = sortOrder))

    suspend fun deleteChain(chain: AnnouncementChain) =
        db.chainDao().delete(AnnouncementChainEntity(id = chain.id, sortOrder = chain.sortOrder))

    suspend fun updateChainOrder(chainId: Long, sortOrder: Int) =
        db.chainDao().update(AnnouncementChainEntity(id = chainId, sortOrder = sortOrder))

    suspend fun insertAnnouncement(announcement: Announcement): Long =
        db.announcementDao().insert(announcement.toEntity())

    suspend fun updateAnnouncement(announcement: Announcement) =
        db.announcementDao().update(announcement.toEntity())

    suspend fun deleteAnnouncement(announcement: Announcement) =
        db.announcementDao().delete(announcement.toEntity())

    suspend fun chainCount(): Int = db.chainDao().count()

    private fun AnnouncementEntity.toDomain() = Announcement(
        id = id,
        chainId = chainId,
        sortOrder = sortOrder,
        isAnchor = isAnchor,
        anchorOffsetSeconds = anchorOffsetSeconds,
        type = AnnouncementType.valueOf(type),
        timeOffsetSeconds = timeOffsetSeconds,
        text = text,
        audioFilePath = audioFilePath,
        beepCount = beepCount,
        estimatedDurationMs = estimatedDurationMs
    )

    private fun Announcement.toEntity() = AnnouncementEntity(
        id = id,
        chainId = chainId,
        sortOrder = sortOrder,
        isAnchor = isAnchor,
        anchorOffsetSeconds = anchorOffsetSeconds,
        type = type.name,
        timeOffsetSeconds = timeOffsetSeconds,
        text = text,
        audioFilePath = audioFilePath,
        beepCount = beepCount,
        estimatedDurationMs = estimatedDurationMs
    )
}
