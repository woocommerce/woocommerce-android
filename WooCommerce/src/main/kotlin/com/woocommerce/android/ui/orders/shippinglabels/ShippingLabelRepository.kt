package com.woocommerce.android.ui.orders.shippinglabels

import com.woocommerce.android.annotations.OpenClassOnDebug
import com.woocommerce.android.model.*
import com.woocommerce.android.tools.SelectedSite
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.shippinglabels.WCShippingLabelModel
import org.wordpress.android.fluxc.model.shippinglabels.WCShippingLabelPackageData
import org.wordpress.android.fluxc.model.shippinglabels.WCShippingRatesResult
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.NOT_FOUND
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.UNKNOWN
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.INVALID_RESPONSE
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.store.WCShippingLabelStore
import javax.inject.Inject
import javax.inject.Singleton

@OpenClassOnDebug
@Singleton
class ShippingLabelRepository @Inject constructor(
    private val shippingLabelStore: WCShippingLabelStore,
    private val selectedSite: SelectedSite,
    private val shippingLabelMapper: ShippingLabelMapper,
) {
    private var accountSettings: ShippingAccountSettings? = null
    private var availablePackages: List<ShippingPackage>? = null
    private var selectableServicePackages: List<ShippingPackage>? = null

    suspend fun refundShippingLabel(orderId: Long, shippingLabelId: Long): WooResult<Boolean> {
        return withContext(Dispatchers.IO) {
            shippingLabelStore.refundShippingLabelForOrder(
                site = selectedSite.get(),
                orderId = orderId,
                remoteShippingLabelId = shippingLabelId
            )
        }
    }

    fun getShippingLabelByOrderIdAndLabelId(
        orderId: Long,
        shippingLabelId: Long
    ): ShippingLabel? {
        return shippingLabelStore.getShippingLabelById(
            selectedSite.get(), orderId, shippingLabelId
        )
            ?.let { shippingLabelMapper.toAppModel(it) }
    }

    suspend fun printShippingLabels(paperSize: String, shippingLabelIds: List<Long>): WooResult<String> {
        return withContext(Dispatchers.IO) {
            shippingLabelStore.printShippingLabels(
                site = selectedSite.get(),
                paperSize = paperSize,
                shippingLabelIds = shippingLabelIds
            )
        }
    }

    suspend fun getShippingPackages(): WooResult<List<ShippingPackage>> {
        return availablePackages?.let { WooResult(it) } ?: shippingLabelStore.getPackageTypes(selectedSite.get())
            .let { result ->
                if (result.isError) return@let WooResult<List<ShippingPackage>>(error = result.error)

                val packagesResult = result.model!!
                val list = mutableListOf<ShippingPackage>()
                packagesResult.customPackages.map {
                    list.add(it.toAppModel())
                }
                packagesResult.predefinedOptions.forEach { option ->
                    list.addAll(option.toAppModel())
                }

                availablePackages = list
                WooResult(availablePackages)
            }
    }

    suspend fun getSelectableServicePackages(): WooResult<List<ShippingPackage>> {
        return selectableServicePackages?.let { WooResult(it) } ?: shippingLabelStore.getAllPredefinedOptions(
            selectedSite.get()
        )
            .let { result ->
                if (result.isError) return@let WooResult<List<ShippingPackage>>(error = result.error)

                selectableServicePackages = result.model!!.flatMap { it.toAppModel() }
                availablePackages?.let { selectableServicePackages = selectableServicePackages?.minus(it) }
                return WooResult(selectableServicePackages)
            }
    }

    suspend fun getShippingRates(
        order: Order,
        origin: Address,
        destination: Address,
        packages: List<ShippingLabelPackage>,
        customsPackages: List<CustomsPackage>?
    ): WooResult<List<WCShippingRatesResult.ShippingPackage>> {
        val carrierRates = shippingLabelStore.getShippingRates(
            site = selectedSite.get(),
            orderId = order.id,
            origin = origin.toShippingLabelModel(),
            destination = destination.toShippingLabelModel(),
            packages = packages.mapIndexed { i, box ->
                val pack = requireNotNull(box.selectedPackage)
                WCShippingLabelModel.ShippingLabelPackage(
                    id = box.packageId,
                    boxId = pack.id,
                    height = pack.dimensions.height,
                    width = pack.dimensions.width,
                    length = pack.dimensions.length,
                    weight = box.weight,
                    isLetter = pack.isLetter
                )
            },
            customsData = customsPackages?.map { it.toDataModel() }
        )

        return when {
            carrierRates.isError -> {
                WooResult(carrierRates.error)
            }
            carrierRates.model == null || carrierRates.model!!.packageRates.isEmpty() -> {
                WooResult(WooError(INVALID_RESPONSE, GenericErrorType.PARSE_ERROR, "Empty response"))
            }
            carrierRates.model!!.packageRates.any { pack ->
                pack.shippingOptions.isEmpty() || pack.shippingOptions.all { option ->
                    option.rates.isEmpty()
                }
            } -> {
                // if any of the packages doesn't have any rates, show the empty state screen
                WooResult(WooError(GENERIC_ERROR, NOT_FOUND, "Empty result"))
            }
            else -> {
                WooResult(carrierRates.model!!.packageRates)
            }
        }
    }

    suspend fun getAccountSettings(forceRefresh: Boolean = false): WooResult<ShippingAccountSettings> {
        if (forceRefresh) accountSettings = null
        return accountSettings?.let { WooResult(it) } ?: shippingLabelStore.getAccountSettings(selectedSite.get())
            .let { result ->
                if (result.isError) return@let WooResult<ShippingAccountSettings>(error = result.error)

                accountSettings = result.model!!.toAppModel()
                WooResult(accountSettings)
            }
    }

    /**
     * Returns the last used package.
     * Please note that this will ignore all errors, and return null for those cases
     */
    suspend fun getLastUsedPackage(): ShippingPackage? {
        return getAccountSettings().model?.lastUsedBoxId?.let { id ->
            getShippingPackages().model?.firstOrNull { it.id == id }
        }
    }

    suspend fun updatePaymentSettings(selectedPaymentMethodId: Int, emailReceipts: Boolean): WooResult<Unit> {
        return shippingLabelStore.updateAccountSettings(
            site = selectedSite.get(),
            selectedPaymentMethodId = selectedPaymentMethodId,
            isEmailReceiptEnabled = emailReceipts
        ).let { result ->
            if (result.isError) return@let WooResult(error = result.error)

            accountSettings = null
            WooResult(Unit)
        }
    }

    suspend fun purchaseLabels(
        orderId: Long,
        origin: Address,
        destination: Address,
        packages: List<ShippingLabelPackage>,
        rates: List<ShippingRate>,
        customsPackages: List<CustomsPackage>?
    ): WooResult<List<ShippingLabel>> {
        val packagesData = packages.mapIndexed { i, labelPackage ->
            val rate = rates.first { it.packageId == labelPackage.packageId }
            WCShippingLabelPackageData(
                id = labelPackage.packageId,
                boxId = labelPackage.selectedPackage!!.id,
                isLetter = labelPackage.selectedPackage.isLetter,
                length = labelPackage.selectedPackage.dimensions.length,
                width = labelPackage.selectedPackage.dimensions.width,
                height = labelPackage.selectedPackage.dimensions.height,
                weight = labelPackage.weight,
                shipmentId = rate.shipmentId,
                rateId = rate.rateId,
                serviceId = rate.serviceId,
                serviceName = rate.serviceName,
                carrierId = rate.carrierId,
                // duplicate items according to their quantities
                products = labelPackage.items.map { item -> List(item.quantity) { item.productId } }.flatten()
            )
        }
        // Retrieve account settings, normally they should be cached at this point, and the response would be
        // instantaneous
        // We fallback to true as it's the default value in the plugin
        val emailReceipts = getAccountSettings().model?.isEmailReceiptEnabled ?: true

        return shippingLabelStore.purchaseShippingLabels(
            site = selectedSite.get(),
            orderId = orderId,
            origin = origin.toShippingLabelModel(),
            destination = destination.toShippingLabelModel(),
            packagesData = packagesData,
            customsData = customsPackages?.map { it.toDataModel() },
            emailReceipts = emailReceipts
        ).let { result ->
            when {
                result.isError -> WooResult(result.error)
                result.model != null -> WooResult(result.model!!.map { shippingLabelMapper.toAppModel(it) })
                else -> WooResult(WooError(GENERIC_ERROR, UNKNOWN))
            }
        }
    }

    suspend fun createCustomPackage(packageToCreate: ShippingPackage): WooResult<Boolean> {
        return shippingLabelStore.createPackages(
            site = selectedSite.get(),
            customPackages = listOf(packageToCreate.toCustomPackageDataModel()),
            predefinedPackages = emptyList()
        ).let { result ->
            when {
                result.model == true -> {
                    availablePackages = availablePackages?.let { it + packageToCreate }
                    WooResult(true)
                }
                result.isError -> WooResult(result.error)
                else -> WooResult(WooError(GENERIC_ERROR, UNKNOWN))
            }
        }
    }

    suspend fun activateServicePackage(packageToCreate: ShippingPackage): WooResult<Boolean> {
        return shippingLabelStore.createPackages(
            site = selectedSite.get(),
            customPackages = emptyList(),
            predefinedPackages = listOf(packageToCreate.toPredefinedOptionDataModel())
        ).let { result ->
            when {
                result.model == true -> {
                    availablePackages = availablePackages?.plus(packageToCreate)
                    selectableServicePackages = selectableServicePackages?.minus(packageToCreate)
                    WooResult(true)
                }
                result.isError -> WooResult(result.error)
                else -> WooResult(WooError(GENERIC_ERROR, UNKNOWN))
            }
        }
    }

    fun clearCache() {
        accountSettings = null
        availablePackages = null
    }
}
