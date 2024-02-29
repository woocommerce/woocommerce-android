package com.woocommerce.android.ui.analytics.hub

import com.woocommerce.android.model.AnalyticsCards
import com.woocommerce.android.ui.analytics.hub.informationcard.AnalyticsCardViewState

sealed class AnalyticsHubCardViewState {
    data object LoadingCardsConfiguration : AnalyticsHubCardViewState()
    data class CardsState(val cardsState: Map<AnalyticsCards, AnalyticsCardViewState>) : AnalyticsHubCardViewState()
}
