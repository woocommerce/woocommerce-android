package com.woocommerce.android.ui.aztec

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent.PRODUCT_DESCRIPTION_AI_BUTTON_TAPPED
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.databinding.FragmentAztecEditorBinding
import com.woocommerce.android.extensions.fastStripHtml
import com.woocommerce.android.extensions.handleResult
import com.woocommerce.android.extensions.isEligibleForAI
import com.woocommerce.android.extensions.navigateBackWithResult
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.dialog.WooDialog
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.ui.main.MainActivity.Companion.BackPressListener
import com.woocommerce.android.ui.products.ai.description.AIProductDescriptionBottomSheetFragment.Companion.KEY_AI_GENERATED_DESCRIPTION_RESULT
import com.woocommerce.android.util.setupTabletSecondPaneToolbar
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.util.ActivityUtils
import org.wordpress.android.util.ToastUtils
import org.wordpress.aztec.Aztec
import org.wordpress.aztec.AztecText.EditorHasChanges.NO_CHANGES
import org.wordpress.aztec.IHistoryListener
import org.wordpress.aztec.ITextFormat
import org.wordpress.aztec.glideloader.GlideImageLoader
import org.wordpress.aztec.toolbar.IAztecToolbarClickListener
import javax.inject.Inject

@AndroidEntryPoint
class AztecEditorFragment :
    BaseFragment(R.layout.fragment_aztec_editor),
    IAztecToolbarClickListener,
    BackPressListener,
    IHistoryListener {
    companion object {
        const val TAG: String = "AztecEditorFragment"
        const val AZTEC_EDITOR_RESULT = "aztec_editor_result"
        const val ARG_AZTEC_REQUEST_CODE = "aztec-request-code"
        const val ARG_AZTEC_EDITOR_TEXT = "editor-text"
        const val ARG_AZTEC_HAS_CHANGES = "editor-has-changes"
        const val ARG_AZTEC_TITLE_FROM_AI_DESCRIPTION = "title-from-ai-description"

        private const val FIELD_IS_HTML_EDITOR_ENABLED = "is_html_editor_enabled"
    }

    @Inject lateinit var analyticsTracker: AnalyticsTrackerWrapper

    @Inject lateinit var selectedSite: SelectedSite

    private lateinit var aztec: Aztec

    private val navArgs: AztecEditorFragmentArgs by navArgs()

    private var isHtmlEditorEnabled: Boolean = false

    private var titleFromProductAIDescriptionDialog: String? = null

    override val activityAppBarStatus: AppBarStatus
        get() = AppBarStatus.Hidden

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = FragmentAztecEditorBinding.bind(view)

        if (navArgs.aztecCaption.isNullOrBlank()) {
            binding.aztecCaption.visibility = View.GONE
        } else {
            binding.aztecCaption.visibility = View.VISIBLE
            binding.aztecCaption.text = navArgs.aztecCaption
        }

        setupTabletSecondPaneToolbar(
            title = navArgs.aztecTitle,
            onMenuItemSelected = { _ -> false },
            onCreateMenu = { toolbar ->
                toolbar.setNavigationOnClickListener {
                    navigateBackWithResult(editorHasChanges())
                }
            }
        )

        with(binding.aiButton) {
            visibility = if (selectedSite.getOrNull()?.isEligibleForAI == true) View.VISIBLE else View.GONE
            setOnClickListener {
                onAIButtonClicked()
            }

            setOnLongClickListener {
                ToastUtils.showToast(requireContext(), R.string.ai_product_toolbar_button_tooltip)
                true
            }
        }

        aztec = Aztec.with(binding.visualEditor, binding.sourceEditor, binding.aztecToolbar, this)
            .setImageGetter(GlideImageLoader(requireContext()))

        if (navArgs.productTitle.isBlank()) {
            aztec.visualEditor.setHint(R.string.product_description_hint_no_title)
        } else {
            aztec.visualEditor.hint = getString(R.string.product_description_hint_with_title, navArgs.productTitle)
        }

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

        handleResults()
    }

    private fun onAIButtonClicked() {
        analyticsTracker.track(
            stat = PRODUCT_DESCRIPTION_AI_BUTTON_TAPPED,
            properties = mapOf(AnalyticsTracker.KEY_SOURCE to AnalyticsTracker.VALUE_AZTEC_EDITOR)
        )

        findNavController().navigateSafely(
            AztecEditorFragmentDirections.actionAztecEditorFragmentToAIProductDescriptionBottomSheetFragment(
                navArgs.productTitle,
                getEditorText()?.fastStripHtml()
            )
        )
    }

    private fun handleResults() {
        handleResult<Pair<String, String>>(KEY_AI_GENERATED_DESCRIPTION_RESULT) { pairResult ->
            aztec.visualEditor.setText(pairResult.first)

            titleFromProductAIDescriptionDialog = pairResult.second
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
            titleFromProductAIDescriptionDialog?.let { title ->
                it.putString(ARG_AZTEC_TITLE_FROM_AI_DESCRIPTION, title)
            }
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
