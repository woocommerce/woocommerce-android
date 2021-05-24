package com.woocommerce.android.ui.orders.shippinglabels.creation

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.model.ContentsType
import com.woocommerce.android.model.CustomsLine
import com.woocommerce.android.model.CustomsPackage
import com.woocommerce.android.model.PackageDimensions
import com.woocommerce.android.model.RestrictionType
import com.woocommerce.android.model.ShippingPackage
import com.woocommerce.android.ui.orders.shippinglabels.ShippingLabelRepository
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.parcelize.Parcelize
import java.math.BigDecimal
import javax.inject.Inject

@HiltViewModel
class ShippingCustomsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    shippingLabelRepository: ShippingLabelRepository
) : ScopedViewModel(savedStateHandle), ShippingCustomsFormListener {
    val viewStateData = LiveDataDelegate(savedState, ViewState())
    private var viewState by viewStateData

    init {
        // TODO fake data
        viewState = ViewState(
            customsPackages = listOf(
                CustomsPackage(
                    id = "default_package",
                    box = ShippingPackage(
                        id = "small_package",
                        title = "Small Box",
                        dimensions = PackageDimensions(10f, 10f, 2f),
                        boxWeight = 0f,
                        category = "USPS",
                        isLetter = false
                    ),
                    returnToSender = true,
                    contentsType = ContentsType.Merchandise,
                    restrictionType = RestrictionType.None,
                    itn = "",
                    lines = listOf(
                        CustomsLine(
                            itemDescription = "Water bottle",
                            hsTariffNumber = "",
                            weight = 1.5f,
                            value = BigDecimal.valueOf(15),
                            originCountry = "United States"
                        ),
                        CustomsLine(
                            itemDescription = "Water bottle 2",
                            hsTariffNumber = "",
                            weight = 1.5f,
                            value = BigDecimal.valueOf(15),
                            originCountry = "United States"
                        )
                    )
                )
            )
        )
    }

    override fun onReturnToSenderChanged(position: Int, returnToSender: Boolean) {
        updatePackage(position, viewState.customsPackages[position].copy(returnToSender = returnToSender))
    }

    override fun onContentsTypeChanged(position: Int, contentsType: ContentsType) {
        updatePackage(position, viewState.customsPackages[position].copy(contentsType = contentsType))
    }

    override fun onRestrictionTypeChanged(position: Int, restrictionType: RestrictionType) {
        updatePackage(position, viewState.customsPackages[position].copy(restrictionType = restrictionType))
    }

    override fun onItnChanged(position: Int, itn: String) {
        updatePackage(position, viewState.customsPackages[position].copy(itn = itn))
    }

    private fun updatePackage(position: Int, item: CustomsPackage) {
        val customsPackages = viewState.customsPackages.toMutableList()
        customsPackages[position] = item
        viewState = viewState.copy(customsPackages = customsPackages)
    }

    fun onDoneButtonClicked() {
        triggerEvent(Exit)
    }

    @Parcelize
    data class ViewState(
        val customsPackages: List<CustomsPackage> = emptyList()
    ) : Parcelable
}
