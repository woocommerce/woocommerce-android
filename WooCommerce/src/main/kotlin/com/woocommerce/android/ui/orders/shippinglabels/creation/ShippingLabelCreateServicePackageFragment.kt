package com.woocommerce.android.ui.orders.shippinglabels.creation

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentShippingLabelCreateServicePackageBinding
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelCreateServicePackageViewModel.PackageSuccessfullyMadeEvent
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.widgets.CustomProgressDialog
import com.woocommerce.android.widgets.SkeletonView
import com.woocommerce.android.widgets.WCEmptyView
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.util.ActivityUtils
import javax.inject.Inject

@AndroidEntryPoint
class ShippingLabelCreateServicePackageFragment :
    BaseFragment(R.layout.fragment_shipping_label_create_service_package) {
    @Inject lateinit var uiMessageResolver: UIMessageResolver
    private val skeletonView: SkeletonView = SkeletonView()
    private var progressDialog: CustomProgressDialog? = null
    private lateinit var doneMenuItem: MenuItem

    private val parentViewModel: ShippingLabelCreatePackageViewModel by viewModels({ requireParentFragment() })
    val viewModel: ShippingLabelCreateServicePackageViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        menu.clear()
        inflater.inflate(R.menu.menu_done, menu)
        doneMenuItem = menu.findItem(R.id.menu_done)
        doneMenuItem.isVisible = viewModel.viewStateData.liveData.value?.canSave ?: false
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentShippingLabelCreateServicePackageBinding.bind(view)
        val packagesAdapter = ShippingLabelServicePackageAdapter(
            viewModel::onPackageSelected,
            viewModel.dimensionUnit
        )

        with(binding.servicePackagesList) {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
            adapter = packagesAdapter
        }

        setupObservers(binding, packagesAdapter)
    }

    private fun setupObservers(
        binding: FragmentShippingLabelCreateServicePackageBinding,
        adapter: ShippingLabelServicePackageAdapter
    ) {
        viewModel.viewStateData.observe(viewLifecycleOwner) { old, new ->
            new.isEmpty.takeIfNotEqualTo(old?.isEmpty) { isEmpty ->
                if (isEmpty) {
                    binding.errorView.show(
                        type = WCEmptyView.EmptyViewType.SHIPPING_LABEL_SERVICE_PACKAGE_LIST
                    )
                } else {
                    binding.errorView.hide()
                }
            }

            new.canSave.takeIfNotEqualTo(old?.canSave) {
                binding.servicePackagesListContainer.isVisible = it
                if (::doneMenuItem.isInitialized) {
                    doneMenuItem.isVisible = it
                }
            }

            new.uiModels.takeIfNotEqualTo(old?.uiModels) { uiModels ->
                adapter.updateData(uiModels)
            }

            new.isLoading.takeIfNotEqualTo(old?.isLoading) {
                showSkeleton(it, binding)
            }
            new.isSavingProgressDialogVisible?.takeIfNotEqualTo(old?.isSavingProgressDialogVisible) { isVisible ->
                if (isVisible) {
                    showProgressDialog(
                        title = R.string.shipping_label_activate_service_package_saving_progress_title,
                        message = R.string.shipping_label_create_package_saving_progress_message
                    )
                } else {
                    hideProgressDialog()
                }
            }
        }

        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is PackageSuccessfullyMadeEvent -> parentViewModel.onPackageCreated(event.madePackage)
                is ShowSnackbar -> uiMessageResolver.getSnack(
                    stringResId = event.message,
                    stringArgs = event.args
                ).show()
                else -> event.isHandled = false
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_done -> {
                ActivityUtils.hideKeyboard(activity)
                viewModel.onCustomFormDoneMenuClicked()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showSkeleton(show: Boolean, binding: FragmentShippingLabelCreateServicePackageBinding) {
        if (show) {
            skeletonView.show(
                binding.servicePackagesList,
                R.layout.skeleton_shipping_label_service_package_list,
                delayed = false
            )
        } else {
            skeletonView.hide()
        }
    }

    private fun showProgressDialog(@StringRes title: Int, @StringRes message: Int) {
        hideProgressDialog()
        progressDialog = CustomProgressDialog.show(
            getString(title),
            getString(message)
        ).also { it.show(parentFragmentManager, CustomProgressDialog.TAG) }
        progressDialog?.isCancelable = false
    }

    private fun hideProgressDialog() {
        progressDialog?.dismiss()
        progressDialog = null
    }
}
