package com.woocommerce.android.aiassistant.ui

import java.util.UUID
import javax.inject.Inject

class AssistantMessageIdGenerator @Inject constructor() {
    fun nextId(): String = UUID.randomUUID().toString()
}
