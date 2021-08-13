package com.woocommerce.android.ui.products.addons.order

import android.content.Context
import android.content.SharedPreferences
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.model.ProductAddon
import com.woocommerce.android.ui.products.addons.AddonRepository
import com.woocommerce.android.ui.products.addons.AddonTestFixtures.defaultOrderAttributes
import com.woocommerce.android.ui.products.addons.AddonTestFixtures.defaultOrderedAddonList
import com.woocommerce.android.ui.products.addons.AddonTestFixtures.defaultProductAddonList
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class OrderedAddonViewModelTest : BaseUnitTest() {
    private lateinit var viewModelUnderTest: OrderedAddonViewModel
    private var addonRepositoryMock: AddonRepository = mock()

    @Before
    fun setUp() {
        viewModelUnderTest = OrderedAddonViewModel(
            mock(),
            coroutinesTestRule.testDispatchers,
            addonRepositoryMock
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
    fun `start should trigger a successful ordered addons data parse`() = coroutinesTestRule.testDispatcher.runBlockingTest {
        configureSuccessfulAddonsResponse()

        val expectedResult = defaultOrderedAddonList

        var actualResult: List<ProductAddon>? = null
        viewModelUnderTest.orderedAddonsData.observeForever {
            actualResult = it
        }

        viewModelUnderTest
            .start(
                321,
                123
            )

        assertThat(actualResult).isEqualTo(expectedResult)
    }

    private fun configureSuccessfulAddonsResponse() {
        whenever(addonRepositoryMock.fetchOrderAddonsData(321, 123))
            .doReturn(Pair(defaultProductAddonList, defaultOrderAttributes))
    }
}
