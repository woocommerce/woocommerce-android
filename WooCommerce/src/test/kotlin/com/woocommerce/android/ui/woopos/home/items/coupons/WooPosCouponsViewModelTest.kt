package com.woocommerce.android.ui.woopos.home.items.coupons

import com.woocommerce.android.ui.woopos.home.WooPosChildrenToParentEventSender
import com.woocommerce.android.ui.woopos.home.items.WooPosCouponsViewState
import com.woocommerce.android.ui.woopos.home.items.navigation.WooPosItemsNavigator
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import org.junit.Before
import org.junit.Rule
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class WooPosCouponsViewModelTest {
    @Rule
    @JvmField
    val coroutineTestRule = WooPosCoroutineTestRule()

    private val listViewStateManager: WooPosCouponsListViewStateManager = mock()
    private val fromChildToParentEventSender: WooPosChildrenToParentEventSender = mock()
    private val navigator: WooPosItemsNavigator = mock()

    @Before
    fun setUp() {
        whenever(listViewStateManager.viewState).thenReturn(flowOf(WooPosCouponsViewState.Loading()))
    }

    private fun createViewModel() =
        WooPosCouponsViewModel(
            listViewStateManager,
            fromChildToParentEventSender,
            navigator,
        )
}
