package com.app.magkraft.network


import com.app.magkraft.model.AddGroupModel
import com.app.magkraft.model.AddLocationModel
import com.app.magkraft.model.CommonResponse
import com.app.magkraft.model.LoginSignupModel
import com.app.magkraft.ui.model.GroupListModel
import com.app.magkraft.ui.model.LocationListModel
import com.app.magkraft.utils.Constants
import retrofit2.Call
import retrofit2.http.*


interface ApiCallInterface {
    /*
   Login API
   */
    @FormUrlEncoded
    @POST(Constants.login)
    fun loginUser(
        @Field("user") email: String,
        @Field("pwd") password: String,
    ): Call<LoginSignupModel>

    /// Add Group
    @FormUrlEncoded
    @POST(Constants.addGroup)
    fun addGroup(
        @Field("Name") name: String,
        @Field("IsActive") status: String,
    ): Call<AddGroupModel>

    /// Get Group
    @GET(Constants.getGroups)
    fun getGroups(): Call<List<GroupListModel>>

    /// Update Group
    @FormUrlEncoded
    @POST(Constants.updateGroup)
    fun updateGroup(
        @Field("Id") id: String,
        @Field("Name") name: String,
        @Field("IsActive") status: String,
        @Field("IsDeleted") isDelete: String,
    ): Call<CommonResponse>

    /// Delete Group
    @FormUrlEncoded
    @POST(Constants.deleteGroup)
    fun deleteGroup(
        @Field("Id") id: String,

        ): Call<CommonResponse>

    /// Add Location
    @FormUrlEncoded
    @POST(Constants.addlocation)
    fun addLocation(
        @Field("Name") name: String,
        @Field("GroupId") groupId: String,
        @Field("IsActive") status: String,
        @Field("IsDeleted") deleted: String,
    ): Call<AddLocationModel>

    /// Get Group
    @GET(Constants.getlocations)
    fun getLocations(): Call<List<LocationListModel>>

    /// Update Group
    @FormUrlEncoded
    @POST(Constants.updatelocation)
    fun updateLocation(
        @Field("Id") id: String,
        @Field("GroupId") groupId: String,
        @Field("Name") name: String,
        @Field("IsActive") status: String,
        @Field("IsDeleted") isDelete: String,
    ): Call<CommonResponse>

    /// Delete Group
    @FormUrlEncoded
    @POST(Constants.deletelocation)
    fun deleteLocation(
        @Field("Id") id: String,

        ): Call<CommonResponse>

    /// Add Employee
    @FormUrlEncoded
    @POST(Constants.register)
    fun register(
        @Field("Name") name: String,
        @Field("Code") code: String,
        @Field("Designation") designation: String,
        @Field("GroupId") groupId: String,
        @Field("LocationId") locationId: String,
        @Field("IsActive") isActive: String,
        @Field("IsDeleted") isDeleted: String,
        @Field("Photo") image: String,
    ): Call<CommonResponse>
}