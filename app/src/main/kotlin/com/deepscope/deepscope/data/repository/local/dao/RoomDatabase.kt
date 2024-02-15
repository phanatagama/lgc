package com.deepscope.deepscope.data.repository.local.dao

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.deepscope.deepscope.data.common.DateConverter
import com.deepscope.deepscope.data.repository.local.entity.CustomerInformationEntity
import com.deepscope.deepscope.data.repository.local.entity.DataImageEntity
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.time.LocalDateTime


// Annotates class to be a Room Database with a table (entity) of the T class
@Database(
    entities = [CustomerInformationEntity::class, DataImageEntity::class],
    version = 2,
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
        /// Migration from version 1 to version 2
        private val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE 'data_image' ADD COLUMN 'type' INTEGER NOT NULL DEFAULT 1"
                )
            }
        }

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
//                            GlobalScope.launch(Dispatchers.IO) {
//                                initPrePopulateDefaultCustomerInformation(context)
//                            }
                        }
                    }
                )
                    .addMigrations(MIGRATION_1_2)
//                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        // Pre-populate default customer information
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

            writeDao.insert(defaultCustomerInformation)
        }
    }
}
