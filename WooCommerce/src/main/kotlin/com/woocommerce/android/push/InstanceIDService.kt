package com.woocommerce.android.push

import com.google.firebase.iid.FirebaseInstanceIdService

class InstanceIDService : FirebaseInstanceIdService() {
    /**
     * Called if InstanceID token is updated. This may occur if the security of
     * the previous token had been compromised. Note that this is also called
     * when the InstanceID token is initially generated, so this is where
     * you retrieve the token.
     */
    override fun onTokenRefresh() {
        // Register for Cloud messaging
        FCMRegistrationIntentService.enqueueWork(this)
    }
}
