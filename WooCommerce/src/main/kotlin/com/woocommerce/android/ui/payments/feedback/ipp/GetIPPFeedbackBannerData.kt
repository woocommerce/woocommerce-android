package com.woocommerce.android.ui.payments.feedback.ipp

import android.os.Parcelable
import androidx.annotation.StringRes
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_IPP_BANNER_CAMPAIGN_NAME_BEGINNER
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_IPP_BANNER_CAMPAIGN_NAME_NEWBIE
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_IPP_BANNER_CAMPAIGN_NAME_NINJA
import com.woocommerce.android.extensions.WOOCOMMERCE_PAYMENTS_PAYMENT_TYPE
import com.woocommerce.android.extensions.daysAgo
import com.woocommerce.android.ui.payments.GetActivePaymentsPlugin
import com.woocommerce.android.ui.payments.cardreader.CashOnDeliverySettingsRepository
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.model.OrderEntity
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.WCInPersonPaymentsStore
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.util.AppLog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

class GetIPPFeedbackBannerData @Inject constructor(
    private val shouldShowFeedbackBanner: ShouldShowFeedbackBanner,
    private val orderStore: WCOrderStore,
    private val cashOnDeliverySettings: CashOnDeliverySettingsRepository,
    private val siteModel: SiteModel,
    private val getActivePaymentsPlugin: GetActivePaymentsPlugin,
    private val logger: AppLogWrapper,
) {
    @Suppress("ReturnCount")
    suspend operator fun invoke(): IPPFeedbackBanner? {
        if (!shouldShowFeedbackBanner()) {
            logger.e(AppLog.T.API, "GetIPPFeedbackBannerData should not be shown.")
            return null
        }

        val activePaymentsPlugin = getActivePaymentsPlugin()

        if (activePaymentsPlugin != WCInPersonPaymentsStore.InPersonPaymentsPluginType.WOOCOMMERCE_PAYMENTS) {
            return IPP_NEWBIE_BANNER
        }

        val wcPayIPPOrders = getIppOrders()
        val hasWCPayIPPOrders = wcPayIPPOrders.isNotEmpty()
        val wcPayIPPOrdersCount = wcPayIPPOrders.size

        val wcPayIPPOrdersInLast30Days = getIppOrdersforLast(STATS_TIME_WINDOW_LENGTH_DAYS)
        val hasIPPOrdersInLast30Days = wcPayIPPOrdersInLast30Days.isNotEmpty()
        val ippOrdersInLast30Days = wcPayIPPOrdersInLast30Days.size

        return when {
            !hasWCPayIPPOrders && cashOnDeliverySettings.isCashOnDeliveryEnabled() -> {
                IPP_NEWBIE_BANNER
            }
            hasIPPOrdersInLast30Days && ippOrdersInLast30Days in IPP_BEGINNER_TRANSACTIONS_RANGE -> {
                IPP_BEGINNER_BANNER
            }
            wcPayIPPOrdersCount > IPP_BEGINNER_TRANSACTIONS_RANGE.last -> {
                IPP_NINJA_BANNER
            }
            else -> {
                null
            }
        }
    }

    private suspend fun getIppOrders(): List<OrderEntity> {
        return orderStore.getOrdersForSite(siteModel).filter { isIPPOrders(it) }
    }

    private suspend fun getIppOrdersforLast(days: Int): List<OrderEntity> {
        val timeWindowStartDate = Date().daysAgo(days)
        return orderStore.getOrdersForSite(siteModel).filter { orderEntity ->
            isIPPOrders(orderEntity) && isIPPOrderDateAfter(orderEntity, timeWindowStartDate)
        }
    }

    private fun isIPPOrderDateAfter(
        it: OrderEntity,
        timeWindowStartDate: Date?
    ) = SimpleDateFormat(
        "yyyy-MM-dd",
        Locale.getDefault()
    ).parse(it.datePaid)?.after(timeWindowStartDate) == true

    private fun isIPPOrders(it: OrderEntity) = it.getMetaDataList().any { wcMetaData ->
        wcMetaData.key == "receipt_url"
    } && it.paymentMethod == WOOCOMMERCE_PAYMENTS_PAYMENT_TYPE

    @Parcelize
    data class IPPFeedbackBanner(
        @StringRes val title: Int,
        @StringRes val message: Int,
        val url: String,
        val campaignName: String,
    ) : Parcelable

    companion object {
        private val IPP_NEWBIE_BANNER by lazy {
            IPPFeedbackBanner(
                R.string.feedback_banner_ipp_title_newbie,
                R.string.feedback_banner_ipp_message_newbie,
                SURVEY_URL_IPP_NEWBIE,
                VALUE_IPP_BANNER_CAMPAIGN_NAME_NEWBIE,
            )
        }

        private val IPP_BEGINNER_BANNER by lazy {
            IPPFeedbackBanner(
                R.string.feedback_banner_ipp_title_beginner,
                R.string.feedback_banner_ipp_message_beginner,
                SURVEY_URL_IPP_BEGINNER,
                VALUE_IPP_BANNER_CAMPAIGN_NAME_BEGINNER,
            )
        }

        private val IPP_NINJA_BANNER by lazy {
            IPPFeedbackBanner(
                R.string.feedback_banner_ipp_title_ninja,
                R.string.feedback_banner_ipp_message_ninja,
                SURVEY_URL_IPP_NINJA,
                VALUE_IPP_BANNER_CAMPAIGN_NAME_NINJA,
            )
        }

        private const val STATS_TIME_WINDOW_LENGTH_DAYS = 30

        private const val SURVEY_URL_IPP_NEWBIE =
            "https://automattic.survey.fm/woo-app-–-cod-survey"
        private const val SURVEY_URL_IPP_BEGINNER =
            "https://automattic.survey.fm/woo-app-–-ipp-first-transaction-survey"
        private const val SURVEY_URL_IPP_NINJA =
            "https://automattic.survey.fm/woo-app-–-ipp-survey-for-power-users"

        private val IPP_BEGINNER_TRANSACTIONS_RANGE = 1..9
    }
}
