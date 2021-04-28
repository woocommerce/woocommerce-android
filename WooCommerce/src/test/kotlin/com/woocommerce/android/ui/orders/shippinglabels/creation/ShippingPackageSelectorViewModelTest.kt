package com.woocommerce.android.ui.orders.shippinglabels.creation

import androidx.lifecycle.SavedStateHandle
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.whenever
import com.woocommerce.android.model.PackageDimensions
import com.woocommerce.android.model.ShippingPackage
import com.woocommerce.android.ui.orders.shippinglabels.ShippingLabelRepository
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingPackageSelectorViewModel.ViewState
import com.woocommerce.android.ui.products.ParameterRepository
import com.woocommerce.android.ui.products.models.SiteParameters
import com.woocommerce.android.util.CoroutineTestRule
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.SavedStateWithArgs
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
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

    @get:Rule
    var coroutinesTestRule = CoroutineTestRule()
    private val savedState: SavedStateWithArgs = spy(
        SavedStateWithArgs(
            SavedStateHandle(),
            null,
            ShippingPackageSelectorFragmentArgs(0)
        )
    )

    private lateinit var viewModel: ShippingPackageSelectorViewModel

    @Before
    fun setup() {
        whenever(parameterRepository.getParameters(any(), any())).thenReturn(
            SiteParameters("", "", "cm", 0f)
        )
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(shippingRepository.getShippingPackages()).thenReturn(WooResult(availablePackages))
        }
        viewModel = ShippingPackageSelectorViewModel(
            savedState,
            coroutinesTestRule.testDispatchers,
            parameterRepository,
            shippingRepository
        )
    }

    @Test
    fun `display list of packages`() = coroutinesTestRule.testDispatcher.runBlockingTest {
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
