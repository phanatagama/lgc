package com.deepid.lgc.data.repository.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.deepid.lgc.data.common.BaseDao
import com.deepid.lgc.data.repository.local.entity.CustomerInformationEntity
import com.deepid.lgc.data.repository.local.entity.CustomerInformationWithImages
import com.deepid.lgc.data.repository.local.entity.DataImageEntity
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

@Dao
interface DataImageDao : BaseDao<DataImageEntity> {
    @Transaction
    @Query("SELECT * FROM data_image")
    fun get(): Flow<List<DataImageEntity>>

    @Query("SELECT * FROM data_image WHERE customerId = :customerId")
    fun get(customerId: Int): Flow<List<DataImageEntity>>

    @Query("SELECT * FROM data_image WHERE customerId = :customerId")
    fun getByCustomerId(customerId: Int): Flow<List<DataImageEntity>>

    @Query("SELECT * FROM data_image WHERE id = :id")
    fun getById(id: Int): Flow<DataImageEntity>

    @Delete
    suspend fun delete(dataImageEntity: List<DataImageEntity>)

    @Query("DELETE FROM data_image WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Transaction
    suspend fun insertAll(dataImageEntity: List<DataImageEntity>) {
        deleteAll()
        insert(dataImageEntity)
    }

    @Update
    suspend fun update(dataImageEntity: List<DataImageEntity>)

    @Query("UPDATE data_image SET customerId = :customerId WHERE id IN (:id)")
    suspend fun updateCustomerId(id: List<Int>, customerId: Int)

    @Query("DELETE FROM data_image")
    suspend fun deleteAll()
}