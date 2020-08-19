package com.woocommerce.android.ui.products.downloads

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.observe
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import com.woocommerce.android.R
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.dialog.CustomDiscardDialog
import com.woocommerce.android.ui.main.MainActivity.Companion.BackPressListener
import com.woocommerce.android.ui.products.ProductDetailViewModel
import com.woocommerce.android.ui.products.downloads.ProductDownloadDetailsViewModel.ProductDownloadDetailsEvent.UpdateFileAndExitEvent
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowDiscardDialog
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ViewModelFactory
import kotlinx.android.synthetic.main.fragment_product_download_details.*
import org.wordpress.android.util.ActivityUtils
import javax.inject.Inject

class ProductDownloadDetailsFragment : BaseFragment(), BackPressListener {
    @Inject lateinit var uiMessageResolver: UIMessageResolver
    @Inject lateinit var viewModelFactory: ViewModelFactory

    private val viewModel: ProductDownloadDetailsViewModel by viewModels { viewModelFactory }
    private val parentViewModel: ProductDetailViewModel by navGraphViewModels(R.id.nav_graph_products)
    private lateinit var doneOrUpdateMenuItem: MenuItem

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        setHasOptionsMenu(true)
        return inflater.inflate(R.layout.fragment_product_download_details, container, false)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
        inflater.inflate(R.menu.menu_product_download_details, menu)

        doneOrUpdateMenuItem = menu.findItem(R.id.menu_done)
        doneOrUpdateMenuItem.isVisible = viewModel.hasChanges
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_done -> {
                viewModel.onDoneOrUpdateClicked()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupObservers(viewModel)
    }

    private fun setupObservers(viewModel: ProductDownloadDetailsViewModel) {
        viewModel.productDownloadDetailsViewStateData.observe(owner = viewLifecycleOwner, observer = { old, new ->
            new.fileDraft.url.takeIfNotEqualTo(product_download_url.getText()) {
                product_download_url.setText(it)
            }
            new.fileDraft.name.takeIfNotEqualTo(product_download_name.getText()) {
                product_download_name.setText(it)
            }
            new.hasChanges.takeIfNotEqualTo(old?.hasChanges) {
                showDoneMenuItem(it)
            }
        })

        viewModel.event.observe(viewLifecycleOwner, { event ->
            when (event) {
                is ShowSnackbar -> uiMessageResolver.showSnack(event.message)
                is Exit -> {
                    ActivityUtils.hideKeyboard(requireActivity())
                    findNavController().navigateUp()
                }
                is ShowDiscardDialog -> CustomDiscardDialog.showDiscardDialog(
                    requireActivity(),
                    event.positiveBtnAction,
                    event.negativeBtnAction,
                    event.messageId
                )
                is UpdateFileAndExitEvent -> {
                    ActivityUtils.hideKeyboard(requireActivity())
                    parentViewModel.updateDownloadableFileInDraft(event.updatedFile)
                    findNavController().navigateUp()
                }
            }
        })

        initListeners()
    }

    private fun initListeners() {
        product_download_url.setOnTextChangedListener {
            viewModel.onFileUrlChanged(it.toString())
        }
        product_download_name.setOnTextChangedListener {
            viewModel.onFileNameChanged(it.toString())
        }
    }

    private fun showDoneMenuItem(show: Boolean) {
        if (::doneOrUpdateMenuItem.isInitialized) {
            doneOrUpdateMenuItem.isVisible = show
        }
    }

    override fun getFragmentTitle(): String {
        return viewModel.screenTitle
    }

    override fun onRequestAllowBackPress(): Boolean {
        return viewModel.onBackButtonClicked()
    }
}
