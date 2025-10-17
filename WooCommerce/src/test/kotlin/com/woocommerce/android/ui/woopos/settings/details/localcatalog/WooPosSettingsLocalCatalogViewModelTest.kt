package com.woocommerce.android.ui.woopos.settings.details.localcatalog

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.localcatalog.PosLocalCatalogSyncResult
import com.woocommerce.android.ui.woopos.localcatalog.WooPosLocalCatalogSyncRepository
import com.woocommerce.android.ui.woopos.localcatalog.WooPosLocalCatalogSyncScheduler
import com.woocommerce.android.ui.woopos.settings.SettingsChildToParentEvent
import com.woocommerce.android.ui.woopos.settings.SettingsParentToChildEvent
import com.woocommerce.android.ui.woopos.settings.WooPosSettingsChildToParentEventSender
import com.woocommerce.android.ui.woopos.settings.WooPosSettingsParentToChildEventReceiver
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import com.woocommerce.android.ui.woopos.util.datastore.WooPosPreferencesRepository
import com.woocommerce.android.ui.woopos.util.datastore.WooPosSyncTimestampManager
import com.woocommerce.android.ui.woopos.util.format.WooPosDateFormatter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel

@ExperimentalCoroutinesApi
class WooPosSettingsLocalCatalogViewModelTest {

    @Rule
    @JvmField
    val coroutineTestRule = WooPosCoroutineTestRule()

    private val syncTimestampManager: WooPosSyncTimestampManager = mock()
    private val localCatalogSyncRepository: WooPosLocalCatalogSyncRepository = mock()
    private val selectedSite: SelectedSite = mock()
    private val dateFormatter: WooPosDateFormatter = mock()
    private val preferencesRepository: WooPosPreferencesRepository = mock()
    private val syncScheduler: WooPosLocalCatalogSyncScheduler = mock()
    private val childToParentEventSender: WooPosSettingsChildToParentEventSender = mock()
    private val parentToChildEventReceiver: WooPosSettingsParentToChildEventReceiver = mock()

    private val siteModel = SiteModel()

    @Before
    fun setup() = runTest {
        whenever(selectedSite.get()).thenReturn(siteModel)
        whenever(dateFormatter.formatCatalogLastUpdate(any(), any())).thenReturn("2 hours ago")
        whenever(preferencesRepository.allowCellularDataUpdate).thenReturn(flowOf(false))
        whenever(syncTimestampManager.getProductsLastSyncTimestamp()).thenReturn(0L)
        whenever(syncTimestampManager.getVariationsLastSyncTimestamp()).thenReturn(0L)
    }

    @Test
    fun `given sync fails, when runFullCatalogSync called, then ShowSyncErrorDialog event is sent`() = runTest {
        // GIVEN
        val errorMessage = "Network error"
        whenever(parentToChildEventReceiver.events).thenReturn(MutableSharedFlow())
        whenever(localCatalogSyncRepository.syncLocalCatalogFull(siteModel))
            .thenReturn(PosLocalCatalogSyncResult.Failure.UnexpectedError(errorMessage))

        val viewModel = createViewModel()
        advanceUntilIdle()

        // WHEN
        viewModel.runFullCatalogSync()
        advanceUntilIdle()

        // THEN
        verify(childToParentEventSender).sendToParent(
            argThat {
                this is SettingsChildToParentEvent.ShowSyncErrorDialog &&
                    this.errorMessage == errorMessage
            }
        )
    }

    @Test
    fun `given sync fails, when runFullCatalogSync called, then catalog status is restored to previous state`() = runTest {
        // GIVEN
        val errorMessage = "Network error"
        whenever(parentToChildEventReceiver.events).thenReturn(MutableSharedFlow())
        whenever(localCatalogSyncRepository.syncLocalCatalogFull(siteModel))
            .thenReturn(PosLocalCatalogSyncResult.Failure.UnexpectedError(errorMessage))

        val viewModel = createViewModel()
        advanceUntilIdle()

        val initialStatus = viewModel.state.value.catalogStatus

        // WHEN
        viewModel.runFullCatalogSync()
        advanceUntilIdle()

        // THEN
        assertThat(viewModel.state.value.catalogStatus).isEqualTo(initialStatus)
    }

    @Test
    fun `given sync succeeds, when runFullCatalogSync called, then catalog status is reloaded`() = runTest {
        // GIVEN
        whenever(parentToChildEventReceiver.events).thenReturn(MutableSharedFlow())
        whenever(localCatalogSyncRepository.syncLocalCatalogFull(siteModel))
            .thenReturn(
                PosLocalCatalogSyncResult.Success(
                    productsSynced = 10,
                    variationsSynced = 5,
                    syncDurationMs = 1000
                )
            )

        val viewModel = createViewModel()
        advanceUntilIdle()

        // WHEN
        viewModel.runFullCatalogSync()
        advanceUntilIdle()

        // THEN
        assertThat(viewModel.state.value.catalogStatus)
            .isInstanceOf(WooPosSettingsLocalCatalogState.CatalogStatus.Available::class.java)
    }

    @Test
    fun `given RetrySyncRequested event, when received from parent, then runFullCatalogSync is triggered`() = runTest {
        // GIVEN
        val eventsFlow = MutableSharedFlow<SettingsParentToChildEvent>()
        whenever(parentToChildEventReceiver.events).thenReturn(eventsFlow)
        whenever(localCatalogSyncRepository.syncLocalCatalogFull(siteModel))
            .thenReturn(
                PosLocalCatalogSyncResult.Success(
                    productsSynced = 10,
                    variationsSynced = 5,
                    syncDurationMs = 1000
                )
            )

        val viewModel = createViewModel()
        advanceUntilIdle()

        // WHEN
        eventsFlow.emit(SettingsParentToChildEvent.RetrySyncRequested)
        advanceUntilIdle()

        // THEN
        assertThat(viewModel.state.value.catalogStatus)
            .isInstanceOf(WooPosSettingsLocalCatalogState.CatalogStatus.Available::class.java)
    }

    @Test
    fun `when runFullCatalogSync called, then catalog status eventually updates to Available`() = runTest {
        // GIVEN
        whenever(parentToChildEventReceiver.events).thenReturn(MutableSharedFlow())
        whenever(localCatalogSyncRepository.syncLocalCatalogFull(siteModel))
            .thenReturn(
                PosLocalCatalogSyncResult.Success(
                    productsSynced = 10,
                    variationsSynced = 5,
                    syncDurationMs = 1000
                )
            )

        val viewModel = createViewModel()
        advanceUntilIdle()

        // WHEN
        viewModel.runFullCatalogSync()
        advanceUntilIdle()

        // THEN
        assertThat(viewModel.state.value.catalogStatus)
            .isInstanceOf(WooPosSettingsLocalCatalogState.CatalogStatus.Available::class.java)
    }

    private fun createViewModel(): WooPosSettingsLocalCatalogViewModel {
        return WooPosSettingsLocalCatalogViewModel(
            syncTimestampManager = syncTimestampManager,
            localCatalogSyncRepository = localCatalogSyncRepository,
            selectedSite = selectedSite,
            dateFormatter = dateFormatter,
            preferencesRepository = preferencesRepository,
            syncScheduler = syncScheduler,
            childToParentEventSender = childToParentEventSender,
            parentToChildEventReceiver = parentToChildEventReceiver,
        )
    }
}
