package com.woocommerce.android.ui.products.addons.order

import android.content.Context
import android.content.SharedPreferences
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.wordpress.android.fluxc.model.order.OrderIdentifier

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class OrderedAddonViewModelTest : BaseUnitTest() {
    private lateinit var viewModelUnderTest: OrderedAddonViewModel

    @Before
    fun setUp() {
        viewModelUnderTest = OrderedAddonViewModel(
            mock(),
            coroutinesTestRule.testDispatchers,
            mock {
                on { fetchOrderAddonsData(any(), any()) }
                    .thenReturn(Pair(listOf(), listOf()))
            }
        )

        mock<SharedPreferences.Editor> { whenever(it.putBoolean(any(), any())).thenReturn(mock()) }
            .let { editor ->
                mock<SharedPreferences> { whenever(it.edit()).thenReturn(editor) }
            }
            .let { prefs ->
                mock<Context> {
                    whenever(it.applicationContext).thenReturn(it)
                    whenever(it.getSharedPreferences(any(), any())).thenReturn(prefs)
                    AppPrefs.init(it)
                }
            }

    }

    @Test
    fun `test`() = coroutinesTestRule.testDispatcher.runBlockingTest {
        viewModelUnderTest.init(
            OrderIdentifier(123, 123),
            123
        )
    }
}
