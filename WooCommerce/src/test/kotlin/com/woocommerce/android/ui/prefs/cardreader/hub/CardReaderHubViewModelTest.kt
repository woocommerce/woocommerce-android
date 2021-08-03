package com.woocommerce.android.ui.prefs.cardreader.hub

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.viewmodel.BaseUnitTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test

class CardReaderHubViewModelTest : BaseUnitTest() {
    private lateinit var viewModel: CardReaderHubViewModel

    @Before
    fun setUp() {
        viewModel = CardReaderHubViewModel(SavedStateHandle())
    }

    @Test
    fun foo() {
        assertThat(viewModel).isNotNull
    }
}
