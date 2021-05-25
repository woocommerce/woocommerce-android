package com.woocommerce.android.ui.orders.shippinglabels.creation

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.model.ContentsType
import com.woocommerce.android.model.CustomsLine
import com.woocommerce.android.model.CustomsPackage
import com.woocommerce.android.model.Location
import com.woocommerce.android.model.PackageDimensions
import com.woocommerce.android.model.RestrictionType
import com.woocommerce.android.model.ShippingPackage
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.ui.orders.shippinglabels.ShippingLabelRepository
import com.woocommerce.android.ui.products.ParameterRepository
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.store.WCDataStore
import java.math.BigDecimal
import javax.inject.Inject

private val ITN_REGEX = Regex("""^(?:(?:AES X\d{14})|(?:NOEEI 30\.\d{1,2}(?:\([a-z]\)(?:\(\d\))?)?))${'$'}""")
private val HS_TARIFF_NUMBER_REGEX = Regex("""^\d{6}$""")

@HiltViewModel
class ShippingCustomsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    shippingLabelRepository: ShippingLabelRepository,
    parameterRepository: ParameterRepository,
    private val dataStore: WCDataStore,
) : ScopedViewModel(savedStateHandle), ShippingCustomsFormListener {
    companion object {
        private const val KEY_PARAMETERS = "key_parameters"
    }

    val viewStateData = LiveDataDelegate(savedState, ViewState())
    private var viewState by viewStateData

    private val parameters by lazy { parameterRepository.getParameters(KEY_PARAMETERS, savedState) }
    val weightUnit: String
        get() = parameters.weightUnit.orEmpty()

    val currencyUnit: String
        get() = parameters.currencySymbol.orEmpty()

    val countries: List<Location>
        get() = dataStore.getCountries().map { it.toAppModel() }

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
                            itemId = 0L,
                            itemDescription = "Water bottle",
                            hsTariffNumber = "",
                            weight = 1.5f,
                            value = BigDecimal.valueOf(15),
                            originCountry = Location("US", "United States")
                        ),
                        CustomsLine(
                            itemId = 1L,
                            itemDescription = "Water bottle 2",
                            hsTariffNumber = "",
                            weight = 1.5f,
                            value = BigDecimal.valueOf(15),
                            originCountry = Location("US", "United States")
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

    override fun onContentsDescriptionChanged(position: Int, contentsDescription: String) {
        updatePackage(position, viewState.customsPackages[position].copy(contentsDescription = contentsDescription))
    }

    override fun onRestrictionTypeChanged(position: Int, restrictionType: RestrictionType) {
        updatePackage(position, viewState.customsPackages[position].copy(restrictionType = restrictionType))
    }

    override fun onRestrictionDescriptionChanged(position: Int, restrictionDescription: String) {
        updatePackage(
            position,
            viewState.customsPackages[position].copy(restrictionDescription = restrictionDescription)
        )
    }

    override fun onItnChanged(position: Int, itn: String) {
        updatePackage(position, viewState.customsPackages[position].copy(itn = itn))
    }

    override fun onItemDescriptionChanged(packagePosition: Int, linePosition: Int, description: String) {
        val newLine = viewState.customsPackages[packagePosition].lines[linePosition].copy(itemDescription = description)
        updateLine(packagePosition, linePosition, newLine)
    }

    override fun onHsTariffNumberChanged(packagePosition: Int, linePosition: Int, hsTariffNumber: String) {
        val newLine = viewState.customsPackages[packagePosition].lines[linePosition].copy(hsTariffNumber = hsTariffNumber)
        updateLine(packagePosition, linePosition, newLine)
    }

    override fun onWeightChanged(packagePosition: Int, linePosition: Int, weight: String) {
        val weightValue = weight.trim('.').ifEmpty { null }?.toFloat() ?: 0f
        val newLine = viewState.customsPackages[packagePosition].lines[linePosition].copy(weight = weightValue)
        updateLine(packagePosition, linePosition, newLine)
    }

    override fun onItemValueChanged(packagePosition: Int, linePosition: Int, itemValue: String) {
        val value = itemValue.trim('.').ifEmpty { null }?.toBigDecimal() ?: BigDecimal.ZERO
        val newLine = viewState.customsPackages[packagePosition].lines[linePosition].copy(value = value)
        updateLine(packagePosition, linePosition, newLine)
    }

    override fun onOriginCountryChanged(packagePosition: Int, linePosition: Int, country: Location) {
        val newLine = viewState.customsPackages[packagePosition].lines[linePosition].copy(originCountry = country)
        updateLine(packagePosition, linePosition, newLine)
    }

    private fun updatePackage(position: Int, item: CustomsPackage) {
        // Early return if the same item is passed
        if (viewState.customsPackages[position] == item) return

        val customsPackages = viewState.customsPackages.toMutableList()
        customsPackages[position] = item
        viewState = viewState.copy(customsPackages = customsPackages)
    }

    private fun updateLine(packagePosition: Int, linePosition: Int, line: CustomsLine) {
        val customsPackage = viewState.customsPackages[packagePosition]
        val customsLines = customsPackage.lines.toMutableList()
        customsLines[linePosition] = line
        updatePackage(packagePosition, customsPackage.copy(lines = customsLines))
    }

    fun onDoneButtonClicked() {
        triggerEvent(Exit)
    }

    @Parcelize
    data class ViewState(
        val customsPackages: List<CustomsPackage> = emptyList()
    ) : Parcelable
}

val CustomsPackage.isItnValid
    get() = itn.isEmpty() || ITN_REGEX.matches(itn)

val CustomsLine.isHsTariffNumberValid
    get() = hsTariffNumber.isEmpty() || HS_TARIFF_NUMBER_REGEX.matches(hsTariffNumber)
