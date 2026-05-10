package com.woocommerce.android.aiassistant.tools

import com.woocommerce.android.aiassistant.chat.TransportDiagnosticsFactory

internal fun testToolFailureDiagnosticsFactory() = ToolFailureDiagnosticsFactory(TransportDiagnosticsFactory())
