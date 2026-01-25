package com.app.magkraft.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val empId: String,
    val name: String,
    val designation: String,
    val groupName: String,
    val embedding: FloatArray,
//    val image: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UserEntity

        if (empId != other.empId) return false
        if (name != other.name) return false
        if (designation != other.designation) return false
        if (groupName != other.groupName) return false
        if (!embedding.contentEquals(other.embedding)) return false
//        if (!image.contentEquals(other.image)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = empId.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + designation.hashCode()
        result = 31 * result + groupName.hashCode()
        result = 31 * result + embedding.contentHashCode()
//        result = 31 * result + image.contentHashCode()
        return result
    }
}
