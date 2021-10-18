package com.woocommerce.android.tools

import com.woocommerce.android.analytics.AnalyticsTracker
import org.wordpress.android.mediapicker.util.Tracker
import org.wordpress.android.mediapicker.util.Tracker.Event
import javax.inject.Inject

class MediaPickerTracker @Inject constructor() : Tracker {
    override fun track(event: Event, properties: Map<String, Any?>) {
        AnalyticsTracker.track(event.toStat(), properties)
    }

    fun Event.toStat(): AnalyticsTracker.Stat {
        return when (this) {
            Event.MEDIA_PICKER_PREVIEW_OPENED -> AnalyticsTracker.Stat.MEDIA_PICKER_PREVIEW_OPENED
            Event.MEDIA_PICKER_RECENT_MEDIA_SELECTED -> AnalyticsTracker.Stat.MEDIA_PICKER_RECENT_MEDIA_SELECTED
            Event.MEDIA_PICKER_OPEN_GIF_LIBRARY -> AnalyticsTracker.Stat.MEDIA_PICKER_OPEN_GIF_LIBRARY
            Event.MEDIA_PICKER_OPEN_DEVICE_LIBRARY -> AnalyticsTracker.Stat.MEDIA_PICKER_OPEN_DEVICE_LIBRARY
            Event.MEDIA_PICKER_CAPTURE_PHOTO -> AnalyticsTracker.Stat.MEDIA_PICKER_CAPTURE_PHOTO
            Event.MEDIA_PICKER_SEARCH_TRIGGERED -> AnalyticsTracker.Stat.MEDIA_PICKER_SEARCH_TRIGGERED
            Event.MEDIA_PICKER_SEARCH_EXPANDED -> AnalyticsTracker.Stat.MEDIA_PICKER_SEARCH_EXPANDED
            Event.MEDIA_PICKER_SEARCH_COLLAPSED -> AnalyticsTracker.Stat.MEDIA_PICKER_SEARCH_COLLAPSED
            Event.MEDIA_PICKER_SHOW_PERMISSIONS_SCREEN -> AnalyticsTracker.Stat.MEDIA_PICKER_SHOW_PERMISSIONS_SCREEN
            Event.MEDIA_PICKER_ITEM_SELECTED -> AnalyticsTracker.Stat.MEDIA_PICKER_ITEM_SELECTED
            Event.MEDIA_PICKER_ITEM_UNSELECTED -> AnalyticsTracker.Stat.MEDIA_PICKER_ITEM_UNSELECTED
            Event.MEDIA_PICKER_SELECTION_CLEARED -> AnalyticsTracker.Stat.MEDIA_PICKER_SELECTION_CLEARED
            Event.MEDIA_PICKER_OPENED -> AnalyticsTracker.Stat.MEDIA_PICKER_OPENED
            Event.MEDIA_PICKER_OPEN_SYSTEM_PICKER -> AnalyticsTracker.Stat.MEDIA_PICKER_OPEN_SYSTEM_PICKER
        }
    }
}
