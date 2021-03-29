package com.woocommerce.android.ui.orders.shippinglabels

import com.woocommerce.android.annotations.OpenClassOnDebug
import com.woocommerce.android.model.Address
import com.woocommerce.android.model.Order
import com.woocommerce.android.model.ShippingAccountSettings
import com.woocommerce.android.model.ShippingLabel
import com.woocommerce.android.model.ShippingLabelPackage
import com.woocommerce.android.model.ShippingPackage
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.shippinglabels.WCShippingLabelModel
import org.wordpress.android.fluxc.model.shippinglabels.WCShippingRatesResult
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.NOT_FOUND
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
    private val selectedSite: SelectedSite
) {
    private var accountSettings: ShippingAccountSettings? = null
    private var availablePackages: List<ShippingPackage>? = null

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
            ?.toAppModel()
    }

    suspend fun printShippingLabel(paperSize: String, shippingLabelId: Long): WooResult<String> {
        return withContext(Dispatchers.IO) {
            shippingLabelStore.printShippingLabel(
                site = selectedSite.get(),
                paperSize = paperSize,
                remoteShippingLabelId = shippingLabelId
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

    suspend fun getShippingRates(
        order: Order,
        origin: Address,
        destination: Address,
        packages: List<ShippingLabelPackage>
    ): WooResult<List<WCShippingRatesResult.ShippingPackage>> {
        val carrierRates = shippingLabelStore.getShippingRates(
            selectedSite.get(),
            order.remoteId,
            origin.toShippingLabelModel(),
            destination.toShippingLabelModel(),
            packages.mapIndexed { i, box ->
                val pack = requireNotNull(box.selectedPackage)
                WCShippingLabelModel.ShippingLabelPackage(
                    id = "package$i",
                    boxId = pack.id,
                    height = pack.dimensions.height,
                    width = pack.dimensions.width,
                    length = pack.dimensions.length,
                    weight = box.weight,
                    isLetter = pack.isLetter
                )
            }
        )

        return when {
            carrierRates.isError -> {
                WooResult(carrierRates.error)
            }
            carrierRates.model == null || carrierRates.model!!.packageRates.isEmpty() -> {
                WooResult(WooError(INVALID_RESPONSE, GenericErrorType.PARSE_ERROR, "Empty response"))
            }
            carrierRates.model!!.packageRates.all { pack ->
                pack.shippingOptions.isEmpty() || pack.shippingOptions.all { option ->
                    option.rates.isEmpty()
                }
            } -> {
                WooResult(WooError(GENERIC_ERROR, NOT_FOUND, "Empty result"))
            }
            else -> {
                WooResult(carrierRates.model!!.packageRates)
            }
        }
    }

    suspend fun getAccountSettings(): WooResult<ShippingAccountSettings> {
        return accountSettings?.let { WooResult(it) } ?: shippingLabelStore.getAccountSettings(selectedSite.get())
            .let { result ->
                if (result.isError) return@let WooResult<ShippingAccountSettings>(error = result.error)

                accountSettings = result.model!!.toAppModel()
                WooResult(accountSettings)
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

    fun clearCache() {
        accountSettings = null
        availablePackages = null
    }
}
