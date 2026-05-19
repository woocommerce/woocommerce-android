package com.woocommerce.android.aiassistant.headless

import android.content.Context
import org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords.ApplicationPasswordsStore
import kotlin.lazyOf

internal object WooAiSmokeApplicationPasswordStore {
    fun installRobolectricPreferences(
        context: Context,
        applicationPasswordsStore: ApplicationPasswordsStore,
    ) {
        val preferences = context.getSharedPreferences(
            "woo-ai-smoke-application-passwords",
            Context.MODE_PRIVATE,
        )
        val field = ApplicationPasswordsStore::class.java.getDeclaredField("encryptedPreferences\$delegate")
        field.isAccessible = true
        field.set(applicationPasswordsStore, lazyOf(preferences))
    }
}
