package com.woocommerce.android.aiassistant.safety

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

internal class ConfirmationPreviewRenderer @Inject constructor(
    @ApplicationContext private val context: Context,
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
    val message: String,
    val fields: List<RenderedConfirmationPreviewField>,
)

data class RenderedConfirmationPreviewField(
    val name: String,
    val label: String,
    val value: String,
)
