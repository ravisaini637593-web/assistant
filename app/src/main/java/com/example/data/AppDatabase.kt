package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        UserPreference::class,
        Routine::class,
        Reminder::class,
        TimerRecord::class,
        ConversationSession::class,
        ConversationMessage::class,
        ToolExecution::class,
        PermissionStateSnapshot::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userPreferenceDao(): UserPreferenceDao
    abstract fun routineDao(): RoutineDao
    abstract fun reminderDao(): ReminderDao
    abstract fun timerRecordDao(): TimerRecordDao
    abstract fun conversationSessionDao(): ConversationSessionDao
    abstract fun conversationMessageDao(): ConversationMessageDao
    abstract fun toolExecutionDao(): ToolExecutionDao
    abstract fun permissionStateSnapshotDao(): PermissionStateSnapshotDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "aura_assistant_db"
                ).fallbackToDestructiveMigration(dropAllTables = true).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
