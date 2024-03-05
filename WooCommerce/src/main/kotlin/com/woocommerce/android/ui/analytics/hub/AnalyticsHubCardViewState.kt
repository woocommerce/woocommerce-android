package com.woocommerce.android.ui.analytics.hub

sealed class AnalyticsHubCardViewState {
    data object LoadingCardsConfiguration : AnalyticsHubCardViewState()
    data class CardsState(val cardsState: List<AnalyticsCardViewState>) : AnalyticsHubCardViewState()
}
