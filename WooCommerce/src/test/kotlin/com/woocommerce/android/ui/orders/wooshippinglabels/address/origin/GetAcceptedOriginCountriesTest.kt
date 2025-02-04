package com.woocommerce.android.ui.orders.wooshippinglabels.address.origin

import com.woocommerce.android.model.Location
import com.woocommerce.android.ui.orders.wooshippinglabels.address.GetAllCountries
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GetAcceptedOriginCountriesTest : BaseUnitTest() {
    private val getCountries: GetAllCountries = mock()
    private val sut = GetAcceptedOriginCountries(getCountries)

    // Only US should be accepted
    private val countries = listOf(
        Location("US", "United States"),
        Location("UK", "United Kingdom"),
        Location("AR", "Argentina"),
        Location("BR", "Brazil")
    )

    @Test
    fun `when getCountries fails then return failure`() = testBlocking {
        val exception = Exception("error")
        whenever(getCountries()).doReturn(Result.failure(exception))

        val result = sut.invoke()

        assert(result.isFailure)
    }

    @Test
    fun `when getCountries succeeds and return only accepted countries`() = testBlocking {
        whenever(getCountries()).doReturn(Result.success(countries))

        val result = sut.invoke()

        assert(result.isSuccess)
        val acceptedCountries = result.getOrNull()
        assert(acceptedCountries!!.size == 1)
        assert(acceptedCountries[0].code == "US")
    }
}
