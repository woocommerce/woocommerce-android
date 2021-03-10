package com.woocommerce.android.ui.aztec

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.navArgs
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.FragmentAztecEditorBinding
import com.woocommerce.android.extensions.navigateBackWithResult
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.dialog.WooDialog
import com.woocommerce.android.ui.main.MainActivity
import com.woocommerce.android.ui.main.MainActivity.Companion.BackPressListener
import org.wordpress.android.util.ActivityUtils
import org.wordpress.aztec.Aztec
import org.wordpress.aztec.AztecText.EditorHasChanges.NO_CHANGES
import org.wordpress.aztec.IHistoryListener
import org.wordpress.aztec.ITextFormat
import org.wordpress.aztec.glideloader.GlideImageLoader
import org.wordpress.aztec.toolbar.IAztecToolbarClickListener

class AztecEditorFragment : BaseFragment(R.layout.fragment_aztec_editor),
    IAztecToolbarClickListener,
    BackPressListener,
    IHistoryListener {
    companion object {
        const val TAG: String = "AztecEditorFragment"
        const val AZTEC_EDITOR_RESULT = "aztec_editor_result"
        const val ARG_AZTEC_REQUEST_CODE = "aztec-request-code"
        const val ARG_AZTEC_EDITOR_TEXT = "editor-text"
        const val ARG_AZTEC_HAS_CHANGES = "editor-has-changes"

        private const val FIELD_IS_HTML_EDITOR_ENABLED = "is_html_editor_enabled"
    }

    private lateinit var aztec: Aztec

    private val navArgs: AztecEditorFragmentArgs by navArgs()

    private var isHtmlEditorEnabled: Boolean = false

    override fun getFragmentTitle() = navArgs.aztecTitle

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (activity as? MainActivity)?.hideBottomNav()

        val binding = FragmentAztecEditorBinding.bind(view)

        if (navArgs.aztecCaption.isNullOrBlank()) {
            binding.aztecCaption.visibility = View.GONE
        } else {
            binding.aztecCaption.visibility = View.VISIBLE
            binding.aztecCaption.text = navArgs.aztecCaption
        }

        aztec = Aztec.with(binding.visualEditor, binding.sourceEditor, binding.aztecToolbar, this)
            .setImageGetter(GlideImageLoader(requireContext()))

        aztec.initSourceEditorHistory()

        aztec.visualEditor.fromHtml(navArgs.aztecText)
        aztec.sourceEditor?.displayStyledAndFormattedHtml(navArgs.aztecText)
        aztec.setHistoryListener(this)

        savedInstanceState?.let { state ->
            isHtmlEditorEnabled = state.getBoolean(FIELD_IS_HTML_EDITOR_ENABLED)
        }

        aztec.visualEditor.post {
            aztec.visualEditor.requestFocus()
            ActivityUtils.showKeyboard(aztec.visualEditor)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(FIELD_IS_HTML_EDITOR_ENABLED, isHtmlEditorEnabled)
        super.onSaveInstanceState(outState)
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    override fun onStop() {
        super.onStop()
        WooDialog.onCleared()
        activity?.let {
            ActivityUtils.hideKeyboard(it)
        }
    }

    override fun onRequestAllowBackPress(): Boolean {
        navigateBackWithResult(editorHasChanges())
        return false
    }

    override fun onToolbarCollapseButtonClicked() {
        // Aztec Toolbar interface methods implemented by default with Aztec. Currently not used
    }

    override fun onToolbarExpandButtonClicked() {
        // Aztec Toolbar interface methods implemented by default with Aztec. Currently not used
    }

    override fun onToolbarFormatButtonClicked(format: ITextFormat, isKeyboardShortcut: Boolean) {
        // Aztec Toolbar interface methods implemented by default with Aztec. Currently not used
    }

    override fun onToolbarHeadingButtonClicked() {
        // Aztec Toolbar interface methods implemented by default with Aztec. Currently not used
    }

    override fun onToolbarHtmlButtonClicked() {
        aztec.toolbar.toggleEditorMode()
        isHtmlEditorEnabled = !isHtmlEditorEnabled
    }

    override fun onToolbarListButtonClicked() {
        // Aztec Toolbar interface methods implemented by default with Aztec. Currently not used
    }

    override fun onToolbarMediaButtonClicked(): Boolean {
        return false
    }

    private fun getEditorText(): String? {
        return if (isHtmlEditorEnabled) {
            aztec.sourceEditor?.getPureHtml(false)
        } else {
            aztec.visualEditor.toHtml()
        }
    }

    private fun editorHasChanges(): Boolean {
        return aztec.sourceEditor?.hasChanges() != NO_CHANGES || aztec.visualEditor.hasChanges() != NO_CHANGES
    }

    private fun navigateBackWithResult(hasChanges: Boolean) {
        val bundle = Bundle().also {
            it.putInt(ARG_AZTEC_REQUEST_CODE, navArgs.requestCode)
            it.putString(ARG_AZTEC_EDITOR_TEXT, getEditorText())
            it.putBoolean(ARG_AZTEC_HAS_CHANGES, hasChanges)
        }
        navigateBackWithResult(AZTEC_EDITOR_RESULT, bundle)
    }

    override fun onRedo() {
        // no-op
    }

    override fun onRedoEnabled() {
        // no-op
    }

    override fun onUndo() {
        // no-op
    }

    override fun onUndoEnabled() {
        // no-op
    }
}
