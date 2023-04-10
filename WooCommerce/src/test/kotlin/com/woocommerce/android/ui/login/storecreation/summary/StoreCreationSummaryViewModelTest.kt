package com.woocommerce.android.ui.login.storecreation.summary

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.ui.login.storecreation.CreateFreeTrialStore
import com.woocommerce.android.ui.login.storecreation.CreateFreeTrialStore.StoreCreationState
import com.woocommerce.android.ui.login.storecreation.NewStore
import com.woocommerce.android.ui.login.storecreation.StoreCreationErrorType.SITE_CREATION_FAILED
import com.woocommerce.android.ui.login.storecreation.summary.StoreCreationSummaryViewModel.OnCancelPressed
import com.woocommerce.android.ui.login.storecreation.summary.StoreCreationSummaryViewModel.OnStoreCreationFailure
import com.woocommerce.android.ui.login.storecreation.summary.StoreCreationSummaryViewModel.OnStoreCreationSuccess
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@OptIn(ExperimentalCoroutinesApi::class)
internal class StoreCreationSummaryViewModelTest : BaseUnitTest() {
    private lateinit var sut: StoreCreationSummaryViewModel
    private lateinit var createStore: CreateFreeTrialStore
    private lateinit var newStore: NewStore
    private val savedState = SavedStateHandle()

    @Test
    fun `when onTryForFreeButtonPressed is called, then start the store creation`() = testBlocking {
        // Given
        val expectedDomain = "test domain"
        val expectedTitle = "test title"
        createSut(expectedDomain, expectedTitle, StoreCreationState.Finished(123L))

        // When
        sut.onTryForFreeButtonPressed()

        // Then
        verify(createStore).invoke(expectedDomain, expectedTitle)
    }

    @Test
    fun `when store creation succeeds, then trigger expected event`() = testBlocking {
        // Given
        val expectedDomain = "test domain"
        val expectedTitle = "test title"
        createSut(expectedDomain, expectedTitle, StoreCreationState.Finished(123L))

        var lastReceivedEvent: MultiLiveEvent.Event? = null
        sut.event.observeForever { lastReceivedEvent = it }

        // When
        sut.onTryForFreeButtonPressed()

        // Then
        assertThat(lastReceivedEvent).isEqualTo(OnStoreCreationSuccess)
    }

    @Test
    fun `when store creation fails, then trigger expected event`() = testBlocking {
        // Given
        val expectedDomain = "test domain"
        val expectedTitle = "test title"
        createSut(expectedDomain, expectedTitle, StoreCreationState.Failed(SITE_CREATION_FAILED))

        var lastReceivedEvent: MultiLiveEvent.Event? = null
        sut.event.observeForever { lastReceivedEvent = it }

        // When
        sut.onTryForFreeButtonPressed()

        // Then
        assertThat(lastReceivedEvent).isEqualTo(OnStoreCreationFailure)
    }

    @Test
    fun `when store creation succeeds, then set site id`() = testBlocking {
        // Given
        val expectedDomain = "test domain"
        val expectedTitle = "test title"
        val expectedSiteId = 321321321L
        createSut(expectedDomain, expectedTitle, StoreCreationState.Finished(expectedSiteId))

        // When
        sut.onTryForFreeButtonPressed()

        // Then
        verify(newStore).update(siteId = expectedSiteId)
    }

    @Test
    fun `when store creation is Idle, then viewState is updated as expected`() = testBlocking {
        // Given
        val expectedDomain = "test domain"
        val expectedTitle = "test title"
        createSut(expectedDomain, expectedTitle, StoreCreationState.Finished(123L))

        var isLoading: Boolean? = null
        sut.isLoading.observeForever { isLoading = it }

        // When
        sut.onTryForFreeButtonPressed()

        // Then
        assertThat(isLoading).isFalse
    }

    @Test
    fun `when store creation is Loading, then viewState is updated as expected`() = testBlocking {
        // Given
        val expectedDomain = "test domain"
        val expectedTitle = "test title"
        createSut(expectedDomain, expectedTitle, StoreCreationState.Loading)

        var isLoading: Boolean? = null
        sut.isLoading.observeForever { isLoading = it }

        // When
        sut.onTryForFreeButtonPressed()

        // Then
        assertThat(isLoading).isTrue
    }

    @Test
    fun `when store creation is Failed, then viewState is updated as expected`() = testBlocking {
        // Given
        val expectedDomain = "test domain"
        val expectedTitle = "test title"
        createSut(expectedDomain, expectedTitle, StoreCreationState.Failed(SITE_CREATION_FAILED))

        var isLoading: Boolean? = null
        sut.isLoading.observeForever { isLoading = it }

        // When
        sut.onTryForFreeButtonPressed()

        // Then
        assertThat(isLoading).isFalse
    }

    @Test
    fun `when store creation is Finished, then viewState is updated as expected`() = testBlocking {
        // Given
        val expectedDomain = "test domain"
        val expectedTitle = "test title"
        createSut(expectedDomain, expectedTitle, StoreCreationState.Finished(123L))

        var isLoading: Boolean? = null
        sut.isLoading.observeForever { isLoading = it }

        // When
        sut.onTryForFreeButtonPressed()

        // Then
        assertThat(isLoading).isFalse
    }

    @Test
    fun `when onCancelPressed happens, then trigger expected event`() = testBlocking {
        // Given
        val expectedDomain = "test domain"
        val expectedTitle = "test title"
        createSut(expectedDomain, expectedTitle, StoreCreationState.Finished(123L))

        var lastReceivedEvent: MultiLiveEvent.Event? = null
        sut.event.observeForever { lastReceivedEvent = it }

        // When
        sut.onCancelPressed()

        // Then
        assertThat(lastReceivedEvent).isEqualTo(OnCancelPressed)
    }

    private fun createSut(
        expectedDomain: String,
        expectedTitle: String,
        expectedCreationState: StoreCreationState
    ) {
        newStore = mock {
            on { data } doReturn NewStore.NewStoreData(
                domain = expectedDomain,
                name = expectedTitle
            )
        }

        createStore = mock {
            onBlocking {
                invoke(newStore.data.domain, newStore.data.name)
            } doAnswer {
                flowOf(expectedCreationState)
            }
        }

        sut = StoreCreationSummaryViewModel(
            savedStateHandle = savedState,
            createStore = createStore,
            newStore = newStore
        )
    }
}
