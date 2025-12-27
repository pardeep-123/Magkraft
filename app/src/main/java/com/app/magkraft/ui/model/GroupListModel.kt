package com.app.magkraft.ui.model

data class GroupListModel(
   val id: Int = 0,
    val name: String,
    val isActive: Boolean
)

data class LocationListModel(
    val groupId: Int = 0,
    val locationName: String,
    val AddressName: String
)

data class EmployeeListModel(
    val employeeId: Int = 0,
    val employeeName: String,
    val groupName: String,
    val isActive: Boolean
)