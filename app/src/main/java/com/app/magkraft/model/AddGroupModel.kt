package com.app.magkraft.model

data class AddGroupModel(
    val `data`: Data,
    val success: Boolean
) {
    data class Data(
        val Id: Int,
        val IsActive: Boolean,
        val IsDeleted: Boolean,
        val Name: String
    )
}

data class AddLocationModel(
    val `data`: Data,
    val success: Boolean
) {
    data class Data(
        val Id: Int,
        val IsActive: Boolean,
        val IsDeleted: Boolean,
        val Name: String,
        val GroupId: Int
    )
}

