package com.woocommerce.android.ui.dashboard.data

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.google.protobuf.InvalidProtocolBufferException
import com.woocommerce.android.ui.mystore.data.CustomDateRange
import java.io.InputStream
import java.io.OutputStream

object CustomDateRangeSerializer : Serializer<CustomDateRange> {
    override val defaultValue: CustomDateRange = CustomDateRange.getDefaultInstance()
    override suspend fun readFrom(input: InputStream): CustomDateRange {
        try {
            return CustomDateRange.parseFrom(input)
        } catch (exception: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot read proto.", exception)
        }
    }

    override suspend fun writeTo(t: CustomDateRange, output: OutputStream) = t.writeTo(output)
}
