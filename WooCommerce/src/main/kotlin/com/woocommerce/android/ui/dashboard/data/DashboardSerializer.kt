package com.woocommerce.android.ui.dashboard.data

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.google.protobuf.InvalidProtocolBufferException
import com.woocommerce.android.ui.mystore.data.DashboardDataModel
import java.io.InputStream
import java.io.OutputStream

internal object DashboardSerializer : Serializer<DashboardDataModel> {
    override val defaultValue: DashboardDataModel = DashboardDataModel.getDefaultInstance()
    override suspend fun readFrom(input: InputStream): DashboardDataModel {
        try {
            return DashboardDataModel.parseFrom(input)
        } catch (exception: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot read proto.", exception)
        }
    }

    override suspend fun writeTo(t: DashboardDataModel, output: OutputStream) = t.writeTo(output)
}
