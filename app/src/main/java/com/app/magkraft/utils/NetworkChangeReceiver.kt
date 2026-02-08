package com.app.magkraft.utils

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.app.magkraft.data.local.db.AppDatabase
import com.app.magkraft.network.ApiClient
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AttendanceSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val db = AppDatabase.getDatabase(applicationContext)
        val dao = db.attendanceDao()

        val pendingList = dao.getPendingAttendances()

        if (pendingList.isEmpty()) return Result.success()

        for (attendance in pendingList) {

            val formattedTime = SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss",
                Locale.getDefault()
            ).format(Date(attendance.timestamp))

            val success = try {
               val response = ApiClient.apiService.markAttendance(
                    attendance.empId,
                   attendance.locationId,
                    formattedTime

                ).execute()
                response.isSuccessful && response.body()?.success == true

            } catch (e: Exception) {
                false
            }

            if (success) {
                dao.markAsSynced(attendance.id)
            } else {
                return Result.retry() // WorkManager will retry automatically
            }
        }

        return Result.success()
    }
}
