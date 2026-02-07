package com.app.magkraft.utils

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.NetworkType
import androidx.work.WorkManager
import com.app.magkraft.data.local.db.AppDatabase
import com.app.magkraft.ml.FaceRecognizer
import com.app.magkraft.network.ApiClient
import java.util.concurrent.TimeUnit
// In Application class
class MyApp : Application() {

    lateinit var repository: EmployeeRepository

    companion object {
        lateinit var instance: MyApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        AppCompatDelegate.setDefaultNightMode(
            AppCompatDelegate.MODE_NIGHT_NO
        )
        FaceRecognizer.initialize(this)

// 1. Initialize Database & DAO
        val database = AppDatabase.getDatabase(this)
        val dao = database.userDao()


        // 3. Initialize AuthPref
        val authPref = AuthPref(this)

        // 4. Create Repository (Pass parameters in the order defined in your class)
        // Order: api, dao, authPref
        repository = EmployeeRepository(ApiClient.apiService, dao, authPref)

        setupRecurringWork()
        // Remove FaceRecognizer.initialize() from here
    }

    private fun setupRecurringWork() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(1, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        // Use 'this' instead of 'context'
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "EmployeeSyncWork",
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }
}
