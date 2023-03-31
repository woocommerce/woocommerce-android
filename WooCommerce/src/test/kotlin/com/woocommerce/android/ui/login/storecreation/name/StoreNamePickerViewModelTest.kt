package com.woocommerce.android.ui.login.storecreation.name

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.ui.login.storecreation.NewStore
import com.woocommerce.android.ui.login.storecreation.StoreCreationRepository
import com.woocommerce.android.ui.login.storecreation.StoreCreationRepository.SiteCreationData
import com.woocommerce.android.ui.login.storecreation.StoreCreationResult
import com.woocommerce.android.ui.login.storecreation.name.StoreNamePickerViewModel.NavigateToStoreInstallation
import com.woocommerce.android.ui.login.storecreation.name.StoreNamePickerViewModel.StoreNamePickerState
import com.woocommerce.android.ui.login.storecreation.plans.PlansViewModel
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@OptIn(ExperimentalCoroutinesApi::class)
internal class StoreNamePickerViewModelTest: BaseUnitTest() {
    private lateinit var sut: StoreNamePickerViewModel
    private lateinit var storeCreationRepository: StoreCreationRepository
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
        createSutWith(expectedSiteCreationData)
    }

    @Test
    fun `when onContinueClicked happens, then free trial store creation starts`() = testBlocking {
        // Given
        var latestEvent: MultiLiveEvent.Event? = null
        sut.event.observeForever { latestEvent = it }

        // When
        sut.onStoreNameChanged("Store name")
        sut.onContinueClicked()

        // Then
        verify(newStore).update(name = "Store name")
        verify(newStore).update(siteId = 123)
        verify(storeCreationRepository).createNewFreeTrialSite(
            eq(expectedSiteCreationData),
            eq(PlansViewModel.NEW_SITE_LANGUAGE_ID),
            any()
        )
        assertThat(latestEvent).isEqualTo(NavigateToStoreInstallation)
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
        createSutWith(expectedSiteCreationData)
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
                AnalyticsTracker.KEY_SOURCE to storeCreationSource
            )
        )
        assertThat(latestEvent).isEqualTo(MultiLiveEvent.Event.Exit)
    }

    private fun createSutWith(expectedSiteCreationData: SiteCreationData) {
        newStore = mock {
            on { data } doReturn NewStore.NewStoreData(
                domain = expectedSiteCreationData.domain,
                name = expectedSiteCreationData.title
            )
        }

        storeCreationRepository = mock {
            onBlocking {
                createNewFreeTrialSite(
                    eq(expectedSiteCreationData),
                    eq(PlansViewModel.NEW_SITE_LANGUAGE_ID),
                    any()
                )
            } doReturn StoreCreationResult.Success(123)
        }

        analyticsTracker = mock()

        sut = StoreNamePickerViewModel(
            savedStateHandle = savedState,
            newStore = newStore,
            repository = storeCreationRepository,
            analyticsTrackerWrapper = analyticsTracker,
            prefsWrapper = prefsWrapper
        )
    }
}
