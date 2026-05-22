package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        CdnEdgeNode::class,
        ContentItem::class,
        SyncActivityRecord::class,
        AiOptimizationRule::class
    ],
    version = 1,
    exportSchema = false
)
abstract class CdnDatabase : RoomDatabase() {
    abstract fun cdnDao(): CdnDao

    companion object {
        @Volatile
        private var INSTANCE: CdnDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): CdnDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CdnDatabase::class.java,
                    "cdn_database"
                )
                .build()
                INSTANCE = instance
                instance
            }
        }

    }
}
