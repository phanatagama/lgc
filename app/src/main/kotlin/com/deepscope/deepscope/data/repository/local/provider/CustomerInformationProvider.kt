package com.deepscope.deepscope.data.repository.local.provider

import com.deepscope.deepscope.data.repository.local.dao.CustomerInformationDao
import com.deepscope.deepscope.domain.model.CustomerInformation
import com.deepscope.deepscope.util.mapToModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class CustomerInformationProvider(
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val customerInformationDao: CustomerInformationDao
) {
    suspend fun insertCustomerInformation(customerInformation: CustomerInformation) {
        withContext(dispatcher) {
            customerInformationDao.insert(customerInformation.mapToEntity())
        }
    }

    fun getCustomerInformation(): Flow<List<CustomerInformation>> {
        return customerInformationDao.get()
            .filterNotNull()
            .map { it.mapToModel() }
            .flowOn(dispatcher)
    }

    fun getCustomerInformation(name: String): Flow<List<CustomerInformation>> {
        return customerInformationDao.get(name)
            .filterNotNull()
            .map { it.mapToModel() }
            .flowOn(dispatcher)
    }

    fun getCustomerInformationById(id: String): Flow<CustomerInformation> {
        return customerInformationDao.getById(id)
            .filterNotNull()
            .map { it.mapToModel() }
            .flowOn(dispatcher)
    }

    suspend fun deleteCustomerInformation(customerInformation: CustomerInformation) {
        withContext(dispatcher) {
            customerInformationDao.delete(customerInformation.mapToEntity())
        }
    }
}