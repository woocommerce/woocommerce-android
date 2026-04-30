package com.woocommerce.android.aiassistant.safety

import androidx.annotation.StringRes

internal data class ConfirmationPreview(
    val message: ConfirmationPreviewText,
    val fields: List<ConfirmationPreviewField> = emptyList(),
)

internal data class ConfirmationPreviewField(
    val name: String,
    val label: ConfirmationPreviewText,
    val value: ConfirmationPreviewText,
)

internal sealed interface ConfirmationPreviewText {
    data class Raw(
        val value: String,
    ) : ConfirmationPreviewText

    data class Resource(
        @StringRes val id: Int,
        val args: List<ConfirmationPreviewText> = emptyList(),
    ) : ConfirmationPreviewText

    data class Quantity(
        val quantity: Int,
        @StringRes val singular: Int,
        @StringRes val multiple: Int,
        val args: List<ConfirmationPreviewText> = emptyList(),
    ) : ConfirmationPreviewText
}
