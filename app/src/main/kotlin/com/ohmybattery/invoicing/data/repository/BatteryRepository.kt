package com.ohmybattery.invoicing.data.repository

import com.ohmybattery.invoicing.data.local.dao.BatteryDao
import com.ohmybattery.invoicing.data.local.entity.BatteryEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BatteryRepository @Inject constructor(private val dao: BatteryDao) {
    fun observeActive(): Flow<List<BatteryEntity>> = dao.observeActive()
    suspend fun insert(item: BatteryEntity): Long = dao.insert(item)
    suspend fun update(item: BatteryEntity) = dao.update(item)
}
