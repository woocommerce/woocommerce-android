package com.woocommerce.android.ui.wpmediapicker

import android.content.DialogInterface
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.FragmentWpmediaPickerBinding
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
import javax.inject.Inject

class WPMediaPickerFragment : BaseFragment(R.layout.fragment_wpmedia_picker),
    WPMediaGalleryListener,
    BackPressListener {
    companion object {
        private const val KEY_IS_CONFIRMING_DISCARD = "is_confirming_discard"
        const val KEY_WP_IMAGE_PICKER_RESULT = "key_wp_image_picker_result"
    }

    @Inject lateinit var viewModelFactory: ViewModelFactory
    @Inject lateinit var uiMessageResolver: UIMessageResolver

    private val viewModel: WPMediaPickerViewModel by viewModels { viewModelFactory }
    private val isMultiSelectAllowed: Boolean
        get() = viewModel.viewStateLiveData.liveData.value?.isMultiSelectionAllowed ?: true

    private val navArgs by navArgs<WPMediaPickerFragmentArgs>()

    private var doneOrUpdateMenuItem: MenuItem? = null
    private var isConfirmingDiscard = false

    private var _binding: FragmentWpmediaPickerBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setHasOptionsMenu(true)
        _binding = FragmentWpmediaPickerBinding.bind(view)

        binding.wpMediaGallery.isMultiSelectionAllowed = navArgs.allowMultiple
        initializeViewModel()

        if (savedInstanceState?.getBoolean(KEY_IS_CONFIRMING_DISCARD) == true) {
            confirmDiscard()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
        inflater.inflate(R.menu.menu_done, menu)
        doneOrUpdateMenuItem = menu.findItem(R.id.menu_done)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)

        doneOrUpdateMenuItem?.isVisible = isMultiSelectAllowed
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
            binding.wpMediaGallery.showImages(it, this, isMultiSelectAllowed)
        })

        viewModel.viewStateLiveData.observe(viewLifecycleOwner) { old, new ->
            new.isLoading?.takeIfNotEqualTo(old?.isLoading) { binding.loadingProgress.isVisible = it }
            new.isLoadingMore?.takeIfNotEqualTo(old?.isLoadingMore) { binding.loadingMoreProgress.isVisible = it }
            new.isEmptyViewVisible?.takeIfNotEqualTo(old?.isEmptyViewVisible) { showEmptyView(it) }
            new.isMultiSelectionAllowed?.takeIfNotEqualTo(old?.isEmptyViewVisible) {
                doneOrUpdateMenuItem?.isVisible = it
            }
        }

        viewModel.event.observe(viewLifecycleOwner, Observer { event ->
            when (event) {
                is ShowSnackbar -> uiMessageResolver.showSnack(event.message)
                is Exit -> findNavController().navigateUp()
            }
        })
    }

    /**
     * If any images are selected set the title to the selection count, otherwise use default title
     */
    override fun getFragmentTitle(): String {
        val count = binding.wpMediaGallery.getSelectedCount()
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
        if (binding.wpMediaGallery.getSelectedCount() > 0) {
            navigateBackWithResult(KEY_WP_IMAGE_PICKER_RESULT, binding.wpMediaGallery.getSelectedImages())
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

        if (!isMultiSelectAllowed && binding.wpMediaGallery.getSelectedCount() == 1) {
            navigateBackWithResult()
        }
    }

    /**
     * User long clicked an image, show it in the image viewer
     */
    override fun onImageLongClicked(image: Product.Image) {
        val action = ProductDetailFragmentDirections.actionGlobalWpMediaViewerFragment(image.source)
        findNavController().navigateSafely(action)
    }

    override fun onRequestAllowBackPress(): Boolean {
        return if (binding.wpMediaGallery.getSelectedCount() > 0) {
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

    private fun showEmptyView(show: Boolean) {
        if (show) {
            binding.emptyText.show()
            binding.wpMediaGallery.hide()
        } else {
            binding.emptyText.hide()
            binding.wpMediaGallery.show()
        }
    }
}
