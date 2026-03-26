package com.woocommerce.android.ui.woopos.settings.details.localcatalog

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.home.ChildToParentEvent
import com.woocommerce.android.ui.woopos.home.ParentToChildrenEvent
import com.woocommerce.android.ui.woopos.home.WooPosChildrenToParentEventSender
import com.woocommerce.android.ui.woopos.home.WooPosParentToChildrenEventReceiver
import com.woocommerce.android.ui.woopos.localcatalog.PosLocalCatalogSyncResult
import com.woocommerce.android.ui.woopos.localcatalog.WooPosLocalCatalogSyncRepository
import com.woocommerce.android.ui.woopos.localcatalog.WooPosLocalCatalogSyncScheduler
import com.woocommerce.android.ui.woopos.util.WooPosCellularCapabilityDetector
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import com.woocommerce.android.ui.woopos.util.datastore.WooPosPreferencesRepository
import com.woocommerce.android.ui.woopos.util.datastore.WooPosSyncTimestampManager
import com.woocommerce.android.ui.woopos.util.format.WooPosDateFormatter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argThat
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
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
    private val childToParentEventSender: WooPosChildrenToParentEventSender = mock()
    private val parentToChildEventReceiver: WooPosParentToChildrenEventReceiver = mock()
    private val cellularCapabilityDetector: WooPosCellularCapabilityDetector = mock()

    private val mockSite: SiteModel = mock()
    private val allowCellularDataFlow = MutableStateFlow(false)
    private val parentEventsFlow = MutableSharedFlow<ParentToChildrenEvent>()

    private lateinit var sut: WooPosSettingsLocalCatalogViewModel

    @Before
    fun setUp() = runTest {
        whenever(selectedSite.get()).thenReturn(mockSite)
        whenever(preferencesRepository.allowCellularDataUpdate).thenReturn(allowCellularDataFlow)
        whenever(parentToChildEventReceiver.events).thenReturn(parentEventsFlow)
        whenever(cellularCapabilityDetector.hasCellularCapability()).thenReturn(true)
        whenever(localCatalogSyncRepository.syncLocalCatalogFull(any()))
            .thenReturn(
                PosLocalCatalogSyncResult.Success(
                    productsSynced = 10,
                    variationsSynced = 5,
                    syncDurationMs = 1000L
                )
            )
        whenever(localCatalogSyncRepository.syncLocalCatalogIncremental(any()))
            .thenReturn(
                PosLocalCatalogSyncResult.Success(
                    productsSynced = 0,
                    variationsSynced = 0,
                    syncDurationMs = 100L
                )
            )

        whenever(syncTimestampManager.getProductsLastSyncTimestamp()).thenReturn(1000L)
        whenever(syncTimestampManager.getVariationsLastSyncTimestamp()).thenReturn(1000L)
        whenever(syncTimestampManager.getFullSyncLastCompletedTimestamp()).thenReturn(1000L)
        whenever(dateFormatter.formatCatalogLastUpdate(anyOrNull(), anyOrNull())).thenReturn("Never")
        whenever(dateFormatter.formatCatalogLastFullSync(anyOrNull())).thenReturn("Never")
        whenever(localCatalogSyncRepository.getProductCount(any())).thenReturn(150)
        whenever(localCatalogSyncRepository.getVariationCount(any())).thenReturn(300)
    }

    @Test
    fun `given cellular data is disabled, when init, then state reflects disabled preference`() = runTest {
        // GIVEN
        allowCellularDataFlow.value = false
        whenever(cellularCapabilityDetector.hasCellularCapability()).thenReturn(true)

        // WHEN
        sut = createViewModel()
        advanceUntilIdle()

        // THEN
        val capability = sut.state.value.cellularCapability
        assertThat(capability).isInstanceOf(WooPosSettingsLocalCatalogState.CellularCapability.Available::class.java)
        assertThat((capability as WooPosSettingsLocalCatalogState.CellularCapability.Available).allowCellularDataUpdate)
            .isFalse()
    }

    @Test
    fun `given cellular data is enabled, when init, then state reflects enabled preference`() = runTest {
        // GIVEN
        allowCellularDataFlow.value = true
        whenever(cellularCapabilityDetector.hasCellularCapability()).thenReturn(true)

        // WHEN
        sut = createViewModel()
        advanceUntilIdle()

        // THEN
        val capability = sut.state.value.cellularCapability
        assertThat(capability).isInstanceOf(WooPosSettingsLocalCatalogState.CellularCapability.Available::class.java)
        assertThat((capability as WooPosSettingsLocalCatalogState.CellularCapability.Available).allowCellularDataUpdate)
            .isTrue()
    }

    @Test
    fun `when toggling cellular data, then updates preferences and notifies scheduler`() = runTest {
        // GIVEN
        sut = createViewModel()

        // WHEN
        sut.toggleCellularDataUpdate(true)
        advanceUntilIdle()

        // THEN
        verify(preferencesRepository).setAllowCellularDataUpdate(true)
        verify(syncScheduler).updateWorkConstraints()
    }

    @Test
    fun `when cellular data preference changes, then state is updated`() = runTest {
        // GIVEN
        whenever(cellularCapabilityDetector.hasCellularCapability()).thenReturn(true)
        sut = createViewModel()
        advanceUntilIdle()

        var capability = sut.state.value.cellularCapability
        assertThat(capability).isInstanceOf(WooPosSettingsLocalCatalogState.CellularCapability.Available::class.java)
        assertThat((capability as WooPosSettingsLocalCatalogState.CellularCapability.Available).allowCellularDataUpdate)
            .isFalse()

        // WHEN
        allowCellularDataFlow.value = true
        advanceUntilIdle()

        // THEN
        capability = sut.state.value.cellularCapability
        assertThat(capability).isInstanceOf(WooPosSettingsLocalCatalogState.CellularCapability.Available::class.java)
        assertThat((capability as WooPosSettingsLocalCatalogState.CellularCapability.Available).allowCellularDataUpdate)
            .isTrue()
    }

    @Test
    fun `when default state is created, then initial state is Loading`() {
        // GIVEN & WHEN
        val initialState = WooPosSettingsLocalCatalogState()

        // THEN
        assertThat(initialState.catalogStatus).isEqualTo(WooPosSettingsLocalCatalogState.CatalogStatus.Loading)
    }

    @Test
    fun `when init, then loading state gets replaced when data loads`() = runTest {
        // WHEN
        whenever(localCatalogSyncRepository.getProductCount(any())).thenReturn(999)
        whenever(localCatalogSyncRepository.getVariationCount(any())).thenReturn(777)
        sut = createViewModel()
        advanceUntilIdle()

        // THEN
        val catalogStatus = sut.state.value.catalogStatus
        assertThat(catalogStatus).isInstanceOf(WooPosSettingsLocalCatalogState.CatalogStatus.Available::class.java)

        val availableStatus = catalogStatus as WooPosSettingsLocalCatalogState.CatalogStatus.Available
        assertThat(availableStatus.productCount).isEqualTo(999)
        assertThat(availableStatus.variationCount).isEqualTo(777)
        assertThat(availableStatus.lastUpdate).isEqualTo("Never")
        assertThat(availableStatus.lastFullUpdate).isEqualTo("Never")
    }

    @Test
    fun `when manual refresh completes, then state is refreshed`() = runTest {
        // GIVEN
        sut = createViewModel()
        advanceUntilIdle()

        val initialStatus = sut.state.value.catalogStatus
        assertThat(initialStatus).isInstanceOf(WooPosSettingsLocalCatalogState.CatalogStatus.Available::class.java)

        // WHEN
        sut.runFullCatalogSync()
        advanceUntilIdle()

        // THEN
        val finalStatus = sut.state.value.catalogStatus
        assertThat(finalStatus).isInstanceOf(WooPosSettingsLocalCatalogState.CatalogStatus.Available::class.java)
        verify(localCatalogSyncRepository).syncLocalCatalogFull(mockSite)
        verify(syncTimestampManager, times(2)).getProductsLastSyncTimestamp()
        verify(syncTimestampManager, times(2)).getVariationsLastSyncTimestamp()
        verify(syncTimestampManager, times(2)).getFullSyncLastCompletedTimestamp()
    }

    @Test
    fun `given sync fails, when runFullCatalogSync called, then ShowSyncErrorDialog event is sent`() = runTest {
        // GIVEN
        val errorMessage = "Network error"
        whenever(localCatalogSyncRepository.syncLocalCatalogFull(mockSite))
            .thenReturn(PosLocalCatalogSyncResult.Failure.UnexpectedError(errorMessage))

        sut = createViewModel()
        advanceUntilIdle()

        // WHEN
        sut.runFullCatalogSync()
        advanceUntilIdle()

        // THEN
        verify(childToParentEventSender).sendToParent(
            argThat {
                this is ChildToParentEvent.SettingsEvent.ShowSyncErrorDialog &&
                    this.errorMessage == errorMessage
            }
        )
    }

    @Test
    fun `given sync fails, when runFullCatalogSync called, then catalog status is restored to previous state`() = runTest {
        // GIVEN
        val errorMessage = "Network error"
        whenever(localCatalogSyncRepository.syncLocalCatalogFull(mockSite))
            .thenReturn(PosLocalCatalogSyncResult.Failure.UnexpectedError(errorMessage))

        sut = createViewModel()
        advanceUntilIdle()

        val initialStatus = sut.state.value.catalogStatus

        // WHEN
        sut.runFullCatalogSync()
        advanceUntilIdle()

        // THEN
        assertThat(sut.state.value.catalogStatus).isEqualTo(initialStatus)
    }

    @Test
    fun `given sync succeeds, when runFullCatalogSync called, then catalog status is reloaded`() = runTest {
        // GIVEN
        whenever(localCatalogSyncRepository.syncLocalCatalogFull(mockSite))
            .thenReturn(
                PosLocalCatalogSyncResult.Success(
                    productsSynced = 10,
                    variationsSynced = 5,
                    syncDurationMs = 1000
                )
            )

        sut = createViewModel()
        advanceUntilIdle()

        // WHEN
        sut.runFullCatalogSync()
        advanceUntilIdle()

        // THEN
        assertThat(sut.state.value.catalogStatus)
            .isInstanceOf(WooPosSettingsLocalCatalogState.CatalogStatus.Available::class.java)
    }

    @Test
    fun `given RetrySyncRequested event, when received from parent, then runFullCatalogSync is triggered`() = runTest {
        // GIVEN
        whenever(localCatalogSyncRepository.syncLocalCatalogFull(mockSite))
            .thenReturn(
                PosLocalCatalogSyncResult.Success(
                    productsSynced = 10,
                    variationsSynced = 5,
                    syncDurationMs = 1000
                )
            )

        sut = createViewModel()
        advanceUntilIdle()

        // WHEN
        parentEventsFlow.emit(ParentToChildrenEvent.SettingsEvent.RetrySyncRequested)
        advanceUntilIdle()

        // THEN
        assertThat(sut.state.value.catalogStatus)
            .isInstanceOf(WooPosSettingsLocalCatalogState.CatalogStatus.Available::class.java)
    }

    @Test
    fun `when runFullCatalogSync called, then catalog status eventually updates to Available`() = runTest {
        // GIVEN
        whenever(localCatalogSyncRepository.syncLocalCatalogFull(mockSite))
            .thenReturn(
                PosLocalCatalogSyncResult.Success(
                    productsSynced = 10,
                    variationsSynced = 5,
                    syncDurationMs = 1000
                )
            )

        sut = createViewModel()
        advanceUntilIdle()

        // WHEN
        sut.runFullCatalogSync()
        advanceUntilIdle()

        // THEN
        assertThat(sut.state.value.catalogStatus)
            .isInstanceOf(WooPosSettingsLocalCatalogState.CatalogStatus.Available::class.java)
    }

    @Test
    fun `given full sync succeeds, when runFullCatalogSync called, then product list refresh is requested`() = runTest {
        // GIVEN
        sut = createViewModel()
        advanceUntilIdle()

        // WHEN
        sut.runFullCatalogSync()
        advanceUntilIdle()

        // THEN
        verify(childToParentEventSender).sendToParent(ChildToParentEvent.RefreshProductList)
    }

    @Test
    fun `given full sync succeeds, when runFullCatalogSync called, then incremental sync is also run`() = runTest {
        // GIVEN
        sut = createViewModel()
        advanceUntilIdle()

        // WHEN
        sut.runFullCatalogSync()
        advanceUntilIdle()

        // THEN
        verify(localCatalogSyncRepository).syncLocalCatalogIncremental(mockSite)
    }

    @Test
    fun `given device has cellular capability, when init, then cellularCapability is Available`() = runTest {
        // GIVEN
        whenever(cellularCapabilityDetector.hasCellularCapability()).thenReturn(true)

        // WHEN
        sut = createViewModel()
        advanceUntilIdle()

        // THEN
        assertThat(sut.state.value.cellularCapability)
            .isInstanceOf(WooPosSettingsLocalCatalogState.CellularCapability.Available::class.java)
    }

    @Test
    fun `given device has no cellular capability, when init, then cellularCapability is None`() = runTest {
        // GIVEN
        whenever(cellularCapabilityDetector.hasCellularCapability()).thenReturn(false)

        // WHEN
        sut = createViewModel()
        advanceUntilIdle()

        // THEN
        assertThat(sut.state.value.cellularCapability)
            .isEqualTo(WooPosSettingsLocalCatalogState.CellularCapability.None)
    }

    @Test
    fun `given device has no cellular capability, when init, then allowCellularDataUpdate is set to false in preferences`() = runTest {
        // GIVEN
        whenever(cellularCapabilityDetector.hasCellularCapability()).thenReturn(false)

        // WHEN
        sut = createViewModel()
        advanceUntilIdle()

        // THEN
        verify(preferencesRepository).setAllowCellularDataUpdate(false)
    }

    @Test
    fun `given device has cellular capability, when init, then allowCellularDataUpdate preference is not modified`() = runTest {
        // GIVEN
        whenever(cellularCapabilityDetector.hasCellularCapability()).thenReturn(true)

        // WHEN
        sut = createViewModel()
        advanceUntilIdle()

        // THEN
        verify(preferencesRepository, times(0)).setAllowCellularDataUpdate(false)
    }

    private fun createViewModel() = WooPosSettingsLocalCatalogViewModel(
        syncTimestampManager = syncTimestampManager,
        localCatalogSyncRepository = localCatalogSyncRepository,
        selectedSite = selectedSite,
        dateFormatter = dateFormatter,
        preferencesRepository = preferencesRepository,
        syncScheduler = syncScheduler,
        childToParentEventSender = childToParentEventSender,
        parentToChildEventReceiver = parentToChildEventReceiver,
        cellularCapabilityDetector = cellularCapabilityDetector,
    )
}
