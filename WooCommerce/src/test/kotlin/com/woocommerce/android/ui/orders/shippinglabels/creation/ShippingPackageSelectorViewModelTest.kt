package com.woocommerce.android.ui.orders.shippinglabels.creation

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.initSavedStateHandle
import com.woocommerce.android.model.PackageDimensions
import com.woocommerce.android.model.ShippingPackage
import com.woocommerce.android.ui.orders.shippinglabels.ShippingLabelRepository
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingPackageSelectorViewModel.ViewState
import com.woocommerce.android.ui.products.ParameterRepository
import com.woocommerce.android.ui.products.models.SiteParameters
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult

@ExperimentalCoroutinesApi
class ShippingPackageSelectorViewModelTest : BaseUnitTest() {
    private val availablePackages = listOf(
        ShippingPackage(
            "id1", "title1", false, "provider1", PackageDimensions(1.0f, 1.0f, 1.0f), 1f
        ),
        ShippingPackage(
            "id2", "title2", false, "provider2", PackageDimensions(1.0f, 1.0f, 1.0f), 1f
        )
    )
    private val parameterRepository: ParameterRepository = mock()
    private val shippingRepository: ShippingLabelRepository = mock()

    private val savedState = ShippingPackageSelectorFragmentArgs(0).initSavedStateHandle()

    private lateinit var viewModel: ShippingPackageSelectorViewModel

    @Before
    fun setup() {
        whenever(parameterRepository.getParameters(any(), any<SavedStateHandle>())).thenReturn(
            SiteParameters(
                currencyCode = "USD",
                currencySymbol = "$",
                currencyFormattingParameters = null,
                weightUnit = "kg",
                dimensionUnit = "cm",
                gmtOffset = 0f
            )
        )
        testBlocking {
            whenever(shippingRepository.getShippingPackages()).thenReturn(WooResult(availablePackages))
        }
        viewModel = ShippingPackageSelectorViewModel(
            savedState,
            parameterRepository,
            shippingRepository
        )
    }

    @Test
    fun `display list of packages`() = testBlocking {
        var viewState: ViewState? = null
        viewModel.viewStateData.observeForever { _, state -> viewState = state }

        assertThat(viewState!!.packagesList).isEqualTo(availablePackages.toList())
        assertThat(viewModel.dimensionUnit).isEqualTo("cm")
    }

    @Suppress("UNCHECKED_CAST")
    @Test
    fun `select a package`() {
        var event: MultiLiveEvent.Event? = null
        viewModel.event.observeForever { event = it }

        viewModel.onPackageSelected(availablePackages.first())

        assertThat(event).isInstanceOf(ExitWithResult::class.java)
        val result = (event as ExitWithResult<ShippingPackageSelectorResult>).data
        assertThat(result.position).isEqualTo(0)
        assertThat(result.selectedPackage).isEqualTo(availablePackages[0])
    }
}
