package com.woocommerce.android.ui.prefs.cardreader.connect

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.test.runBlockingTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.pay.WCTerminalStoreLocationError
import org.wordpress.android.fluxc.model.pay.WCTerminalStoreLocationErrorType
import org.wordpress.android.fluxc.model.pay.WCTerminalStoreLocationResult
import org.wordpress.android.fluxc.store.WCPayStore

class CardReaderLocationRepositoryTest : BaseUnitTest() {
    private val wcPayStore: WCPayStore = mock()
    private val selectedSite: SelectedSite = mock {
        on { getIfExists() }.thenReturn(mock())
    }

    private val repository = CardReaderLocationRepository(wcPayStore, selectedSite)

    @Test
    fun `given store returns success, when get default location, then location returned`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            // GIVEN
            val locationId = "locationId"
            whenever(wcPayStore.getStoreLocationForSite(any())).thenReturn(
                WCTerminalStoreLocationResult(
                    locationId,
                    null,
                    null,
                    null,
                )
            )

            // WHEN
            val result = repository.getDefaultLocationId()

            // THEN
            assertThat(result).isEqualTo(CardReaderLocationRepository.LocationIdFetchingResult.Success(locationId))
        }

    @Test
    fun `given store returns generic error, when get default location, then other error returned`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            // GIVEN
            whenever(wcPayStore.getStoreLocationForSite(any())).thenReturn(
                WCTerminalStoreLocationResult(
                    WCTerminalStoreLocationError(),
                )
            )

            // WHEN
            val result = repository.getDefaultLocationId()

            // THEN
            assertThat(result).isInstanceOf(
                CardReaderLocationRepository.LocationIdFetchingResult.Error.Other::class.java
            )
        }

    @Test
    fun `given store returns missing address error, when get default location, then missing address error returned`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            // GIVEN
            val url = "https://wordpress.com"
            whenever(wcPayStore.getStoreLocationForSite(any())).thenReturn(
                WCTerminalStoreLocationResult(
                    WCTerminalStoreLocationError(WCTerminalStoreLocationErrorType.MissingAddress(url)),
                )
            )

            // WHEN
            val result = repository.getDefaultLocationId()

            // THEN
            assertThat(result).isEqualTo(
                CardReaderLocationRepository.LocationIdFetchingResult.Error.MissingAddress(url)
            )
        }
}
