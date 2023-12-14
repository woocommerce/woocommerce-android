package com.woocommerce.android.e2e.screens.notifications

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.woocommerce.android.R
import com.woocommerce.android.e2e.helpers.util.Screen
import com.woocommerce.android.e2e.screens.TabNavComponent
import com.woocommerce.android.model.Notification
import com.woocommerce.android.notifications.NotificationChannelType
import com.woocommerce.android.notifications.WooNotificationBuilder
import com.woocommerce.android.notifications.WooNotificationType.NEW_ORDER

/**
 * This is not a screen per-se, as it shows the notification drawer with a push notification.
 * This is why we provide an [R.id.dashboard] as the [elementID].
 */
class NotificationsScreen(private val wooNotificationBuilder: WooNotificationBuilder) :
    Screen(R.id.dashboard) {
    init {
        displayNotification()
        openNotificationShade()
    }

    private fun displayNotification() {
        wooNotificationBuilder.buildAndDisplayWooNotification(
            0,
            notification = Notification(
                noteId = 1,
                uniqueId = 1L,
                remoteNoteId = 1L,
                remoteSiteId = 1L,
                icon = "https://s.wp.com/wp-content/mu-plugins/notes/images/update-payment-2x.png",
                noteTitle = getTranslatedString(R.string.tests_notification_new_order_title),
                noteMessage = getTranslatedString(R.string.tests_notification_new_order_message),
                noteType = NEW_ORDER,
                channelType = NotificationChannelType.NEW_ORDER
            ),
            isGroupNotification = false
        )
    }

    private fun openNotificationShade() {
        val uiDevice: UiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        uiDevice.openNotification()
        idleFor(1000)
    }

    fun goBackToApp(): TabNavComponent {
        val uiDevice: UiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        uiDevice.pressBack()

        return TabNavComponent()
    }
}
