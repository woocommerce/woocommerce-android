package org.wordpress.android.fluxc.store

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.payments.woo.WooPaymentsDepositsOverview
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.network.rest.wpcom.wc.payments.woo.WooPaymentsDepositsOverviewApiResponse
import org.wordpress.android.fluxc.network.rest.wpcom.wc.payments.woo.WooPaymentsRestClient
import org.wordpress.android.fluxc.persistence.dao.WooPaymentsDepositsOverviewDao
import org.wordpress.android.fluxc.persistence.mappers.WooPaymentsDepositsOverviewMapper
import org.wordpress.android.fluxc.test
import org.wordpress.android.fluxc.tools.initCoroutineEngine

class WCWooPaymentsStoreTest {
    private val restClient = mock<WooPaymentsRestClient>()
    private val dao: WooPaymentsDepositsOverviewDao = mock()
    private val mapper: WooPaymentsDepositsOverviewMapper = mock()

    private val store = WCWooPaymentsStore(
        initCoroutineEngine(),
        restClient,
        dao,
        mapper,
    )

    @Test
    fun `given rest returns error, when fetchConnectionToken, then error returned`() = test {
        // GIVEN
        val error = mock<WooError>()
        val restResult = WooPayload<WooPaymentsDepositsOverviewApiResponse>(error)
        whenever(restClient.fetchDepositsOverview(any())).thenReturn(restResult)

        // WHEN
        val result = store.fetchDepositsOverview(mock())

        // THEN
        assertThat(result.isError).isTrue
        assertThat(result.error).isEqualTo(error)
    }

    @Test
    fun `given rest returns success, when fetchConnectionToken, then success returned`() = test {
        // GIVEN
        val response = mock<WooPaymentsDepositsOverviewApiResponse>()
        val mappedModel = mock<WooPaymentsDepositsOverview>()
        val restResult = WooPayload(response)
        whenever(restClient.fetchDepositsOverview(any())).thenReturn(restResult)
        whenever(mapper.mapApiResponseToModel(response)).thenReturn(mappedModel)

        // WHEN
        val result = store.fetchDepositsOverview(mock())

        // THEN
        assertThat(result).isEqualTo(WooPayload(mappedModel))
    }
}
