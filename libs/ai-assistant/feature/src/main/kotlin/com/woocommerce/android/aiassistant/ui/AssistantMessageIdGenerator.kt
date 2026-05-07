package com.woocommerce.android.aiassistant.ui

import java.util.UUID

interface AssistantMessageIdGenerator {
    fun nextId(): String
}

object UuidAssistantMessageIdGenerator : AssistantMessageIdGenerator {
    override fun nextId(): String = UUID.randomUUID().toString()
}
