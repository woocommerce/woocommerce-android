package org.wordpress.android.fluxc.network.rest.wpcom.wc.product.pos

import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.generated.endpoint.WOOCOMMERCE
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.pos.WooPosGenerateCatalogResponse
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooNetwork

class WooPosProductRestClientTest {
    private val wooNetwork: WooNetwork = mock()
    private val sut = WooPosProductRestClient(wooNetwork)
    private val site = SiteModel()

    @Test
    fun `given site is in -6 timezone, when adjusting timestamp, then time is shifted backwards`() {
        // WHEN
        val result = sut.adjustUtcToSiteLocalTime("2024-01-15T06:30:00", "-6")

        // THEN
        assertThat(result).isEqualTo("2024-01-15T00:30:00")
    }

    @Test
    fun `given site is in +5,5 timezone, when adjusting timestamp, then time is shifted forward`() {
        // WHEN
        val result = sut.adjustUtcToSiteLocalTime("2024-01-15T06:30:00", "5.5")

        // THEN
        assertThat(result).isEqualTo("2024-01-15T12:00:00")
    }

    @Test
    fun `given zero UTC offset, when adjusting timestamp, then time is unchanged`() {
        // WHEN
        val result = sut.adjustUtcToSiteLocalTime("2024-01-15T06:30:00", "0")

        // THEN
        assertThat(result).isEqualTo("2024-01-15T06:30:00")
    }

    @Test
    fun `given null offset, when adjusting timestamp, then time is unchanged`() {
        // WHEN
        val result = sut.adjustUtcToSiteLocalTime("2024-01-15T06:30:00", null)

        // THEN
        assertThat(result).isEqualTo("2024-01-15T06:30:00")
    }

    @Test
    fun `given non-numeric offset, when adjusting timestamp, then time is unchanged`() {
        // WHEN
        val result = sut.adjustUtcToSiteLocalTime("2024-01-15T06:30:00", "America/Chicago")

        // THEN
        assertThat(result).isEqualTo("2024-01-15T06:30:00")
    }

    @Test
    fun `given negative offset causing date rollback, when adjusting timestamp, then date changes correctly`() {
        // WHEN
        val result = sut.adjustUtcToSiteLocalTime("2024-01-15T02:00:00", "-6")

        // THEN
        assertThat(result).isEqualTo("2024-01-14T20:00:00")
    }

    @Test
    fun `given positive offset causing date rollforward, when adjusting timestamp, then date changes correctly`() {
        // WHEN
        val result = sut.adjustUtcToSiteLocalTime("2024-01-15T22:00:00", "5.5")

        // THEN
        assertThat(result).isEqualTo("2024-01-16T03:30:00")
    }

    @Test
    fun `given force is true, when generating catalog, then request body contains force true`() = runTest {
        // GIVEN
        whenever(
            wooNetwork.executePostGsonRequest(
                any(), any(), eq(WooPosGenerateCatalogResponse::class.java), any()
            )
        ) doReturn WPAPIResponse.Success(WooPosGenerateCatalogResponse(), emptyList())
        val bodyCaptor = argumentCaptor<Map<String, Any>>()

        // WHEN
        sut.postGenerateCatalog(site, force = true)

        // THEN
        verify(wooNetwork).executePostGsonRequest(
            site = eq(site),
            path = eq(WOOCOMMERCE.catalog.create.pathPosV1),
            clazz = eq(WooPosGenerateCatalogResponse::class.java),
            body = bodyCaptor.capture()
        )
        assertThat(bodyCaptor.firstValue["force"]).isEqualTo("true")
    }

    @Test
    fun `given force is not set, when generating catalog, then request body has no force attribute`() = runTest {
        // GIVEN
        whenever(
            wooNetwork.executePostGsonRequest(
                any(), any(), eq(WooPosGenerateCatalogResponse::class.java), any()
            )
        ) doReturn WPAPIResponse.Success(WooPosGenerateCatalogResponse(), emptyList())
        val bodyCaptor = argumentCaptor<Map<String, Any>>()

        // WHEN
        sut.postGenerateCatalog(site)

        // THEN
        verify(wooNetwork).executePostGsonRequest(
            site = eq(site),
            path = eq(WOOCOMMERCE.catalog.create.pathPosV1),
            clazz = eq(WooPosGenerateCatalogResponse::class.java),
            body = bodyCaptor.capture()
        )
        assertThat(bodyCaptor.firstValue).doesNotContainKey("force")
    }
}
