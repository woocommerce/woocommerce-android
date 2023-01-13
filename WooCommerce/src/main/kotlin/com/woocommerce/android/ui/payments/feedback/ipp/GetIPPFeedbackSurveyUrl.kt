package com.woocommerce.android.ui.payments.feedback.ipp

import com.woocommerce.android.extensions.daysAgo
import com.woocommerce.android.extensions.formatToYYYYmmDD
import com.woocommerce.android.ui.payments.GetActivePaymentsPlugin
import org.jetbrains.annotations.VisibleForTesting
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.payments.inperson.WCPaymentTransactionsSummaryResult
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.store.WCInPersonPaymentsStore
import java.util.Date
import javax.inject.Inject

class GetIPPFeedbackSurveyUrl @Inject constructor(
    private val shouldShowFeedbackBanner: ShouldShowFeedbackBanner,
    private val ippStore: WCInPersonPaymentsStore,
    private val siteModel: SiteModel,
    private val getActivePaymentsPlugin: GetActivePaymentsPlugin,
) {
    @Suppress("ReturnCount")
    @Throws(IllegalStateException::class)
    suspend operator fun invoke(): String {
        requireShouldShowFeedbackBanner()

        val activePaymentsPlugin = requireWooCommercePaymentsPlugin()

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
            0 -> SURVEY_URL_IPP_NEWBIE
            in IPP_BEGINNER_TRANSACTIONS_RANGE -> SURVEY_URL_IPP_BEGINNER
            else -> SURVEY_URL_IPP_NINJA
        }
    }

    private fun requireTransactionsCount(response: WooPayload<WCPaymentTransactionsSummaryResult>): Int {
        return response.result?.transactionsCount ?: throw IllegalStateException("Transactions count must not be null")
    }

    private fun requireSuccessfulTransactionsSummaryResponse(response: WooPayload<WCPaymentTransactionsSummaryResult>) {
        if (response.isError || response.result == null) {
            throw IllegalStateException("Failed to fetch transactions summary")
        }
    }

    private fun requirePositiveNumberOfTransactions(numberOfTransactions: Int) {
        if (numberOfTransactions < 0) throw IllegalStateException("Number of transactions should be positive.")
    }

    private suspend fun requireWooCommercePaymentsPlugin(): WCInPersonPaymentsStore.InPersonPaymentsPluginType =
        getActivePaymentsPlugin() ?: throw IllegalStateException("No active payments plugin found.")

    private suspend fun requireShouldShowFeedbackBanner() {
        if (!shouldShowFeedbackBanner()) {
            throw IllegalStateException("IPP feedback banner should not be shown to the current user & site.")
        }
    }

    companion object {
        @VisibleForTesting
        const val STATS_TIME_WINDOW_LENGTH_DAYS = 30

        @VisibleForTesting
        const val SURVEY_URL_IPP_NEWBIE = "https://woocommerce.com/newbie"

        @VisibleForTesting
        const val SURVEY_URL_IPP_BEGINNER = "https://woocommerce.com/beginner"

        @VisibleForTesting
        const val SURVEY_URL_IPP_NINJA = "https://woocommerce.com/ninja"

        private val IPP_BEGINNER_TRANSACTIONS_RANGE = 1..10
    }
}
