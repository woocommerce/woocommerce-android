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
import androidx.navigation.fragment.navArgs
import androidx.navigation.navGraphViewModels
import com.woocommerce.android.R
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.main.MainActivity.Companion.BackPressListener
import com.woocommerce.android.ui.products.ProductDetailViewModel
import com.woocommerce.android.ui.products.downloads.ProductDownloadDetailsViewModel.ProductDownloadDetailsEvent.AddFileAndExitEvent
import com.woocommerce.android.ui.products.downloads.ProductDownloadDetailsViewModel.ProductDownloadDetailsEvent.DeleteFileEvent
import com.woocommerce.android.ui.products.downloads.ProductDownloadDetailsViewModel.ProductDownloadDetailsEvent.UpdateFileAndExitEvent
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowDialog
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
    private val navArgs by navArgs<ProductDownloadDetailsFragmentArgs>()
    private lateinit var doneOrUpdateMenuItem: MenuItem

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        setHasOptionsMenu(true)
        return inflater.inflate(R.layout.fragment_product_download_details, container, false)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
        if (navArgs.isEditing) {
            inflater.inflate(R.menu.menu_product_download_details, menu)
        } else {
            inflater.inflate(R.menu.menu_done, menu)
        }

        doneOrUpdateMenuItem = menu.findItem(R.id.menu_done)
        doneOrUpdateMenuItem.isVisible = viewModel.showDoneButton
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_done -> {
                viewModel.onDoneOrUpdateClicked()
                true
            }
            R.id.menu_delete -> {
                viewModel.onDeleteButtonClicked()
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
            new.showDoneButton.takeIfNotEqualTo(old?.showDoneButton) {
                showDoneMenuItem(it)
            }
            if (new.urlErrorMessage != old?.urlErrorMessage || new.nameErrorMessage != old?.nameErrorMessage) {
                updateErrorMessages(new.urlErrorMessage, new.nameErrorMessage)
            }
        })

        viewModel.event.observe(viewLifecycleOwner, { event ->
            when (event) {
                is ShowSnackbar -> uiMessageResolver.showSnack(event.message)
                is Exit -> {
                    ActivityUtils.hideKeyboard(requireActivity())
                    findNavController().navigateUp()
                }
                is ShowDialog -> event.showDialog()
                is UpdateFileAndExitEvent -> {
                    ActivityUtils.hideKeyboard(requireActivity())
                    parentViewModel.updateDownloadableFileInDraft(event.updatedFile)
                    findNavController().navigateUp()
                }
                is AddFileAndExitEvent -> {
                    ActivityUtils.hideKeyboard(requireActivity())
                    parentViewModel.addDownloadableFileToDraft(event.file)
                    findNavController().navigateUp()
                }
                is DeleteFileEvent -> {
                    parentViewModel.deleteDownloadableFile(event.file)
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

    private fun updateErrorMessages(urlErrorMessage: Int?, nameErrorMessage: Int?) {
        product_download_url.error = if (urlErrorMessage != null) getString(urlErrorMessage) else null
        product_download_name.error = if (nameErrorMessage != null) getString(nameErrorMessage) else null
        enableDoneButton(urlErrorMessage == null && nameErrorMessage == null)
    }

    private fun enableDoneButton(enable: Boolean) {
        if (::doneOrUpdateMenuItem.isInitialized) {
            doneOrUpdateMenuItem.isEnabled = enable
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
