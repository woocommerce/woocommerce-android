package com.woocommerce.android.ui.payments.feedback.ipp

import android.os.Parcelable
import androidx.annotation.StringRes
import com.woocommerce.android.R
import com.woocommerce.android.extensions.daysAgo
import com.woocommerce.android.extensions.formatToYYYYmmDD
import com.woocommerce.android.ui.payments.GetActivePaymentsPlugin
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.payments.inperson.WCPaymentTransactionsSummaryResult
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.store.WCInPersonPaymentsStore
import java.util.Date
import javax.inject.Inject

class GetIPPFeedbackBannerData @Inject constructor(
    private val shouldShowFeedbackBanner: ShouldShowFeedbackBanner,
    private val ippStore: WCInPersonPaymentsStore,
    private val siteModel: SiteModel,
    private val getActivePaymentsPlugin: GetActivePaymentsPlugin,
) {
    @Suppress("ReturnCount", "MaxLineLength")
    suspend operator fun invoke(): IPPFeedbackBanner {
        requireShouldShowFeedbackBanner()

        val activePaymentsPlugin = checkNotNull(getActivePaymentsPlugin())

        if (activePaymentsPlugin != WCInPersonPaymentsStore.InPersonPaymentsPluginType.WOOCOMMERCE_PAYMENTS) {
            return IPP_NEWBIE_BANNER
        }

        val timeWindowStartDate = Date().daysAgo(STATS_TIME_WINDOW_LENGTH_DAYS)
        val response = ippStore.fetchTransactionsSummary(
            activePaymentsPlugin,
            siteModel,
            timeWindowStartDate.formatToYYYYmmDD()
        )

        requireSuccessfulTransactionsSummaryResponse(response)

        val numberOfTransactions = requireTransactionsCount(response)

        requirePositiveNumberOfTransactions(numberOfTransactions)

        return when (numberOfTransactions) {
            0 -> if (hasUserEverMadeIppTransaction()) {
                IPP_BEGINNER_BANNER
            } else {
                IPP_NEWBIE_BANNER
            }
            in IPP_BEGINNER_TRANSACTIONS_RANGE -> IPP_BEGINNER_BANNER
            else -> IPP_NINJA_BANNER
        }
    }

    private fun requireTransactionsCount(response: WooPayload<WCPaymentTransactionsSummaryResult>): Int {
        return checkNotNull(response.result?.transactionsCount) { "Transactions count must not be null" }
    }

    private fun requireSuccessfulTransactionsSummaryResponse(response: WooPayload<WCPaymentTransactionsSummaryResult>) {
        if (response.isError || response.result == null) {
            throw IllegalStateException("Failed to fetch transactions summary")
        }
    }

    private fun requirePositiveNumberOfTransactions(numberOfTransactions: Int) {
        if (numberOfTransactions < 0) throw IllegalStateException("Number of transactions should be positive.")
    }

    private suspend fun requireShouldShowFeedbackBanner() {
        if (!shouldShowFeedbackBanner()) {
            throw IllegalStateException("IPP feedback banner should not be shown to the current user & site.")
        }
    }

    private suspend fun hasUserEverMadeIppTransaction(): Boolean {
        val activePaymentsPlugin = checkNotNull(getActivePaymentsPlugin())

        if (activePaymentsPlugin != WCInPersonPaymentsStore.InPersonPaymentsPluginType.WOOCOMMERCE_PAYMENTS) {
            return false
        }

        val response = ippStore.fetchTransactionsSummary(activePaymentsPlugin, siteModel)

        requireSuccessfulTransactionsSummaryResponse(response)

        val numberOfTransactions = requireTransactionsCount(response)

        requirePositiveNumberOfTransactions(numberOfTransactions)

        return numberOfTransactions > 0
    }

    @Parcelize
    data class IPPFeedbackBanner(
        @StringRes val title: Int,
        @StringRes val message: Int,
        val url: String
    ) : Parcelable

    companion object {
        private val IPP_NEWBIE_BANNER by lazy {
            IPPFeedbackBanner(BANNER_TITLE_NEWBIE, BANNER_MESSAGE_NEWBIE, SURVEY_URL_IPP_NEWBIE)
        }

        private val IPP_BEGINNER_BANNER by lazy {
            IPPFeedbackBanner(
                BANNER_TITLE_BEGINNER,
                BANNER_MESSAGE_BEGINNER,
                SURVEY_URL_IPP_BEGINNER
            )
        }

        private val IPP_NINJA_BANNER by lazy {
            IPPFeedbackBanner(BANNER_TITLE_NINJA, BANNER_MESSAGE_NINJA, SURVEY_URL_IPP_NINJA)
        }

        private const val STATS_TIME_WINDOW_LENGTH_DAYS = 30

        private const val SURVEY_URL_IPP_NEWBIE = "https://automattic.survey.fm/woo-app-–-cod-survey"

        private const val SURVEY_URL_IPP_BEGINNER =
            "https://automattic.survey.fm/woo-app-–-ipp-first-transaction-survey"

        private const val SURVEY_URL_IPP_NINJA = "https://automattic.survey.fm/woo-app-–-ipp-survey-for-power-users"

        private const val BANNER_TITLE_NEWBIE = R.string.feedback_banner_ipp_title_newbie

        private const val BANNER_TITLE_BEGINNER = R.string.feedback_banner_ipp_title_beginner

        private const val BANNER_TITLE_NINJA = R.string.feedback_banner_ipp_title_ninja

        private const val BANNER_MESSAGE_NEWBIE = R.string.feedback_banner_ipp_message_newbie

        private const val BANNER_MESSAGE_BEGINNER = R.string.feedback_banner_ipp_message_beginner

        private const val BANNER_MESSAGE_NINJA = R.string.feedback_banner_ipp_message_ninja

        private val IPP_BEGINNER_TRANSACTIONS_RANGE = 1..10
    }
}
