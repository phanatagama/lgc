package com.deepid.lgc.data.repository.local.provider

import com.deepid.lgc.data.repository.local.dao.CustomerInformationDao
import com.deepid.lgc.data.repository.local.dao.DataImageDao
import com.deepid.lgc.data.repository.local.entity.CustomerInformationWithImages
import com.deepid.lgc.domain.model.CustomerInformation
import com.deepid.lgc.domain.model.DataImage
import com.deepid.lgc.util.mapToModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class CustomerInformationProvider(
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val customerInformationDao: CustomerInformationDao,
    private val dataImageDao: DataImageDao
) {
    suspend fun insertCustomerInformation(customerInformation: CustomerInformation) {
        withContext(dispatcher) {
            customerInformationDao.insert(customerInformation.mapToEntity())
        }
    }

    suspend fun insertDataImage(dataImage: List<DataImage>, customerId: String) {
        withContext(dispatcher) {
            dataImageDao.insert(dataImage.map { it.mapToEntity(customerId) })
        }
    }

    fun getCustomerInformation(): Flow<List<CustomerInformation>> {
        return customerInformationDao.get().filterNotNull().map { it.mapToModel() }
            .flowOn(dispatcher)
    }

    fun getCustomerInformation(name: String): Flow<List<CustomerInformation>> {
        return customerInformationDao.get(name).filterNotNull().map { it.mapToModel() }
            .flowOn(dispatcher)
    }

    fun getCustomerInformationById(id: String): Flow<CustomerInformationWithImages> {
        return customerInformationDao.getById(id).filterNotNull()
            .flowOn(dispatcher)
    }

}