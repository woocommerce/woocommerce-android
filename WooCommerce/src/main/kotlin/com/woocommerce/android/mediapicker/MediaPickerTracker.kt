package com.woocommerce.android.mediapicker

import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.mediapicker.api.Tracker
import org.wordpress.android.mediapicker.api.Tracker.Event.MEDIA_PERMISSION_DENIED
import org.wordpress.android.mediapicker.api.Tracker.Event.MEDIA_PERMISSION_GRANTED
import org.wordpress.android.mediapicker.api.Tracker.Event.MEDIA_PICKER_CAPTURE_PHOTO
import org.wordpress.android.mediapicker.api.Tracker.Event.MEDIA_PICKER_ITEM_SELECTED
import org.wordpress.android.mediapicker.api.Tracker.Event.MEDIA_PICKER_ITEM_UNSELECTED
import org.wordpress.android.mediapicker.api.Tracker.Event.MEDIA_PICKER_OPENED
import org.wordpress.android.mediapicker.api.Tracker.Event.MEDIA_PICKER_OPEN_DEVICE_LIBRARY
import org.wordpress.android.mediapicker.api.Tracker.Event.MEDIA_PICKER_OPEN_GIF_LIBRARY
import org.wordpress.android.mediapicker.api.Tracker.Event.MEDIA_PICKER_OPEN_SYSTEM_PICKER
import org.wordpress.android.mediapicker.api.Tracker.Event.MEDIA_PICKER_OPEN_WORDPRESS_MEDIA_LIBRARY_PICKER
import org.wordpress.android.mediapicker.api.Tracker.Event.MEDIA_PICKER_PREVIEW_OPENED
import org.wordpress.android.mediapicker.api.Tracker.Event.MEDIA_PICKER_RECENT_MEDIA_SELECTED
import org.wordpress.android.mediapicker.api.Tracker.Event.MEDIA_PICKER_SEARCH_COLLAPSED
import org.wordpress.android.mediapicker.api.Tracker.Event.MEDIA_PICKER_SEARCH_EXPANDED
import org.wordpress.android.mediapicker.api.Tracker.Event.MEDIA_PICKER_SEARCH_TRIGGERED
import org.wordpress.android.mediapicker.api.Tracker.Event.MEDIA_PICKER_SELECTION_CLEARED
import org.wordpress.android.mediapicker.api.Tracker.Event.MEDIA_PICKER_SHOW_PERMISSIONS_SCREEN
import javax.inject.Inject

class MediaPickerTracker @Inject constructor() : Tracker {
    override fun track(event: Tracker.Event, properties: Map<String, Any?>) {
        AnalyticsTracker.track(event.toStat(), properties)
    }

    @Suppress("ComplexMethod")
    private fun Tracker.Event.toStat(): Stat = when (this) {
        MEDIA_PICKER_PREVIEW_OPENED -> Stat.MEDIA_PICKER_PREVIEW_OPENED
        MEDIA_PICKER_RECENT_MEDIA_SELECTED -> Stat.MEDIA_PICKER_RECENT_MEDIA_SELECTED
        MEDIA_PICKER_OPEN_GIF_LIBRARY -> Stat.MEDIA_PICKER_OPEN_GIF_LIBRARY
        MEDIA_PICKER_OPEN_DEVICE_LIBRARY -> Stat.MEDIA_PICKER_OPEN_DEVICE_LIBRARY
        MEDIA_PICKER_CAPTURE_PHOTO -> Stat.MEDIA_PICKER_CAPTURE_PHOTO
        MEDIA_PICKER_SEARCH_TRIGGERED -> Stat.MEDIA_PICKER_SEARCH_TRIGGERED
        MEDIA_PICKER_SEARCH_EXPANDED -> Stat.MEDIA_PICKER_SEARCH_EXPANDED
        MEDIA_PICKER_SEARCH_COLLAPSED -> Stat.MEDIA_PICKER_SEARCH_COLLAPSED
        MEDIA_PICKER_SHOW_PERMISSIONS_SCREEN -> Stat.MEDIA_PICKER_SHOW_PERMISSIONS_SCREEN
        MEDIA_PICKER_ITEM_SELECTED -> Stat.MEDIA_PICKER_ITEM_SELECTED
        MEDIA_PICKER_ITEM_UNSELECTED -> Stat.MEDIA_PICKER_ITEM_UNSELECTED
        MEDIA_PICKER_SELECTION_CLEARED -> Stat.MEDIA_PICKER_SELECTION_CLEARED
        MEDIA_PICKER_OPENED -> Stat.MEDIA_PICKER_OPENED
        MEDIA_PICKER_OPEN_SYSTEM_PICKER -> Stat.MEDIA_PICKER_OPEN_SYSTEM_PICKER
        MEDIA_PERMISSION_GRANTED -> Stat.APP_PERMISSION_GRANTED
        MEDIA_PERMISSION_DENIED -> Stat.APP_PERMISSION_DENIED
        MEDIA_PICKER_OPEN_WORDPRESS_MEDIA_LIBRARY_PICKER -> TODO()
    }
}
