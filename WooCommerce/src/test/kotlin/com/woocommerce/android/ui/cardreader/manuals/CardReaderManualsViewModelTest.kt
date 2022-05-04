package com.woocommerce.android.ui.cardreader.manuals

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.viewmodel.BaseUnitTest
import org.junit.Assert.*

import org.junit.Before

class CardReaderManualsViewModelTest : BaseUnitTest() {
    private lateinit var viewModel: CardReaderManualsViewModel
    private val savedStateHandle: SavedStateHandle = SavedStateHandle()

    @Before
    fun setUp() {
        viewModel = CardReaderManualsViewModel(savedStateHandle)
    }
}
