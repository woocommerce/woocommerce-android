package com.woocommerce.android.ui.payments.feedback.ipp

import android.os.Parcelable
import androidx.annotation.StringRes
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_IPP_BANNER_CAMPAIGN_NAME_BEGINNER
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_IPP_BANNER_CAMPAIGN_NAME_NEWBIE
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_IPP_BANNER_CAMPAIGN_NAME_NINJA
import com.woocommerce.android.extensions.daysAgo
import com.woocommerce.android.extensions.formatToYYYYmmDD
import com.woocommerce.android.ui.payments.GetActivePaymentsPlugin
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.payments.inperson.WCPaymentTransactionsSummaryResult
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.store.WCInPersonPaymentsStore
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.util.AppLog
import java.util.Date
import javax.inject.Inject

class GetIPPFeedbackBannerData @Inject constructor(
    private val shouldShowFeedbackBanner: ShouldShowFeedbackBanner,
    private val ippStore: WCInPersonPaymentsStore,
    private val siteModel: SiteModel,
    private val getActivePaymentsPlugin: GetActivePaymentsPlugin,
    private val logger: AppLogWrapper,
) {
    @Suppress("ReturnCount", "MaxLineLength")
    suspend operator fun invoke(): IPPFeedbackBanner? {
        if (!shouldShowFeedbackBanner()) return null

        val activePaymentsPlugin = getActivePaymentsPlugin()

        if (activePaymentsPlugin != WCInPersonPaymentsStore.InPersonPaymentsPluginType.WOOCOMMERCE_PAYMENTS) {
            return IPP_NEWBIE_BANNER
        }

        val timeWindowStartDate = Date().daysAgo(STATS_TIME_WINDOW_LENGTH_DAYS)
        val response = ippStore.fetchTransactionsSummary(
            activePaymentsPlugin,
            siteModel,
            timeWindowStartDate.formatToYYYYmmDD()
        )

        if (!response.isSuccessful()) {
            logger.e(AppLog.T.API, "Error fetching transactions summary: ${response.error}")
            return IPP_NEWBIE_BANNER
        }

        val numberOfTransactionsInLast30Days = response.result?.transactionsCount
        if (numberOfTransactionsInLast30Days == null || numberOfTransactionsInLast30Days < 0) {
            logger.e(AppLog.T.API, "Transactions count data is not")
            return null
        }

        return when (numberOfTransactionsInLast30Days) {
            0 -> if (hasUserEverMadeIppTransaction()) {
                IPP_BEGINNER_BANNER
            } else {
                IPP_NEWBIE_BANNER
            }
            in IPP_BEGINNER_TRANSACTIONS_RANGE -> IPP_BEGINNER_BANNER
            else -> IPP_NINJA_BANNER
        }
    }

    private fun WooPayload<WCPaymentTransactionsSummaryResult>.isSuccessful(): Boolean {
        return !isError && result != null
    }

    @Suppress("ReturnCount")
    private suspend fun hasUserEverMadeIppTransaction(): Boolean {
        val activePaymentsPlugin = checkNotNull(getActivePaymentsPlugin())

        if (activePaymentsPlugin != WCInPersonPaymentsStore.InPersonPaymentsPluginType.WOOCOMMERCE_PAYMENTS) {
            return false
        }

        val response = ippStore.fetchTransactionsSummary(activePaymentsPlugin, siteModel)

        if (!response.isSuccessful()) {
            logger.e(AppLog.T.API, "Error fetching transactions summary: ${response.error.message}")
            return false
        }

        val numberOfTransactions = response.result?.transactionsCount ?: return false

        return numberOfTransactions > 0
    }

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
                BANNER_TITLE_NEWBIE,
                BANNER_MESSAGE_NEWBIE,
                SURVEY_URL_IPP_NEWBIE,
                VALUE_IPP_BANNER_CAMPAIGN_NAME_NEWBIE,
            )
        }

        private val IPP_BEGINNER_BANNER by lazy {
            IPPFeedbackBanner(
                BANNER_TITLE_BEGINNER,
                BANNER_MESSAGE_BEGINNER,
                SURVEY_URL_IPP_BEGINNER,
                VALUE_IPP_BANNER_CAMPAIGN_NAME_BEGINNER,
            )
        }

        private val IPP_NINJA_BANNER by lazy {
            IPPFeedbackBanner(
                BANNER_TITLE_NINJA,
                BANNER_MESSAGE_NINJA,
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

        private const val BANNER_TITLE_NEWBIE = R.string.feedback_banner_ipp_title_newbie
        private const val BANNER_TITLE_BEGINNER = R.string.feedback_banner_ipp_title_beginner
        private const val BANNER_TITLE_NINJA = R.string.feedback_banner_ipp_title_ninja

        private const val BANNER_MESSAGE_NEWBIE = R.string.feedback_banner_ipp_message_newbie
        private const val BANNER_MESSAGE_BEGINNER = R.string.feedback_banner_ipp_message_beginner
        private const val BANNER_MESSAGE_NINJA = R.string.feedback_banner_ipp_message_ninja

        private val IPP_BEGINNER_TRANSACTIONS_RANGE = 1..10
    }
}
