package com.woocommerce.android.push

import com.google.firebase.messaging.FirebaseMessagingService

/**
 * A dummy implementation of the FCMMessageService.
 * This is needed to avoid a race condition between Hilt's test initialization, and the injection
 * done by @AndroidEntryPoint in the service when receiving a new token.
 */
class FCMMessageService : FirebaseMessagingService()
