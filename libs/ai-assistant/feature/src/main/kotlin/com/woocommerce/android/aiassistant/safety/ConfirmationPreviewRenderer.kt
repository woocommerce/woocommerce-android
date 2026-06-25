package com.woocommerce.android.aiassistant.safety

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

internal class ConfirmationPreviewRenderer @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun render(preview: ConfirmationPreview): RenderedConfirmationPreview =
        RenderedConfirmationPreview(
            message = render(preview.summary),
            fields = preview.rows.map {
                RenderedConfirmationDiffRow(
                    name = it.name,
                    label = render(it.label),
                    value = render(it.value),
                    beforeValue = it.beforeValue?.let(::render),
                )
            },
            isBulk = preview.isBulk,
            bulkEntries = preview.bulkEntries,
        )

    @Suppress("SpreadOperator")
    private fun render(text: ConfirmationPreviewText): String = when (text) {
        is ConfirmationPreviewText.Raw -> text.value
        is ConfirmationPreviewText.Resource -> {
            val args = text.args.map(::render).toTypedArray()
            context.getString(text.id, *args)
        }
        is ConfirmationPreviewText.Quantity -> {
            val id = if (text.quantity == 1) text.singular else text.multiple
            val args = listOf<Any>(text.quantity) + text.args.map(::render)
            context.getString(id, *args.toTypedArray())
        }
    }
}

internal data class RenderedConfirmationPreview(
    val message: String,
    val fields: List<RenderedConfirmationDiffRow>,
    val isBulk: Boolean,
    val bulkEntries: List<ConfirmationBulkEntry> = emptyList(),
) {
    constructor(message: String, fields: List<RenderedConfirmationPreviewField>) : this(
        message = message,
        fields = fields,
        isBulk = false,
    )

    val summary: String
        get() = message

    val rows: List<RenderedConfirmationPreviewField>
        get() = fields
}

internal data class RenderedConfirmationDiffRow(
    val name: String,
    val label: String,
    val value: String,
    val beforeValue: String? = null,
) {
    val afterValue: String
        get() = value
}

internal typealias RenderedConfirmationPreviewField = RenderedConfirmationDiffRow
