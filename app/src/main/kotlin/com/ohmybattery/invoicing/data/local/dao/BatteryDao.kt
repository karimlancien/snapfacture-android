package com.ohmybattery.invoicing.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ohmybattery.invoicing.data.local.entity.BatteryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BatteryDao {
    @Query("SELECT * FROM batteries WHERE active = 1 ORDER BY sortOrder ASC, priceTtcCents ASC")
    fun observeActive(): Flow<List<BatteryEntity>>

    @Query("SELECT COUNT(*) FROM batteries")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<BatteryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: BatteryEntity): Long

    @Update
    suspend fun update(item: BatteryEntity)
}
