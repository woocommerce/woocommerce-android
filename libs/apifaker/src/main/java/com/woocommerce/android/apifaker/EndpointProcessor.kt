package com.woocommerce.android.apifaker

import com.woocommerce.android.apifaker.db.EndpointDao
import com.woocommerce.android.apifaker.models.FakeResponse
import okhttp3.Request
import okio.Buffer
import javax.inject.Inject

internal class EndpointProcessor @Inject constructor(
    private val endpointDao: EndpointDao
) {
    fun fakeRequestIfNeeded(request: Request): FakeResponse? {
        // TODO match against WPApi/WPCom, and extract path accordingly
        val path = request.url.encodedPath
        val requestBody = request.body
        val bodyContent = if (requestBody != null) {
            val buffer = Buffer()
            requestBody.writeTo(buffer)

            buffer.readUtf8()
        } else ""


        return endpointDao.queryEndpoint(path, bodyContent)?.response
    }
}
