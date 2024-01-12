package com.deepid.lgc.data.repository.local.dao

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.deepid.lgc.data.common.DateConverter
import com.deepid.lgc.data.repository.local.entity.CustomerInformationEntity
import com.deepid.lgc.data.repository.local.entity.DataImageEntity
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.time.LocalDateTime

// Annotates class to be a Room Database with a table (entity) of the T class
@Database(
    entities = [CustomerInformationEntity::class, DataImageEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(DateConverter::class)
abstract class AppRoomDatabase : RoomDatabase() {

    abstract fun customerInformationDao(): CustomerInformationDao
    abstract fun dataImageDao(): DataImageDao

    @OptIn(DelicateCoroutinesApi::class)
    companion object {
        // Singleton prevents multiple instances of database opening at the
        // same time.
        @Volatile
        private var INSTANCE: AppRoomDatabase? = null

        private const val DB_NAME = "app_database"


        fun getDatabase(context: Context): AppRoomDatabase {
            // if the INSTANCE is not null, then return it,
            // if it is, then create the database
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppRoomDatabase::class.java,
                    DB_NAME
                ).addCallback(
                    object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)

                            GlobalScope.launch(Dispatchers.IO) {
                                initPrePopulateDefaultCustomerInformation(context)
                            }
                        }
                    }
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                // return instance
                instance
            }
        }

        private suspend fun initPrePopulateDefaultCustomerInformation(context: Context) {
            val dateTime = LocalDateTime.now()
            val defaultCustomerInformation = CustomerInformationEntity(
                id = CustomerInformationEntity.DEFAULT_ID,
                name = "Others",
                description = "Others",
                address = "Korea",
                issueDate = dateTime,
                birthDate = dateTime
            )
            val writeDao = getDatabase(context).customerInformationDao()

            writeDao.insert(listOf(defaultCustomerInformation))
        }
    }
}