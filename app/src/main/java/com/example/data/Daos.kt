package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserPreferenceDao {
    @Query("SELECT value FROM user_preferences WHERE `key` = :key LIMIT 1")
    suspend fun get(key: String): String?

    @Query("SELECT * FROM user_preferences")
    fun getAllFlow(): Flow<List<UserPreference>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(pref: UserPreference)

    @Query("DELETE FROM user_preferences WHERE `key` = :key")
    suspend fun delete(key: String)
}

@Dao
interface RoutineDao {
    @Query("SELECT * FROM routines ORDER BY createdAt DESC")
    fun getAll(): Flow<List<Routine>>

    @Query("SELECT * FROM routines WHERE isEnabled = 1")
    suspend fun getEnabledRoutines(): List<Routine>

    @Query("SELECT * FROM routines WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): Routine?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(routine: Routine): Long

    @Update
    suspend fun update(routine: Routine)

    @Delete
    suspend fun delete(routine: Routine)
}

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders ORDER BY dueTimestamp ASC")
    fun getAll(): Flow<List<Reminder>>

    @Query("SELECT * FROM reminders WHERE isCompleted = 0 ORDER BY dueTimestamp ASC")
    fun getPending(): Flow<List<Reminder>>

    @Query("SELECT * FROM reminders WHERE isCompleted = 0 AND dueTimestamp <= :now")
    suspend fun getDueReminders(now: Long): List<Reminder>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reminder: Reminder): Long

    @Update
    suspend fun update(reminder: Reminder)

    @Delete
    suspend fun delete(reminder: Reminder)

    @Query("DELETE FROM reminders WHERE isCompleted = 1")
    suspend fun clearCompleted()
}

@Dao
interface TimerRecordDao {
    @Query("SELECT * FROM timers WHERE isRunning = 1 ORDER BY endTimeMillis ASC")
    fun getActiveTimers(): Flow<List<TimerRecord>>

    @Query("SELECT * FROM timers ORDER BY createdAt DESC")
    fun getAllTimers(): Flow<List<TimerRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(timer: TimerRecord): Long

    @Query("UPDATE timers SET isRunning = 0 WHERE id = :id")
    suspend fun stopTimer(id: Long)

    @Delete
    suspend fun delete(timer: TimerRecord)

    @Query("DELETE FROM timers")
    suspend fun clearAll()
}

@Dao
interface ConversationSessionDao {
    @Query("SELECT * FROM conversation_sessions ORDER BY startTime DESC")
    fun getAll(): Flow<List<ConversationSession>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: ConversationSession): Long

    @Delete
    suspend fun delete(session: ConversationSession)

    @Query("DELETE FROM conversation_sessions")
    suspend fun clearAll()
}

@Dao
interface ConversationMessageDao {
    @Query("SELECT * FROM conversation_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getMessagesForSession(sessionId: Long): Flow<List<ConversationMessage>>

    @Query("SELECT * FROM conversation_messages ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentMessages(limit: Int = 50): Flow<List<ConversationMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: ConversationMessage): Long

    @Query("DELETE FROM conversation_messages WHERE sessionId = :sessionId")
    suspend fun deleteBySession(sessionId: Long)

    @Query("DELETE FROM conversation_messages")
    suspend fun clearAll()
}

@Dao
interface ToolExecutionDao {
    @Query("SELECT * FROM tool_executions ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentExecutions(limit: Int = 100): Flow<List<ToolExecution>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(execution: ToolExecution): Long

    @Query("DELETE FROM tool_executions")
    suspend fun clearAll()
}

@Dao
interface PermissionStateSnapshotDao {
    @Query("SELECT * FROM permission_snapshots")
    fun getAll(): Flow<List<PermissionStateSnapshot>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(snapshot: PermissionStateSnapshot)
}
