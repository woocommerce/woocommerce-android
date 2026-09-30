package org.wordpress.android.fluxc.network.rest.wpapi

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class WPAPISuccessListenerTest {
    @Test
    fun `given a status code, when the response is delivered, then the success keeps it`() {
        var success: WPAPIResponse.Success<String>? = null

        wpApiSuccessListener<String> { success = it }.onResponse("data", emptyList(), 201)

        assertThat(success).isEqualTo(WPAPIResponse.Success("data", emptyList(), statusCode = 201))
    }

    @Test
    fun `given no status code, when the response is delivered, then the success has none`() {
        var success: WPAPIResponse.Success<String>? = null

        wpApiSuccessListener<String> { success = it }.onResponse("data", emptyList())

        assertThat(success?.statusCode).isNull()
    }
}
