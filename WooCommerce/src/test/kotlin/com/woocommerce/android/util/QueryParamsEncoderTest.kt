package com.woocommerce.android.util

import com.woocommerce.android.BuildConfig
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.net.URLEncoder

class QueryParamsEncoderTest {
    private val queryParramsEncoder = QueryParamsEncoder()
    @Test
    fun `when getEncodedQueryParams called, then proper encoded query params returned`() {
        // WHEN
        val encoderQueryParams = queryParramsEncoder.getEncodedQueryParams()

        // THEN
        if (BuildConfig.DEBUG) {
            assertThat(encoderQueryParams).isEqualTo(
                URLEncoder.encode(
                    "build_type=developer&platform=android&version=${BuildConfig.VERSION_NAME}",
                    Charsets.UTF_8.name()
                )
            )
        } else {
            assertThat(encoderQueryParams).isEqualTo(
                URLEncoder.encode(
                    "platform=android&version=${BuildConfig.VERSION_NAME}",
                    Charsets.UTF_8.name()
                )
            )
        }
    }
}
