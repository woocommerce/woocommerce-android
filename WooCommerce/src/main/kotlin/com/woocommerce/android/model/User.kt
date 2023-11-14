package com.woocommerce.android.model

import android.os.Parcel
import android.os.Parcelable
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parceler
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.model.user.WCUserModel

@Parcelize
data class User(
    val id: Long,
    val firstName: String,
    val lastName: String,
    val username: String,
    val email: String,
    val roles: List<UserRole>
) : Parcelable {
    @IgnoredOnParcel
    val isEligible: Boolean = roles.any { it.isEligible }

    fun getUserNameForDisplay(): String {
        val name = "$firstName $lastName".trim()
        return when {
            name.isNotEmpty() -> name
            username.isNotEmpty() -> username
            else -> email
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
            .map { UserRole.fromString(it.trim('"')) }
    )
}

sealed class UserRole(val value: String) : Parcelable {
    companion object {
        fun fromString(role: String): UserRole {
            return when (role) {
                "owner" -> Owner
                "administrator" -> Administrator
                "shop_manager" -> ShopManager
                "editor" -> Editor
                "author" -> Author
                "customer" -> Customer
                "subscriber" -> Subscriber
                else -> Other(role)
            }
        }
    }

    abstract val isEligible: Boolean

    override fun equals(other: Any?): Boolean {
        if (other == null || other::class != this::class) return false
        return (other as UserRole).value == value
    }

    override fun hashCode(): Int = value.hashCode()

    @Parcelize
    object Owner : UserRole("owner") {
        override val isEligible: Boolean
            get() = true
    }

    @Parcelize
    object Administrator : UserRole("administrator") {
        override val isEligible: Boolean
            get() = true
    }

    @Parcelize
    object ShopManager : UserRole("shop_manager") {
        override val isEligible: Boolean
            get() = true
    }

    @Parcelize
    object Editor : UserRole("editor") {
        override val isEligible: Boolean
            get() = false
    }

    @Parcelize
    object Author : UserRole("author") {
        override val isEligible: Boolean
            get() = false
    }

    @Parcelize
    object Customer : UserRole("customer") {
        override val isEligible: Boolean
            get() = false
    }

    @Parcelize
    object Subscriber : UserRole("subscriber") {
        override val isEligible: Boolean
            get() = false
    }

    @Parcelize
    class Other(role: String) : UserRole(role) {
        private companion object : Parceler<Other> {
            override fun create(parcel: Parcel): Other {
                return Other(parcel.readString().orEmpty())
            }

            override fun Other.write(parcel: Parcel, flags: Int) {
                parcel.writeString(value)
            }
        }

        override val isEligible: Boolean
            get() = false
    }
}
