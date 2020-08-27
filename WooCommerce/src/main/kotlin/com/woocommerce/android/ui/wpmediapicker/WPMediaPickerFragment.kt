package com.woocommerce.android.ui.wpmediapicker

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.extensions.hide
import com.woocommerce.android.extensions.navigateBackWithResult
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.extensions.show
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.model.Product
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.dialog.WooDialog
import com.woocommerce.android.ui.main.MainActivity.Companion.BackPressListener
import com.woocommerce.android.ui.products.ProductDetailFragmentDirections
import com.woocommerce.android.ui.wpmediapicker.WPMediaGalleryView.WPMediaGalleryListener
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ViewModelFactory
import kotlinx.android.synthetic.main.fragment_wpmedia_picker.*
import javax.inject.Inject

class WPMediaPickerFragment : BaseFragment(), WPMediaGalleryListener, BackPressListener {
    companion object {
        const val ARG_SELECTED_IMAGES = "selected_image_ids"
        private const val KEY_IS_CONFIRMING_DISCARD = "is_confirming_discard"
    }

    @Inject lateinit var viewModelFactory: ViewModelFactory
    @Inject lateinit var uiMessageResolver: UIMessageResolver

    private val viewModel: WPMediaPickerViewModel by viewModels { viewModelFactory }

    private val navArgs by navArgs<WPMediaPickerFragmentArgs>()

    private var isConfirmingDiscard = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        setHasOptionsMenu(true)
        return inflater.inflate(R.layout.fragment_wpmedia_picker, container, false)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
        inflater.inflate(R.menu.menu_done, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        wpMediaGallery.allowMultiSelect = navArgs.multiSelect
        initializeViewModel()

        if (savedInstanceState?.getBoolean(KEY_IS_CONFIRMING_DISCARD) == true) {
            confirmDiscard()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(KEY_IS_CONFIRMING_DISCARD, isConfirmingDiscard)
        super.onSaveInstanceState(outState)
    }

    override fun onStop() {
        super.onStop()
        WooDialog.onCleared()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_done -> {
                navigateBackWithResult()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    private fun initializeViewModel() {
        setupObservers()
        viewModel.start()
    }

    private fun setupObservers() {
        viewModel.mediaList.observe(viewLifecycleOwner, Observer {
            wpMediaGallery.showImages(it, this)
        })

        viewModel.viewStateLiveData.observe(viewLifecycleOwner) { old, new ->
            new.isLoading?.takeIfNotEqualTo(old?.isLoading) { showLoadingProgress(it) }
            new.isLoadingMore?.takeIfNotEqualTo(old?.isLoadingMore) { showLoadingMoreProgress(it) }
            new.isEmptyViewVisible?.takeIfNotEqualTo(old?.isEmptyViewVisible) { showEmptyView(it) }
        }

        viewModel.event.observe(viewLifecycleOwner, Observer { event ->
            when (event) {
                is ShowSnackbar -> uiMessageResolver.showSnack(event.message)
                is Exit -> activity?.onBackPressed()
            }
        })
    }

    /**
     * If any images are selected set the title to the selection count, otherwise use default title
     */
    override fun getFragmentTitle(): String {
        val count = wpMediaGallery.getSelectedCount()
        return if (count == 0) {
            getString(R.string.wpmedia_picker_title)
        } else {
            String.format(getString(R.string.selection_count), count)
        }
    }

    /**
     * Pass the selected images back to the product detail fragment if any are selected, otherwise
     * simply navigate back
     */
    private fun navigateBackWithResult() {
        if (wpMediaGallery.getSelectedCount() > 0) {
            val bundle = Bundle().also {
                it.putParcelableArrayList(ARG_SELECTED_IMAGES, wpMediaGallery.getSelectedImages())
            }
            requireActivity().navigateBackWithResult(
                navArgs.requestCode,
                bundle,
                R.id.nav_host_fragment_main,
                findNavController().previousBackStackEntry?.destination?.id ?: R.id.productDetailFragment
            )
        } else {
            findNavController().navigateUp()
        }
    }

    /**
     * User has scrolled to the bottom of the gallery, fire a request to load more
     */
    override fun onRequestLoadMore() {
        viewModel.onLoadMoreRequested()
    }

    /**
     * User has selected/deselected in the gallery, update the title to show the selection count
     */
    override fun onSelectionCountChanged() {
        requireActivity().title = getFragmentTitle()
    }

    /**
     * User long clicked an image, show it in the image viewer
     */
    override fun onImageLongClicked(image: Product.Image) {
        val action = ProductDetailFragmentDirections.actionGlobalWpMediaViewerFragment(image.source)
        findNavController().navigateSafely(action)
    }

    override fun onRequestAllowBackPress(): Boolean {
        return if (wpMediaGallery.getSelectedCount() > 0) {
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
                    findNavController().navigateUp()
                },
                negativeButtonId = R.string.keep_editing,
                negBtnAction = DialogInterface.OnClickListener { _, _ ->
                    isConfirmingDiscard = false
                })
    }

    private fun showLoadingProgress(show: Boolean) {
        loadingProgress.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun showLoadingMoreProgress(show: Boolean) {
        loadingMoreProgress.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun showEmptyView(show: Boolean) {
        if (show) {
            emptyText.show()
            wpMediaGallery.hide()
        } else {
            emptyText.hide()
            wpMediaGallery.show()
        }
    }
}
