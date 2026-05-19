package com.woocommerce.android.aiassistant.chat.woomobileai

import kotlinx.serialization.Serializable

@Serializable
internal data class WrapperErrorEnvelope(
    val code: String? = null,
    val message: String? = null,
    val data: WrapperErrorData? = null,
)

@Serializable
internal data class WrapperErrorData(
    val status: Int? = null,
)
