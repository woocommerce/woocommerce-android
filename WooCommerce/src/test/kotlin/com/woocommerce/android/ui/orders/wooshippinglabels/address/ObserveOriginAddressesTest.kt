package com.woocommerce.android.ui.orders.wooshippinglabels.address

import com.woocommerce.android.ui.orders.wooshippinglabels.datasource.WooShippingAddressDataStore
import com.woocommerce.android.ui.orders.wooshippinglabels.models.OriginShippingAddress
import com.woocommerce.android.viewmodel.BaseUnitTest
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ObserveOriginAddressesTest : BaseUnitTest() {
    private val dataStore: WooShippingAddressDataStore = mock()
    private val fetchOriginAddresses: FetchOriginAddresses = mock()
    private val defaultOriginAddresses = OriginShippingAddress(
        id = "1",
        firstName = "John",
        lastName = "Doe",
        company = "",
        address1 = "123 Main St",
        address2 = "",
        city = "Anytown",
        state = "CA",
        postcode = "12345",
        country = "US",
        email = "william.henry.harrison@example-pet-store.com",
        phone = "555-555-5555",
        isDefault = true,
        isVerified = true
    )

    val sut = ObserveOriginAddresses(
        addressDataStore = dataStore,
        fetchOriginAddresses = fetchOriginAddresses
    )

    @Test
    fun `when there is NO cached data and fetch account settings fails then return null`() = testBlocking {
        whenever(dataStore.observeOriginAddresses()).doReturn(flowOf(null))
        whenever(fetchOriginAddresses.invoke()).doReturn(Result.failure(Exception("Random error")))
        val result = sut.invoke().toList()

        assertTrue(result.size == 1)
        assertTrue(result.first() == null)
    }

    @Test
    fun `when there is cached data and fetch account settings fails then return null`() = testBlocking {
        val cachedResult = listOf(defaultOriginAddresses)
        whenever(dataStore.observeOriginAddresses()).doReturn(flowOf(cachedResult))
        whenever(fetchOriginAddresses.invoke()).doReturn(Result.failure(Exception("Random error")))
        val result = sut.invoke().toList()

        assertTrue(result.size == 2)
        assertTrue(result.first() == cachedResult)
        assertTrue(result.last() == null)
    }

    @Test
    fun `refresh data only once`() = testBlocking {
        val firstResult = listOf(defaultOriginAddresses)
        val secondResult = listOf(defaultOriginAddresses.copy(id = "updated id"))
        whenever(dataStore.observeOriginAddresses()).doReturn(
            flowOf(firstResult, secondResult)
        )
        whenever(fetchOriginAddresses.invoke()).doReturn(Result.failure(Exception("Random error")))

        sut.invoke().toList()

        verify(fetchOriginAddresses).invoke()
    }
}
