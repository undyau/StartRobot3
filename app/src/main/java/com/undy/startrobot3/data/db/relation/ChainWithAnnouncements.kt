package com.undy.startrobot3.data.db.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.undy.startrobot3.data.db.entity.AnnouncementChainEntity
import com.undy.startrobot3.data.db.entity.AnnouncementEntity

data class ChainWithAnnouncements(
    @Embedded val chain: AnnouncementChainEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "chain_id"
    )
    val announcements: List<AnnouncementEntity>
)
