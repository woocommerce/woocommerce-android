package com.woocommerce.android.ui.orders.shippinglabels.creation

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.extensions.sumByBigDecimal
import com.woocommerce.android.model.ContentsType
import com.woocommerce.android.model.CustomsLine
import com.woocommerce.android.model.CustomsPackage
import com.woocommerce.android.model.Location
import com.woocommerce.android.model.PackageDimensions
import com.woocommerce.android.model.RestrictionType
import com.woocommerce.android.model.ShippingPackage
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.ui.orders.shippinglabels.ShippingLabelRepository
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingCustomsViewModel.LineValidationState
import com.woocommerce.android.ui.products.ParameterRepository
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.store.WCDataStore
import java.math.BigDecimal
import javax.inject.Inject

private val ITN_REGEX = Regex("""^(?:(?:AES X\d{14})|(?:NOEEI 30\.\d{1,2}(?:\([a-z]\)(?:\(\d\))?)?))${'$'}""")
private val HS_TARIFF_NUMBER_REGEX = Regex("""^\d{6}$""")
private val USPS_ITN_REQUIRED_DESTINATIONS = arrayOf("IR", "SY", "KP", "CU", "SD")

@HiltViewModel
class ShippingCustomsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    shippingLabelRepository: ShippingLabelRepository,
    parameterRepository: ParameterRepository,
    private val dataStore: WCDataStore,
    private val resourceProvider: ResourceProvider
) : ScopedViewModel(savedStateHandle), ShippingCustomsFormListener {
    companion object {
        private const val KEY_PARAMETERS = "key_parameters"
    }

    val viewStateData = LiveDataDelegate(savedState, ViewState())
    private var viewState by viewStateData

    private val args: ShippingCustomsFragmentArgs by savedStateHandle.navArgs()

    private val parameters by lazy { parameterRepository.getParameters(KEY_PARAMETERS, savedState) }
    val weightUnit: String
        get() = parameters.weightUnit.orEmpty()

    val currencyUnit: String
        get() = parameters.currencySymbol.orEmpty()

    val countries: List<Location>
        get() = dataStore.getCountries().map { it.toAppModel() }

    init {
        // TODO fake data
        val fakePackages = listOf(
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
                        quantity = 5.0,
                        weight = 1.5f,
                        value = BigDecimal.valueOf(15),
                        originCountry = Location("US", "United States")
                    ),
                    CustomsLine(
                        itemId = 1L,
                        itemDescription = "Water bottle 2",
                        hsTariffNumber = "",
                        quantity = 2.0,
                        weight = 1.5f,
                        value = BigDecimal.valueOf(15),
                        originCountry = Location("US", "United States")
                    )
                )
            )
        )
        viewState = ViewState(
            customsPackages = fakePackages.map {
                CustomsPackageUiState(
                    data = it,
                    validationState = validatePackage(it)
                )
            }
        )
    }

    override fun onReturnToSenderChanged(position: Int, returnToSender: Boolean) {
        updatePackage(position, viewState.customsPackages[position].data.copy(returnToSender = returnToSender))
    }

    override fun onContentsTypeChanged(position: Int, contentsType: ContentsType) {
        updatePackage(position, viewState.customsPackages[position].data.copy(contentsType = contentsType))
    }

    override fun onContentsDescriptionChanged(position: Int, contentsDescription: String) {
        updatePackage(
            position,
            viewState.customsPackages[position].data.copy(contentsDescription = contentsDescription)
        )
    }

    override fun onRestrictionTypeChanged(position: Int, restrictionType: RestrictionType) {
        updatePackage(position, viewState.customsPackages[position].data.copy(restrictionType = restrictionType))
    }

    override fun onRestrictionDescriptionChanged(position: Int, restrictionDescription: String) {
        updatePackage(
            position,
            viewState.customsPackages[position].data.copy(restrictionDescription = restrictionDescription)
        )
    }

    override fun onItnChanged(position: Int, itn: String) {
        updatePackage(position, viewState.customsPackages[position].data.copy(itn = itn))
    }

    override fun onItemDescriptionChanged(packagePosition: Int, linePosition: Int, description: String) {
        val newLine = viewState.customsPackages[packagePosition].data.lines[linePosition].copy(itemDescription = description)
        updateLine(packagePosition, linePosition, newLine)
    }

    override fun onHsTariffNumberChanged(packagePosition: Int, linePosition: Int, hsTariffNumber: String) {
        val newLine = viewState.customsPackages[packagePosition].data.lines[linePosition].copy(hsTariffNumber = hsTariffNumber)
        updateLine(packagePosition, linePosition, newLine)
    }

    override fun onWeightChanged(packagePosition: Int, linePosition: Int, weight: String) {
        val weightValue = weight.trim('.').ifEmpty { null }?.toFloat() ?: 0f
        val newLine = viewState.customsPackages[packagePosition].data.lines[linePosition].copy(weight = weightValue)
        updateLine(packagePosition, linePosition, newLine)
    }

    override fun onItemValueChanged(packagePosition: Int, linePosition: Int, itemValue: String) {
        val value = itemValue.trim('.').ifEmpty { null }?.toBigDecimal() ?: BigDecimal.ZERO
        val newLine = viewState.customsPackages[packagePosition].data.lines[linePosition].copy(value = value)
        updateLine(packagePosition, linePosition, newLine)
    }

    override fun onOriginCountryChanged(packagePosition: Int, linePosition: Int, country: Location) {
        val newLine = viewState.customsPackages[packagePosition].data.lines[linePosition].copy(originCountry = country)
        updateLine(packagePosition, linePosition, newLine)
    }

    private fun updateLine(packagePosition: Int, linePosition: Int, line: CustomsLine) {
        val customsPackage = viewState.customsPackages[packagePosition]
        val customsLines = customsPackage.data.lines.toMutableList()
        customsLines[linePosition] = line
        updatePackage(packagePosition, customsPackage.data.copy(lines = customsLines))
    }

    private fun updatePackage(position: Int, item: CustomsPackage) {
        // Early return if the same item is passed
        if (viewState.customsPackages[position].data == item) return

        val customsPackages = viewState.customsPackages.toMutableList()
        customsPackages[position] = CustomsPackageUiState(item, validatePackage(item))
        viewState = viewState.copy(customsPackages = customsPackages)
    }

    private fun validatePackage(item: CustomsPackage): PackageValidationState {
        return PackageValidationState(
            itnErrorMessage = validateItn(item),
            linesValidationState = item.lines.map {
                LineValidationState(
                    hsTariffErrorMessage = validateHsTarrif(it.hsTariffNumber)
                )
            }
        )
    }

    fun onDoneButtonClicked() {
        triggerEvent(Exit)
    }

    private fun validateItn(customsPackage: CustomsPackage): String? {
        val itn = customsPackage.itn
        return if (itn.isNotEmpty()) {
            if (ITN_REGEX.matches(itn)) null
            else resourceProvider.getString(R.string.shipping_label_customs_itn_invalid_format)
        } else {
            val classesAbove2500usd = customsPackage.lines
                .filter { it.hsTariffNumber.isNotEmpty() && validateHsTarrif(it.hsTariffNumber) == null }
                .groupBy { it.hsTariffNumber }
                .map { entry -> Pair(entry.key, entry.value.sumByBigDecimal { it.quantity.toBigDecimal() * it.value }) }
                .filter { (_, value) -> value > BigDecimal.valueOf(2500.0) }

            when {
                classesAbove2500usd.isNotEmpty() -> {
                    resourceProvider.getString(
                        R.string.shipping_label_customs_itn_required_items_over_2500,
                        classesAbove2500usd[0].first
                    )
                }
                USPS_ITN_REQUIRED_DESTINATIONS.contains(args.destinationCountryCode) -> {
                    val destinationCountryName = countries.firstOrNull { it.code == args.destinationCountryCode }
                        ?.name ?: args.destinationCountryCode

                    resourceProvider.getString(
                        R.string.shipping_label_customs_itn_required_country,
                        destinationCountryName
                    )
                }
                else -> null
            }
        }
    }

    private fun validateHsTarrif(hsTariffNumber: String): String? {
        return if (hsTariffNumber.isEmpty() || HS_TARIFF_NUMBER_REGEX.matches(hsTariffNumber)) null
        else resourceProvider.getString(R.string.shipping_label_customs_itn_invalid_format)
    }

    @Parcelize
    data class ViewState(
        val customsPackages: List<CustomsPackageUiState> = emptyList()
    ) : Parcelable

    @Parcelize
    data class CustomsPackageUiState(
        val data: CustomsPackage,
        val validationState: PackageValidationState
    ) : Parcelable {
        val customsLinesUiState: List<CustomsLineUiState>
            get() = data.lines.mapIndexed { index, customsLine ->
                CustomsLineUiState(customsLine, validationState.linesValidationState[index])
            }
    }

    @Parcelize
    data class PackageValidationState(
        val itnErrorMessage: String? = null,
        val linesValidationState: List<LineValidationState> = emptyList()
    ) : Parcelable

    @Parcelize
    data class LineValidationState(
        val hsTariffErrorMessage: String? = null
    ) : Parcelable
}

typealias CustomsLineUiState = Pair<CustomsLine, LineValidationState>

