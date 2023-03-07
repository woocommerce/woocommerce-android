package com.woocommerce.android.apifaker.db

import androidx.room.TypeConverter
import com.woocommerce.android.apifaker.models.EndpointType

internal class EndpointTypeConverter {
    @TypeConverter
    fun fromEndpointType(endpointType: EndpointType?): String? {
        if (endpointType == null) return null
        return endpointType::class.simpleName +
            if (endpointType is EndpointType.Custom) ":${endpointType.host}" else ""
    }

    @TypeConverter
    fun toEndpointType(value: String?): EndpointType? {
        if (value == null) return null
        val parts = value.split(":")
        return when (parts[0]) {
            EndpointType.WPApi::class.simpleName -> EndpointType.WPApi
            EndpointType.WPCom::class.simpleName -> EndpointType.WPCom
            else -> EndpointType.Custom(parts[1])
        }
    }
}
