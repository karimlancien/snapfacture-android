package com.ohmybattery.invoicing.data.repository

import com.ohmybattery.invoicing.data.local.dao.ClientDao
import com.ohmybattery.invoicing.data.local.entity.ClientEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClientRepository @Inject constructor(private val dao: ClientDao) {
    fun observeAll(): Flow<List<ClientEntity>> = dao.observeAll()
    suspend fun search(query: String): List<ClientEntity> =
        if (query.isBlank()) emptyList() else dao.search(query.trim())
    suspend fun get(id: Long): ClientEntity? = dao.get(id)
    suspend fun insert(client: ClientEntity): Long = dao.insert(client)
    suspend fun update(client: ClientEntity) = dao.update(client)
}
