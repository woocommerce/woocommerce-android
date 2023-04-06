package com.woocommerce.android.ui.login.storecreation.summary

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.ui.login.storecreation.CreateFreeTrialStore
import com.woocommerce.android.ui.login.storecreation.CreateFreeTrialStore.StoreCreationState
import com.woocommerce.android.viewmodel.BaseUnitTest
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
    lateinit var sut: StoreCreationSummaryViewModel
    lateinit var createStore: CreateFreeTrialStore
    private val savedState = SavedStateHandle()

    @Test
    fun `when onTryForFreeButtonPressed is called, then start the store creation`() = testBlocking {
        val expectedDomain = "test domain"
        val expectedTitle = "test title"
        createSut(expectedDomain, expectedTitle)

        sut.onTryForFreeButtonPressed()

        verify(createStore).invoke(expectedDomain, expectedTitle)
    }

    private fun createSut(
        expectedDomain: String,
        expectedTitle: String,
        expectedCreationState: StoreCreationState = StoreCreationState.Finished
    ) {
        val creationStateFlow = MutableStateFlow<StoreCreationState>(StoreCreationState.Idle)

        createStore = mock {
            on { state } doReturn creationStateFlow

            onBlocking {
                invoke(expectedDomain, expectedTitle)
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
            createStore = createStore
        )
    }
}
