package com.woocommerce.android.ui.login.storecreation.name

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.support.help.HelpOrigin
import com.woocommerce.android.ui.login.storecreation.CreateFreeTrialStore
import com.woocommerce.android.ui.login.storecreation.CreateFreeTrialStore.StoreCreationState
import com.woocommerce.android.ui.login.storecreation.NewStore
import com.woocommerce.android.ui.login.storecreation.StoreCreationErrorType
import com.woocommerce.android.ui.login.storecreation.StoreCreationRepository.SiteCreationData
import com.woocommerce.android.ui.login.storecreation.name.StoreNamePickerViewModel.NavigateToSummary
import com.woocommerce.android.ui.login.storecreation.name.StoreNamePickerViewModel.StoreNamePickerState
import com.woocommerce.android.ui.login.storecreation.plans.PlansViewModel
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify

@OptIn(ExperimentalCoroutinesApi::class)
internal class StoreNamePickerViewModelTest : BaseUnitTest() {
    private lateinit var sut: StoreNamePickerViewModel
    private lateinit var createStore: CreateFreeTrialStore
    private lateinit var newStore: NewStore
    private lateinit var analyticsTracker: AnalyticsTrackerWrapper
    private lateinit var prefsWrapper: AppPrefsWrapper
    private val savedState = SavedStateHandle()

    private val expectedSiteCreationData = SiteCreationData(
        siteDesign = PlansViewModel.NEW_SITE_THEME,
        domain = "test domain",
        title = "test title",
        segmentId = null
    )

    @Before
    fun setUp() {
        prefsWrapper = mock()
        createSut()
    }

    @Test
    fun `when onContinueClicked starts loading, then the state is updated to reflect it`() = testBlocking {
        // Given
        createSut(StoreCreationState.Loading)

        var latestState: StoreNamePickerState? = null
        sut.storePickerState.observeForever { latestState = it }

        // When
        sut.onStoreNameChanged("Store name")
        sut.onContinueClicked()

        // Then
        verify(newStore).update(name = "Store name")
        verify(createStore, never()).invoke(
            expectedSiteCreationData.domain,
            expectedSiteCreationData.title
        )
        assertThat(latestState).isEqualTo(
            StoreNamePickerState.Contentful(
                storeName = "Store name",
                isCreatingStore = true
            )
        )
    }

    @Test
    fun `when onContinueClicked happens and store creation succeed, then free trial store creation starts`() = testBlocking {
        // Given
        var latestEvent: MultiLiveEvent.Event? = null
        sut.event.observeForever { latestEvent = it }

        var latestState: StoreNamePickerState? = null
        sut.storePickerState.observeForever { latestState = it }

        // When
        sut.onStoreNameChanged("Store name")
        sut.onContinueClicked()

        // Then
        verify(newStore).update(name = "Store name")
        verify(newStore, never()).update(siteId = 123)
        verify(createStore, never()).invoke(
            expectedSiteCreationData.domain,
            expectedSiteCreationData.title
        )
        assertThat(latestEvent).isEqualTo(NavigateToSummary)
        assertThat(latestState).isEqualTo(
            StoreNamePickerState.Contentful(
                storeName = "Store name",
                isCreatingStore = false
            )
        )
    }

    @Test
    fun `when onContinueClicked happens and store creation fails, then error state is triggered`() = testBlocking {
        // Given
        val storeCreationErrorType = StoreCreationErrorType.FREE_TRIAL_ASSIGNMENT_FAILED
        createSut(StoreCreationState.Failed(storeCreationErrorType))

        var latestEvent: MultiLiveEvent.Event? = null
        var latestState: StoreNamePickerState? = null

        sut.event.observeForever { latestEvent = it }
        sut.storePickerState.observeForever { latestState = it }

        // When
        sut.onStoreNameChanged("Store name")
        sut.onContinueClicked()

        // Then
        verify(newStore).update(name = "Store name")
        verify(createStore, never()).invoke(
            expectedSiteCreationData.domain,
            expectedSiteCreationData.title
        )
        assertThat(latestEvent).isEqualTo(NavigateToSummary)
        assertThat(latestState).isEqualTo(StoreNamePickerState.Error(storeCreationErrorType))
    }

    @Test
    fun `when onStoreNameChanges happens, then viewState is updated as expected`() = testBlocking {
        // Given
        val viewStateChanges = mutableListOf<StoreNamePickerState>()
        sut.storePickerState.observeForever {
            viewStateChanges.add(it)
        }

        // When
        sut.onStoreNameChanged("Store name")

        // Then
        assertThat(viewStateChanges).hasSize(2)
        assertThat(viewStateChanges[0]).isEqualTo(
            StoreNamePickerState.Contentful(
                storeName = "",
                isCreatingStore = false
            )
        )
        assertThat(viewStateChanges[1]).isEqualTo(
            StoreNamePickerState.Contentful(
                storeName = "Store name",
                isCreatingStore = false
            )
        )
    }

    @Test
    fun `when onCancelPressed happens, then the tracks and events are triggered as expected`() {
        // Given
        val storeCreationSource = "test source"
        prefsWrapper = mock {
            on { getStoreCreationSource() } doReturn storeCreationSource
        }
        createSut()
        var latestEvent: MultiLiveEvent.Event? = null
        sut.event.observeForever { latestEvent = it }

        // When
        sut.onCancelPressed()

        // Then
        verify(analyticsTracker).track(
            AnalyticsEvent.SITE_CREATION_DISMISSED,
            mapOf(
                AnalyticsTracker.KEY_STEP to AnalyticsTracker.VALUE_STEP_STORE_NAME,
                AnalyticsTracker.KEY_FLOW to AnalyticsTracker.VALUE_NATIVE,
                AnalyticsTracker.KEY_SOURCE to storeCreationSource,
                AnalyticsTracker.KEY_IS_FREE_TRIAL to FeatureFlag.FREE_TRIAL_M2.isEnabled()
            )
        )
        assertThat(latestEvent).isEqualTo(MultiLiveEvent.Event.Exit)
    }

    @Test
    fun `when viewModel is created, then the site creation step track is triggered`() {
        verify(analyticsTracker).track(
            AnalyticsEvent.SITE_CREATION_STEP,
            mapOf(
                AnalyticsTracker.KEY_STEP to AnalyticsTracker.VALUE_STEP_STORE_NAME
            )
        )
    }

    @Test
    fun `when onExitTriggered happens, then trigger Exit event`() {
        // Given
        var latestEvent: MultiLiveEvent.Event? = null
        sut.event.observeForever { latestEvent = it }

        // When
        sut.onExitTriggered()

        // Then
        assertThat(latestEvent).isEqualTo(MultiLiveEvent.Event.Exit)
    }

    @Test
    fun `when onHelpPressed happens, then trigger NavigateToHelpScreen event`() {
        // Given
        var latestEvent: MultiLiveEvent.Event? = null
        sut.event.observeForever { latestEvent = it }

        // When
        sut.onHelpPressed()

        // Then
        assertThat(latestEvent).isEqualTo(MultiLiveEvent.Event.NavigateToHelpScreen(HelpOrigin.STORE_CREATION))
    }

    private fun createSut(
        expectedCreationState: StoreCreationState = StoreCreationState.Finished
    ) {
        createStore = mock {
            on { state } doReturn MutableStateFlow(expectedCreationState)
        }

        newStore = mock()
        analyticsTracker = mock()
        sut = StoreNamePickerViewModel(
            savedStateHandle = savedState,
            newStore = newStore,
            createStore = createStore,
            analyticsTrackerWrapper = analyticsTracker,
            prefsWrapper = prefsWrapper
        )
    }
}
