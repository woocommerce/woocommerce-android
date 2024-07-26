package com.woocommerce.android.support.zendesk

import android.content.Context
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.BuildConfig
import com.woocommerce.android.extensions.isNotNullOrEmpty
import com.woocommerce.android.support.SupportHelper
import com.woocommerce.android.tools.SelectedSite
import com.zendesk.logger.Logger
import org.wordpress.android.fluxc.store.AccountStore
import zendesk.core.AnonymousIdentity
import zendesk.core.Identity
import zendesk.core.Zendesk
import zendesk.support.Support
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ZendeskSettings @Inject constructor(
    private val supportHelper: SupportHelper,
    private val accountStore: AccountStore,
    private val selectedSite: SelectedSite,
) {
    private var setupDone = false

    val instance: Zendesk?
        get() = Zendesk.INSTANCE.takeIfInitialized()

    val requestProvider
        get() = Support.INSTANCE
            .takeIfInitialized()
            ?.provider()
            ?.requestProvider()

    /**
     * These two properties are used to keep track of the Zendesk identity set. Since we allow users' to change their
     * supportEmail and reset their identity on logout, we need to ensure that the correct identity is set all times.
     * Check [requireIdentity], [refreshIdentity] & [clearIdentity] for more details about how Zendesk identity works.
     */
    var supportEmail: String? = null
        get() = AppPrefs.getSupportEmail()
            .takeIf { it.isNotEmpty() }
            ?: supportHelper.getSupportEmailSuggestion(accountStore.account, selectedSite.getIfExists())

        set(value) {
            if (value != field) {
                AppPrefs.setSupportEmail(value)
                refreshIdentity()
            }
        }

    var supportName: String? = null
        get() = AppPrefs.getSupportName()
            .takeIf { it.isNotEmpty() }
            ?: supportHelper.getSupportNameSuggestion(accountStore.account, selectedSite.getIfExists())

        set(value) {
            if (value != field) {
                AppPrefs.setSupportName(value)
                refreshIdentity()
            }
        }

    /**
     * Although rare, Zendesk SDK might reset the identity due to a 401 error. This seems to happen if the identity
     * is changed and another Zendesk action happens before the identity change could be completed. In order to avoid
     * such issues, we check both Zendesk identity and the [supportEmail] to decide whether identity is set.
     */
    val isIdentitySet: Boolean
        get() = supportEmail.isNotNullOrEmpty() && instance?.identity != null

    /**
     * This function sets up the Zendesk singleton instance with the passed in credentials. This step is required
     * for the rest of Zendesk functions to work and it should only be called once, probably during the Application
     * setup. It'll also enable Zendesk logs for DEBUG builds.
     */
    @JvmOverloads
    @Synchronized
    fun setup(
        context: Context,
        zendeskUrl: String = BuildConfig.ZENDESK_DOMAIN,
        applicationId: String = BuildConfig.ZENDESK_APP_ID,
        oauthClientId: String = BuildConfig.ZENDESK_OAUTH_CLIENT_ID,
        enableLogs: Boolean = BuildConfig.DEBUG
    ) {
        if (setupDone) {
            return
        }
        val zendeskInstance = Zendesk.INSTANCE
        if (zendeskUrl.isEmpty() || applicationId.isEmpty() || oauthClientId.isEmpty()) {
            return
        }
        zendeskInstance.init(context, zendeskUrl, applicationId, oauthClientId)
        Logger.setLoggable(enableLogs)
        Support.INSTANCE.init(zendeskInstance)
        refreshIdentity()
        setupDone = true
    }

    /**
     * This is a helper function to clear the Zendesk identity. It'll remove the credentials from AppPrefs and update
     * the Zendesk identity with a new anonymous one without an email or name. Due to the way Zendesk anonymous identity
     * works, this will clear all the users' tickets.
     *
     * We should also clear the Zendesk identity of the user on logout and it will need to be set again
     * when the user wants to create a new ticket.
     */
    fun clearIdentity() {
        AppPrefs.removeSupportEmail()
        AppPrefs.removeSupportName()
        refreshIdentity()
    }

    /**
     * This is a helper function that'll ensure the Zendesk identity is set with the credentials from AppPrefs.
     *
     * We should refresh the Zendesk identity when the email or the name has been updated. We also check whether
     * Zendesk SDK has cleared the identity. Check out the documentation for [isIdentitySet] for more details.
     */
    private fun refreshIdentity() {
        instance?.setIdentity(createZendeskIdentity(supportEmail, supportName))
    }

    /**
     * This is a helper function which creates an anonymous Zendesk identity with the email and name passed in. They can
     * both be `null` as they are not required for a valid identity.
     *
     * An important thing to note is that whenever a different set of values are passed in, a different identity will be
     * created which will reset the ticket list for the user. So, for example, even if the passed in email is the same,
     * if the name is different, it'll reset Zendesk's local DB.
     *
     * This is currently the way we handle identity for Zendesk, but it's possible that we may switch to a JWT based
     * authentication which will avoid the resetting issue, but will mean that we'll need to involve our own servers in the
     * authentication. More information can be found in their documentation:
     * https://developer.zendesk.com/embeddables/docs/android-support-sdk/sdk_set_identity#setting-a-unique-identity
     */
    private fun createZendeskIdentity(email: String?, name: String?): Identity {
        val identity = AnonymousIdentity.Builder()
        if (!email.isNullOrEmpty()) {
            identity.withEmailIdentifier(email)
        }
        if (!name.isNullOrEmpty()) {
            identity.withNameIdentifier(name)
        }
        return identity.build()
    }

    private fun <T> T.takeIfInitialized(): T? =
        this.takeIf { setupDone }
}
