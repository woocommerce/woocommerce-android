package com.woocommerce.android.ui.plans.domain

import java.time.ZonedDateTime

data class SitePlan(
    val name: String,
    val expirationDate: ZonedDateTime,
    val type: Type,
) {
    val formattedPlanName: String
        get() {
            return if (type == Type.FREE_TRIAL) {
                "Free Trial"
            } else {
                name.removePrefix("WordPress.com")
                    .removePrefix("Woo Express:")
                    .trimIndent()
            }
        }

    enum class Type {
        FREE_TRIAL, OTHER
    }

    companion object {
        val EMPTY = SitePlan("", ZonedDateTime.now(), Type.OTHER)
    }
}
