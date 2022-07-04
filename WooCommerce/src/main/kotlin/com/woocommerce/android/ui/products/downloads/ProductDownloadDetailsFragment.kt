package com.woocommerce.android.ui.products.downloads

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.fragment.app.viewModels
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.lifecycle.observe
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.DownloadableFileAction
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_DOWNLOADABLE_FILE_ACTION
import com.woocommerce.android.databinding.FragmentProductDownloadDetailsBinding
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
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.util.ActivityUtils
import javax.inject.Inject

@AndroidEntryPoint
class ProductDownloadDetailsFragment :
    BaseFragment(R.layout.fragment_product_download_details), BackPressListener {
    @Inject lateinit var uiMessageResolver: UIMessageResolver

    private val viewModel: ProductDownloadDetailsViewModel by viewModels()
    private val parentViewModel: ProductDetailViewModel by hiltNavGraphViewModels(R.id.nav_graph_products)
    private val navArgs by navArgs<ProductDownloadDetailsFragmentArgs>()
    private lateinit var doneOrUpdateMenuItem: MenuItem

    private var _binding: FragmentProductDownloadDetailsBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentProductDownloadDetailsBinding.bind(view)

        setHasOptionsMenu(true)
        setupObservers(viewModel)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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

                val action = if (navArgs.isEditing) DownloadableFileAction.UPDATED else DownloadableFileAction.ADDED
                AnalyticsTracker.track(
                    AnalyticsEvent.PRODUCTS_DOWNLOADABLE_FILE,
                    mapOf(KEY_DOWNLOADABLE_FILE_ACTION to action.value)
                )

                true
            }
            R.id.menu_delete -> {
                viewModel.onDeleteButtonClicked()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupObservers(viewModel: ProductDownloadDetailsViewModel) {
        viewModel.productDownloadDetailsViewStateData.observe(
            owner = viewLifecycleOwner,
            observer = { old, new ->
                new.fileDraft.url.takeIfNotEqualTo(binding.productDownloadUrl.text) {
                    binding.productDownloadUrl.text = it
                }
                new.fileDraft.name.takeIfNotEqualTo(binding.productDownloadName.text) {
                    binding.productDownloadName.text = it
                }
                new.showDoneButton.takeIfNotEqualTo(old?.showDoneButton) {
                    showDoneMenuItem(it)
                }
                if (new.urlErrorMessage != old?.urlErrorMessage || new.nameErrorMessage != old?.nameErrorMessage) {
                    updateErrorMessages(new.urlErrorMessage, new.nameErrorMessage)
                }
            }
        )

        viewModel.event.observe(viewLifecycleOwner) { event ->
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

                    AnalyticsTracker.track(
                        AnalyticsEvent.PRODUCTS_DOWNLOADABLE_FILE,
                        mapOf(KEY_DOWNLOADABLE_FILE_ACTION to DownloadableFileAction.DELETED.value)
                    )

                    findNavController().navigateUp()
                }
            }
        }

        initListeners()
    }

    private fun initListeners() {
        binding.productDownloadUrl.setOnTextChangedListener {
            viewModel.onFileUrlChanged(it.toString())
        }
        binding.productDownloadName.setOnTextChangedListener {
            viewModel.onFileNameChanged(it.toString())
        }
    }

    private fun updateErrorMessages(urlErrorMessage: Int?, nameErrorMessage: Int?) {
        binding.productDownloadUrl.error = if (urlErrorMessage != null) getString(urlErrorMessage) else null
        binding.productDownloadName.error = if (nameErrorMessage != null) getString(nameErrorMessage) else null
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
