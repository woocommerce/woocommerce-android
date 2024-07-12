package com.woocommerce.android.ui.analytics.hub
import com.woocommerce.android.model.AnalyticsCards
import com.woocommerce.android.ui.analytics.hub.informationcard.AnalyticsHubInformationSectionViewState
import com.woocommerce.android.ui.analytics.hub.listcard.AnalyticsHubListCardItemViewState

sealed interface AnalyticsCardViewState {
    val card: AnalyticsCards
}

sealed class AnalyticsHubInformationViewState : AnalyticsCardViewState {
    data class LoadingViewState(override val card: AnalyticsCards) : AnalyticsHubInformationViewState()
    data class NoDataState(
        override val card: AnalyticsCards,
        val title: String,
        val message: String
    ) : AnalyticsHubInformationViewState()

    data class NoSupportedState(
        override val card: AnalyticsCards,
        val title: String,
        val message: String,
        val description: String
    ) : AnalyticsHubInformationViewState()
    data class DataViewState(
        override val card: AnalyticsCards,
        val title: String,
        val leftSection: AnalyticsHubInformationSectionViewState,
        val rightSection: AnalyticsHubInformationSectionViewState,
        val reportUrl: String?
    ) : AnalyticsHubInformationViewState()
}

sealed class AnalyticsHubListViewState : AnalyticsCardViewState {
    data class LoadingViewState(override val card: AnalyticsCards) : AnalyticsHubListViewState()
    data class NoDataState(override val card: AnalyticsCards, val message: String) : AnalyticsHubListViewState()
    data class DataViewState(
        override val card: AnalyticsCards,
        val title: String,
        val subTitle: String,
        val subTitleValue: String,
        val delta: Int?,
        val listLeftHeader: String,
        val listRightHeader: String,
        val items: List<AnalyticsHubListCardItemViewState>,
        val reportUrl: String?
    ) : AnalyticsHubListViewState() {
        val sign: String
            get() = when {
                delta == null -> ""
                delta == 0 -> ""
                delta > 0 -> "+"
                else -> "-"
            }
    }
}

sealed class AnalyticsHubCustomSelectionListViewState : AnalyticsCardViewState {
    data class LoadingAdsViewState(override val card: AnalyticsCards) : AnalyticsHubCustomSelectionListViewState()
    data class NoAdsState(
        override val card: AnalyticsCards,
        val message: String
    ) : AnalyticsHubCustomSelectionListViewState()
    data class DataViewState(
        override val card: AnalyticsCards
    ) : AnalyticsHubCustomSelectionListViewState()
}
