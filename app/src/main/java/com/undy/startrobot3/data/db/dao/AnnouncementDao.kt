package com.undy.startrobot3.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.undy.startrobot3.data.db.entity.AnnouncementEntity

@Dao
interface AnnouncementDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(announcement: AnnouncementEntity): Long

    @Update
    suspend fun update(announcement: AnnouncementEntity)

    @Delete
    suspend fun delete(announcement: AnnouncementEntity)

    @Query("DELETE FROM announcements WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM announcements WHERE chain_id = :chainId ORDER BY sort_order ASC")
    suspend fun getForChain(chainId: Long): List<AnnouncementEntity>
}
