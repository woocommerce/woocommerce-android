package com.woocommerce.android.push

import com.google.firebase.messaging.FirebaseMessagingService

class InstanceIDService : FirebaseMessagingService() {
    /**
     * Called if InstanceID token is updated. This may occur if the security of
     * the previous token had been compromised. Note that this is also called
     * when the InstanceID token is initially generated, so this is where
     * you retrieve the token.
     */
    // [START refresh_token]
    override fun onNewToken(s: String) {
        super.onNewToken(s)
        FCMRegistrationIntentService.enqueueWork(this)
    }
}
