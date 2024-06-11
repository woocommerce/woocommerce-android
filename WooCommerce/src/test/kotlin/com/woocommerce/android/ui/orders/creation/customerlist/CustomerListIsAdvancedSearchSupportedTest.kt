package com.woocommerce.android.ui.orders.creation.customerlist

import com.woocommerce.android.util.GetWooCorePluginCachedVersion
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class CustomerListIsAdvancedSearchSupportedTest : BaseUnitTest() {
    private val getWooVersion: GetWooCorePluginCachedVersion = mock()

    private val action = CustomerListIsAdvancedSearchSupported(
        dispatchers = coroutinesTestRule.testDispatchers,
        getWooVersion = getWooVersion,
    )

    @Test
    fun `given version lower than 8, when action invoked, then false returned`() = testBlocking {
        // GIVEN
        val version = "7.9.9"
        whenever(getWooVersion()).thenReturn(version)

        // WHEN
        val result = action()

        // THEN
        assertThat(result).isFalse()
    }

    @Test
    fun `given version null, when action invoked, then false returned`() = testBlocking {
        // GIVEN
        val version = null
        whenever(getWooVersion()).thenReturn(version)

        // WHEN
        val result = action()

        // THEN
        assertThat(result).isFalse()
    }

    @Test
    fun `given version 8, when action invoked, then true returned`() = testBlocking {
        // GIVEN
        val version = "8.0.0"
        whenever(getWooVersion()).thenReturn(version)

        // WHEN
        val result = action()

        // THEN
        assertThat(result).isTrue()
    }

    @Test
    fun `given version more than 8, when action invoked, then true returned`() = testBlocking {
        // GIVEN
        val version = "8.0.1"
        whenever(getWooVersion()).thenReturn(version)

        // WHEN
        val result = action()

        // THEN
        assertThat(result).isTrue()
    }
}
