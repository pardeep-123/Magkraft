package com.app.magkraft.ui.model

data class GroupListModel(
    val Id: Int,
    val IsActive: Boolean,
    val IsDeleted: Boolean,
    val Name: String?
)

data class LocationListModel(
    val Id: Int,
    val GroupId: Int,
    val IsActive: Boolean,
    val IsDeleted: Boolean,
    val Name: String,
    val GroupName: String
)

data class EmployeeListModel(
    val employeeId: Int = 0,
    val employeeName: String,
    val groupName: String,
    val isActive: Boolean
)