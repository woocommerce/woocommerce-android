package com.woocommerce.android.aiassistant.safety

import android.content.Context

internal class ConfirmationPreviewRenderer(
    private val context: Context,
) {
    fun render(preview: ConfirmationPreview): RenderedConfirmationPreview =
        RenderedConfirmationPreview(
            message = render(preview.message),
            fields = preview.fields.map {
                RenderedConfirmationPreviewField(
                    name = it.name,
                    label = render(it.label),
                    value = render(it.value),
                )
            },
        )

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
    val fields: List<RenderedConfirmationPreviewField>,
)

internal data class RenderedConfirmationPreviewField(
    val name: String,
    val label: String,
    val value: String,
)
