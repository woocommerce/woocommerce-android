package com.woocommerce.android

import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import com.woocommerce.android.notifications.NotificationChannelType
import com.woocommerce.android.ui.payments.cardreader.onboarding.PersistentOnboardingData
import com.woocommerce.android.ui.payments.cardreader.onboarding.PluginType
import com.woocommerce.android.ui.prefs.domain.DomainFlowSource
import com.woocommerce.android.ui.promobanner.PromoBannerType
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject

class AppPrefsWrapper @Inject constructor() {
    var savedPrivacyBannerSettings by AppPrefs::savedPrivacySettings

    var wasAIProductDescriptionPromoDialogShown by AppPrefs::wasAIProductDescriptionPromoDialogShown

    var isAIProductDescriptionTooltipDismissed by AppPrefs::isAIProductDescriptionTooltipDismissed

    var aiContentGenerationTone by AppPrefs::aiContentGenerationTone

    var aiProductCreationIsFirstAttempt by AppPrefs::aiProductCreationIsFirstAttempt

    var isBlazeCelebrationScreenShown by AppPrefs::isBlazeCelebrationScreenShown

    var wasAIProductDescriptionCelebrationShown by AppPrefs::wasAIProductDescriptionCelebrationShown

    var chaChingSoundIssueDialogDismissed by AppPrefs::chaChingSoundIssueDialogDismissed

    var timesAiProductCreationSurveyDisplayed by AppPrefs::timesAiProductCreationSurveyDisplayed

    var isAiProductCreationSurveyDismissed by AppPrefs::isAiProductCreationSurveyDismissed

    fun getAppInstallationDate() = AppPrefs.installationDate

    fun getReceiptUrl(localSiteId: Int, remoteSiteId: Long, selfHostedSiteId: Long, orderId: Long) =
        AppPrefs.getReceiptUrl(localSiteId, remoteSiteId, selfHostedSiteId, orderId)

    fun setReceiptUrl(
        localSiteId: Int,
        remoteSiteId: Long,
        selfHostedSiteId: Long,
        orderId: Long,
        url: String
    ) = AppPrefs.setReceiptUrl(localSiteId, remoteSiteId, selfHostedSiteId, orderId, url)

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

    fun selectedUpdateReaderOption() = AppPrefs.updateReaderOptionSelected

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

    fun setLoginSiteAddress(loginSiteAddress: String) = AppPrefs.setLoginSiteAddress(loginSiteAddress)

    fun removeLoginSiteAddress() = AppPrefs.removeLoginSiteAddress()

    fun getLoginSiteAddress() = AppPrefs.getLoginSiteAddress().takeIf { it.isNotEmpty() }

    fun setWcShippingBannerDismissed(dismissed: Boolean, currentSiteId: Int) {
        AppPrefs.setWcShippingBannerDismissed(dismissed, currentSiteId)
    }

    fun getWcShippingBannerDismissed(currentSiteId: Int) = AppPrefs.getWcShippingBannerDismissed(currentSiteId)

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

    fun setActiveStatsTab(selectionName: String) {
        AppPrefs.setActiveStatsTab(selectionName)
    }

    fun getActiveStoreStatsTab() = AppPrefs.getActiveStatsTab()

    fun setActiveTopPerformersTab(selectionName: String) {
        AppPrefs.setActiveTopPerformersTab(selectionName)
    }

    fun getActiveTopPerformersTab() = AppPrefs.getActiveTopPerformersTab()

    fun getActiveCouponsTab() = AppPrefs.getActiveCouponsTab()

    fun setActiveCouponsTab(selectionName: String) {
        AppPrefs.setActiveCouponsTab(selectionName)
    }

    fun setCustomDomainsSource(source: DomainFlowSource) {
        AppPrefs.setCustomDomainsSource(source.name)
    }

    fun getCustomDomainsSource(): DomainFlowSource = enumValueOf(AppPrefs.getCustomDomainsSource())
    fun getCustomDomainsSourceAsString(): String = AppPrefs.getCustomDomainsSource().lowercase()

    fun setJetpackInstallationIsFromBanner(isFromBanner: Boolean) {
        AppPrefs.setJetpackInstallationIsFromBanner(isFromBanner)
    }

    fun getJetpackInstallationIsFromBanner() = AppPrefs.getJetpackInstallationIsFromBanner()

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
     * Used for storing IPP feedback banner interaction data.
     */
    fun isIPPFeedbackSurveyCompleted() = AppPrefs.isIPPFeedbackSurveyCompleted()

    fun setIPPFeedbackSurveyCompleted(completed: Boolean) = AppPrefs.setIPPFeedbackSurveyCompleted(completed)

    fun getIPPFeedbackBannerLastDismissed() = AppPrefs.getIPPFeedbackBannerLastDismissed()

    fun setIPPFeedbackBannerDismissedRemindLater(dismissDateTime: Long) =
        AppPrefs.setIPPFeedbackBannerDismissedRemindLater(dismissDateTime)

    fun isIPPFeedbackBannerDismissedForever() = AppPrefs.isIPPFeedbackBannerDismissedForever()

    fun setIPPFeedbackBannerDismissedForever(dismissedForever: Boolean) =
        AppPrefs.setIPPFeedbackBannerDismissedForever(dismissedForever)

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

    fun updateOnboardingCompletedStatus(siteId: Int, completed: Boolean) {
        AppPrefs.updateOnboardingCompletedStatus(siteId, completed)
    }

    fun isOnboardingCompleted(siteId: Int): Boolean = AppPrefs.areOnboardingTaskCompletedFor(siteId)

    fun setStoreOnboardingShown(siteId: Int) {
        AppPrefs.setStoreOnboardingShown(siteId)
    }

    fun getStoreOnboardingShown(siteId: Int): Boolean = AppPrefs.getStoreOnboardingShown(siteId)

    fun setStorePhoneNumber(siteId: Int, phoneNumber: String) = AppPrefs.setStorePhoneNumber(siteId, phoneNumber)

    fun getStorePhoneNumber(siteId: Int): String = AppPrefs.getStorePhoneNumber(siteId)

    fun recordAIDescriptionTooltipShown() = AppPrefs.incrementAIDescriptionTooltipShownNumber()
    fun getAIDescriptionTooltipShownNumber() = AppPrefs.getAIDescriptionTooltipShownNumber()

    fun isCrashReportingEnabled(): Boolean = AppPrefs.isCrashReportingEnabled()

    fun setCrashReportingEnabled(enabled: Boolean) {
        AppPrefs.setCrashReportingEnabled(enabled)
    }

    fun setTimezoneTrackEventTriggeredFor(siteId: Long, localTimezone: String, storeTimezone: String) {
        AppPrefs.setTimezoneTrackEventTriggeredFor(siteId, localTimezone, storeTimezone)
    }

    fun isTimezoneTrackEventNeverTriggeredFor(siteId: Long, localTimezone: String, storeTimezone: String) =
        AppPrefs.isTimezoneTrackEventTriggeredFor(siteId, localTimezone, storeTimezone).not()

    fun getWCStoreID(siteID: Long) = AppPrefs.getWCStoreID(siteID)

    fun setWCStoreID(siteID: Long, storeID: String?) {
        AppPrefs.setWCStoreID(siteID, storeID)
    }

    fun incrementNotificationChannelTypeSuffix(channel: NotificationChannelType) =
        AppPrefs.incrementNotificationChannelTypeSuffix(channel)

    fun getNotificationChannelTypeSuffix(channel: NotificationChannelType): Int? =
        AppPrefs.getNotificationChannelTypeSuffix(channel)

    fun setBlazeNoCampaignReminderShown(siteId: Long) {
        AppPrefs.setBlazeNoCampaignReminderShown(siteId)
    }

    fun getBlazeNoCampaignReminderShown(siteId: Long) = AppPrefs.getBlazeNoCampaignReminderShown(siteId)

    fun setBlazeAbandonedCampaignReminderShown(siteId: Long) {
        AppPrefs.setBlazeAbandonedCampaignReminderShown(siteId)
    }

    fun getBlazeAbandonedCampaignReminderShown(siteId: Long) = AppPrefs.getBlazeAbandonedCampaignReminderShown(siteId)
}
