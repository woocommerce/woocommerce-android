package com.woocommerce.android.apifaker.db

import androidx.room.TypeConverter
import com.woocommerce.android.apifaker.models.ApiType

internal class EndpointTypeConverter {
    @TypeConverter
    fun fromEndpointType(apiType: ApiType?): String? {
        if (apiType == null) return null
        return apiType::class.simpleName +
            if (apiType is ApiType.Custom) ":${apiType.host}" else ""
    }

    @TypeConverter
    fun toEndpointType(value: String?): ApiType? {
        if (value == null) return null
        val parts = value.split(":")
        return when (parts[0]) {
            ApiType.WPApi::class.simpleName -> ApiType.WPApi
            ApiType.WPCom::class.simpleName -> ApiType.WPCom
            else -> ApiType.Custom(parts[1])
        }
    }
}
