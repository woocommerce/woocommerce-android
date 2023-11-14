package com.woocommerce.android.ui.orders.shippinglabels.creation

import com.woocommerce.android.R
import com.woocommerce.android.initSavedStateHandle
import com.woocommerce.android.model.PackageDimensions
import com.woocommerce.android.model.ShippingPackage
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

@ExperimentalCoroutinesApi
class ShippingLabelCreatePackageViewModelTest : BaseUnitTest() {
    private lateinit var viewModel: ShippingLabelCreatePackageViewModel

    private val packageToCreate = ShippingPackage(
        id = "",
        title = "Test Package",
        isLetter = false,
        category = ShippingPackage.CUSTOM_PACKAGE_CATEGORY,
        PackageDimensions(1.0f, 1.0f, 1.0f),
        1f
    )

    fun setup() {
        val savedState = ShippingLabelCreatePackageFragmentArgs(0).initSavedStateHandle()
        viewModel = ShippingLabelCreatePackageViewModel(savedState)
    }

    @Test
    fun `when handling package creation success, then show Snackbar and navigate to edit label package screen`() {
        setup()
        val events = mutableListOf<Event>()
        viewModel.event.observeForever {
            events.add(it)
        }

        viewModel.onPackageCreated(packageToCreate)

        assertThat(events[0]).isEqualTo(
            ShowSnackbar(
                message = R.string.shipping_label_create_custom_package_success_message,
                args = arrayOf(packageToCreate.title)
            )
        )
        assertThat(events[1]).isEqualTo(
            Event.ExitWithResult(
                ShippingPackageSelectorResult(
                    position = 0,
                    selectedPackage = packageToCreate
                )
            )
        )
    }
}
