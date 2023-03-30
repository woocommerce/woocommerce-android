package com.woocommerce.android.ui.login.storecreation.name

import androidx.lifecycle.SavedStateHandle
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock

internal class StoreNamePickerViewModelTest {
    private lateinit var sut: StoreNamePickerViewModel
    private val savedState = SavedStateHandle()

    @Before
    fun setUp() {
        sut = StoreNamePickerViewModel(
            savedStateHandle = savedState,
            newStore = mock(),
            repository = mock(),
            analyticsTrackerWrapper = mock(),
            prefsWrapper = mock()
        )
    }

    @Test
    fun `initial test`() {
        assertThat(true).isTrue
    }
}
