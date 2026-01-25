package com.app.magkraft.utils

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.app.magkraft.data.local.db.AppDatabase
import com.app.magkraft.data.local.db.UserEntity
import com.app.magkraft.network.ApiClient
import kotlinx.coroutines.Dispatchers
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class EmployeeViewModel(application: Application)
    : AndroidViewModel(application) {

    private val repository: EmployeeRepository

    init {
        val context = application.applicationContext

        repository = EmployeeRepository(
            api = ApiClient.apiService,
            dao = AppDatabase.getDatabase(context).userDao(),
            authPref = AuthPref(context)
        )
    }

    fun syncEmployees() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.syncEmployees()
        }
    }

    suspend fun getEmployees(): List<UserEntity> {
        return repository.getEmployees()
    }
}

