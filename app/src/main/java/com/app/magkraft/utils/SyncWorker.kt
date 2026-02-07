package com.app.magkraft.utils

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class SyncWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        // You'll need to inject or initialize your repository here
        // Get the repository from your MyApp instance
        val repository = (applicationContext as MyApp).repository

        return try {
            val result = repository.syncEmployees()
            if (result.isSuccess) {
                Result.success()
            } else {
                Result.retry() // Retries later if API fails
            }
        } catch (e: Exception) {
            Result.retry()
        }
    }
}