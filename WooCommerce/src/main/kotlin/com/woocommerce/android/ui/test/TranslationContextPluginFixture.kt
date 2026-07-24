package com.woocommerce.android.ui.test

import android.content.Context
import com.woocommerce.android.R

/**
 * Test-only resource usages for exercising translation-context suggestions.
 */
internal object TranslationContextPluginTest {
    fun orderEditorLabels(context: Context): List<String> = listOf(
        context.getString(R.string.translation_context_test_order_save_button),
        context.getString(R.string.translation_context_test_order_processing_status),
        context.getString(R.string.translation_context_test_order_note_hint)
    )
}
