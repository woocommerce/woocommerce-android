package com.woocommerce.android.ui.orders.wooshippinglabels

import com.woocommerce.android.ui.orders.wooshippinglabels.address.GetStatesByCountryCode
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.data.WCLocationModel
import org.wordpress.android.fluxc.store.WCDataStore

@OptIn(ExperimentalCoroutinesApi::class)
class GetStatesByCountryCodeTest : BaseUnitTest() {
    private val dataStore: WCDataStore = mock()
    private val sut = GetStatesByCountryCode(dataStore, coroutinesTestRule.testDispatchers)

    @Test
    fun `when there states are empty return empty list`() = testBlocking {
        val states = emptyList<WCLocationModel>()
        whenever(dataStore.getStates(any())).doReturn(states)

        val result = sut.invoke("US")

        assertThat(result.size).isEqualTo(0)
    }

    @Test
    fun `when there are states then return the expected list`() = testBlocking {
        val states = listOf(
            WCLocationModel(1).also {
                it.code = "CA"
                it.name = "California"
            },
            WCLocationModel(2).also {
                it.code = "FL"
                it.name = "Florida"
            }
        )
        whenever(dataStore.getStates(any())).doReturn(states)

        val result = sut.invoke("US")

        assertThat(result.size).isEqualTo(states.size)
        states.forEachIndexed { i, state ->
            assertThat(result[i].code).isEqualTo(state.code)
            assertThat(result[i].name).isEqualTo(state.name)
        }
    }
}
