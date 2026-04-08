package org.wordpress.android.fluxc.persistence.entity

import androidx.room.Entity
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.wordpress.android.fluxc.model.LocalOrRemoteId
import org.wordpress.android.fluxc.persistence.entity.WooShippingPackagesEntity.CarrierPackageGroup
import org.wordpress.android.fluxc.persistence.entity.WooShippingPackagesEntity.CarrierPackageGroups
import org.wordpress.android.fluxc.persistence.entity.WooShippingPackagesEntity.CarrierType
import org.wordpress.android.fluxc.persistence.entity.WooShippingPackagesEntity.Package
import org.wordpress.android.fluxc.persistence.entity.WooShippingPackagesEntity.StoreOptions
import org.wordpress.android.fluxc.utils.asStringOrNull

@Entity(primaryKeys = ["localSiteId"])
@TypeConverters(WooShippingPackagesConverters::class)
data class WooShippingPackagesEntity(
    val localSiteId: LocalOrRemoteId.LocalId,
    val storeOptions: StoreOptions,
    val savedPackages: List<Package>,
    val carrierPackageGroups: List<CarrierPackageGroups>
) {
    data class StoreOptions(
        val currencySymbol: String?,
        val dimensionUnit: String?,
        val weightUnit: String?,
        val originCountry: String?
    )

    data class Package(
        val id: String?,
        val name: String?,
        val dimensions: String?,
        val weight: String?,
        val isLetter: Boolean,
        val dimensionUnit: String?,
        val isUserDefined: Boolean = false,
        val weightUnit: String?,
        val groupName: String? = null,
        val saved: Boolean
    )

    data class CarrierPackageGroups(
        val carrierType: CarrierType?,
        val packageGroups: List<CarrierPackageGroup>?
    )

    data class CarrierPackageGroup(val description: String?, val packages: List<Package>?)

    enum class CarrierType(val id: String) {
        USPS("usps"),
        FEDEX("fedex"),
        DHL("dhlexpress"),
        UPS("upsdap");

        companion object {
            fun fromId(id: String): CarrierType? = entries.firstOrNull { it.id == id }
        }
    }
}

internal class WooShippingPackagesConverters {
    @TypeConverter
    fun fromStoreOptions(storeOptions: StoreOptions?): String? = storeOptions?.let {
        JsonObject().apply {
            addProperty("currency_symbol", it.currencySymbol)
            addProperty("dimension_unit", it.dimensionUnit)
            addProperty("weight_unit", it.weightUnit)
            addProperty("origin_country", it.originCountry)
        }.toString()
    }

    @TypeConverter
    fun toStoreOptions(value: String?): StoreOptions? = value?.let { json ->
        JsonParser.parseString(json).asJsonObject.let { jsonObject ->
            StoreOptions(
                currencySymbol = jsonObject.get("currency_symbol")?.asStringOrNull,
                dimensionUnit = jsonObject.get("dimension_unit")?.asStringOrNull,
                weightUnit = jsonObject.get("weight_unit")?.asStringOrNull,
                originCountry = jsonObject.get("origin_country")?.asStringOrNull
            )
        }
    }

    @TypeConverter
    fun fromPackageList(value: List<Package>?): String? = value?.let {
        JsonArray().apply {
            it.forEach { item ->
                add(
                    JsonObject().apply {
                        addProperty("id", item.id)
                        addProperty("name", item.name)
                        addProperty("dimensions", item.dimensions)
                        addProperty("weight", item.weight)
                        addProperty("is_letter", item.isLetter)
                        addProperty("dimension_unit", item.dimensionUnit)
                        addProperty("is_user_defined", item.isUserDefined)
                        addProperty("weight_unit", item.weightUnit)
                        addProperty("group_name", item.groupName)
                        addProperty("saved", item.saved)
                    }
                )
            }
        }.toString()
    }

    @TypeConverter
    fun toPackageList(value: String?): List<Package>? = value?.let {
        JsonParser.parseString(it).asJsonArray.mapNotNull { jsonElement ->
            val jsonObject = jsonElement as? JsonObject ?: return null
            Package(
                id = jsonObject.get("id")?.asStringOrNull,
                name = jsonObject.get("name")?.asStringOrNull,
                dimensions = jsonObject.get("dimensions")?.asStringOrNull,
                weight = jsonObject.get("weight")?.asStringOrNull,
                isLetter = jsonObject.get("is_letter")?.asBoolean == true,
                dimensionUnit = jsonObject.get("dimension_unit")?.asStringOrNull,
                isUserDefined = jsonObject.get("is_user_defined")?.asBoolean == true,
                weightUnit = jsonObject.get("weight_unit")?.asStringOrNull,
                groupName = jsonObject.get("group_name")?.asStringOrNull,
                saved = jsonObject.get("saved")?.asBoolean == true,
            )
        }
    }

    @TypeConverter
    fun fromCarrierPackageGroups(value: List<CarrierPackageGroups>?): String? = value?.let {
        JsonArray().apply {
            it.forEach { item ->
                add(
                    JsonObject().apply {
                        addProperty("carrier_type", item.carrierType?.id)
                        val packageGroups = JsonArray().apply {
                            item.packageGroups?.forEach { packageGroup ->
                                add(
                                    JsonObject().apply {
                                        addProperty("description", packageGroup.description)
                                        addProperty("packages", fromPackageList(packageGroup.packages))
                                    }
                                )
                            }
                        }
                        add("package_groups", packageGroups)
                    }
                )
            }
        }.toString()
    }

    @TypeConverter
    fun toCarrierPackageGroups(value: String?): List<CarrierPackageGroups>? = value?.let {
        JsonParser.parseString(it).asJsonArray.mapNotNull { jsonElement ->
            val jsonObject = jsonElement as? JsonObject ?: return null
            CarrierPackageGroups(
                carrierType = jsonObject.get("carrier_type")?.asStringOrNull?.let { id -> CarrierType.fromId(id) },
                packageGroups = toPackageGroups(jsonObject.getAsJsonArray("package_groups")),
            )
        }
    }

    private fun toPackageGroups(value: JsonArray?): List<CarrierPackageGroup>? = value?.mapNotNull { jsonElement ->
        val jsonObject = jsonElement as? JsonObject ?: return null
        CarrierPackageGroup(
            description = jsonObject.get("description")?.asStringOrNull,
            packages = toPackageList(jsonObject.get("packages")?.asStringOrNull),
        )
    }
}
