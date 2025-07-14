package org.wordpress.android.fluxc.persistence.converters

import androidx.room.TypeConverter
import org.wordpress.android.fluxc.persistence.entity.CouponEntity.DiscountType

class DiscountTypeConverter {
    @TypeConverter
    fun fromDiscountType(type: DiscountType) = type.value

    @TypeConverter
    fun toDiscountType(value: String) = DiscountType.fromString(value)
}
