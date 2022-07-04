package com.woocommerce.android.mediapicker

import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import org.wordpress.android.mediapicker.api.Tracker
import javax.inject.Inject

class MediaPickerTracker @Inject constructor() : Tracker {
    override fun track(event: Tracker.Event, properties: Map<String, Any?>) {
        AnalyticsTracker.track(event.toStat(), properties)
    }

    @Suppress("ComplexMethod")
    private fun Tracker.Event.toStat(): AnalyticsEvent = when (this) {
        Tracker.Event.MEDIA_PERMISSION_GRANTED -> AnalyticsEvent.APP_PERMISSION_GRANTED
        Tracker.Event.MEDIA_PERMISSION_DENIED -> AnalyticsEvent.APP_PERMISSION_DENIED
        Tracker.Event.MEDIA_PICKER_PREVIEW_OPENED -> AnalyticsEvent.MEDIA_PICKER_PREVIEW_OPENED
        Tracker.Event.MEDIA_PICKER_RECENT_MEDIA_SELECTED -> AnalyticsEvent.MEDIA_PICKER_RECENT_MEDIA_SELECTED
        Tracker.Event.MEDIA_PICKER_OPEN_GIF_LIBRARY -> AnalyticsEvent.MEDIA_PICKER_OPEN_GIF_LIBRARY
        Tracker.Event.MEDIA_PICKER_OPEN_DEVICE_LIBRARY -> AnalyticsEvent.MEDIA_PICKER_OPEN_DEVICE_LIBRARY
        Tracker.Event.MEDIA_PICKER_CAPTURE_PHOTO -> AnalyticsEvent.MEDIA_PICKER_CAPTURE_PHOTO
        Tracker.Event.MEDIA_PICKER_SEARCH_TRIGGERED -> AnalyticsEvent.MEDIA_PICKER_SEARCH_TRIGGERED
        Tracker.Event.MEDIA_PICKER_SEARCH_EXPANDED -> AnalyticsEvent.MEDIA_PICKER_SEARCH_EXPANDED
        Tracker.Event.MEDIA_PICKER_SEARCH_COLLAPSED -> AnalyticsEvent.MEDIA_PICKER_SEARCH_COLLAPSED
        Tracker.Event.MEDIA_PICKER_SHOW_PERMISSIONS_SCREEN -> AnalyticsEvent.MEDIA_PICKER_SHOW_PERMISSIONS_SCREEN
        Tracker.Event.MEDIA_PICKER_ITEM_SELECTED -> AnalyticsEvent.MEDIA_PICKER_ITEM_SELECTED
        Tracker.Event.MEDIA_PICKER_ITEM_UNSELECTED -> AnalyticsEvent.MEDIA_PICKER_ITEM_UNSELECTED
        Tracker.Event.MEDIA_PICKER_SELECTION_CLEARED -> AnalyticsEvent.MEDIA_PICKER_SELECTION_CLEARED
        Tracker.Event.MEDIA_PICKER_OPENED -> AnalyticsEvent.MEDIA_PICKER_OPENED
        Tracker.Event.MEDIA_PICKER_OPEN_SYSTEM_PICKER -> AnalyticsEvent.MEDIA_PICKER_OPEN_SYSTEM_PICKER
        Tracker.Event.MEDIA_PICKER_OPEN_WORDPRESS_MEDIA_LIBRARY_PICKER -> {
            AnalyticsEvent.MEDIA_PICKER_OPEN_WORDPRESS_MEDIA_LIBRARY_PICKER
        }
    }
}
