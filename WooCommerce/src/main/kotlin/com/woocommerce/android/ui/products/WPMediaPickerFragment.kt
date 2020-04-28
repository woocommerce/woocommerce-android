package com.woocommerce.android.ui.products

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.woocommerce.android.R
import com.woocommerce.android.RequestCodes
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.extensions.navigateBackWithResult
import com.woocommerce.android.model.Product.Image
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.viewmodel.ViewModelFactory
import com.woocommerce.android.widgets.WPMediaLibraryImageGalleryView.OnWPMediaGalleryClickListener
import kotlinx.android.synthetic.main.fragment_wpmedia_picker.*
import javax.inject.Inject

class WPMediaPickerFragment : BaseFragment(), OnWPMediaGalleryClickListener {
    @Inject lateinit var viewModelFactory: ViewModelFactory
    private val viewModel: WPMediaPickerViewModel by viewModels { viewModelFactory }

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
        initializeViewModel()
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
        setupObservers(viewModel)
        viewModel.start()
    }

    private fun setupObservers(viewModel: WPMediaPickerViewModel) {
        viewModel.mediaList.observe(viewLifecycleOwner, Observer {
            wpMediaGallery.showImages(it, this)
        })
    }

    override fun getFragmentTitle() = getString(R.string.product_wpmedia_title)

    private fun navigateBackWithResult() {
        val bundle = Bundle().also {
            // TODO
        }
        requireActivity().navigateBackWithResult(
                RequestCodes.WPMEDIA_LIBRARY_PICKER,
                bundle,
                R.id.nav_host_fragment_main,
                R.id.productDetailFragment
        )
    }

    override fun onWPMediaClicked(image: Image, imageView: View) {
        // TODO
    }
}
