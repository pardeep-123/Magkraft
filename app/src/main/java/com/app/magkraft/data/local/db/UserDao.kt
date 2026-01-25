package com.app.magkraft.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface UserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: List<UserEntity>)

    @Query("DELETE FROM users")
    suspend fun clearUsers()

    @Query("SELECT * FROM users")
    suspend fun getAllUsers(): List<UserEntity>
}
