package com.woocommerce.android.cardreader.internal.temporary

import retrofit2.Call
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

/**
 * This is a temporary class which will be removed when we implement the endpoints in FluxC.
 */
interface ApiEndpoints {
    @POST("connection_token")
    fun getConnectionToken(): Call<ConnectionToken>

    @FormUrlEncoded
    @POST("capture_payment_intent")
    fun capturePaymentIntent(@Field("payment_intent_id") id: String): Call<Void>
}
