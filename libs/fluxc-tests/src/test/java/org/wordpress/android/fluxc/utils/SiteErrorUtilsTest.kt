package org.wordpress.android.fluxc.utils

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType
import org.wordpress.android.fluxc.network.UnexpectedStoreResponse
import org.wordpress.android.fluxc.network.UnexpectedStoreResponseKind
import org.wordpress.android.fluxc.store.SiteStore.SiteErrorType

class SiteErrorUtilsTest {
    @Test
    fun `given an error with unexpected response details, when converting to a site error, then keep the details`() {
        val unexpectedStoreResponse = UnexpectedStoreResponse(
            kind = UnexpectedStoreResponseKind.UNEXPECTED_CONTENT,
            statusCode = 200,
            contentType = "text/html",
            requestType = "GET /wp-json",
            excerpt = "Just a moment..."
        )
        val error = BaseNetworkError(GenericErrorType.PARSE_ERROR).apply {
            this.unexpectedStoreResponse = unexpectedStoreResponse
        }

        val siteError = SiteErrorUtils.genericToSiteError(error)

        assertThat(siteError.type).isEqualTo(SiteErrorType.GENERIC_ERROR)
        assertThat(siteError.unexpectedStoreResponse).isEqualTo(unexpectedStoreResponse)
    }
}
