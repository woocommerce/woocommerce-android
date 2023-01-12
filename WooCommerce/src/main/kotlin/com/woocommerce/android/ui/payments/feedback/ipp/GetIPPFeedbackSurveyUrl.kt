package com.woocommerce.android.ui.payments.feedback.ipp

import com.woocommerce.android.extensions.daysAgo
import com.woocommerce.android.extensions.formatToYYYYmmDD
import com.woocommerce.android.ui.payments.GetActivePaymentsPlugin
import org.jetbrains.annotations.VisibleForTesting
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.WCInPersonPaymentsStore
import java.util.Date
import javax.inject.Inject

class GetIPPFeedbackSurveyUrl @Inject constructor(
    private val shouldShowFeedbackBanner: ShouldShowFeedbackBanner,
    private val ippStore: WCInPersonPaymentsStore,
    private val siteModel: SiteModel,
    private val getActivePaymentsPlugin: GetActivePaymentsPlugin,
) {
    @Throws(IllegalStateException::class)
    suspend fun invoke(): String? {
        if (!shouldShowFeedbackBanner.invoke()) {
            throw IllegalStateException("IPP feedback banner should not be shown to the current user & site.")
        }

        val activePaymentsPlugin =
            getActivePaymentsPlugin.invoke() ?: throw IllegalStateException("No active payments plugin found.")

        val timeWindowStartDate = Date().daysAgo(STATS_TIME_WINDOW_LENGTH_DAYS)
        val response =
            ippStore.fetchTransactionsSummary(activePaymentsPlugin, siteModel, timeWindowStartDate.formatToYYYYmmDD())

        if (response.isError || response.result == null) return null

        val numberOfTransactions = response.result?.transactionsCount ?: return null

        return when (numberOfTransactions) {
            0 -> SURVEY_URL_IPP_NEWBIE
            in 1..10 -> SURVEY_URL_IPP_BEGINNER
            else -> SURVEY_URL_IPP_NINJA
        }
    }

    companion object {
        @VisibleForTesting const val STATS_TIME_WINDOW_LENGTH_DAYS = 30

        // todo: update the survey urls
        @VisibleForTesting const val SURVEY_URL_IPP_NEWBIE = "https://woocommerce.com/newbie"
        @VisibleForTesting const val SURVEY_URL_IPP_BEGINNER = "https://woocommerce.com/beginner"
        @VisibleForTesting const val SURVEY_URL_IPP_NINJA = "https://woocommerce.com/ninja"
    }
}
