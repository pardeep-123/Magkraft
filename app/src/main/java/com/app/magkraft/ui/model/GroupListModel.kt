package com.app.magkraft.ui.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.io.Serializable

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

@Parcelize
data class EmployeeListModel(
    val Code: String,
    val Designation: String,
    val GroupId: Int,
    val GroupName: String,
    val Id: Int,
    val IsActive: Boolean,
    val IsDeleted: Boolean,
    val LocationId: Int,
    val LocationName: String,
    val Name: String,
    val Photo: String
) : Parcelable