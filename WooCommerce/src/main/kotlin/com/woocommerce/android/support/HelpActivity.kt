package com.woocommerce.android.support

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.AppUrls
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.ActivityHelpBinding
import com.woocommerce.android.extensions.show
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.ChromeCustomTabUtils
import com.woocommerce.android.util.PackageUtils
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.SiteStore
import javax.inject.Inject

@AndroidEntryPoint
class HelpActivity : AppCompatActivity() {
    @Inject lateinit var accountStore: AccountStore
    @Inject lateinit var siteStore: SiteStore
    @Inject lateinit var supportHelper: SupportHelper
    @Inject lateinit var zendeskHelper: ZendeskHelper
    @Inject lateinit var selectedSite: SelectedSite

    private lateinit var binding: ActivityHelpBinding

    private val originFromExtras by lazy {
        (intent.extras?.get(ORIGIN_KEY) as Origin?) ?: Origin.UNKNOWN
    }

    private val extraTagsFromExtras by lazy {
        intent.extras?.getStringArrayList(EXTRA_TAGS_KEY)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityHelpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar.toolbar as Toolbar)
        supportActionBar?.setHomeButtonEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.contactContainer.setOnClickListener { createNewZendeskTicket(TicketType.General) }
        binding.identityContainer.setOnClickListener { showIdentityDialog(TicketType.General) }
        binding.myTicketsContainer.setOnClickListener { showZendeskTickets() }
        binding.faqContainer.setOnClickListener { showZendeskFaq() }
        binding.appLogContainer.setOnClickListener { showApplicationLog() }
        if (userIsLoggedIn() && selectedSite.exists()) {
            binding.ssrContainer.show()
            binding.ssrContainer.setOnClickListener { showSSR() }
        }

        with(binding.contactPaymentsContainer) {
            setOnClickListener { createNewZendeskTicket(TicketType.Payments) }
        }

        binding.textVersion.text = getString(R.string.version_with_name_param, PackageUtils.getVersionName(this))

        /**
         * If the user taps on a Zendesk notification, we want to show them the `My Tickets` page. However, this
         * should only be triggered when the activity is first created, otherwise if the user comes back from
         * `My Tickets` and rotates the screen (or triggers the activity re-creation in any other way) it'll navigate
         * them to `My Tickets` again since the `originFromExtras` will still be [Origin.ZENDESK_NOTIFICATION].
         */
        if (savedInstanceState == null && originFromExtras == Origin.ZENDESK_NOTIFICATION) {
            showZendeskTickets()
        }
    }

    override fun onResume() {
        super.onResume()
        refreshContactEmailText()
        AnalyticsTracker.trackViewShown(this)
    }

    override fun onStart() {
        super.onStart()
        ChromeCustomTabUtils.connect(this, AppUrls.APP_HELP_CENTER)
    }

    override fun onStop() {
        super.onStop()
        ChromeCustomTabUtils.disconnect(this)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun userIsLoggedIn() = accountStore.hasAccessToken()

    private fun createNewZendeskTicket(ticketType: TicketType) {
        if (!AppPrefs.hasSupportEmail()) {
            showIdentityDialog(ticketType)
            return
        }

        zendeskHelper.createNewTicket(
            context = this,
            origin = originFromExtras,
            selectedSite = selectedSiteOrNull(),
            extraTags = extraTagsFromExtras,
            ticketType = ticketType,
        )
    }

    private fun showIdentityDialog(ticketType: TicketType) {
        val emailSuggestion = if (AppPrefs.hasSupportEmail()) {
            AppPrefs.getSupportEmail()
        } else {
            supportHelper
                .getSupportEmailAndNameSuggestion(accountStore.account, selectedSiteOrNull()).first
        }

        supportHelper.showSupportIdentityInputDialog(this, emailSuggestion, isNameInputHidden = true) { email, _ ->
            zendeskHelper.setSupportEmail(email)
            AnalyticsTracker.track(AnalyticsEvent.SUPPORT_IDENTITY_SET)
            createNewZendeskTicket(ticketType)
        }
        AnalyticsTracker.track(AnalyticsEvent.SUPPORT_IDENTITY_FORM_VIEWED)
    }

    private fun refreshContactEmailText() {
        val supportEmail = AppPrefs.getSupportEmail()
        binding.identityContainer.optionValue = if (supportEmail.isNotEmpty()) {
            supportEmail
        } else {
            getString(R.string.support_contact_email_not_set)
        }
    }

    /**
     * Help activity may have been called during the login flow before the selected site has been set
     */
    private fun selectedSiteOrNull(): SiteModel? {
        return if (selectedSite.exists()) {
            selectedSite.get()
        } else {
            null
        }
    }

    private fun showZendeskTickets() {
        AnalyticsTracker.track(AnalyticsEvent.SUPPORT_TICKETS_VIEWED)
        zendeskHelper.showAllTickets(this, originFromExtras, selectedSiteOrNull(), extraTagsFromExtras)
    }

    private fun showZendeskFaq() {
        AnalyticsTracker.track(AnalyticsEvent.SUPPORT_HELP_CENTER_VIEWED)
        ChromeCustomTabUtils.launchUrl(this, AppUrls.APP_HELP_CENTER)
        /* TODO: for now we simply link to the online woo mobile support documentation, but we should show the
        Zendesk FAQ once it's ready
        zendeskHelper
                .showZendeskHelpCenter(this, originFromExtras, selectedSiteOrNull(), extraTagsFromExtras)
        */
    }

    private fun showApplicationLog() {
        AnalyticsTracker.track(AnalyticsEvent.SUPPORT_APPLICATION_LOG_VIEWED)
        startActivity(Intent(this, WooLogViewerActivity::class.java))
    }

    private fun showSSR() {
        startActivity(Intent(this, SSRActivity::class.java))
    }

    enum class Origin(private val stringValue: String) {
        UNKNOWN("origin:unknown"),
        SETTINGS("origin:settings"),
        CARD_READER_ONBOARDING("origin:card_reader_onboarding"),
        FEEDBACK_SURVEY("origin:feedback_survey"),
        USER_ELIGIBILITY_ERROR("origin:user_eligibility_error"),
        MY_STORE("origin:my_store"),
        ZENDESK_NOTIFICATION("origin:zendesk-notification"),
        LOGIN_EMAIL("origin:login-email"),
        LOGIN_MAGIC_LINK("origin:login-magic-link"),
        LOGIN_EMAIL_PASSWORD("origin:login-wpcom-password"),
        LOGIN_2FA("origin:login-2fa"),
        LOGIN_SITE_ADDRESS("origin:login-site-address"),
        LOGIN_SOCIAL("origin:login-social"),
        LOGIN_USERNAME_PASSWORD("origin:login-username-password"),
        LOGIN_EPILOGUE("origin:login-epilogue"),
        LOGIN_CONNECTED_EMAIL_HELP("origin:login-connected-email-help"),
        SIGNUP_EMAIL("origin:signup-email"),
        SIGNUP_MAGIC_LINK("origin:signup-magic-link"),
        JETPACK_INSTALLATION("origin:jetpack-installation");

        override fun toString(): String {
            return stringValue
        }
    }

    companion object {
        private const val ORIGIN_KEY = "ORIGIN_KEY"
        private const val EXTRA_TAGS_KEY = "EXTRA_TAGS_KEY"

        @JvmStatic
        fun createIntent(
            context: Context,
            origin: Origin,
            extraSupportTags: List<String>?
        ): Intent {
            val intent = Intent(context, HelpActivity::class.java)
            intent.putExtra(ORIGIN_KEY, origin)
            if (extraSupportTags != null && extraSupportTags.isNotEmpty()) {
                intent.putStringArrayListExtra(EXTRA_TAGS_KEY, extraSupportTags as ArrayList<String>?)
            }
            return intent
        }
    }
}
