package com.woocommerce.android.ui.aztec

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IdRes
import androidx.navigation.fragment.navArgs
import com.woocommerce.android.R
import com.woocommerce.android.RequestCodes
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.AZTEC_EDITOR_DONE_BUTTON_TAPPED
import com.woocommerce.android.extensions.navigateBackWithResult
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.dialog.WooDialog
import com.woocommerce.android.ui.main.MainActivity
import com.woocommerce.android.ui.main.MainActivity.Companion.BackPressListener
import kotlinx.android.synthetic.main.fragment_aztec_editor.*
import org.wordpress.android.util.ActivityUtils
import org.wordpress.aztec.Aztec
import org.wordpress.aztec.AztecText.EditorHasChanges.NO_CHANGES
import org.wordpress.aztec.IHistoryListener
import org.wordpress.aztec.ITextFormat
import org.wordpress.aztec.glideloader.GlideImageLoader
import org.wordpress.aztec.toolbar.IAztecToolbarClickListener

class AztecEditorFragment : BaseFragment(), IAztecToolbarClickListener, BackPressListener, IHistoryListener {
    companion object {
        const val TAG: String = "AztecEditorFragment"
        const val ARG_AZTEC_EDITOR_TEXT = "editor-text"
        const val ARG_AZTEC_HAS_CHANGES = "editor-has-changes"

        private const val FIELD_IS_CONFIRMING_DISCARD = "is_confirming_discard"
        private const val FIELD_IS_HTML_EDITOR_ENABLED = "is_html_editor_enabled"
    }

    private lateinit var aztec: Aztec

    private val navArgs: AztecEditorFragmentArgs by navArgs()

    private var isConfirmingDiscard = false
    private var shouldShowDiscardDialog = true
    private var isHtmlEditorEnabled: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)
        return inflater.inflate(R.layout.fragment_aztec_editor, container, false)
    }

    override fun getFragmentTitle() = navArgs.aztecTitle

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        (activity as? MainActivity)?.hideBottomNav()

        if (navArgs.aztecCaption.isNullOrBlank()) {
            aztecCaption.visibility = View.GONE
        } else {
            aztecCaption.visibility = View.VISIBLE
            aztecCaption.text = navArgs.aztecCaption
        }

        aztec = Aztec.with(visualEditor, sourceEditor, aztecToolbar, this)
                .setImageGetter(GlideImageLoader(requireContext()))

        aztec.initSourceEditorHistory()

        aztec.visualEditor.fromHtml(navArgs.aztecText)
        aztec.sourceEditor?.displayStyledAndFormattedHtml(navArgs.aztecText)
        aztec.setHistoryListener(this)

        savedInstanceState?.let { state ->
            isHtmlEditorEnabled = state.getBoolean(FIELD_IS_HTML_EDITOR_ENABLED)
            if (state.getBoolean(FIELD_IS_CONFIRMING_DISCARD)) {
                confirmDiscard()
            }
        }

        aztec.visualEditor.post {
            aztec.visualEditor.requestFocus()
            ActivityUtils.showKeyboard(aztec.visualEditor)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
        inflater.inflate(R.menu.menu_done, menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        menu.findItem(R.id.menu_done)?.isVisible = editorHasChanges()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_done -> {
                AnalyticsTracker.track(AZTEC_EDITOR_DONE_BUTTON_TAPPED)
                shouldShowDiscardDialog = false
                navigateBackWithResult(editorHasChanges())
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(FIELD_IS_CONFIRMING_DISCARD, isConfirmingDiscard)
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

    /**
     * Prevent back press in the main activity if the user made changes so we can confirm the discard
     */
    override fun onRequestAllowBackPress(): Boolean {
        return if (editorHasChanges() && shouldShowDiscardDialog) {
            confirmDiscard()
            false
        } else {
            true
        }
    }

    private fun confirmDiscard() {
        isConfirmingDiscard = true
        WooDialog.showDialog(
                requireActivity(),
                messageId = R.string.discard_message,
                positiveButtonId = R.string.discard,
                posBtnAction = DialogInterface.OnClickListener { _, _ ->
                    isConfirmingDiscard = false
                    navigateBackWithResult(false)
                },
                negativeButtonId = R.string.keep_editing,
                negBtnAction = DialogInterface.OnClickListener { _, _ ->
                    isConfirmingDiscard = false
                })
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
        val hasChanges = if (isHtmlEditorEnabled) {
            aztec.sourceEditor?.hasChanges()
        } else {
            aztec.visualEditor.hasChanges()
        }
        return hasChanges != NO_CHANGES
    }

    private fun navigateBackWithResult(hasChanges: Boolean) {
        val bundle = Bundle().also {
            it.putString(ARG_AZTEC_EDITOR_TEXT, getEditorText())
            it.putBoolean(ARG_AZTEC_HAS_CHANGES, hasChanges)
        }
        @IdRes val destinationId = if (navArgs.requestCode == RequestCodes.PRODUCT_SETTINGS_PURCHASE_NOTE) {
            R.id.productSettingsFragment
        } else {
            R.id.productDetailFragment
        }
        requireActivity().navigateBackWithResult(
                navArgs.requestCode,
                bundle,
                R.id.nav_host_fragment_main,
                destinationId
        )
    }

    override fun onRedo() {
        requireActivity().invalidateOptionsMenu()
    }

    override fun onRedoEnabled() {
        requireActivity().invalidateOptionsMenu()
    }

    override fun onUndo() {
        requireActivity().invalidateOptionsMenu()
    }

    override fun onUndoEnabled() {
        requireActivity().invalidateOptionsMenu()
    }
}
