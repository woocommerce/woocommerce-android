package com.woocommerce.android.aiassistant.chat

import com.woocommerce.android.aiassistant.core.chat.TransportDiagnostics
import okhttp3.Response
import javax.inject.Inject

internal class TransportDiagnosticsFactory @Inject constructor() {
    fun from(response: Response?): TransportDiagnostics? {
        response ?: return null
        return TransportDiagnostics(httpStatus = response.code)
    }
}
