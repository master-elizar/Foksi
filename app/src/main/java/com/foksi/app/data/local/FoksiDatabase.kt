package com.foksi.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.foksi.app.data.local.dao.AttachmentDao
import com.foksi.app.data.local.dao.CategoryDao
import com.foksi.app.data.local.dao.ChecklistDao
import com.foksi.app.data.local.dao.EventDao
import com.foksi.app.data.local.dao.ReminderDao
import com.foksi.app.data.local.dao.ScheduleDao
import com.foksi.app.data.local.entity.AttachmentEntity
import com.foksi.app.data.local.entity.CategoryEntity
import com.foksi.app.data.local.entity.ChecklistItemEntity
import com.foksi.app.data.local.entity.EventEntity
import com.foksi.app.data.local.entity.ReminderEntity
import com.foksi.app.data.local.entity.ScheduledNotificationEntity

@Database(
    entities = [
        EventEntity::class,
        ReminderEntity::class,
        ChecklistItemEntity::class,
        AttachmentEntity::class,
        CategoryEntity::class,
        ScheduledNotificationEntity::class,
    ],
    version = FoksiDatabase.VERSION,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class FoksiDatabase : RoomDatabase() {

    abstract fun eventDao(): EventDao
    abstract fun reminderDao(): ReminderDao
    abstract fun checklistDao(): ChecklistDao
    abstract fun attachmentDao(): AttachmentDao
    abstract fun categoryDao(): CategoryDao
    abstract fun scheduleDao(): ScheduleDao

    companion object {
        const val VERSION = 2
        private const val NAME = "foksi.db"

        /**
         * Real migrations live here. They are applied in order, so a user upgrading from any
         * older build keeps their data instead of losing it to a destructive fallback.
         */
        /** v1 → v2: birthdays gained a "do we know the birth year" flag. */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE events ADD COLUMN birthYearKnown INTEGER NOT NULL DEFAULT 1"
                )
            }
        }

        val MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_1_2)

        @Volatile
        private var instance: FoksiDatabase? = null

        fun get(context: Context): FoksiDatabase = instance ?: synchronized(this) {
            instance ?: build(context.applicationContext).also { instance = it }
        }

        private fun build(context: Context): FoksiDatabase =
            Room.databaseBuilder(context, FoksiDatabase::class.java, NAME)
                .addMigrations(*MIGRATIONS)
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        DefaultData.insertDefaultCategories(db)
                    }
                })
                .fallbackToDestructiveMigrationOnDowngrade()
                .build()
    }
}
