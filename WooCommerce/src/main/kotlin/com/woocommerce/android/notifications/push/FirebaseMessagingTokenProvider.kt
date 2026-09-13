package com.woocommerce.android.notifications.push

import com.google.firebase.Firebase
import com.google.firebase.messaging.messaging
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class FirebaseMessagingTokenProvider @Inject constructor() {
    suspend fun getToken(): String = Firebase.messaging.token.await()
}
