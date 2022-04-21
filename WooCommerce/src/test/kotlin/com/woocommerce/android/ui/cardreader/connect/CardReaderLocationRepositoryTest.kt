package com.woocommerce.android.ui.cardreader.connect

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.payments.inperson.WCTerminalStoreLocationError
import org.wordpress.android.fluxc.model.payments.inperson.WCTerminalStoreLocationErrorType
import org.wordpress.android.fluxc.model.payments.inperson.WCTerminalStoreLocationResult
import org.wordpress.android.fluxc.store.WCInPersonPaymentsStore

@ExperimentalCoroutinesApi
class CardReaderLocationRepositoryTest : BaseUnitTest() {
    private val store: WCInPersonPaymentsStore = mock()
    private val selectedSite: SelectedSite = mock {
        on { getIfExists() }.thenReturn(mock())
    }

    private val repository = CardReaderLocationRepository(store, selectedSite)

    @Test
    fun `given store returns success, when get default location, then location returned`() =
        testBlocking {
            // GIVEN
            val locationId = "locationId"
            whenever(store.getStoreLocationForSite(any(), any())).thenReturn(
                WCTerminalStoreLocationResult(
                    locationId,
                    null,
                    null,
                    null,
                )
            )

            // WHEN
            val result = repository.getDefaultLocationId(mock())

            // THEN
            assertThat(result).isEqualTo(CardReaderLocationRepository.LocationIdFetchingResult.Success(locationId))
        }

    @Test
    fun `given store returns generic error, when get default location, then other error returned`() =
        testBlocking {
            // GIVEN
            whenever(store.getStoreLocationForSite(any(), any())).thenReturn(
                WCTerminalStoreLocationResult(
                    WCTerminalStoreLocationError(),
                )
            )

            // WHEN
            val result = repository.getDefaultLocationId(mock())

            // THEN
            assertThat(result).isInstanceOf(
                CardReaderLocationRepository.LocationIdFetchingResult.Error.Other::class.java
            )
        }

    @Test
    fun `given store returns missing address error, when get default location, then missing address error returned`() =
        testBlocking {
            // GIVEN
            val url = "https://wordpress.com"
            whenever(store.getStoreLocationForSite(any(), any())).thenReturn(
                WCTerminalStoreLocationResult(
                    WCTerminalStoreLocationError(WCTerminalStoreLocationErrorType.MissingAddress(url)),
                )
            )

            // WHEN
            val result = repository.getDefaultLocationId(mock())

            // THEN
            assertThat(result).isEqualTo(
                CardReaderLocationRepository.LocationIdFetchingResult.Error.MissingAddress(url)
            )
        }

    @Test
    fun `given store returns invalid postcode error, when get default location, then invalid pc error returned`() =
        testBlocking {
            // GIVEN
            whenever(store.getStoreLocationForSite(any(), any())).thenReturn(
                WCTerminalStoreLocationResult(
                    WCTerminalStoreLocationError(WCTerminalStoreLocationErrorType.InvalidPostalCode),
                )
            )

            // WHEN
            val result = repository.getDefaultLocationId(mock())

            // THEN
            assertThat(result).isEqualTo(
                CardReaderLocationRepository.LocationIdFetchingResult.Error.InvalidPostalCode
            )
        }
}
