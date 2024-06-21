@file:Suppress("SameParameterValue")

package com.woocommerce.android

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.content.SharedPreferences.Editor
import androidx.preference.PreferenceManager
import com.woocommerce.android.AppPrefs.CardReaderOnboardingStatus.CARD_READER_ONBOARDING_NOT_COMPLETED
import com.woocommerce.android.AppPrefs.CardReaderOnboardingStatus.valueOf
import com.woocommerce.android.AppPrefs.DeletablePrefKey.CARD_READER_DO_NOT_SHOW_CASH_ON_DELIVERY_DISABLED_ONBOARDING_STATE
import com.woocommerce.android.AppPrefs.DeletablePrefKey.CARD_READER_IS_PLUGIN_EXPLICITLY_SELECTED
import com.woocommerce.android.AppPrefs.DeletablePrefKey.CARD_READER_ONBOARDING_COMPLETED_STATUS_V2
import com.woocommerce.android.AppPrefs.DeletablePrefKey.CARD_READER_PREFERRED_PLUGIN
import com.woocommerce.android.AppPrefs.DeletablePrefKey.CARD_READER_PREFERRED_PLUGIN_VERSION
import com.woocommerce.android.AppPrefs.DeletablePrefKey.CARD_READER_STATEMENT_DESCRIPTOR
import com.woocommerce.android.AppPrefs.DeletablePrefKey.CARD_READER_UPSELL_BANNER_DIALOG_DISMISSED_FOREVER
import com.woocommerce.android.AppPrefs.DeletablePrefKey.CARD_READER_UPSELL_BANNER_DIALOG_DISMISSED_REMIND_ME_LATER
import com.woocommerce.android.AppPrefs.DeletablePrefKey.DATABASE_DOWNGRADED
import com.woocommerce.android.AppPrefs.DeletablePrefKey.IMAGE_OPTIMIZE_ENABLED
import com.woocommerce.android.AppPrefs.DeletablePrefKey.ORDER_FILTER_CUSTOM_DATE_RANGE_END
import com.woocommerce.android.AppPrefs.DeletablePrefKey.ORDER_FILTER_CUSTOM_DATE_RANGE_START
import com.woocommerce.android.AppPrefs.DeletablePrefKey.ORDER_FILTER_PREFIX
import com.woocommerce.android.AppPrefs.DeletablePrefKey.PRODUCT_SORTING_PREFIX
import com.woocommerce.android.AppPrefs.DeletablePrefKey.RECEIPT_PREFIX
import com.woocommerce.android.AppPrefs.DeletablePrefKey.UPDATE_SIMULATED_READER_OPTION
import com.woocommerce.android.AppPrefs.DeletablePrefKey.WC_STORE_ID
import com.woocommerce.android.AppPrefs.DeletableSitePrefKey.AUTO_TAX_RATE_ID
import com.woocommerce.android.AppPrefs.UndeletablePrefKey.APPLICATION_STORE_SNAPSHOT_TRACKED_FOR_SITE
import com.woocommerce.android.AppPrefs.UndeletablePrefKey.ONBOARDING_CAROUSEL_DISPLAYED
import com.woocommerce.android.AppPrefs.UndeletablePrefKey.STORE_ONBOARDING_SHOWN_AT_LEAST_ONCE
import com.woocommerce.android.AppPrefs.UndeletablePrefKey.STORE_ONBOARDING_TASKS_COMPLETED
import com.woocommerce.android.AppPrefs.UndeletablePrefKey.STORE_PHONE_NUMBER
import com.woocommerce.android.extensions.orNullIfEmpty
import com.woocommerce.android.extensions.packageInfo
import com.woocommerce.android.notifications.NotificationChannelType
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.payments.cardreader.onboarding.PersistentOnboardingData
import com.woocommerce.android.ui.payments.cardreader.onboarding.PluginType
import com.woocommerce.android.ui.prefs.DeveloperOptionsViewModel.DeveloperOptionsViewState.UpdateOptions
import com.woocommerce.android.ui.prefs.domain.DomainFlowSource
import com.woocommerce.android.ui.products.ProductType
import com.woocommerce.android.ui.products.ai.AboutProductSubViewModel.AiTone
import com.woocommerce.android.ui.promobanner.PromoBannerType
import com.woocommerce.android.util.PreferenceUtils
import com.woocommerce.android.util.ThemeOption
import com.woocommerce.android.util.ThemeOption.DEFAULT
import java.util.Calendar
import java.util.Date

// Guaranteed to hold a reference to the application context, which is safe
@SuppressLint("StaticFieldLeak")
@SuppressWarnings("LargeClass")
object AppPrefs {
    interface PrefKey

    @JvmInline
    value class PrefKeyString(val key: String) : PrefKey

    private lateinit var context: Context

    /**
     * Application related preferences. When the user logs out, these preferences are erased.
     */
    private enum class DeletablePrefKey : PrefKey {
        SUPPORT_EMAIL,
        SUPPORT_NAME,
        IS_USING_V4_API,
        HAS_UNSEEN_REVIEWS,
        SELECTED_SHIPMENT_TRACKING_PROVIDER_NAME,
        SELECTED_SHIPMENT_TRACKING_PROVIDER_IS_CUSTOM,
        LOGIN_SITE_ADDRESS,
        DATABASE_DOWNGRADED,
        IS_PRODUCTS_FEATURE_ENABLED,
        IS_PRODUCT_ADDONS_ENABLED,
        LOGIN_USER_BYPASSED_JETPACK_REQUIRED,
        SELECTED_ORDER_LIST_TAB_POSITION,
        IMAGE_OPTIMIZE_ENABLED,
        SELECTED_APP_THEME,
        SELECTED_PRODUCT_TYPE,
        SELECTED_PRODUCT_IS_VIRTUAL,
        UNIFIED_LOGIN_LAST_ACTIVE_SOURCE,
        UNIFIED_LOGIN_LAST_ACTIVE_FLOW,
        IS_USER_ELIGIBLE,
        USER_EMAIL,
        RECEIPT_PREFIX,
        CARD_READER_ONBOARDING_COMPLETED_STATUS_V2,
        CARD_READER_IS_PLUGIN_EXPLICITLY_SELECTED,
        CARD_READER_PREFERRED_PLUGIN,
        CARD_READER_PREFERRED_PLUGIN_VERSION,
        CARD_READER_STATEMENT_DESCRIPTOR,
        ORDER_FILTER_PREFIX,
        ORDER_FILTER_CUSTOM_DATE_RANGE_START,
        ORDER_FILTER_CUSTOM_DATE_RANGE_END,
        PRODUCT_SORTING_PREFIX,
        PRE_LOGIN_NOTIFICATION_WORK_REQUEST,
        PRE_LOGIN_NOTIFICATION_DISPLAYED,
        PRE_LOGIN_NOTIFICATION_DISPLAYED_TYPE,
        LOGIN_EMAIL,
        CARD_READER_UPSELL_BANNER_DIALOG_DISMISSED_FOREVER,
        CARD_READER_UPSELL_BANNER_DIALOG_DISMISSED_REMIND_ME_LATER,
        CARD_READER_DO_NOT_SHOW_CASH_ON_DELIVERY_DISABLED_ONBOARDING_STATE,
        ACTIVE_STATS_GRANULARITY,
        ACTIVE_TOP_PERFORMERS_GRANULARITY,
        DASHBOARD_COUPONS_CARD_TAB,
        USE_SIMULATED_READER,
        UPDATE_SIMULATED_READER_OPTION,
        ENABLE_SIMULATED_INTERAC,
        CUSTOM_DOMAINS_SOURCE,
        JETPACK_INSTALLATION_FROM_BANNER,
        NOTIFICATIONS_PERMISSION_BAR,
        IS_EU_SHIPPING_NOTICE_DISMISSED,
        HAS_SAVED_PRIVACY_SETTINGS,
        WAS_AI_DESCRIPTION_PROMO_DIALOG_SHOWN,
        IS_AI_DESCRIPTION_TOOLTIP_DISMISSED,
        NUMBER_OF_TIMES_AI_DESCRIPTION_TOOLTIP_SHOWN,
        STORE_CREATION_PROFILER_ANSWERS,
        AI_CONTENT_GENERATION_TONE,
        AI_PRODUCT_CREATION_IS_FIRST_ATTEMPT,
        BLAZE_CELEBRATION_SCREEN_SHOWN,
        WC_STORE_ID,
        CHA_CHING_SOUND_ISSUE_DIALOG_DISMISSED,
        TIMES_AI_PRODUCT_CREATION_SURVEY_DISPLAYED,
        AI_PRODUCT_CREATION_SURVEY_DISMISSED,
    }

    /**
     * Application related preferences. When the user changes a site, these preferences are erased.
     */
    private enum class DeletableSitePrefKey : PrefKey {
        TRACKING_EXTENSION_AVAILABLE,
        JETPACK_BENEFITS_BANNER_DISMISSAL_DATE,
        AI_PRODUCT_DESCRIPTION_CELEBRATION_SHOWN,
        AUTO_TAX_RATE_ID,
    }

    /**
     * These preferences won't be deleted when the user disconnects.
     * They should be used for device specific or user-independent preferences.
     */
    private enum class UndeletablePrefKey : PrefKey {
        // The last stored versionCode of the app
        LAST_APP_VERSION_CODE,

        // Whether or not automatic crash reporting is enabled
        ENABLE_CRASH_REPORTING,

        // The app update for this version was cancelled by the user
        CANCELLED_APP_VERSION_CODE,

        // Date of the app installation
        APP_INSTALATION_DATE,

        // last connected card reader's id
        LAST_CONNECTED_CARD_READER_ID,

        // show card reader tutorial after a reader is connected
        SHOW_CARD_READER_CONNECTED_TUTORIAL,

        // The last version of the app where an announcement was shown,
        LAST_VERSION_WITH_ANNOUNCEMENT,

        // card reader welcome dialog was shown
        CARD_READER_WELCOME_SHOWN,

        WC_PREF_NOTIFICATIONS_TOKEN,

        // Hide banner in order detail to install WC Shipping plugin
        WC_SHIPPING_BANNER_DISMISSED,

        ONBOARDING_CAROUSEL_DISPLAYED,

        // If the IPP feedback survey was completed
        IPP_FEEDBACK_SURVEY_COMPLETED,

        // Timestamp of when the IPP feedback request banner was last dismissed
        IPP_FEEDBACK_SURVEY_BANNER_LAST_DISMISSED,

        // Was the IPP feedback survey banner dismissed forever
        IPP_FEEDBACK_SURVEY_BANNER_DISMISSED_FOREVER,

        // Was the Tap To Pay used at least once
        TTP_WAS_USED_AT_LEAST_ONCE,

        // Whether onboarding tasks have been completed or not for a given site
        STORE_ONBOARDING_TASKS_COMPLETED,

        // Was store onboarding shown at least once
        STORE_ONBOARDING_SHOWN_AT_LEAST_ONCE,

        // Time when the last successful payment was made with a card reader
        CARD_READER_LAST_SUCCESSFUL_PAYMENT_TIME,

        // A phone number associated with the store (used in shipping labels)
        STORE_PHONE_NUMBER,

        USER_SEEN_NEW_FEATURE_MORE_SCREEN,

        USER_CLICKED_ON_PAYMENTS_MORE_SCREEN,

        APPLICATION_STORE_SNAPSHOT_TRACKED_FOR_SITE,
    }

    fun init(context: Context) {
        AppPrefs.context = context.applicationContext
        if (relativeInstallationDate == null) relativeInstallationDate = Calendar.getInstance().time
    }

    /**
     * This property tries to acquire the installation date as informed by the Android OS
     * if the value can't be obtained it falls back to the relative installation date controlled by the app
     */
    val installationDate: Date?
        get() = try {
            context
                .packageManager
                .packageInfo(context.packageName, 0)
                .firstInstallTime.let {
                    Date(it)
                }
        } catch (ex: Throwable) {
            relativeInstallationDate
        }

    /**
     * This property informs a installation date relative to the moment the shared preferences data
     * is empty, which can be the accurate installation date or only the moment where the app was updated
     * to support this property, or even the date the app was opened right after the user cleared the store data
     * in the Android App Settings.
     *
     * Considering that, this should be used only as a fall back data to be able to decide
     * an approximate installation date if there's none available
     */
    private var relativeInstallationDate: Date?
        get() = getString(UndeletablePrefKey.APP_INSTALATION_DATE)
            .toLongOrNull()
            ?.let { Date(it) }
        private set(value) = value
            ?.time.toString()
            .let { setString(UndeletablePrefKey.APP_INSTALATION_DATE, it) }

    var isProductAddonsEnabled: Boolean
        get() = getBoolean(DeletablePrefKey.IS_PRODUCT_ADDONS_ENABLED, false)
        set(value) = setBoolean(DeletablePrefKey.IS_PRODUCT_ADDONS_ENABLED, value)

    var isSimulatedReaderEnabled: Boolean
        get() = getBoolean(DeletablePrefKey.USE_SIMULATED_READER, false)
        set(value) = setBoolean(DeletablePrefKey.USE_SIMULATED_READER, value)

    var isInteracEnabled: Boolean
        get() = getBoolean(DeletablePrefKey.ENABLE_SIMULATED_INTERAC, false)
        set(value) = setBoolean(DeletablePrefKey.ENABLE_SIMULATED_INTERAC, value)

    var updateReaderOptionSelected: String
        get() = getString(UPDATE_SIMULATED_READER_OPTION, UpdateOptions.RANDOM.toString())
        set(option) = setString(UPDATE_SIMULATED_READER_OPTION, option)

    var isEUShippingNoticeDismissed: Boolean
        get() = getBoolean(DeletablePrefKey.IS_EU_SHIPPING_NOTICE_DISMISSED, false)
        set(value) = setBoolean(DeletablePrefKey.IS_EU_SHIPPING_NOTICE_DISMISSED, value)

    var chaChingSoundIssueDialogDismissed: Boolean
        get() = getBoolean(DeletablePrefKey.CHA_CHING_SOUND_ISSUE_DIALOG_DISMISSED, false)
        set(value) = setBoolean(DeletablePrefKey.CHA_CHING_SOUND_ISSUE_DIALOG_DISMISSED, value)

    fun getProductSortingChoice(currentSiteId: Int) = getString(getProductSortingKey(currentSiteId)).orNullIfEmpty()

    fun setProductSortingChoice(currentSiteId: Int, value: String) {
        setString(getProductSortingKey(currentSiteId), value)
    }

    private fun getProductSortingKey(currentSiteId: Int) =
        PrefKeyString("$PRODUCT_SORTING_PREFIX:$currentSiteId")

    fun getLastAppVersionCode(): Int {
        return getDeletableInt(UndeletablePrefKey.LAST_APP_VERSION_CODE)
    }

    fun setLastAppVersionCode(versionCode: Int) {
        setDeletableInt(UndeletablePrefKey.LAST_APP_VERSION_CODE, versionCode)
    }

    fun getCancelledAppVersionCode(): Int {
        return getDeletableInt(UndeletablePrefKey.CANCELLED_APP_VERSION_CODE)
    }

    fun setCancelledAppVersionCode(versionCode: Int) {
        setDeletableInt(UndeletablePrefKey.CANCELLED_APP_VERSION_CODE, versionCode)
    }

    fun getFCMToken() = getString(UndeletablePrefKey.WC_PREF_NOTIFICATIONS_TOKEN)

    fun setFCMToken(token: String) {
        setString(UndeletablePrefKey.WC_PREF_NOTIFICATIONS_TOKEN, token)
    }

    fun setSupportEmail(email: String?) {
        if (email.isNullOrEmpty()) {
            remove(DeletablePrefKey.SUPPORT_EMAIL)
        } else {
            setString(DeletablePrefKey.SUPPORT_EMAIL, email)
        }
    }

    fun getSupportEmail() = getString(DeletablePrefKey.SUPPORT_EMAIL)

    fun hasSupportEmail() = getSupportEmail().isNotEmpty()

    fun removeSupportEmail() {
        remove(DeletablePrefKey.SUPPORT_EMAIL)
    }

    fun setSupportName(name: String?) {
        if (name.isNullOrEmpty()) {
            remove(DeletablePrefKey.SUPPORT_NAME)
        } else {
            setString(DeletablePrefKey.SUPPORT_NAME, name)
        }
    }

    fun getSupportName() = getString(DeletablePrefKey.SUPPORT_NAME)

    fun removeSupportName() {
        remove(DeletablePrefKey.SUPPORT_NAME)
    }

    /**
     * Card Reader Upsell Banner
     */
    fun setCardReaderUpsellBannerDismissedForever(
        isDismissed: Boolean,
        localSiteId: Int,
        remoteSiteId: Long,
        selfHostedSiteId: Long
    ) {
        setBoolean(
            getCardReaderUpsellDismissedForeverKey(
                localSiteId,
                remoteSiteId,
                selfHostedSiteId
            ),
            isDismissed
        )
    }

    fun isCardReaderUpsellBannerDismissedForever(
        localSiteId: Int,
        remoteSiteId: Long,
        selfHostedSiteId: Long
    ) = getBoolean(
        getCardReaderUpsellDismissedForeverKey(
            localSiteId,
            remoteSiteId,
            selfHostedSiteId
        ),
        false
    )

    fun setCardReaderUpsellBannerRemindMeLater(
        lastDialogDismissedInMillis: Long,
        localSiteId: Int,
        remoteSiteId: Long,
        selfHostedSiteId: Long
    ) {
        setLong(
            getCardReaderUpsellDismissedRemindMeLaterKey(
                localSiteId,
                remoteSiteId,
                selfHostedSiteId
            ),
            lastDialogDismissedInMillis
        )
    }

    fun getCardReaderUpsellBannerLastDismissed(
        localSiteId: Int,
        remoteSiteId: Long,
        selfHostedSiteId: Long
    ) = getLong(
        getCardReaderUpsellDismissedRemindMeLaterKey(
            localSiteId,
            remoteSiteId,
            selfHostedSiteId
        )
    )

    private fun getCardReaderUpsellDismissedForeverKey(
        localSiteId: Int,
        remoteSiteId: Long,
        selfHostedSiteId: Long
    ) = PrefKeyString(
        "$CARD_READER_UPSELL_BANNER_DIALOG_DISMISSED_FOREVER:$localSiteId:$remoteSiteId:$selfHostedSiteId"
    )

    private fun getCardReaderUpsellDismissedRemindMeLaterKey(
        localSiteId: Int,
        remoteSiteId: Long,
        selfHostedSiteId: Long
    ) = PrefKeyString(
        "$CARD_READER_UPSELL_BANNER_DIALOG_DISMISSED_REMIND_ME_LATER:$localSiteId:$remoteSiteId:$selfHostedSiteId"
    )

    /**
     * Method to check if the v4 stats UI is supported.
     */

    fun isV4StatsSupported() = getBoolean(DeletablePrefKey.IS_USING_V4_API, false)

    fun setV4StatsSupported(isUsingV4Api: Boolean) = setBoolean(DeletablePrefKey.IS_USING_V4_API, isUsingV4Api)

    fun isUserEligible() = getBoolean(DeletablePrefKey.IS_USER_ELIGIBLE, true)

    fun setIsUserEligible(isUserEligible: Boolean) = setBoolean(DeletablePrefKey.IS_USER_ELIGIBLE, isUserEligible)

    fun getUserEmail() = getString(DeletablePrefKey.USER_EMAIL)

    fun setUserEmail(email: String) = setString(DeletablePrefKey.USER_EMAIL, email)

    fun getReceiptUrl(localSiteId: Int, remoteSiteId: Long, selfHostedSiteId: Long, orderId: Long) =
        getString(getReceiptKey(localSiteId, remoteSiteId, selfHostedSiteId, orderId))

    fun setReceiptUrl(localSiteId: Int, remoteSiteId: Long, selfHostedSiteId: Long, orderId: Long, url: String) =
        setString(
            getReceiptKey(localSiteId, remoteSiteId, selfHostedSiteId, orderId),
            url
        )

    private fun getReceiptKey(localSiteId: Int, remoteSiteId: Long, selfHostedSiteId: Long, orderId: Long) =
        PrefKeyString("$RECEIPT_PREFIX:$localSiteId:$remoteSiteId:$selfHostedSiteId:$orderId")

    private fun getPromoBannerKey(bannerType: PromoBannerType) =
        PrefKeyString("PROMO_BANNER_SHOWN_${bannerType.name}")

    fun isPromoBannerShown(bannerType: PromoBannerType): Boolean {
        return getBoolean(getPromoBannerKey(bannerType), false)
    }

    fun setPromoBannerShown(bannerType: PromoBannerType, shown: Boolean) {
        setBoolean(getPromoBannerKey(bannerType), shown)
    }

    fun setLastConnectedCardReaderId(readerId: String) =
        setString(UndeletablePrefKey.LAST_CONNECTED_CARD_READER_ID, readerId)

    fun getLastConnectedCardReaderId() = getString(UndeletablePrefKey.LAST_CONNECTED_CARD_READER_ID).orNullIfEmpty()

    fun removeLastConnectedCardReaderId() = remove(UndeletablePrefKey.LAST_CONNECTED_CARD_READER_ID)

    fun getShowCardReaderConnectedTutorial() = getBoolean(UndeletablePrefKey.SHOW_CARD_READER_CONNECTED_TUTORIAL, true)

    fun setShowCardReaderConnectedTutorial(show: Boolean) =
        setBoolean(UndeletablePrefKey.SHOW_CARD_READER_CONNECTED_TUTORIAL, show)

    fun getLastVersionWithAnnouncement() =
        getString(UndeletablePrefKey.LAST_VERSION_WITH_ANNOUNCEMENT, "0")

    fun setLastVersionWithAnnouncement(version: String) =
        setString(UndeletablePrefKey.LAST_VERSION_WITH_ANNOUNCEMENT, version)

    fun setCardReaderWelcomeDialogShown() =
        setBoolean(UndeletablePrefKey.CARD_READER_WELCOME_SHOWN, true)

    fun isCrashReportingEnabled(): Boolean {
        // default to False for debug builds
        val default = !BuildConfig.DEBUG
        return getBoolean(UndeletablePrefKey.ENABLE_CRASH_REPORTING, default)
    }

    fun setCrashReportingEnabled(enabled: Boolean) {
        setBoolean(UndeletablePrefKey.ENABLE_CRASH_REPORTING, enabled)
    }

    fun getSelectedShipmentTrackingProviderName(): String =
        getString(DeletablePrefKey.SELECTED_SHIPMENT_TRACKING_PROVIDER_NAME)

    fun setSelectedShipmentTrackingProviderName(providerName: String) {
        setString(DeletablePrefKey.SELECTED_SHIPMENT_TRACKING_PROVIDER_NAME, providerName)
    }

    fun getIsSelectedShipmentTrackingProviderCustom(): Boolean =
        getBoolean(DeletablePrefKey.SELECTED_SHIPMENT_TRACKING_PROVIDER_IS_CUSTOM, false)

    fun setIsSelectedShipmentTrackingProviderNameCustom(isCustomProvider: Boolean) {
        setBoolean(DeletablePrefKey.SELECTED_SHIPMENT_TRACKING_PROVIDER_IS_CUSTOM, isCustomProvider)
    }

    fun setLoginSiteAddress(loginSiteAddress: String) {
        setString(DeletablePrefKey.LOGIN_SITE_ADDRESS, loginSiteAddress)
    }

    fun getLoginSiteAddress() = getString(DeletablePrefKey.LOGIN_SITE_ADDRESS)

    fun removeLoginSiteAddress() {
        remove(DeletablePrefKey.LOGIN_SITE_ADDRESS)
    }

    fun setLoginUserBypassedJetpackRequired(bypassedLogin: Boolean = true) {
        setBoolean(DeletablePrefKey.LOGIN_USER_BYPASSED_JETPACK_REQUIRED, bypassedLogin)
    }

    fun getDatabaseDowngraded() = getBoolean(DATABASE_DOWNGRADED, false)

    fun setDatabaseDowngraded(value: Boolean) {
        setBoolean(DATABASE_DOWNGRADED, value)
    }

    fun setSelectedProductType(type: ProductType) = setString(DeletablePrefKey.SELECTED_PRODUCT_TYPE, type.value)

    fun getSelectedProductType(): String = getString(DeletablePrefKey.SELECTED_PRODUCT_TYPE, "")

    fun setSelectedProductIsVirtual(isVirtual: Boolean) =
        setBoolean(DeletablePrefKey.SELECTED_PRODUCT_IS_VIRTUAL, isVirtual)

    fun isSelectedProductVirtual(): Boolean = getBoolean(DeletablePrefKey.SELECTED_PRODUCT_IS_VIRTUAL, false)

    fun getImageOptimizationEnabled() = getBoolean(IMAGE_OPTIMIZE_ENABLED, true)

    fun setImageOptimizationEnabled(enabled: Boolean) {
        setBoolean(IMAGE_OPTIMIZE_ENABLED, enabled)
    }

    fun getAppTheme(): ThemeOption =
        ThemeOption.valueOf(getString(DeletablePrefKey.SELECTED_APP_THEME, DEFAULT.toString()))

    fun setAppTheme(theme: ThemeOption) {
        setString(DeletablePrefKey.SELECTED_APP_THEME, theme.toString())
    }

    /**
     * Used during the unified login process to track the last source the user was in before
     * closing the app so if the user opens and finishes the flow at a later day the tracks
     * events will be complete.
     */
    fun getUnifiedLoginLastSource(): String? {
        val result = getString(DeletablePrefKey.UNIFIED_LOGIN_LAST_ACTIVE_SOURCE)
        return if (result.isNotEmpty()) {
            result
        } else {
            null
        }
    }

    fun setUnifiedLoginLastSource(source: String) {
        setString(DeletablePrefKey.UNIFIED_LOGIN_LAST_ACTIVE_SOURCE, source)
    }

    fun setUnifiedLoginLastFlow(flow: String) {
        setString(DeletablePrefKey.UNIFIED_LOGIN_LAST_ACTIVE_FLOW, flow)
    }

    fun isCashOnDeliveryDisabledStateSkipped(
        localSiteId: Int,
        remoteSiteId: Long,
        selfHostedSiteId: Long,
    ): Boolean {
        return getBoolean(
            getCashOnDeliveryDisabledStateSkippedStatusKey(localSiteId, remoteSiteId, selfHostedSiteId),
            false
        )
    }

    fun setCashOnDeliveryDisabledStateSkipped(
        localSiteId: Int,
        remoteSiteId: Long,
        selfHostedSiteId: Long,
        isSkipped: Boolean
    ) {
        setBoolean(
            getCashOnDeliveryDisabledStateSkippedStatusKey(localSiteId, remoteSiteId, selfHostedSiteId),
            isSkipped
        )
    }

    fun getCardReaderOnboardingStatus(
        localSiteId: Int,
        remoteSiteId: Long,
        selfHostedSiteId: Long
    ): CardReaderOnboardingStatus {
        return valueOf(
            getString(
                getCardReaderOnboardingStatusKey(
                    localSiteId,
                    remoteSiteId,
                    selfHostedSiteId
                ),
                CARD_READER_ONBOARDING_NOT_COMPLETED.name
            )
        )
    }

    fun isCardReaderWelcomeDialogShown() = getBoolean(UndeletablePrefKey.CARD_READER_WELCOME_SHOWN, false)

    fun isCardReaderPluginExplicitlySelected(
        localSiteId: Int,
        remoteSiteId: Long,
        selfHostedSiteId: Long
    ) = getBoolean(
        getIsPluginExplicitlySelectedKey(
            localSiteId,
            remoteSiteId,
            selfHostedSiteId
        ),
        false
    )

    fun getCardReaderPreferredPlugin(
        localSiteId: Int,
        remoteSiteId: Long,
        selfHostedSiteId: Long
    ): PluginType? {
        val storedValue = getString(
            getCardReaderPreferredPluginKey(
                localSiteId,
                remoteSiteId,
                selfHostedSiteId
            )
        )
        return storedValue.orNullIfEmpty()?.let { PluginType.valueOf(it) }
    }

    fun getCardReaderPreferredPluginVersion(
        localSiteId: Int,
        remoteSiteId: Long,
        selfHostedSiteId: Long,
        preferredPlugin: PluginType,
    ) = PreferenceUtils.getString(
        getPreferences(),
        getCardReaderPreferredPluginVersionKey(
            localSiteId,
            remoteSiteId,
            selfHostedSiteId,
            preferredPlugin
        ),
        null
    )

    fun setCardReaderOnboardingData(
        localSiteId: Int,
        remoteSiteId: Long,
        selfHostedSiteId: Long,
        data: PersistentOnboardingData,
    ) {
        setString(
            getCardReaderOnboardingStatusKey(localSiteId, remoteSiteId, selfHostedSiteId),
            data.status.toString()
        )
        setString(
            getCardReaderPreferredPluginKey(localSiteId, remoteSiteId, selfHostedSiteId),
            data.preferredPlugin?.toString().orEmpty()
        )
        data.preferredPlugin?.let { plugin ->
            PreferenceUtils.setString(
                getPreferences(),
                getCardReaderPreferredPluginVersionKey(
                    localSiteId,
                    remoteSiteId,
                    selfHostedSiteId,
                    plugin
                ),
                data.version
            )
        }
    }

    fun setIsCardReaderPluginExplicitlySelectedFlag(
        localSiteId: Int,
        remoteSiteId: Long,
        selfHostedSiteId: Long,
        isPluginExplicitlySelected: Boolean
    ) {
        setBoolean(
            getIsPluginExplicitlySelectedKey(localSiteId, remoteSiteId, selfHostedSiteId),
            isPluginExplicitlySelected
        )
    }

    private fun getCashOnDeliveryDisabledStateSkippedStatusKey(
        localSiteId: Int,
        remoteSiteId: Long,
        selfHostedSiteId: Long
    ) = PrefKeyString(
        "$CARD_READER_DO_NOT_SHOW_CASH_ON_DELIVERY_DISABLED_ONBOARDING_STATE:" +
            "$localSiteId:$remoteSiteId:$selfHostedSiteId"
    )

    private fun getCardReaderOnboardingStatusKey(
        localSiteId: Int,
        remoteSiteId: Long,
        selfHostedSiteId: Long
    ) = PrefKeyString("$CARD_READER_ONBOARDING_COMPLETED_STATUS_V2:$localSiteId:$remoteSiteId:$selfHostedSiteId")

    private fun getIsPluginExplicitlySelectedKey(
        localSiteId: Int,
        remoteSiteId: Long,
        selfHostedSiteId: Long
    ) = PrefKeyString("$CARD_READER_IS_PLUGIN_EXPLICITLY_SELECTED:$localSiteId:$remoteSiteId:$selfHostedSiteId")

    private fun getCardReaderPreferredPluginKey(
        localSiteId: Int,
        remoteSiteId: Long,
        selfHostedSiteId: Long
    ) = PrefKeyString("$CARD_READER_PREFERRED_PLUGIN:$localSiteId:$remoteSiteId:$selfHostedSiteId")

    private fun getCardReaderPreferredPluginVersionKey(
        localSiteId: Int,
        remoteSiteId: Long,
        selfHostedSiteId: Long,
        plugin: PluginType,
    ) = "$CARD_READER_PREFERRED_PLUGIN_VERSION:$localSiteId:$remoteSiteId:$selfHostedSiteId:$plugin"

    fun setCardReaderStatementDescriptor(
        statementDescriptor: String?,
        localSiteId: Int,
        remoteSiteId: Long,
        selfHostedSiteId: Long
    ) {
        setString(
            getCardReaderStatementDescriptorKey(localSiteId, remoteSiteId, selfHostedSiteId),
            statementDescriptor.orEmpty()
        )
    }

    fun getCardReaderStatementDescriptor(
        localSiteId: Int,
        remoteSiteId: Long,
        selfHostedSiteId: Long
    ): String? = getString(
        getCardReaderStatementDescriptorKey(
            localSiteId,
            remoteSiteId,
            selfHostedSiteId
        )
    ).orNullIfEmpty()

    private fun getCardReaderStatementDescriptorKey(
        localSiteId: Int,
        remoteSiteId: Long,
        selfHostedSiteId: Long
    ) = PrefKeyString("$CARD_READER_STATEMENT_DESCRIPTOR:$localSiteId:$remoteSiteId:$selfHostedSiteId")

    fun getJetpackBenefitsDismissalDate(): Long {
        return getLong(DeletableSitePrefKey.JETPACK_BENEFITS_BANNER_DISMISSAL_DATE, 0L)
    }

    fun recordJetpackBenefitsDismissal() {
        return setLong(DeletableSitePrefKey.JETPACK_BENEFITS_BANNER_DISMISSAL_DATE, System.currentTimeMillis())
    }

    fun isTrackingExtensionAvailable(): Boolean {
        return getBoolean(DeletableSitePrefKey.TRACKING_EXTENSION_AVAILABLE, false)
    }

    fun setTrackingExtensionAvailable(isAvailable: Boolean) {
        setBoolean(DeletableSitePrefKey.TRACKING_EXTENSION_AVAILABLE, isAvailable)
    }

    var wasAIProductDescriptionCelebrationShown: Boolean
        get() = getBoolean(
            key = DeletableSitePrefKey.AI_PRODUCT_DESCRIPTION_CELEBRATION_SHOWN,
            default = false
        )
        set(value) = setBoolean(
            key = DeletableSitePrefKey.AI_PRODUCT_DESCRIPTION_CELEBRATION_SHOWN,
            value = value
        )

    fun setOrderFilters(currentSiteId: Int, filterCategory: String, filterValue: String) {
        setString(getOrderFilterKey(currentSiteId, filterCategory), filterValue)
    }

    fun getOrderFilters(currentSiteId: Int, filterCategory: String) = getString(
        getOrderFilterKey(currentSiteId, filterCategory)
    )

    private fun getOrderFilterKey(currentSiteId: Int, filterCategory: String) =
        PrefKeyString("$ORDER_FILTER_PREFIX:$currentSiteId:$filterCategory")

    fun getOrderFilterCustomDateRange(selectedSiteId: Int): Pair<Long, Long> {
        val startDateMillis = getLong(
            PrefKeyString("$ORDER_FILTER_CUSTOM_DATE_RANGE_START:$selectedSiteId")
        )
        val endDateMillis = getLong(
            PrefKeyString("$ORDER_FILTER_CUSTOM_DATE_RANGE_END:$selectedSiteId")
        )
        return Pair(startDateMillis, endDateMillis)
    }

    fun setOrderFilterCustomDateRange(selectedSiteId: Int, startDateMillis: Long, endDateMillis: Long) {
        setLong(
            PrefKeyString("$ORDER_FILTER_CUSTOM_DATE_RANGE_START:$selectedSiteId"),
            startDateMillis
        )
        setLong(
            PrefKeyString("$ORDER_FILTER_CUSTOM_DATE_RANGE_END:$selectedSiteId"),
            endDateMillis
        )
    }

    fun setWcShippingBannerDismissed(dismissed: Boolean, currentSiteId: Int) {
        setBoolean(
            PrefKeyString("${UndeletablePrefKey.WC_SHIPPING_BANNER_DISMISSED}:$currentSiteId"),
            dismissed
        )
    }

    fun getWcShippingBannerDismissed(currentSiteId: Int) =
        getBoolean(
            PrefKeyString("${UndeletablePrefKey.WC_SHIPPING_BANNER_DISMISSED}:$currentSiteId"),
            false
        )

    fun getLocalNotificationWorkRequestId() = getString(DeletablePrefKey.PRE_LOGIN_NOTIFICATION_WORK_REQUEST)

    fun setLocalNotificationWorkRequestId(workRequestId: String) {
        setString(DeletablePrefKey.PRE_LOGIN_NOTIFICATION_WORK_REQUEST, workRequestId)
    }

    fun getPreLoginNotificationDisplayedType() = getString(DeletablePrefKey.PRE_LOGIN_NOTIFICATION_DISPLAYED_TYPE)

    fun setPreLoginNotificationDisplayedType(notificationType: String) {
        setString(DeletablePrefKey.PRE_LOGIN_NOTIFICATION_DISPLAYED_TYPE, notificationType)
    }

    fun setPreLoginNotificationDisplayed(displayed: Boolean) {
        setBoolean(DeletablePrefKey.PRE_LOGIN_NOTIFICATION_DISPLAYED, displayed)
    }

    fun isPreLoginNotificationBeenDisplayed(): Boolean =
        getBoolean(DeletablePrefKey.PRE_LOGIN_NOTIFICATION_DISPLAYED, false)

    fun getLoginEmail() = getString(DeletablePrefKey.LOGIN_EMAIL)

    fun setLoginEmail(email: String) {
        setString(DeletablePrefKey.LOGIN_EMAIL, email)
    }

    fun setOnboardingCarouselDisplayed(displayed: Boolean) {
        setBoolean(ONBOARDING_CAROUSEL_DISPLAYED, displayed)
    }

    fun hasOnboardingCarouselBeenDisplayed(): Boolean =
        getBoolean(ONBOARDING_CAROUSEL_DISPLAYED, false)

    fun isUserSeenNewFeatureOnMoreScreen(): Boolean =
        getBoolean(UndeletablePrefKey.USER_SEEN_NEW_FEATURE_MORE_SCREEN, false)

    fun setUserSeenNewFeatureOnMoreScreen() {
        setBoolean(UndeletablePrefKey.USER_SEEN_NEW_FEATURE_MORE_SCREEN, true)
    }

    fun isPaymentsIconWasClickedOnMoreScreen(): Boolean =
        getBoolean(UndeletablePrefKey.USER_CLICKED_ON_PAYMENTS_MORE_SCREEN, false)

    fun setPaymentsIconWasClickedOnMoreScreen() {
        setBoolean(UndeletablePrefKey.USER_CLICKED_ON_PAYMENTS_MORE_SCREEN, true)
    }

    fun setActiveStatsTab(selectionName: String) {
        setString(DeletablePrefKey.ACTIVE_STATS_GRANULARITY, selectionName)
    }

    fun getActiveStatsTab() = getString(DeletablePrefKey.ACTIVE_STATS_GRANULARITY)

    fun setActiveTopPerformersTab(selectionName: String) {
        setString(DeletablePrefKey.ACTIVE_TOP_PERFORMERS_GRANULARITY, selectionName)
    }

    fun getActiveCouponsTab() = getString(DeletablePrefKey.DASHBOARD_COUPONS_CARD_TAB)

    fun setActiveCouponsTab(selectionName: String) {
        setString(DeletablePrefKey.DASHBOARD_COUPONS_CARD_TAB, selectionName)
    }

    fun getActiveTopPerformersTab() = getString(DeletablePrefKey.ACTIVE_TOP_PERFORMERS_GRANULARITY)

    fun setCustomDomainsSource(source: String) {
        setString(DeletablePrefKey.CUSTOM_DOMAINS_SOURCE, source)
    }

    fun getCustomDomainsSource() = getString(DeletablePrefKey.CUSTOM_DOMAINS_SOURCE, DomainFlowSource.SETTINGS.name)

    fun setJetpackInstallationIsFromBanner(isFromBanner: Boolean) {
        setBoolean(DeletablePrefKey.JETPACK_INSTALLATION_FROM_BANNER, isFromBanner)
    }

    fun getJetpackInstallationIsFromBanner() = getBoolean(DeletablePrefKey.JETPACK_INSTALLATION_FROM_BANNER, false)

    fun setWasNotificationsPermissionBarDismissed(source: Boolean) {
        setBoolean(DeletablePrefKey.NOTIFICATIONS_PERMISSION_BAR, source)
    }

    fun getWasNotificationsPermissionBarDismissed() = getBoolean(DeletablePrefKey.NOTIFICATIONS_PERMISSION_BAR, false)

    /**
     * Used for storing IPP feedback banner interaction data.
     */
    fun isIPPFeedbackSurveyCompleted() = getBoolean(UndeletablePrefKey.IPP_FEEDBACK_SURVEY_COMPLETED, false)

    fun setIPPFeedbackSurveyCompleted(completed: Boolean) {
        setBoolean(UndeletablePrefKey.IPP_FEEDBACK_SURVEY_COMPLETED, completed)
    }

    fun getIPPFeedbackBannerLastDismissed() = getLong(UndeletablePrefKey.IPP_FEEDBACK_SURVEY_BANNER_LAST_DISMISSED, -1L)

    fun setIPPFeedbackBannerDismissedRemindLater(dismissDateTime: Long) {
        setLong(UndeletablePrefKey.IPP_FEEDBACK_SURVEY_BANNER_LAST_DISMISSED, dismissDateTime)
    }

    fun isIPPFeedbackBannerDismissedForever() =
        getBoolean(UndeletablePrefKey.IPP_FEEDBACK_SURVEY_BANNER_DISMISSED_FOREVER, false)

    fun setIPPFeedbackBannerDismissedForever(dismissedForever: Boolean) {
        setBoolean(UndeletablePrefKey.IPP_FEEDBACK_SURVEY_BANNER_DISMISSED_FOREVER, dismissedForever)
    }

    fun isTTPWasUsedAtLeastOnce() =
        getBoolean(UndeletablePrefKey.TTP_WAS_USED_AT_LEAST_ONCE, false)

    fun setTTPWasUsedAtLeastOnce() {
        setBoolean(UndeletablePrefKey.TTP_WAS_USED_AT_LEAST_ONCE, true)
    }

    fun updateOnboardingCompletedStatus(siteId: Int, completed: Boolean) {
        setBoolean(
            key = getStoreOnboardingKeyFor(siteId),
            value = completed
        )
    }

    fun areOnboardingTaskCompletedFor(siteId: Int) = getBoolean(
        key = getStoreOnboardingKeyFor(siteId),
        default = false
    )

    private fun getStoreOnboardingKeyFor(siteId: Int) =
        PrefKeyString("$STORE_ONBOARDING_TASKS_COMPLETED:$siteId")

    fun setStoreOnboardingShown(siteId: Int) {
        setBoolean(
            key = PrefKeyString("$STORE_ONBOARDING_SHOWN_AT_LEAST_ONCE:$siteId"),
            value = true
        )
    }

    fun getStoreOnboardingShown(siteId: Int): Boolean =
        getBoolean(
            key = PrefKeyString("$STORE_ONBOARDING_SHOWN_AT_LEAST_ONCE:$siteId"),
            default = false
        )

    fun getCardReaderLastSuccessfulPaymentTime() =
        getLong(UndeletablePrefKey.CARD_READER_LAST_SUCCESSFUL_PAYMENT_TIME, 0L)

    fun setCardReaderSuccessfulPaymentTime() {
        setLong(UndeletablePrefKey.CARD_READER_LAST_SUCCESSFUL_PAYMENT_TIME, System.currentTimeMillis())
    }

    var savedPrivacySettings: Boolean
        get() = getBoolean(
            key = DeletablePrefKey.HAS_SAVED_PRIVACY_SETTINGS,
            default = false
        )
        set(value) = setBoolean(
            key = DeletablePrefKey.HAS_SAVED_PRIVACY_SETTINGS,
            value = value
        )

    var wasAIProductDescriptionPromoDialogShown: Boolean
        get() = getBoolean(
            key = DeletablePrefKey.WAS_AI_DESCRIPTION_PROMO_DIALOG_SHOWN,
            default = false
        )
        set(value) = setBoolean(
            key = DeletablePrefKey.WAS_AI_DESCRIPTION_PROMO_DIALOG_SHOWN,
            value = value
        )

    var isAIProductDescriptionTooltipDismissed: Boolean
        get() = getBoolean(
            key = DeletablePrefKey.IS_AI_DESCRIPTION_TOOLTIP_DISMISSED,
            default = false
        )
        set(value) = setBoolean(
            key = DeletablePrefKey.IS_AI_DESCRIPTION_TOOLTIP_DISMISSED,
            value = value
        )

    var isNotificationsPermissionBarDismissed: Boolean
        get() = getBoolean(
            key = DeletablePrefKey.NOTIFICATIONS_PERMISSION_BAR,
            default = false
        )
        set(value) = setBoolean(
            key = DeletablePrefKey.NOTIFICATIONS_PERMISSION_BAR,
            value = value
        )

    var aiContentGenerationTone: AiTone
        get() = AiTone.fromString(getString(key = DeletablePrefKey.AI_CONTENT_GENERATION_TONE))
        set(value) = setString(
            key = DeletablePrefKey.AI_CONTENT_GENERATION_TONE,
            value = value.slug
        )

    var aiProductCreationIsFirstAttempt: Boolean
        get() = getBoolean(
            key = DeletablePrefKey.AI_PRODUCT_CREATION_IS_FIRST_ATTEMPT,
            default = true
        )
        set(value) = setBoolean(
            key = DeletablePrefKey.AI_PRODUCT_CREATION_IS_FIRST_ATTEMPT,
            value = value
        )

    var isBlazeCelebrationScreenShown: Boolean
        get() = getBoolean(
            key = DeletablePrefKey.BLAZE_CELEBRATION_SCREEN_SHOWN,
            default = false
        )
        set(value) = setBoolean(
            key = DeletablePrefKey.BLAZE_CELEBRATION_SCREEN_SHOWN,
            value = value
        )

    var timesAiProductCreationSurveyDisplayed: Int
        get() = getInt(
            key = DeletablePrefKey.TIMES_AI_PRODUCT_CREATION_SURVEY_DISPLAYED,
            default = 0
        )
        set(value) = setInt(
            key = DeletablePrefKey.TIMES_AI_PRODUCT_CREATION_SURVEY_DISPLAYED,
            value = value
        )

    var isAiProductCreationSurveyDismissed: Boolean
        get() = getBoolean(
            key = DeletablePrefKey.AI_PRODUCT_CREATION_SURVEY_DISMISSED,
            default = false
        )
        set(value) = setBoolean(
            key = DeletablePrefKey.AI_PRODUCT_CREATION_SURVEY_DISMISSED,
            value = value
        )

    fun incrementAIDescriptionTooltipShownNumber() {
        val currentTotal = getInt(DeletablePrefKey.NUMBER_OF_TIMES_AI_DESCRIPTION_TOOLTIP_SHOWN, 0)
        setInt(DeletablePrefKey.NUMBER_OF_TIMES_AI_DESCRIPTION_TOOLTIP_SHOWN, currentTotal + 1)
    }

    fun getAIDescriptionTooltipShownNumber() =
        getInt(DeletablePrefKey.NUMBER_OF_TIMES_AI_DESCRIPTION_TOOLTIP_SHOWN, 0)

    fun setStorePhoneNumber(siteId: Int, phoneNumber: String) {
        setString(
            key = PrefKeyString("$STORE_PHONE_NUMBER:$siteId"),
            value = phoneNumber
        )
    }

    fun getStorePhoneNumber(siteId: Int): String =
        getString(
            key = PrefKeyString("$STORE_PHONE_NUMBER:$siteId"),
        )

    fun setTimezoneTrackEventTriggeredFor(siteId: Long, localTimezone: String, storeTimezone: String) {
        setBoolean(
            key = PrefKeyString("$siteId$localTimezone$storeTimezone"),
            value = true
        )
    }

    fun isTimezoneTrackEventTriggeredFor(siteId: Long, localTimezone: String, storeTimezone: String) =
        getBoolean(
            key = PrefKeyString("$siteId$localTimezone$storeTimezone"),
            default = false
        )

    fun setApplicationStoreSnapshotTrackedForSite(
        localSiteId: Int,
        remoteSiteId: Long,
        selfHostedSiteId: Long
    ) {
        setBoolean(
            key = PrefKeyString(
                "$APPLICATION_STORE_SNAPSHOT_TRACKED_FOR_SITE:$localSiteId:$remoteSiteId:$selfHostedSiteId"
            ),
            value = true
        )
    }

    fun isApplicationStoreSnapshotTrackedForSite(
        localSiteId: Int,
        remoteSiteId: Long,
        selfHostedSiteId: Long
    ) = getBoolean(
        key = PrefKeyString(
            "$APPLICATION_STORE_SNAPSHOT_TRACKED_FOR_SITE:$localSiteId:$remoteSiteId:$selfHostedSiteId"
        ),
        default = false
    )

    fun getWCStoreID(siteID: Long): String? = getString(
        key = PrefKeyString(
            "$WC_STORE_ID:$siteID"
        )
    ).orNullIfEmpty()

    fun setWCStoreID(siteID: Long, storeID: String?) {
        val key = PrefKeyString("$WC_STORE_ID:$siteID")
        if (storeID.isNullOrEmpty()) {
            remove(key)
        } else {
            setString(key, storeID)
        }
    }

    /**
     * Auto-tax-rate setting
     */
    fun isAutoTaxRateEnabled(): Boolean {
        getLong(AUTO_TAX_RATE_ID, -1L).let {
            return it != -1L
        }
    }

    fun getAutoTaxRateId() = getLong(AUTO_TAX_RATE_ID, -1)

    fun setAutoTaxRateId(taxRateId: Long) {
        setLong(AUTO_TAX_RATE_ID, taxRateId)
    }

    fun disableAutoTaxRate() {
        remove(AUTO_TAX_RATE_ID)
    }

    fun incrementNotificationChannelTypeSuffix(channel: NotificationChannelType) {
        val prefKey = PrefKeyString(channel.name)
        val currentSuffix = getInt(prefKey, 0)
        setInt(prefKey, currentSuffix + 1)
    }

    fun getNotificationChannelTypeSuffix(channel: NotificationChannelType): Int? {
        return getInt(PrefKeyString(channel.name), 0).takeIf { it != 0 }
    }

    /**
     * Remove all user and site-related preferences.
     */
    fun resetUserPreferences() {
        val editor = getPreferences().edit()
        DeletablePrefKey.values().forEach { a -> editor.remove(a.name) }
        editor.remove(SelectedSite.SELECTED_SITE_LOCAL_ID)
        removePreferencesWithDynamicKey(editor)
        editor.apply()

        resetSitePreferences()
    }

    /**
     * This method removes entries in shared preferences which use dynamically created keys.
     *
     * For example order receipts are stored under "RECEIPT_PREFIX:siteId:...:orderId" - each entry has a different
     * key based on the currently selected site and the order it's related to.
     */
    private fun removePreferencesWithDynamicKey(editor: Editor) {
        getPreferences()
            .all
            .filter { it.key.contains(RECEIPT_PREFIX.toString(), ignoreCase = true) }
            .forEach {
                editor.remove(it.key)
            }
    }

    /**
     * Remove all site-related preferences.
     */
    fun resetSitePreferences() {
        val editor = getPreferences().edit()
        DeletableSitePrefKey.values().forEach { editor.remove(it.name) }
        editor.apply()
    }

    private fun getInt(key: PrefKey, default: Int = 0) =
        PreferenceUtils.getInt(getPreferences(), key.toString(), default)

    private fun setInt(key: PrefKey, value: Int) =
        PreferenceUtils.setInt(getPreferences(), key.toString(), value)

    private fun getLong(key: PrefKey, default: Long = 0L) =
        getLong(key.toString(), default)

    private fun getLong(keyName: String, default: Long = 0L) =
        PreferenceUtils.getLong(getPreferences(), keyName, default)

    private fun setLong(key: PrefKey, value: Long) =
        setLong(key.toString(), value)

    private fun setLong(keyName: String, value: Long) =
        PreferenceUtils.setLong(getPreferences(), keyName, value)

    private fun getString(key: PrefKey, defaultValue: String = ""): String {
        return getString(key.toString(), defaultValue)
    }

    private fun getString(keyName: String, defaultValue: String = ""): String {
        return PreferenceUtils.getString(getPreferences(), keyName, defaultValue) ?: defaultValue
    }

    private fun setString(key: PrefKey, value: String) =
        setString(key.toString(), value)

    private fun setString(keyName: String, value: String) =
        PreferenceUtils.setString(getPreferences(), keyName, value)

    fun getBoolean(key: PrefKey, default: Boolean) =
        PreferenceUtils.getBoolean(getPreferences(), key.toString(), default)

    fun setBoolean(key: PrefKey, value: Boolean = false) =
        PreferenceUtils.setBoolean(getPreferences(), key.toString(), value)

    fun getPreferences() = PreferenceManager.getDefaultSharedPreferences(context)

    private fun remove(key: PrefKey) {
        remove(key.toString())
    }

    private fun remove(keyName: String) {
        getPreferences().edit().remove(keyName).apply()
    }

    fun exists(key: PrefKey) = getPreferences().contains(key.toString())

    /**
     * Methods used to store values in SharedPreferences that are not backed up
     * when app is installed/uninstalled. Currently, only used for storing appVersionCode.
     * We might want to migrate this to it's own class if we are to use this for other
     * attributes as well.
     */
    private fun getDeletableInt(key: PrefKey, default: Int = 0) =
        PreferenceUtils.getInt(getDeleteablePreferences(), key.toString(), default)

    private fun setDeletableInt(key: PrefKey, value: Int) =
        PreferenceUtils.setInt(getDeleteablePreferences(), key.toString(), value)

    private fun getDeleteablePreferences(): SharedPreferences {
        return context.getSharedPreferences(
            "${context.packageName}_deletable_preferences",
            Context.MODE_PRIVATE
        )
    }

    enum class CardReaderOnboardingStatus {
        CARD_READER_ONBOARDING_COMPLETED,
        CARD_READER_ONBOARDING_PENDING,
        CARD_READER_ONBOARDING_NOT_COMPLETED,
    }
}
