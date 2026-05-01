package com.woocommerce.android.aiassistant.safety

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

internal class ConfirmationPreviewRenderer @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun render(preview: ConfirmationPreview): RenderedConfirmationPreview =
        RenderedConfirmationPreview(
            summary = render(preview.summary),
            rows = preview.rows.map {
                RenderedConfirmationDiffRow(
                    name = it.name,
                    label = render(it.label),
                    value = render(it.value),
                )
            },
            isBulk = preview.isBulk,
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

data class RenderedConfirmationPreview(
    val summary: String,
    val rows: List<RenderedConfirmationDiffRow>,
    val isBulk: Boolean,
) {
    constructor(
        message: String,
        fields: List<RenderedConfirmationPreviewField>,
    ) : this(
        summary = message,
        rows = fields,
        isBulk = false,
    )

    val message: String
        get() = summary

    val fields: List<RenderedConfirmationPreviewField>
        get() = rows
}

data class RenderedConfirmationDiffRow(
    val name: String,
    val label: String,
    val value: String,
)

typealias RenderedConfirmationPreviewField = RenderedConfirmationDiffRow
