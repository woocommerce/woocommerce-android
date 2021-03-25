/*  The MIT License

    Copyright (c) 2018- Stripe, Inc. (https://stripe.com)

    Permission is hereby granted, free of charge, to any person obtaining a copy
    of this software and associated documentation files (the "Software"), to deal
    in the Software without restriction, including without limitation the rights
    to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
    copies of the Software, and to permit persons to whom the Software is
    furnished to do so, subject to the following conditions:

    The above copyright notice and this permission notice shall be included in
    all copies or substantial portions of the Software.

    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
    IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
    FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
    AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
    LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
    OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
    THE SOFTWARE.
*/
package com.woocommerce.android.cardreader.internal.temporary

import com.woocommerce.android.cardreader.CardReaderStore
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException

/**
 * This is a temporary class which will be removed when we implement the endpoints in FluxC.
 * Note:  This code has been copied (https://github.com/stripe/stripe-terminal-android) and modified.
 * It will be removed as soon as we implement the endpoints in FluxC.
 */
internal class CardReaderStoreImpl : CardReaderStore {
    private val backendUrl = "http://0.0.0.0:4567"

    private val client = OkHttpClient.Builder()
        .build()
    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(backendUrl)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    private val service: ApiEndpoints = retrofit.create(ApiEndpoints::class.java)

    override suspend fun getConnectionToken(): String {
        try {
            val result = service.getConnectionToken().execute()
            if (result.isSuccessful && result.body() != null) {
                return result.body()!!.secret
            } else {
                throw RuntimeException("Creating connection token failed")
            }
        } catch (e: IOException) {
            throw RuntimeException("Creating connection token failed", e)
        }
    }

    override suspend fun capturePaymentIntent(id: String): Boolean =
        service.capturePaymentIntent(id).execute().isSuccessful
}
