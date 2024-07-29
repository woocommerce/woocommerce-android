package com.woocommerce.android.ui.analytics.hub

import com.woocommerce.android.model.AnalyticsCards

sealed class AnalyticsHubCardViewState(
    open val cardsState: List<AnalyticsCardViewState>
) {
    /**
    A default Loading configuration for when the Analytics Hub has started
    but the AnalyticCardConfiguration list is still unknown.
     */
    data object LoadingCardsConfiguration : AnalyticsHubCardViewState(
        cardsState = listOf(
            AnalyticsHubInformationViewState.LoadingViewState(AnalyticsCards.Revenue),
            AnalyticsHubInformationViewState.LoadingViewState(AnalyticsCards.Orders),
            AnalyticsHubListViewState.LoadingViewState(AnalyticsCards.Products),
        )
    )
    data class CardsState(
        override val cardsState: List<AnalyticsCardViewState>
    ) : AnalyticsHubCardViewState(cardsState)
}
