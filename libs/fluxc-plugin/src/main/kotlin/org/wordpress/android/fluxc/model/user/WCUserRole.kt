package org.wordpress.android.fluxc.model.user

enum class WCUserRole(val value: String = "") {
    OWNER("owner"),
    ADMINISTRATOR("administrator"),
    EDITOR("editor"),
    AUTHOR("author"),
    CUSTOMER("customer"),
    SUBSCRIBER("subscriber"),
    SHOP_MANAGER("shop_manager"),
    OTHER;

    companion object {
        private val valueMap = values().associateBy(WCUserRole::value)

        /**
         * Convert the base value into the associated UserRole object.
         * There are plugins available that can add custom roles.
         * So if we are unable to find a matching [WCUserRole] object, [OTHER] is returned.
         * This is not currently supported by our app.
         */
        fun fromValue(value: String) = valueMap[value] ?: OTHER
    }

    fun isSupported() = this == ADMINISTRATOR || this == SHOP_MANAGER
}
