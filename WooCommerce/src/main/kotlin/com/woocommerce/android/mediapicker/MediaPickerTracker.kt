package com.woocommerce.android.mediapicker

import com.woocommerce.android.analytics.AnalyticsTracker
import org.wordpress.android.mediapicker.api.Tracker
import javax.inject.Inject

class MediaPickerTracker @Inject constructor() : Tracker {
    override fun track(event: Tracker.Event, properties: Map<String, Any?>) {
        AnalyticsTracker.track(event.toStat(), properties)
    }

    @Suppress("ComplexMethod")
    private fun Tracker.Event.toStat(): AnalyticsTracker.Stat = when (this) {
        Tracker.Event.MEDIA_PERMISSION_GRANTED -> AnalyticsTracker.Stat.APP_PERMISSION_GRANTED
        Tracker.Event.MEDIA_PERMISSION_DENIED -> AnalyticsTracker.Stat.APP_PERMISSION_DENIED
        Tracker.Event.MEDIA_PICKER_PREVIEW_OPENED -> AnalyticsTracker.Stat.MEDIA_PICKER_PREVIEW_OPENED
        Tracker.Event.MEDIA_PICKER_RECENT_MEDIA_SELECTED -> AnalyticsTracker.Stat.MEDIA_PICKER_RECENT_MEDIA_SELECTED
        Tracker.Event.MEDIA_PICKER_OPEN_GIF_LIBRARY -> AnalyticsTracker.Stat.MEDIA_PICKER_OPEN_GIF_LIBRARY
        Tracker.Event.MEDIA_PICKER_OPEN_DEVICE_LIBRARY -> AnalyticsTracker.Stat.MEDIA_PICKER_OPEN_DEVICE_LIBRARY
        Tracker.Event.MEDIA_PICKER_CAPTURE_PHOTO -> AnalyticsTracker.Stat.MEDIA_PICKER_CAPTURE_PHOTO
        Tracker.Event.MEDIA_PICKER_SEARCH_TRIGGERED -> AnalyticsTracker.Stat.MEDIA_PICKER_SEARCH_TRIGGERED
        Tracker.Event.MEDIA_PICKER_SEARCH_EXPANDED -> AnalyticsTracker.Stat.MEDIA_PICKER_SEARCH_EXPANDED
        Tracker.Event.MEDIA_PICKER_SEARCH_COLLAPSED -> AnalyticsTracker.Stat.MEDIA_PICKER_SEARCH_COLLAPSED
        Tracker.Event.MEDIA_PICKER_SHOW_PERMISSIONS_SCREEN -> AnalyticsTracker.Stat.MEDIA_PICKER_SHOW_PERMISSIONS_SCREEN
        Tracker.Event.MEDIA_PICKER_ITEM_SELECTED -> AnalyticsTracker.Stat.MEDIA_PICKER_ITEM_SELECTED
        Tracker.Event.MEDIA_PICKER_ITEM_UNSELECTED -> AnalyticsTracker.Stat.MEDIA_PICKER_ITEM_UNSELECTED
        Tracker.Event.MEDIA_PICKER_SELECTION_CLEARED -> AnalyticsTracker.Stat.MEDIA_PICKER_SELECTION_CLEARED
        Tracker.Event.MEDIA_PICKER_OPENED -> AnalyticsTracker.Stat.MEDIA_PICKER_OPENED
        Tracker.Event.MEDIA_PICKER_OPEN_SYSTEM_PICKER -> AnalyticsTracker.Stat.MEDIA_PICKER_OPEN_SYSTEM_PICKER
        Tracker.Event.MEDIA_PICKER_OPEN_WORDPRESS_MEDIA_LIBRARY_PICKER -> {
            AnalyticsTracker.Stat.MEDIA_PICKER_OPEN_WORDPRESS_MEDIA_LIBRARY_PICKER
        }
    }
}
