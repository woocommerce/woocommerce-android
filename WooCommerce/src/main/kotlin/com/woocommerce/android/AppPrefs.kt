@file:Suppress("SameParameterValue")

package com.woocommerce.android

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.content.SharedPreferences.Editor
import androidx.preference.PreferenceManager
import com.woocommerce.android.AppPrefs.CardReaderOnboardingStatus.CARD_READER_ONBOARDING_COMPLETED
import com.woocommerce.android.AppPrefs.CardReaderOnboardingStatus.CARD_READER_ONBOARDING_NOT_COMPLETED
import com.woocommerce.android.AppPrefs.DeletablePrefKey.CARD_READER_ONBOARDING_COMPLETED_STATUS_V2
import com.woocommerce.android.AppPrefs.DeletablePrefKey.CARD_READER_PREFERRED_PLUGIN
import com.woocommerce.android.AppPrefs.DeletablePrefKey.CARD_READER_PREFERRED_PLUGIN_VERSION
import com.woocommerce.android.AppPrefs.DeletablePrefKey.CARD_READER_STATEMENT_DESCRIPTOR
import com.woocommerce.android.AppPrefs.DeletablePrefKey.DATABASE_DOWNGRADED
import com.woocommerce.android.AppPrefs.DeletablePrefKey.IMAGE_OPTIMIZE_ENABLED
import com.woocommerce.android.AppPrefs.DeletablePrefKey.IS_COUPONS_ENABLED
import com.woocommerce.android.AppPrefs.DeletablePrefKey.ORDER_FILTER_CUSTOM_DATE_RANGE_END
import com.woocommerce.android.AppPrefs.DeletablePrefKey.ORDER_FILTER_CUSTOM_DATE_RANGE_START
import com.woocommerce.android.AppPrefs.DeletablePrefKey.ORDER_FILTER_PREFIX
import com.woocommerce.android.AppPrefs.DeletablePrefKey.PRODUCT_SORTING_PREFIX
import com.woocommerce.android.AppPrefs.DeletablePrefKey.RECEIPT_PREFIX
import com.woocommerce.android.extensions.orNullIfEmpty
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.cardreader.onboarding.PersistentOnboardingData
import com.woocommerce.android.ui.cardreader.onboarding.PluginType
import com.woocommerce.android.ui.products.ProductType
import com.woocommerce.android.util.PreferenceUtils
import com.woocommerce.android.util.ThemeOption
import com.woocommerce.android.util.ThemeOption.DEFAULT
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T
import java.util.Calendar
import java.util.Date

// Guaranteed to hold a reference to the application context, which is safe
@SuppressLint("StaticFieldLeak")
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
        IS_COUPONS_ENABLED,
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
        CARD_READER_PREFERRED_PLUGIN,
        CARD_READER_PREFERRED_PLUGIN_VERSION,
        CARD_READER_STATEMENT_DESCRIPTOR,
        ORDER_FILTER_PREFIX,
        ORDER_FILTER_CUSTOM_DATE_RANGE_START,
        ORDER_FILTER_CUSTOM_DATE_RANGE_END,
        PRODUCT_SORTING_PREFIX,
    }

    /**
     * Application related preferences. When the user changes a site, these preferences are erased.
     */
    private enum class DeletableSitePrefKey : PrefKey {
        TRACKING_EXTENSION_AVAILABLE,
        JETPACK_BENEFITS_BANNER_DISMISSAL_DATE,
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

        // Enable notifications for new orders
        NOTIFS_ORDERS_ENABLED,

        // Enable notifications for new reviews
        NOTIFS_REVIEWS_ENABLED,

        // Play cha-ching sound on new order notifications
        NOTIFS_ORDERS_CHA_CHING_ENABLED,

        // Number of times the "mark all notifications read" icon was tapped
        NUM_TIMES_MARK_ALL_NOTIFS_READ_SNACK_SHOWN,

        // The app update for this version was cancelled by the user
        CANCELLED_APP_VERSION_CODE,

        // Application permissions
        ASKED_PERMISSION_CAMERA,

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
                .getPackageInfo(context.packageName, 0)
                .firstInstallTime
                .let { Date(it) }
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

    var isCouponsEnabled: Boolean
        get() = getBoolean(IS_COUPONS_ENABLED, false)
        set(value) = setBoolean(IS_COUPONS_ENABLED, value)

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
        if (!email.isNullOrEmpty()) {
            setString(DeletablePrefKey.SUPPORT_EMAIL, email)
        } else {
            remove(DeletablePrefKey.SUPPORT_EMAIL)
        }
    }

    fun getSupportEmail() = getString(DeletablePrefKey.SUPPORT_EMAIL)

    fun hasSupportEmail() = getSupportEmail().isNotEmpty()

    fun removeSupportEmail() {
        remove(DeletablePrefKey.SUPPORT_EMAIL)
    }

    fun setSupportName(name: String) {
        setString(DeletablePrefKey.SUPPORT_NAME, name)
    }

    fun getSupportName() = getString(DeletablePrefKey.SUPPORT_NAME)

    fun removeSupportName() {
        remove(DeletablePrefKey.SUPPORT_NAME)
    }

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

    /**
     * Flag to check products features are enabled
     */
    fun isProductsFeatureEnabled() = getBoolean(DeletablePrefKey.IS_PRODUCTS_FEATURE_ENABLED, false)

    fun setIsProductsFeatureEnabled(isProductsFeatureEnabled: Boolean) =
        setBoolean(DeletablePrefKey.IS_PRODUCTS_FEATURE_ENABLED, isProductsFeatureEnabled)

    fun isCrashReportingEnabled(): Boolean {
        // default to False for debug builds
        val default = !BuildConfig.DEBUG
        return getBoolean(UndeletablePrefKey.ENABLE_CRASH_REPORTING, default)
    }

    fun setCrashReportingEnabled(enabled: Boolean) {
        setBoolean(UndeletablePrefKey.ENABLE_CRASH_REPORTING, enabled)
    }

    fun isOrderNotificationsEnabled() = getBoolean(UndeletablePrefKey.NOTIFS_ORDERS_ENABLED, true)

    fun setOrderNotificationsEnabled(enabled: Boolean) {
        setBoolean(UndeletablePrefKey.NOTIFS_ORDERS_ENABLED, enabled)
    }

    fun isReviewNotificationsEnabled() = getBoolean(UndeletablePrefKey.NOTIFS_REVIEWS_ENABLED, true)

    fun setReviewNotificationsEnabled(enabled: Boolean) {
        setBoolean(UndeletablePrefKey.NOTIFS_REVIEWS_ENABLED, enabled)
    }

    fun isOrderNotificationsChaChingEnabled() = getBoolean(UndeletablePrefKey.NOTIFS_ORDERS_CHA_CHING_ENABLED, true)

    fun setOrderNotificationsChaChingEnabled(enabled: Boolean) {
        setBoolean(UndeletablePrefKey.NOTIFS_ORDERS_CHA_CHING_ENABLED, enabled)
    }

    fun getNumTimesMarkAllReadSnackShown(): Int =
        getInt(UndeletablePrefKey.NUM_TIMES_MARK_ALL_NOTIFS_READ_SNACK_SHOWN, 0)

    fun incNumTimesMarkAllReadSnackShown() {
        val numTimesShown = getNumTimesMarkAllReadSnackShown() + 1
        setInt(UndeletablePrefKey.NUM_TIMES_MARK_ALL_NOTIFS_READ_SNACK_SHOWN, numTimesShown)
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

    fun getLoginUserBypassedJetpackRequired() =
        getBoolean(DeletablePrefKey.LOGIN_USER_BYPASSED_JETPACK_REQUIRED, false)

    fun removeLoginUserBypassedJetpackRequired() {
        remove(DeletablePrefKey.LOGIN_USER_BYPASSED_JETPACK_REQUIRED)
    }

    fun getDatabaseDowngraded() = getBoolean(DATABASE_DOWNGRADED, false)

    fun setDatabaseDowngraded(value: Boolean) {
        setBoolean(DATABASE_DOWNGRADED, value)
    }

    fun setSelectedOrderListTab(selectedOrderListTabPosition: Int) {
        setInt(DeletablePrefKey.SELECTED_ORDER_LIST_TAB_POSITION, selectedOrderListTabPosition)
    }

    fun getSelectedOrderListTabPosition() =
        getInt(DeletablePrefKey.SELECTED_ORDER_LIST_TAB_POSITION, -1)

    fun setSelectedProductType(type: ProductType) = setString(DeletablePrefKey.SELECTED_PRODUCT_TYPE, type.value)

    fun getSelectedProductType(): String = getString(DeletablePrefKey.SELECTED_PRODUCT_TYPE, "")

    fun setSelectedProductIsVirtual(isVirtual: Boolean) =
        setBoolean(DeletablePrefKey.SELECTED_PRODUCT_IS_VIRTUAL, isVirtual)

    fun isSelectedProductVirtual(): Boolean = getBoolean(DeletablePrefKey.SELECTED_PRODUCT_IS_VIRTUAL, false)

    /**
     * Checks if the user has a saved order list tab position yet. If no position has been saved,
     * then the value will be the default of -1.
     *
     * @return True if the saved position is not the default -1, else false
     */
    fun hasSelectedOrderListTabPosition() = getSelectedOrderListTabPosition() > -1

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

    /**
     * Used during the unified login process to track the last flow the user was in before
     * closing the app so if the user opens and finishes the flow at a later day the tracks
     * events will be complete.
     */
    fun getUnifiedLoginLastFlow(): String? {
        val result = getString(DeletablePrefKey.UNIFIED_LOGIN_LAST_ACTIVE_FLOW)
        return if (result.isNotEmpty()) {
            result
        } else {
            null
        }
    }

    fun setUnifiedLoginLastFlow(flow: String) {
        setString(DeletablePrefKey.UNIFIED_LOGIN_LAST_ACTIVE_FLOW, flow)
    }

    fun getCardReaderOnboardingStatus(
        localSiteId: Int,
        remoteSiteId: Long,
        selfHostedSiteId: Long
    ): CardReaderOnboardingStatus {
        return CardReaderOnboardingStatus.valueOf(
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

    fun isCardReaderOnboardingCompleted(localSiteId: Int, remoteSiteId: Long, selfHostedSiteId: Long): Boolean {
        val completedStatus = getCardReaderOnboardingStatus(
            localSiteId,
            remoteSiteId,
            selfHostedSiteId
        )
        return completedStatus == CARD_READER_ONBOARDING_COMPLETED
    }

    fun isCardReaderWelcomeDialogShown() = getBoolean(UndeletablePrefKey.CARD_READER_WELCOME_SHOWN, false)

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

    private fun getCardReaderOnboardingStatusKey(
        localSiteId: Int,
        remoteSiteId: Long,
        selfHostedSiteId: Long
    ) = PrefKeyString("$CARD_READER_ONBOARDING_COMPLETED_STATUS_V2:$localSiteId:$remoteSiteId:$selfHostedSiteId")

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
        PreferenceUtils.getLong(getPreferences(), key.toString(), default)

    private fun setLong(key: PrefKey, value: Long) =
        PreferenceUtils.setLong(getPreferences(), key.toString(), value)

    private fun getString(key: PrefKey, defaultValue: String = ""): String {
        return PreferenceUtils.getString(getPreferences(), key.toString(), defaultValue) ?: defaultValue
    }

    private fun setString(key: PrefKey, value: String) =
        PreferenceUtils.setString(getPreferences(), key.toString(), value)

    fun getBoolean(key: PrefKey, default: Boolean) =
        PreferenceUtils.getBoolean(getPreferences(), key.toString(), default)

    fun setBoolean(key: PrefKey, value: Boolean = false) =
        PreferenceUtils.setBoolean(getPreferences(), key.toString(), value)

    fun getPreferences() = PreferenceManager.getDefaultSharedPreferences(context)

    private fun remove(key: PrefKey) {
        getPreferences().edit().remove(key.toString()).apply()
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

    /*
     * key in shared preferences which stores a boolean telling whether the app has already
     * asked for the passed permission
     */
    fun getPermissionAskedKey(permission: String): PrefKey? {
        when (permission) {
            android.Manifest.permission.CAMERA ->
                return UndeletablePrefKey.ASKED_PERMISSION_CAMERA
            else -> {
                WooLog.w(T.UTILS, "No key for requested permission: $permission")
                return null
            }
        }
    }

    enum class CardReaderOnboardingStatus {
        CARD_READER_ONBOARDING_COMPLETED,
        CARD_READER_ONBOARDING_PENDING,
        CARD_READER_ONBOARDING_NOT_COMPLETED,
    }
}
