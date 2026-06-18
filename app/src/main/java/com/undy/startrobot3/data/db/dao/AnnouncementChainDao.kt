package com.undy.startrobot3.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.undy.startrobot3.data.db.entity.AnnouncementChainEntity
import com.undy.startrobot3.data.db.relation.ChainWithAnnouncements
import kotlinx.coroutines.flow.Flow

@Dao
interface AnnouncementChainDao {

    @Transaction
    @Query("SELECT * FROM announcement_chains ORDER BY sort_order ASC")
    fun getAllChains(): Flow<List<ChainWithAnnouncements>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(chain: AnnouncementChainEntity): Long

    @Update
    suspend fun update(chain: AnnouncementChainEntity)

    @Delete
    suspend fun delete(chain: AnnouncementChainEntity)

    @Query("SELECT COUNT(*) FROM announcement_chains")
    suspend fun count(): Int
}
