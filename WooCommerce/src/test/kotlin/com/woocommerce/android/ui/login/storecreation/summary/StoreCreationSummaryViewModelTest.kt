package com.woocommerce.android.ui.login.storecreation.summary

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.ui.login.storecreation.CreateFreeTrialStore
import com.woocommerce.android.ui.login.storecreation.CreateFreeTrialStore.StoreCreationState
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

internal class StoreCreationSummaryViewModelTest: BaseUnitTest() {
    lateinit var sut: StoreCreationSummaryViewModel
    lateinit var createStore: CreateFreeTrialStore
    private val savedState = SavedStateHandle()

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
