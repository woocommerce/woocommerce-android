package com.woocommerce.android.ui.orders.shippinglabels.creation

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.extensions.sumByBigDecimal
import com.woocommerce.android.model.ContentsType
import com.woocommerce.android.model.CustomsLine
import com.woocommerce.android.model.CustomsLine.Companion.MINIMUM_EU_DESCRIPTION_LENGTH
import com.woocommerce.android.model.CustomsPackage
import com.woocommerce.android.model.Location
import com.woocommerce.android.model.RestrictionType
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingCustomsViewModel.LineValidationState
import com.woocommerce.android.ui.products.ParameterRepository
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.store.WCDataStore
import java.math.BigDecimal
import javax.inject.Inject

/**
 * For information regarding the format of the ITN, check the Appendix A of
 * [Export Compliance Customs Data Requirements](https://postalpro.usps.com/node/3973)
 */
private val ITN_REGEX = Regex("""^(?:(?:AES X\d{14})|(?:NOEEI 30\.\d{1,2}(?:\([a-z]\)(?:\(\d\))?)?))${'$'}""")

/**
 * HS tariff number has to be 6 digits number.
 */
private val HS_TARIFF_NUMBER_REGEX = Regex("""^\d{6}$""")

private val USPS_ITN_REQUIRED_DESTINATIONS = arrayOf("IR", "SY", "KP", "CU", "SD")

@HiltViewModel
class ShippingCustomsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val selectedSide: SelectedSite,
    parameterRepository: ParameterRepository,
    private val dataStore: WCDataStore,
    private val resourceProvider: ResourceProvider
) : ScopedViewModel(savedStateHandle), ShippingCustomsFormListener {
    companion object {
        private const val KEY_PARAMETERS = "key_parameters"
    }

    /**
     * Saving more than necessary into the SavedState has associated risks which were not known at the time this
     * field was implemented - after we ensure we don't save unnecessary data, we can
     * replace @Suppress("OPT_IN_USAGE") with @OptIn(LiveDelegateSavedStateAPI::class).
     */
    @Suppress("OPT_IN_USAGE")
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

    val isEUShippingScenario
        get() = args.isEUShippingScenario

    init {
        loadData()
    }

    private fun loadData() {
        launch {
            viewState = viewState.copy(isProgressViewShown = true)
            if (!loadCountriesIfNeeded()) return@launch
            val packagesUiStates = args.customsPackages.toList().ifEmpty {
                createDefaultCustomPackages()
            }.mapIndexed { index, item ->
                CustomsPackageUiState(item, item.validate(), isExpanded = index == 0)
            }
            viewState = viewState.copy(
                isProgressViewShown = false,
                isShippingNoticeVisible = isEUShippingScenario,
                customsPackages = packagesUiStates
            )
        }
    }

    private fun createDefaultCustomPackages(): List<CustomsPackage> {
        return args.shippingPackages.map { labelPackage ->
            CustomsPackage(
                id = labelPackage.packageId,
                labelPackage = labelPackage,
                contentsType = ContentsType.Merchandise,
                restrictionType = RestrictionType.None,
                returnToSender = true,
                itn = "",
                lines = labelPackage.items.map { item ->
                    val attributes = item.attributesDescription.ifEmpty { null }?.let { " $it" } ?: ""
                    val defaultDescription = item.name.substringBefore("-").trim() + attributes
                    CustomsLine(
                        productId = item.productId,
                        itemDescription = defaultDescription,
                        quantity = item.quantity,
                        value = item.value,
                        weight = item.weight,
                        hsTariffNumber = "",
                        originCountry = countries.first { it.code == args.originCountryCode }
                    )
                }
            )
        }
    }

    private suspend fun loadCountriesIfNeeded(): Boolean {
        if (countries.isEmpty()) {
            val result = dataStore.fetchCountriesAndStates(selectedSide.get())
            if (result.isError) {
                triggerEvent(ShowSnackbar(R.string.error_generic))
                triggerEvent(Exit)
                return false
            }
        }
        return true
    }

    fun onDoneButtonClicked() {
        triggerEvent(ExitWithResult(viewState.customsPackages.map { it.data }))
    }

    fun onBackButtonClicked() {
        triggerEvent(Exit)
    }

    fun onShippingNoticeLearnMoreClicked(learnMoreUrl: String) {
        triggerEvent(OpenShippingInstructions(learnMoreUrl))
    }

    fun onShippingNoticeDismissClicked() {
        viewState = viewState.copy(isShippingNoticeVisible = false)
    }

    override fun onPackageExpandedChanged(position: Int, isExpanded: Boolean) {
        val customsPackages = viewState.customsPackages.toMutableList()
        customsPackages[position] = customsPackages[position].copy(isExpanded = isExpanded)
        viewState = viewState.copy(customsPackages = customsPackages)
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
        val newLine = viewState.customsPackages[packagePosition].data
            .lines[linePosition].copy(itemDescription = description)
        updateLine(packagePosition, linePosition, newLine)
    }

    override fun onHsTariffNumberChanged(packagePosition: Int, linePosition: Int, hsTariffNumber: String) {
        val newLine = viewState.customsPackages[packagePosition].data
            .lines[linePosition].copy(hsTariffNumber = hsTariffNumber)
        updateLine(packagePosition, linePosition, newLine)
    }

    override fun onWeightChanged(packagePosition: Int, linePosition: Int, weight: String) {
        val weightValue = weight.trim('.').ifEmpty { null }?.toFloat()
        val newLine = viewState.customsPackages[packagePosition].data.lines[linePosition].copy(weight = weightValue)
        updateLine(packagePosition, linePosition, newLine)
    }

    override fun onItemValueChanged(packagePosition: Int, linePosition: Int, itemValue: String) {
        val value = itemValue.trim('.').ifEmpty { null }?.toBigDecimal()
        val newLine = viewState.customsPackages[packagePosition].data.lines[linePosition].copy(value = value)
        updateLine(packagePosition, linePosition, newLine)
    }

    override fun onOriginCountryChanged(packagePosition: Int, linePosition: Int, country: Location) {
        val newLine = viewState.customsPackages[packagePosition].data.lines[linePosition].copy(originCountry = country)
        updateLine(packagePosition, linePosition, newLine)
    }

    override fun onShippingNoticeClicked() {
        viewState = viewState.copy(isShippingNoticeVisible = true)
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
        customsPackages[position] = customsPackages[position].copy(data = item, validationState = item.validate())
        viewState = viewState.copy(customsPackages = customsPackages)
    }

    private fun CustomsPackage.validate(): PackageValidationState {
        fun CustomsPackage.validateContentsType(): String? {
            return when {
                contentsType != ContentsType.Other -> null
                contentsDescription.isNullOrBlank() -> {
                    resourceProvider.getString(R.string.shipping_label_customs_contents_type_description_missing)
                }
                else -> null
            }
        }

        fun CustomsPackage.validateRestrictionType(): String? {
            return when {
                restrictionType != RestrictionType.Other -> null
                restrictionDescription.isNullOrBlank() -> {
                    resourceProvider.getString(R.string.shipping_label_customs_restriction_type_description_missing)
                }
                else -> null
            }
        }

        fun CustomsPackage.validateItn(): String? {
            val itn = itn
            return if (itn.isNotEmpty()) {
                if (ITN_REGEX.matches(itn)) {
                    null
                } else {
                    resourceProvider.getString(R.string.shipping_label_customs_itn_invalid_format)
                }
            } else {
                val classesAbove2500usd = lines
                    .filter { it.hsTariffNumber.isNotEmpty() && it.validateHsTariff() == null }
                    .groupBy { it.hsTariffNumber }
                    .map { entry ->
                        Pair(
                            entry.key,
                            entry.value.sumByBigDecimal {
                                it.quantity.toBigDecimal() * (it.value ?: BigDecimal.ZERO)
                            }
                        )
                    }
                    .filter { (_, value) -> value > BigDecimal.valueOf(2500.0) }

                when {
                    classesAbove2500usd.isNotEmpty() -> {
                        resourceProvider.getString(R.string.shipping_label_customs_itn_required_items_over_2500)
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

        return PackageValidationState(
            itnErrorMessage = validateItn(),
            contentsDescriptionErrorMessage = validateContentsType(),
            restrictionDescriptionErrorMessage = validateRestrictionType(),
            linesValidationState = lines.map { it.validate() }
        )
    }

    private fun CustomsLine.validate(): LineValidationState {
        fun CustomsLine.validateItemDescription(): String? {
            return if (itemDescription.isBlank()) {
                resourceProvider.getString(R.string.shipping_label_customs_required_field)
            } else if (isEUShippingScenario && itemDescription.length < MINIMUM_EU_DESCRIPTION_LENGTH) {
                resourceProvider.getString(R.string.shipping_label_customs_item_description_too_short)
            } else {
                null
            }
        }

        fun CustomsLine.validateWeight(): String? {
            return when (weight) {
                null -> resourceProvider.getString(R.string.shipping_label_customs_required_field)
                0f -> resourceProvider.getString(R.string.shipping_label_customs_weight_zero_error)
                else -> null
            }
        }

        fun CustomsLine.validateValue(): String? {
            return when (value) {
                null -> resourceProvider.getString(R.string.shipping_label_customs_required_field)
                BigDecimal.ZERO -> resourceProvider.getString(R.string.shipping_label_customs_value_zero_error)
                else -> null
            }
        }

        return LineValidationState(
            itemDescriptionErrorMessage = validateItemDescription(),
            hsTariffErrorMessage = validateHsTariff(),
            weightErrorMessage = validateWeight(),
            valueErrorMessage = validateValue()
        )
    }

    private fun CustomsLine.validateHsTariff(): String? {
        return if (hsTariffNumber.isEmpty() || HS_TARIFF_NUMBER_REGEX.matches(hsTariffNumber)) {
            null
        } else {
            resourceProvider.getString(R.string.shipping_label_customs_hs_tariff_invalid_format)
        }
    }

    @Parcelize
    data class ViewState(
        val customsPackages: List<CustomsPackageUiState> = emptyList(),
        val isProgressViewShown: Boolean = false,
        val isShippingNoticeVisible: Boolean = false
    ) : Parcelable {
        @IgnoredOnParcel
        val canSubmitForm: Boolean
            get() = customsPackages.all { it.validationState.isValid }
    }

    @Parcelize
    data class CustomsPackageUiState(
        val data: CustomsPackage,
        val validationState: PackageValidationState,
        val isExpanded: Boolean
    ) : Parcelable {
        val customsLinesUiState: List<CustomsLineUiState>
            get() = data.lines.mapIndexed { index, customsLine ->
                CustomsLineUiState(customsLine, validationState.linesValidationState[index])
            }
    }

    @Parcelize
    data class PackageValidationState(
        val itnErrorMessage: String? = null,
        val contentsDescriptionErrorMessage: String? = null,
        val restrictionDescriptionErrorMessage: String? = null,
        val linesValidationState: List<LineValidationState> = emptyList()
    ) : Parcelable {
        @IgnoredOnParcel
        val isValid
            get() = itnErrorMessage == null && contentsDescriptionErrorMessage == null &&
                restrictionDescriptionErrorMessage == null && linesValidationState.all { it.isValid }
    }

    @Parcelize
    data class LineValidationState(
        val itemDescriptionErrorMessage: String? = null,
        val hsTariffErrorMessage: String? = null,
        val weightErrorMessage: String? = null,
        val valueErrorMessage: String? = null
    ) : Parcelable {
        @IgnoredOnParcel
        val isValid
            get() = itemDescriptionErrorMessage == null && hsTariffErrorMessage == null &&
                weightErrorMessage == null && valueErrorMessage == null
    }

    data class OpenShippingInstructions(val url: String) : MultiLiveEvent.Event()
}

typealias CustomsLineUiState = Pair<CustomsLine, LineValidationState>
