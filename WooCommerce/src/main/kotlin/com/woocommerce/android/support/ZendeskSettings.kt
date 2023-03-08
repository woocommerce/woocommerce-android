package com.woocommerce.android.support

import android.content.Context
import com.woocommerce.android.BuildConfig
import com.woocommerce.android.util.PackageUtils
import com.zendesk.logger.Logger
import zendesk.core.PushRegistrationProvider
import zendesk.core.Zendesk
import zendesk.support.Support

class ZendeskSettings {
    val instance: Zendesk?
        get() = Zendesk.INSTANCE.takeIfInitialized()

    val pushRegistrationProvider: PushRegistrationProvider?
        get() = instance
            ?.provider()
            ?.pushRegistrationProvider()

    val requestProvider
        get() = Support.INSTANCE
            .takeIfInitialized()
            ?.provider()
            ?.requestProvider()

    private var setupDone = false

    /**
     * This function sets up the Zendesk singleton instance with the passed in credentials. This step is required
     * for the rest of Zendesk functions to work and it should only be called once, probably during the Application
     * setup. It'll also enable Zendesk logs for DEBUG builds.
     */
    @JvmOverloads
    fun setup(
        context: Context,
        zendeskUrl: String,
        applicationId: String,
        oauthClientId: String,
        enableLogs: Boolean = BuildConfig.DEBUG
    ) {
        val zendeskInstance = Zendesk.INSTANCE
        if (zendeskInstance.isInitialized) {
            if (PackageUtils.isTesting()) return
            else error("Zendesk shouldn't be initialized more than once!")
        }
        if (zendeskUrl.isEmpty() || applicationId.isEmpty() || oauthClientId.isEmpty()) {
            return
        }
        zendeskInstance.init(context, zendeskUrl, applicationId, oauthClientId)
        Logger.setLoggable(enableLogs)
        Support.INSTANCE.init(zendeskInstance)
        setupDone = true
        // refreshIdentity()
    }

    private fun <T> T.takeIfInitialized(): T? =
        this.takeIf { setupDone }
}
