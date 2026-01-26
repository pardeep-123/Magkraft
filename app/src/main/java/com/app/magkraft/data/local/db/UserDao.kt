package com.app.magkraft.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

//@Dao
//interface UserDao {
//
//    @Insert(onConflict = OnConflictStrategy.REPLACE)
//    suspend fun insertUser(user: List<UserEntity>)
//
//    @Query("DELETE FROM users")
//    suspend fun clearUsers()
//
//    @Query("SELECT * FROM users")
//    suspend fun getAllUsers(): List<UserEntity>
//}

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(users: List<UserEntity>)

    @Query("DELETE FROM users")
    suspend fun clearUsers()

    // 🔄 Use Flow for real-time updates
    @Query("SELECT * FROM users")
    fun getAllUsers(): Flow<List<UserEntity>>

    // ✅ Atomic Sync: Deletes and Inserts in one database transaction
    @Transaction
    suspend fun updateAllUsers(users: List<UserEntity>) {
        clearUsers()
        insertUser(users)
    }
}