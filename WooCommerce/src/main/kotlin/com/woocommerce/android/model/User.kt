package com.woocommerce.android.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.model.user.WCUserModel

@Parcelize
data class User(
    val id: Long,
    val firstName: String,
    val lastName: String,
    val username: String,
    val email: String,
    val roles: List<String>
) : Parcelable {
    fun getUserNameForDisplay(): String {
        val name = "$firstName $lastName"
        return when {
            name.isEmpty() && username.isEmpty() -> email
            name.isEmpty() && username.isNotEmpty() -> username
            else -> name
        }
    }
}

fun WCUserModel.toAppModel(): User {
    return User(
        id = this.remoteUserId,
        firstName = this.firstName,
        lastName = this.lastName,
        username = this.username,
        email = this.email,
        roles = this.roles
            .trim() // remove extra spaces between commas
            .removePrefix("[") // remove the String prefix
            .removeSuffix("]") // remove the String suffix
            .split(",")
            .toList()
    )
}
