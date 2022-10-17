package com.woocommerce.android

import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import com.woocommerce.android.ui.payments.cardreader.onboarding.PersistentOnboardingData
import com.woocommerce.android.ui.payments.cardreader.onboarding.PluginType
import com.woocommerce.android.ui.promobanner.PromoBannerType
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject

class AppPrefsWrapper @Inject constructor() {
    var isCouponsEnabled: Boolean by AppPrefs::isCouponsEnabled

    fun getReceiptUrl(localSiteId: Int, remoteSiteId: Long, selfHostedSiteId: Long, orderId: Long) =
        AppPrefs.getReceiptUrl(localSiteId, remoteSiteId, selfHostedSiteId, orderId)

    fun setReceiptUrl(
        localSiteId: Int,
        remoteSiteId: Long,
        selfHostedSiteId: Long,
        orderId: Long,
        url: String
    ) = AppPrefs.setReceiptUrl(localSiteId, remoteSiteId, selfHostedSiteId, orderId, url)

    fun isCardReaderOnboardingCompleted(localSiteId: Int, remoteSiteId: Long, selfHostedSiteId: Long) =
        AppPrefs.isCardReaderOnboardingCompleted(localSiteId, remoteSiteId, selfHostedSiteId)

    fun isCardReaderWelcomeDialogShown() = AppPrefs.isCardReaderWelcomeDialogShown()

    fun isCardReaderPluginExplicitlySelected(localSiteId: Int, remoteSiteId: Long, selfHostedSiteId: Long) =
        AppPrefs.isCardReaderPluginExplicitlySelected(localSiteId, remoteSiteId, selfHostedSiteId)

    fun getCardReaderPreferredPlugin(
        localSiteId: Int,
        remoteSiteId: Long,
        selfHostedSiteId: Long
    ): PluginType? = AppPrefs.getCardReaderPreferredPlugin(localSiteId, remoteSiteId, selfHostedSiteId)

    fun getCardReaderPreferredPluginVersion(
        localSiteId: Int,
        remoteSiteId: Long,
        selfHostedSiteId: Long,
        preferredPlugin: PluginType,
    ): String? = AppPrefs.getCardReaderPreferredPluginVersion(
        localSiteId,
        remoteSiteId,
        selfHostedSiteId,
        preferredPlugin
    )

    fun setCardReaderOnboardingData(
        localSiteId: Int,
        remoteSiteId: Long,
        selfHostedSiteId: Long,
        data: PersistentOnboardingData,
    ) {
        AppPrefs.setCardReaderOnboardingData(
            localSiteId,
            remoteSiteId,
            selfHostedSiteId,
            data,
        )
    }

    fun setIsCardReaderPluginExplicitlySelectedFlag(
        localSiteId: Int,
        remoteSiteId: Long,
        selfHostedSiteId: Long,
        isPluginExplicitlySelected: Boolean = false
    ) {
        AppPrefs.setIsCardReaderPluginExplicitlySelectedFlag(
            localSiteId,
            remoteSiteId,
            selfHostedSiteId,
            isPluginExplicitlySelected
        )
    }

    fun setCardReaderStatementDescriptor(
        statementDescriptor: String?,
        localSiteId: Int,
        remoteSiteId: Long,
        selfHostedSiteId: Long
    ) = AppPrefs.setCardReaderStatementDescriptor(statementDescriptor, localSiteId, remoteSiteId, selfHostedSiteId)

    fun getCardReaderStatementDescriptor(
        localSiteId: Int,
        remoteSiteId: Long,
        selfHostedSiteId: Long
    ) = AppPrefs.getCardReaderStatementDescriptor(localSiteId, remoteSiteId, selfHostedSiteId)

    fun isCashOnDeliveryDisabledStateSkipped(
        localSiteId: Int,
        remoteSiteId: Long,
        selfHostedSiteId: Long
    ) = AppPrefs.isCashOnDeliveryDisabledStateSkipped(localSiteId, remoteSiteId, selfHostedSiteId)

    fun setCashOnDeliveryDisabledStateSkipped(
        localSiteId: Int,
        remoteSiteId: Long,
        selfHostedSiteId: Long,
        isSkipped: Boolean
    ) = AppPrefs.setCashOnDeliveryDisabledStateSkipped(
        localSiteId,
        remoteSiteId,
        selfHostedSiteId,
        isSkipped
    )

    fun setLastConnectedCardReaderId(readerId: String) = AppPrefs.setLastConnectedCardReaderId(readerId)

    fun getLastConnectedCardReaderId() = AppPrefs.getLastConnectedCardReaderId()

    fun setCardReaderWelcomeDialogShown() = AppPrefs.setCardReaderWelcomeDialogShown()

    fun removeLastConnectedCardReaderId() = AppPrefs.removeLastConnectedCardReaderId()

    fun isOrderNotificationsEnabled() = AppPrefs.isOrderNotificationsEnabled()

    fun isReviewNotificationsEnabled() = AppPrefs.isReviewNotificationsEnabled()

    fun isOrderNotificationsChaChingEnabled() = AppPrefs.isOrderNotificationsChaChingEnabled()

    fun getJetpackBenefitsDismissalDate(): Long {
        return AppPrefs.getJetpackBenefitsDismissalDate()
    }

    fun recordJetpackBenefitsDismissal() {
        AppPrefs.recordJetpackBenefitsDismissal()
    }

    fun setOrderFilters(selectedSiteId: Int, filterCategory: String, filterValue: String) {
        AppPrefs.setOrderFilters(selectedSiteId, filterCategory, filterValue)
    }

    fun getOrderFilters(selectedSiteId: Int, filterCategory: String) =
        AppPrefs.getOrderFilters(selectedSiteId, filterCategory)

    fun setOrderFilterCustomDateRange(selectedSiteId: Int, startDateMillis: Long, endDateMillis: Long) {
        AppPrefs.setOrderFilterCustomDateRange(selectedSiteId, startDateMillis, endDateMillis)
    }

    fun getOrderFilterCustomDateRange(selectedSiteId: Int): Pair<Long, Long> =
        AppPrefs.getOrderFilterCustomDateRange(selectedSiteId)

    fun setV4StatsSupported(supported: Boolean) {
        AppPrefs.setV4StatsSupported(supported)
    }

    fun isUserEligible(): Boolean = AppPrefs.isUserEligible()

    fun getFCMToken() = AppPrefs.getFCMToken()

    fun setFCMToken(token: String) = AppPrefs.setFCMToken(token)

    fun getProductSortingChoice(siteId: Int) = AppPrefs.getProductSortingChoice(siteId)

    fun setProductSortingChoice(siteId: Int, value: String) = AppPrefs.setProductSortingChoice(siteId, value)

    fun getUnifiedLoginLastSource() = AppPrefs.getUnifiedLoginLastSource()

    fun removeLoginSiteAddress() = AppPrefs.removeLoginSiteAddress()

    fun getLoginSiteAddress() = AppPrefs.getLoginSiteAddress().takeIf { it.isNotEmpty() }

    fun setWcShippingBannerDismissed(dismissed: Boolean, currentSiteId: Int) {
        AppPrefs.setWcShippingBannerDismissed(dismissed, currentSiteId)
    }

    fun getWcShippingBannerDismissed(currentSiteId: Int) = AppPrefs.getWcShippingBannerDismissed(currentSiteId)

    fun getPreLoginNotificationWorkRequestId() = AppPrefs.getLocalNotificationWorkRequestId()

    fun setPreLoginNotificationWorkRequestId(workRequestId: String) =
        AppPrefs.setLocalNotificationWorkRequestId(workRequestId)

    fun hasPreLoginNotificationBeenDisplayed() = AppPrefs.isPreLoginNotificationBeenDisplayed()

    fun setPreLoginNotificationDisplayedType(notificationType: String) =
        AppPrefs.setPreLoginNotificationDisplayedType(notificationType)

    fun getPreLoginNotificationDisplayedType() = AppPrefs.getPreLoginNotificationDisplayedType()

    fun setPreLoginNotificationDisplayed(displayed: Boolean) =
        AppPrefs.setPreLoginNotificationDisplayed(displayed)

    fun setLoginEmail(email: String) =
        AppPrefs.setLoginEmail(email)

    fun getLoginEmail() = AppPrefs.getLoginEmail()

    fun isUserSeenNewFeatureOnMoreScreen() = AppPrefs.isUserSeenNewFeatureOnMoreScreen()

    fun setUserSeenNewFeatureOnMoreScreen() = AppPrefs.setUserSeenNewFeatureOnMoreScreen()

    fun isPaymentsIconWasClickedOnMoreScreen() = AppPrefs.isPaymentsIconWasClickedOnMoreScreen()

    fun setPaymentsIconWasClickedOnMoreScreen() = AppPrefs.setPaymentsIconWasClickedOnMoreScreen()

    fun setOnboardingCarouselDisplayed(displayed: Boolean) =
        AppPrefs.setOnboardingCarouselDisplayed(displayed)

    fun hasOnboardingCarouselBeenDisplayed(): Boolean = AppPrefs.hasOnboardingCarouselBeenDisplayed()

    fun setActiveStatsGranularity(currentSiteId: Int, statsGranularity: String) {
        AppPrefs.setActiveStatsGranularity(currentSiteId, statsGranularity)
    }

    fun getActiveStatsGranularity(currentSiteId: Int) =
        AppPrefs.getActiveStatsGranularity(currentSiteId)

    /**
     * Card Reader Upsell
     */
    fun setCardReaderUpsellBannerDismissed(
        isDismissed: Boolean,
        localSiteId: Int,
        remoteSiteId: Long,
        selfHostedSiteId: Long
    ) = AppPrefs.setCardReaderUpsellBannerDismissedForever(
        isDismissed,
        localSiteId,
        remoteSiteId,
        selfHostedSiteId
    )

    fun isCardReaderUpsellBannerDismissedForever(
        localSiteId: Int,
        remoteSiteId: Long,
        selfHostedSiteId: Long
    ) = AppPrefs.isCardReaderUpsellBannerDismissedForever(
        localSiteId,
        remoteSiteId,
        selfHostedSiteId
    )

    fun setCardReaderUpsellBannerRemindMeLater(
        lastDialogDismissedInMillis: Long,
        localSiteId: Int,
        remoteSiteId: Long,
        selfHostedSiteId: Long
    ) = AppPrefs.setCardReaderUpsellBannerRemindMeLater(
        lastDialogDismissedInMillis,
        localSiteId,
        remoteSiteId,
        selfHostedSiteId
    )

    fun getCardReaderUpsellBannerLastDismissed(
        localSiteId: Int,
        remoteSiteId: Long,
        selfHostedSiteId: Long
    ) = AppPrefs.getCardReaderUpsellBannerLastDismissed(
        localSiteId,
        remoteSiteId,
        selfHostedSiteId
    )

    fun getSelectedProductType() = AppPrefs.getSelectedProductType()

    fun isSelectedProductVirtual() = AppPrefs.isSelectedProductVirtual()

    fun isPromoBannerShown(bannerType: PromoBannerType) = AppPrefs.isPromoBannerShown(bannerType)

    fun setPromoBannerShown(bannerType: PromoBannerType, shown: Boolean) {
        AppPrefs.setPromoBannerShown(bannerType, shown)
    }

    fun isV4StatsSupported() = AppPrefs.isV4StatsSupported()

    /**
     * Observes changes to the preferences
     */
    fun observePrefs(): Flow<Unit> {
        return callbackFlow {
            val listener = OnSharedPreferenceChangeListener { _, _ ->
                trySend(Unit)
            }
            AppPrefs.getPreferences().registerOnSharedPreferenceChangeListener(listener)

            awaitClose {
                AppPrefs.getPreferences().unregisterOnSharedPreferenceChangeListener(listener)
            }
        }
    }
}
