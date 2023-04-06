package com.woocommerce.android.ui.login.storecreation.summary

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.ui.login.storecreation.CreateFreeTrialStore
import com.woocommerce.android.ui.login.storecreation.CreateFreeTrialStore.StoreCreationState
import com.woocommerce.android.ui.login.storecreation.NewStore
import com.woocommerce.android.ui.login.storecreation.StoreCreationErrorType.SITE_CREATION_FAILED
import com.woocommerce.android.ui.login.storecreation.summary.StoreCreationSummaryViewModel.OnStoreCreationFailure
import com.woocommerce.android.ui.login.storecreation.summary.StoreCreationSummaryViewModel.OnStoreCreationSuccess
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@OptIn(ExperimentalCoroutinesApi::class)
internal class StoreCreationSummaryViewModelTest: BaseUnitTest() {
    private lateinit var sut: StoreCreationSummaryViewModel
    private lateinit var createStore: CreateFreeTrialStore
    private lateinit var newStore: NewStore
    private val savedState = SavedStateHandle()

    @Test
    fun `when onTryForFreeButtonPressed is called, then start the store creation`() = testBlocking {
        // Given
        val expectedDomain = "test domain"
        val expectedTitle = "test title"
        createSut(expectedDomain, expectedTitle, StoreCreationState.Idle)

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
        createSut(expectedDomain, expectedTitle, StoreCreationState.Finished)

        var lastReceivedEvent: MultiLiveEvent.Event? = null
        sut.event.observeForever { lastReceivedEvent = it }

        sut.onTryForFreeButtonPressed()

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

        sut.onTryForFreeButtonPressed()

        assertThat(lastReceivedEvent).isEqualTo(OnStoreCreationFailure)
    }

    private fun createSut(
        expectedDomain: String,
        expectedTitle: String,
        expectedCreationState: StoreCreationState
    ) {
        val creationStateFlow = MutableStateFlow<StoreCreationState>(StoreCreationState.Idle)

        newStore = mock {
            on { data } doReturn NewStore.NewStoreData(
                domain = expectedDomain,
                name = expectedTitle
            )
        }

        createStore = mock {
            on { state } doReturn creationStateFlow

            onBlocking {
                invoke(newStore.data.domain, newStore.data.name)
            } doAnswer {
                creationStateFlow.value = expectedCreationState
                if (expectedCreationState is StoreCreationState.Finished) {
                    flowOf(123)
                } else {
                    flowOf(null)
                }
            }
        }

        sut = StoreCreationSummaryViewModel(
            savedStateHandle = savedState,
            createStore = createStore,
            newStore = newStore
        )
    }
}
