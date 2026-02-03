package org.wordpress.android.fluxc.action;

import org.wordpress.android.fluxc.annotations.Action;
import org.wordpress.android.fluxc.annotations.ActionEnum;
import org.wordpress.android.fluxc.annotations.action.IAction;
import org.wordpress.android.fluxc.model.notification.NotificationModel;
import org.wordpress.android.fluxc.store.WpComPushNotificationStore.FetchNotificationHashesResponsePayload;
import org.wordpress.android.fluxc.store.WpComPushNotificationStore.FetchNotificationPayload;
import org.wordpress.android.fluxc.store.WpComPushNotificationStore.FetchNotificationResponsePayload;
import org.wordpress.android.fluxc.store.WpComPushNotificationStore.FetchNotificationsPayload;
import org.wordpress.android.fluxc.store.WpComPushNotificationStore.FetchNotificationsResponsePayload;

@ActionEnum
public enum NotificationAction implements IAction {
    // Remote actions
    @Action(payloadType = FetchNotificationsPayload.class)
    FETCH_NOTIFICATIONS, // Fetch notifications
    @Action(payloadType = FetchNotificationPayload.class)
    FETCH_NOTIFICATION, // Fetch a single notification

    // Remote responses
    @Action(payloadType = FetchNotificationHashesResponsePayload.class)
    FETCHED_NOTIFICATION_HASHES, // Response to an internal request to fetch notification hashes for synchronization
    @Action(payloadType = FetchNotificationsResponsePayload.class)
    FETCHED_NOTIFICATIONS, // Response to fetching notifications
    @Action(payloadType = FetchNotificationResponsePayload.class)
    FETCHED_NOTIFICATION, // Response to fetching a single notification

    // Local actions
    @Action(payloadType = NotificationModel.class)
    UPDATE_NOTIFICATION // Save updates to db
}
