package org.wordpress.android.fluxc.network.rest.wpcom.wc.shippinglabels

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import org.wordpress.android.fluxc.generated.endpoint.WOOCOMMERCE
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.shippinglabels.WCPackagesResult.CustomPackage
import org.wordpress.android.fluxc.model.shippinglabels.WCPackagesResult.PredefinedOption
import org.wordpress.android.fluxc.model.shippinglabels.WCShippingLabelModel.ShippingLabelAddress
import org.wordpress.android.fluxc.model.shippinglabels.WCShippingLabelModel.ShippingLabelPackage
import org.wordpress.android.fluxc.model.shippinglabels.WCShippingLabelPackageData
import org.wordpress.android.fluxc.model.shippinglabels.WCShippingPackageCustoms
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooNetwork
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.network.utils.toMap
import org.wordpress.android.fluxc.utils.toWooPayload
import java.math.BigDecimal
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.collections.toMap as asMap

@Singleton
class ShippingLabelRestClient @Inject constructor(private val wooNetwork: WooNetwork) {
    suspend fun fetchShippingLabelsForOrder(
        orderId: Long,
        site: SiteModel
    ): WooPayload<ShippingLabelApiResponse> {
        val url = WOOCOMMERCE.connect.label.order(orderId).pathV1

        return wooNetwork.executeGetGsonRequest(
            site = site,
            path = url,
            clazz = ShippingLabelApiResponse::class.java
        ).toWooPayload()
    }

    suspend fun refundShippingLabelForOrder(
        site: SiteModel,
        orderId: Long,
        remoteShippingLabelId: Long
    ): WooPayload<ShippingLabelApiResponse> {
        val url = WOOCOMMERCE.connect.label.order(orderId).shippingLabelId(remoteShippingLabelId).refund.pathV1

        return wooNetwork.executePostGsonRequest(
            site = site,
            path = url,
            clazz = ShippingLabelApiResponse::class.java
        ).toWooPayload()
    }

    suspend fun printShippingLabels(
        site: SiteModel,
        paperSize: String,
        shippingLabelIds: List<Long>
    ): WooPayload<PrintShippingLabelApiResponse> {
        val url = WOOCOMMERCE.connect.label.print.pathV1
        val params = mapOf(
                "paper_size" to paperSize,
                "label_id_csv" to shippingLabelIds.joinToString(","),
                "caption_csv" to "",
                "json" to "true"
        )

        return wooNetwork.executeGetGsonRequest(
            site = site,
            path = url,
            clazz = PrintShippingLabelApiResponse::class.java,
            params = params
        ).toWooPayload()
    }

    suspend fun checkShippingLabelCreationEligibility(
        site: SiteModel,
        orderId: Long,
        canCreatePackage: Boolean,
        canCreatePaymentMethod: Boolean,
        canCreateCustomsForm: Boolean
    ): WooPayload<SLCreationEligibilityApiResponse> {
        val url = WOOCOMMERCE.connect.label.order(orderId).creation_eligibility.pathV1
        val params = mapOf(
                "can_create_package" to canCreatePackage.toString(),
                "can_create_payment_method" to canCreatePaymentMethod.toString(),
                "can_create_customs_form" to canCreateCustomsForm.toString()
        )

        return wooNetwork.executeGetGsonRequest(
            site = site,
            path = url,
            clazz = SLCreationEligibilityApiResponse::class.java,
            params = params
        ).toWooPayload()
    }

    suspend fun verifyAddress(
        site: SiteModel,
        address: ShippingLabelAddress,
        type: ShippingLabelAddress.Type
    ): WooPayload<VerifyAddressResponse> {
        val url = WOOCOMMERCE.connect.normalize_address.pathV1
        val body = mapOf(
            "address" to address.toMap(),
            "type" to type.name.lowercase(Locale.ROOT)
        )

        return wooNetwork.executePostGsonRequest(
            site = site,
            path = url,
            clazz = VerifyAddressResponse::class.java,
            body = body
        ).toWooPayload()
    }

    suspend fun getPackageTypes(
        site: SiteModel
    ): WooPayload<GetPackageTypesResponse> {
        val url = WOOCOMMERCE.connect.packages.pathV1

        return wooNetwork.executeGetGsonRequest(
            site = site,
            path = url,
            clazz = GetPackageTypesResponse::class.java
        ).toWooPayload()
    }

    suspend fun getAccountSettings(
        site: SiteModel
    ): WooPayload<AccountSettingsApiResponse> {
        val url = WOOCOMMERCE.connect.account.settings.pathV1

        return wooNetwork.executeGetGsonRequest(
            site = site,
            path = url,
            clazz = AccountSettingsApiResponse::class.java
        ).toWooPayload()
    }

    suspend fun updateAccountSettings(site: SiteModel, request: UpdateSettingsApiRequest): WooPayload<Boolean> {
        val url = WOOCOMMERCE.connect.account.settings.pathV1

        return wooNetwork.executePostGsonRequest(
            site = site,
            path = url,
            clazz = JsonObject::class.java,
            body = request.toMap()
        ).toWooPayload { it["success"].asBoolean }
    }

    @Suppress("LongParameterList")
    suspend fun getShippingRates(
        site: SiteModel,
        orderId: Long,
        origin: ShippingLabelAddress,
        destination: ShippingLabelAddress,
        packages: List<ShippingLabelPackage>,
        customsData: List<WCShippingPackageCustoms>?
    ): WooPayload<ShippingRatesApiResponse> {
        val url = WOOCOMMERCE.connect.label.order(orderId).rates.pathV1

        val body = mapOf(
            "origin" to origin.toMap(),
            "destination" to destination.toMap(),
            "packages" to packages.map { labelPackage ->
                val customs = customsData?.first { it.id == labelPackage.id }
                labelPackage.toMap() + (customs?.toMap() ?: emptyMap())
            }
        )

        return wooNetwork.executePostGsonRequest(
            site = site,
            path = url,
            clazz = ShippingRatesApiResponse::class.java,
            body = body
        ).toWooPayload()
    }

    @Suppress("LongParameterList")
    suspend fun purchaseShippingLabels(
        site: SiteModel,
        orderId: Long,
        origin: ShippingLabelAddress,
        destination: ShippingLabelAddress,
        packagesData: List<WCShippingLabelPackageData>,
        customsData: List<WCShippingPackageCustoms>?,
        emailReceipts: Boolean = false
    ): WooPayload<ShippingLabelStatusApiResponse> {
        val url = WOOCOMMERCE.connect.label.order(orderId).pathV1

        val body = mapOf(
                "async" to true,
                "origin" to origin,
                "destination" to destination,
                "packages" to packagesData.map { labelPackage ->
                    val customs = customsData?.first { it.id == labelPackage.id }
                    labelPackage.toMap() + (customs?.toMap() ?: emptyMap())
                },
                "email_receipt" to emailReceipts,
                "source" to "wc-android",
        )

        return wooNetwork.executePostGsonRequest(
            site = site,
            path = url,
            clazz = ShippingLabelStatusApiResponse::class.java,
            body = body
        ).toWooPayload()
    }

    suspend fun fetchShippingLabelsStatus(
        site: SiteModel,
        orderId: Long,
        labelIds: List<Long>
    ): WooPayload<ShippingLabelStatusApiResponse> {
        val url = WOOCOMMERCE.connect.label.order(orderId).shippingLabels(labelIds.joinToString(separator = ",")).pathV1

        return wooNetwork.executeGetGsonRequest(
            site = site,
            path = url,
            clazz = ShippingLabelStatusApiResponse::class.java
        ).toWooPayload()
    }

    // Supports both creating custom package(s), or activating existing predefined package(s).
    // Creating singular or multiple items, as well as mixed items at once are supported.
    suspend fun createPackages(
        site: SiteModel,
        customPackages: List<CustomPackage> = emptyList(),
        predefinedOptions: List<PredefinedOption> = emptyList()
    ): WooPayload<Boolean> {
        // We need at least one of the lists to not be empty to continue with API call.
        if (customPackages.isEmpty() && predefinedOptions.isEmpty()) {
            return WooPayload(false)
        }

        val url = WOOCOMMERCE.connect.packages.pathV1

        // 1. Mapping for custom packages
        // Here we convert each CustomPackage instance into a Map with proper key names.
        // This list of Maps will then be used as the "customs" key's value in the API request.
        val mappedCustomPackages = customPackages.map {
            mapOf(
                    "name" to it.title,
                    "is_letter" to it.isLetter,
                    "inner_dimensions" to it.dimensions,
                    "box_weight" to it.boxWeight,

                    /*
                    In wp-admin, The two values below are not user-editable but are saved during
                    package creation with hardcoded values. Here we replicate the same behavior.
                     */
                    "is_user_defined" to true,
                    "max_weight" to 0
            )
        }

        // 2. Mapping for predefined options.
        val predefinedParam = predefinedOptions
                // First we group all options by carrier, because the list of predefinedOptions can contain
                // multiple instances with the same carrier name.
                //  For example, "USPS Priority Mail Express Boxes" and "USPS Priority Mail Boxes" are two separate
                //  options having the same carrier: "usps".
                .groupBy { it.carrier }

                // Next, build a predefinedParam Map replicating the required JSON request structure.
                // Example structure:
                //
                //    "carrier_1": [ "package_1", "package_2", ... ],
                //    "carrier_2": [ "package_3", "package_4", "package_5" ... ],
                .map { (carrier, options) ->
                    carrier to options
                            .flatMap { it.predefinedPackages } // Put all found package(s) in a list
                            .map { it.id } // Grab all found package id(s)
                }.asMap() // Convert list of Map to Map

        val body = mapOf(
                "custom" to mappedCustomPackages,
                "predefined" to predefinedParam
        )

        return wooNetwork.executePostGsonRequest(
            site = site,
            path = url,
            clazz = JsonObject::class.java,
            body = body
        ).toWooPayload { it["success"].asBoolean }
    }

    data class PrintShippingLabelApiResponse(
        val mimeType: String,
        val b64Content: String,
        val success: Boolean
    )

    data class ShippingRatesApiResponse(
        @SerializedName("success") val isSuccess: Boolean,
        @SerializedName("rates") private val boxesJson: JsonElement
    ) {
        companion object {
            private val gson by lazy { Gson() }
        }

        val boxes: Map<String, Map<String, ShippingOption>>
            get() {
                val responseType = object : TypeToken<Map<String, Map<String, ShippingOption>>>() {}.type
                return gson.fromJson(boxesJson, responseType) as? Map<String, Map<String, ShippingOption>> ?: emptyMap()
            }

        data class ShippingOption(
            val rates: List<Rate>
        ) {
            data class Rate(
                val title: String,
                val insurance: String?,
                val rate: BigDecimal,
                @SerializedName("rate_id") val rateId: String,
                @SerializedName("service_id") val serviceId: String,
                @SerializedName("carrier_id") val carrierId: String,
                @SerializedName("shipment_id") val shipmentId: String,
                @SerializedName("tracking") val hasTracking: Boolean,
                @SerializedName("retail_rate") val retailRate: BigDecimal,
                @SerializedName("is_selected") val isSelected: Boolean,
                @SerializedName("free_pickup") val isPickupFree: Boolean,
                @SerializedName("delivery_days") val deliveryDays: Int,
                @SerializedName("delivery_date_guaranteed") val deliveryDateGuaranteed: Boolean,
                @SerializedName("delivery_date") val deliveryDate: Date?
            )
        }
    }

    data class VerifyAddressResponse(
        @SerializedName("success") val isSuccess: Boolean,
        @SerializedName("is_trivial_normalization") val isTrivialNormalization: Boolean,
        @SerializedName("normalized") val suggestedAddress: ShippingLabelAddress?,
        @SerializedName("field_errors") val error: Error?
    ) {
        data class Error(
            @SerializedName("general") val message: String?,
            @SerializedName("address") val address: String?
        )
    }

    data class GetPackageTypesResponse(
        @SerializedName("success") val isSuccess: Boolean,
        val storeOptions: StoreOptions,
        val formSchema: FormSchema,
        val formData: FormData
    ) {
        companion object {
            private val gson by lazy { Gson() }
        }

        data class StoreOptions(
            @SerializedName("currency_symbol") val currency: String,
            @SerializedName("dimension_unit") val dimensionUnit: String,
            @SerializedName("weight_unit") val weightUnit: String,
            @SerializedName("origin_country") val originCountry: String
        )

        data class FormData(
            @SerializedName("custom") val customData: List<CustomData>,
            @SerializedName("predefined") val predefinedJson: JsonElement
        ) {
            data class CustomData(
                val name: String,
                @SerializedName("inner_dimensions") val innerDimensions: String?,
                @SerializedName("outer_dimensions") val outerDimensions: String?,
                @SerializedName("dimensions") val dimensions: String?,
                @SerializedName("box_weight") val boxWeight: Float?,
                @SerializedName("max_weight") val maxWeight: Float?,
                @SerializedName("is_user_defined") val isUserDefined: Boolean?,
                @SerializedName("is_letter") val isLetter: Boolean
            )

            val predefinedData: Map<String, List<String?>>
                get() {
                    val responseType = object : TypeToken<Map<String, List<String?>>>() {}.type
                    return gson.fromJson(predefinedJson, responseType) as? Map<String, List<String?>> ?: emptyMap()
                }
        }

        data class FormSchema(
            @SerializedName("custom") val customSchema: CustomSchema?,
            @SerializedName("predefined") val predefinedJson: JsonElement?
        ) {
            data class CustomSchema(
                val title: String,
                val description: String
            )

            data class PackageOption(
                val title: String,
                val definitions: List<PackageDefinition>
            ) {
                data class PackageDefinition(
                    val id: String,
                    val name: String,
                    val dimensions: Any?,
                    @SerializedName("outer_dimensions") val outerDimensions: String,
                    @SerializedName("inner_dimensions") val innerDimensions: String,
                    @SerializedName("box_weight") val boxWeight: Float?,
                    @SerializedName("max_weight") val maxWeight: Float?,
                    @SerializedName("is_letter") val isLetter: Boolean,
                    @SerializedName("is_flat_rate") val isFlatRate: Boolean?,
                    @SerializedName("can_ship_international") val canShipInternational: Boolean?,
                    @SerializedName("group_id") val groupId: String?,
                    @SerializedName("service_group_ids") val serviceGroupIds: List<String>?
                )
            }

            val predefinedSchema: Map<String, Map<String, PackageOption>>
                get() {
                    val responseType = object : TypeToken<Map<String, Map<String, PackageOption>>>() {}.type
                    return gson.fromJson(predefinedJson, responseType) as?
                            Map<String, Map<String, PackageOption>> ?: emptyMap()
                }
        }
    }
}
