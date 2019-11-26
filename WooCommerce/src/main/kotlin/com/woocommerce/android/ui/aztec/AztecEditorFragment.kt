package com.woocommerce.android.ui.aztec

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.navArgs
import com.woocommerce.android.R
import com.woocommerce.android.extensions.navigateBackWithResult
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
        const val AZTEC_EDITOR_REQUEST_CODE = 3001
        const val ARG_AZTEC_EDITOR_TEXT = "editor-text"
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

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
        inflater.inflate(R.menu.menu_done, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_done -> {
                // TODO: add event for click here
                val bundle = Bundle()
                bundle.putString(ARG_AZTEC_EDITOR_TEXT, aztec.visualEditor.text.toString())
                requireActivity().navigateBackWithResult(
                        AZTEC_EDITOR_REQUEST_CODE,
                        bundle,
                        R.id.nav_host_fragment_main,
                        R.id.productDetailFragment
                )
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
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

