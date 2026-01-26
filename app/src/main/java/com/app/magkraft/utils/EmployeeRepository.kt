package com.app.magkraft.utils

import com.app.magkraft.base64ToFloatArray
import com.app.magkraft.data.local.db.UserDao
import com.app.magkraft.data.local.db.UserEntity
import com.app.magkraft.network.ApiCallInterface
import com.app.magkraft.network.ApiClient
import com.app.magkraft.ui.model.EmployeeListModel
import kotlinx.coroutines.flow.Flow


class EmployeeRepository(
    private val api: ApiCallInterface,
    private val dao: UserDao,
    private val authPref: AuthPref
) {

    // Expose the Flow directly to the ViewModel
    fun getEmployeesFlow(): Flow<List<UserEntity>> = dao.getAllUsers()

    suspend fun syncEmployees() {
        // 1️⃣ Get groupId from SharedPreferences
        val groupId = authPref.getLocation("groupId")


        // 2️⃣ Call API
        val response = api.getEmployeesByGroupId(groupId)
        if (response.isSuccessful && response.body() != null) {

            val users = response.body()!!
                .filter { it.IsActive && !it.IsDeleted }
                .map { it.toUserEntity() }

            dao.updateAllUsers(users)
//            dao.clearUsers()
//            dao.insertUser(users)   // ✅ list insert
        }

//        dao.clearUsers()
//        dao.insertUser(users)
    }

    fun EmployeeListModel.toUserEntity(): UserEntity {
        return UserEntity(
            empId = Id.toString(),
            name = Name,
            designation = Designation,
            groupName = GroupName,
            embedding = base64ToFloatArray(Photo),

            )
    }
//    suspend fun getEmployees(): List<UserEntity> {
//        return dao.getAllUsers()
//    }
}
