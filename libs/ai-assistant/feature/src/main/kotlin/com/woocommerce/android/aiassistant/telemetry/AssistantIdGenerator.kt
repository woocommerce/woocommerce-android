package com.woocommerce.android.aiassistant.telemetry

import java.util.UUID
import javax.inject.Inject

class AssistantIdGenerator @Inject constructor() {
    fun nextId(): String = UUID.randomUUID().toString()
}
