package com.woocommerce.android.ui.login

import org.wordpress.android.fluxc.store.AccountStore.AuthEmailFlow

enum class MagicLinkFlow(private val value: String) : AuthEmailFlow {
    JetpackConnection("jetpack-connection");

    override fun getName(): String = value

    companion object {
        fun fromString(value: String): MagicLinkFlow? {
            return MagicLinkFlow.values().firstOrNull { it.value == value }
        }
    }
}
