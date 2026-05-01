package com.woocommerce.android.aiassistant.safety

import androidx.annotation.StringRes

internal data class ConfirmationPreview(
    val summary: ConfirmationPreviewText,
    val rows: List<ConfirmationPreviewRow> = emptyList(),
    val isBulk: Boolean = false,
) {
    constructor(
        message: ConfirmationPreviewText,
        fields: List<ConfirmationPreviewField> = emptyList(),
    ) : this(
        summary = message,
        rows = fields,
    )

    val message: ConfirmationPreviewText
        get() = summary

    val fields: List<ConfirmationPreviewField>
        get() = rows
}

internal data class ConfirmationPreviewRow(
    val name: String,
    val label: ConfirmationPreviewText,
    val value: ConfirmationPreviewText,
)

internal typealias ConfirmationPreviewField = ConfirmationPreviewRow

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
