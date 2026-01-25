package com.app.magkraft.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface AttendanceDao {

    @Insert
    suspend fun insertAttendance(attendance: AttendanceEntity)

    @Query("""
        SELECT timestamp 
        FROM attendance 
        WHERE empId = :empId 
        ORDER BY timestamp DESC 
        LIMIT 1
    """)
    suspend fun getLastAttendanceTime(empId: String): Long?
}
