package com.woocommerce.android.analytics

import com.automattic.eventhorizon.Trackable

/**
 * Adapts EventHorizon-generated [Trackable] events to the app's [IAnalyticsEvent] interface,
 * so they can flow through [AnalyticsTracker] and the existing Tracks pipeline.
 */
class EventHorizonAnalyticsEvent(trackable: Trackable) : IAnalyticsEvent {
    override val siteless: Boolean = false
    override val isPosEvent: Boolean = false
    override val name: String = trackable.analyticsName
}
