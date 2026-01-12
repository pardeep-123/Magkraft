package com.app.magkraft.model

data class LoginSignupModel(
    val `data`: Data,
    val status: String,
    val error: String,
    val token: String
) {
    data class Data(
        val FullName: String,
        val Id: Int,
        val IsActive: Boolean,
        val IsDeleted: Boolean,
        val Password: String,
        val UserName: String,
        val UserType: Int
    )
}