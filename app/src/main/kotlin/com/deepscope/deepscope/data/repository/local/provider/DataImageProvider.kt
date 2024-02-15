package com.deepscope.deepscope.data.repository.local.provider

import com.deepscope.deepscope.data.repository.local.dao.DataImageDao
import com.deepscope.deepscope.domain.model.DataImage
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DataImageProvider(private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
                        private val dataImageDao: DataImageDao
) {
    suspend fun insertDataImage(dataImage: List<DataImage>, customerId: String) {
        withContext(dispatcher) {
            dataImageDao.insert(dataImage.map { it.mapToEntity(customerId) })
        }
    }
}
