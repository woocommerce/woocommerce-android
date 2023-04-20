package com.woocommerce.android.ui.plans.domain

import java.time.ZonedDateTime

data class SitePlan(
    val name: String,
    val expirationDate: ZonedDateTime,
    val type: Type,
) {
    enum class Type {
        FREE_TRIAL, OTHER
    }

    companion object {
        val EMPTY = SitePlan("", ZonedDateTime.now(), Type.OTHER)
    }
}
