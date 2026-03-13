package com.woocommerce.android.ui.test

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.woocommerce.android.R

/**
 * Test fragment for exercising translation context suggestions.
 * This file is intentionally added to test the translation context Danger plugin.
 */
class TranslationContextTestFragment : Fragment() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val saveButton = view.findViewById<Button>(R.id.save_button)
        saveButton.text = getString(R.string.test_order_action_save)

        val statusLabel = view.findViewById<TextView>(R.id.status_label)
        statusLabel.text = getString(R.string.test_order_status_processing)

        val titleLabel = view.findViewById<TextView>(R.id.title_label)
        titleLabel.text = getString(R.string.test_store_settings_title)

        val noteField = view.findViewById<EditText>(R.id.note_field)
        noteField.hint = getString(R.string.test_product_note_placeholder)

        val closeButton = view.findViewById<Button>(R.id.close_button)
        closeButton.contentDescription = getString(R.string.test_shipping_label_close)

        val draftBadge = view.findViewById<TextView>(R.id.draft_badge)
        draftBadge.text = getString(R.string.test_order_draft_status)
    }
}
