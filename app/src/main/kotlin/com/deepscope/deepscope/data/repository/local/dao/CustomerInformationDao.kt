package com.deepscope.deepscope.data.repository.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.deepscope.deepscope.data.repository.local.entity.CustomerInformationEntity
import com.deepscope.deepscope.data.repository.local.entity.CustomerInformationWithImages
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerInformationDao : BaseDao<CustomerInformationEntity> {
    @Query("SELECT * FROM customer_information")
    fun get(): Flow<List<CustomerInformationEntity>>

    @Query("SELECT * FROM customer_information WHERE id = :id")
    fun getById(id: String): Flow<CustomerInformationWithImages>

    @Query("SELECT * FROM customer_information WHERE name LIKE '%' || :name || '%'")
    fun get(name: String): Flow<List<CustomerInformationEntity>>

    @Transaction
    @Query("SELECT * FROM customer_information")
    fun getCustomerInformationWithImages(): Flow<List<CustomerInformationWithImages>>

    @Transaction
    suspend fun insertAll(listCustomerInformationEntity: List<CustomerInformationEntity>) {
        deleteAll()
        insert(listCustomerInformationEntity)
    }

    @Query("DELETE FROM customer_information")
    suspend fun deleteAll()
}
