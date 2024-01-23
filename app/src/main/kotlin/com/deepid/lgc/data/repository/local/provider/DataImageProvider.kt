package com.deepid.lgc.data.repository.local.provider

import com.deepid.lgc.data.repository.local.dao.DataImageDao
import com.deepid.lgc.domain.model.DataImage
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
