package com.woocommerce.android.aiassistant.chat

import com.woocommerce.android.aiassistant.di.AiAssistantModule
import kotlinx.serialization.json.Json

internal fun assistantJsonForTests(): Json = AiAssistantModule.provideAiAssistantJson()
