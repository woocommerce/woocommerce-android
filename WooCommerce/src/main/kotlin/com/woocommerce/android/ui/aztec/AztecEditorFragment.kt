package com.woocommerce.android.ui.aztec

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.navArgs
import com.woocommerce.android.R
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.main.MainActivity
import com.woocommerce.android.util.GlideImageLoader
import kotlinx.android.synthetic.main.fragment_aztec_editor.*
import org.wordpress.android.util.ActivityUtils
import org.wordpress.aztec.Aztec
import org.wordpress.aztec.ITextFormat
import org.wordpress.aztec.toolbar.IAztecToolbarClickListener

class AztecEditorFragment : BaseFragment(), IAztecToolbarClickListener {
    companion object {
        const val TAG: String = "AztecEditorFragment"
    }

    private lateinit var aztec: Aztec

    private val navArgs: AztecEditorFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)
        return inflater.inflate(R.layout.fragment_aztec_editor, container, false)
    }

    override fun getFragmentTitle() = getString(R.string.product_description)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        (activity as? MainActivity)?.hideBottomNav()

        aztec = Aztec.with(visualEditor, sourceEditor, aztecToolbar, this)
                .setImageGetter(GlideImageLoader(requireContext()))

        aztec.initSourceEditorHistory()

        aztec.visualEditor.fromHtml(navArgs.aztecText)
        aztec.sourceEditor?.displayStyledAndFormattedHtml(navArgs.aztecText)
    }

    override fun onDestroy() {
        activity?.let {
            ActivityUtils.hideKeyboard(it)
        }
        super.onDestroy()
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
    }

    override fun onToolbarListButtonClicked() {
        // Aztec Toolbar interface methods implemented by default with Aztec. Currently not used
    }

    override fun onToolbarMediaButtonClicked(): Boolean {
        return false
    }
}
