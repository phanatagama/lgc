package com.deepscope.deepscope.data.repository.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.deepscope.deepscope.data.repository.local.entity.DataImageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DataImageDao : BaseDao<DataImageEntity> {
    @Transaction
    @Query("SELECT * FROM data_image")
    fun get(): Flow<List<DataImageEntity>>

//    @Query("SELECT * FROM data_image WHERE customerId = :customerId")
//    fun get(customerId: String): Flow<List<DataImageEntity>>

    @Query("SELECT * FROM data_image WHERE customerId = :customerId")
    fun getByCustomerId(customerId: String): Flow<List<DataImageEntity>>

    @Query("SELECT * FROM data_image WHERE id = :id")
    fun get(id: String): Flow<DataImageEntity>

    @Delete
    suspend fun delete(dataImageEntity: List<DataImageEntity>)

    @Query("DELETE FROM data_image WHERE id = :id")
    suspend fun deleteById(id: String)

    @Transaction
    suspend fun insertAll(dataImageEntity: List<DataImageEntity>) {
        deleteAll()
        insert(dataImageEntity)
    }

    @Update
    suspend fun update(dataImageEntity: List<DataImageEntity>)

    @Query("UPDATE data_image SET customerId = :customerId WHERE id IN (:id)")
    suspend fun updateCustomerId(id: List<String>, customerId: String)

    @Query("DELETE FROM data_image")
    suspend fun deleteAll()
}
