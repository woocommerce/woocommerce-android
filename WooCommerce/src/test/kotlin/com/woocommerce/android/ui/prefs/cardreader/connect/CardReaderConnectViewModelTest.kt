package com.woocommerce.android.ui.prefs.cardreader.connect

import com.nhaarman.mockitokotlin2.mock
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.viewmodel.TestDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.InternalCoroutinesApi
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@InternalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class CardReaderConnectViewModelTest {
    private lateinit var viewModel: CardReaderConnectViewModel

    @Before
    fun setUp() {
        viewModel = CardReaderConnectViewModel(
            mock(),
            CoroutineDispatchers(TestDispatcher, TestDispatcher, TestDispatcher),
            mock()
        )
    }

    @Test
    fun foo() {

    }
}
