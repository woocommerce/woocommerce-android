package com.woocommerce.android.ui.orders.shippinglabels.creation

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.observe
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentEditShippingLabelPackagesBinding
import com.woocommerce.android.extensions.handleResult
import com.woocommerce.android.extensions.navigateBackWithNotice
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.main.MainActivity.Companion.BackPressListener
import com.woocommerce.android.ui.orders.shippinglabels.creation.EditShippingLabelPackagesViewModel.OpenPackageSelectorEvent
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.ViewModelFactory
import com.woocommerce.android.widgets.SkeletonView
import javax.inject.Inject

class EditShippingLabelPackagesFragment : BaseFragment(R.layout.fragment_edit_shipping_label_packages),
    BackPressListener {
    companion object {
        const val EDIT_PACKAGES_CLOSED = "edit_address_closed"
    }

    @Inject lateinit var viewModelFactory: ViewModelFactory
    val viewModel: EditShippingLabelPackagesViewModel by viewModels { viewModelFactory }

    private lateinit var doneMenuItem: MenuItem

    private val packagesAdapter: ShippingLabelPackagesAdapter by lazy {
        ShippingLabelPackagesAdapter(
            viewModel.parameters,
            viewModel::onWeightEdited,
            viewModel::onPackageSpinnerClicked
        )
    }

    private val skeletonView: SkeletonView = SkeletonView()

    override fun getFragmentTitle() = getString(R.string.orderdetail_shipping_label_item_package_info)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)

        inflater.inflate(R.menu.menu_done, menu)
        doneMenuItem = menu.findItem(R.id.menu_done)
        doneMenuItem.isVisible = viewModel.viewStateData.liveData.value?.isDataValid ?: false
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentEditShippingLabelPackagesBinding.bind(view)
        with(binding.packagesList) {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
            adapter = packagesAdapter
        }

        setupObservers(binding)
        setupResultHandlers()
    }

    private fun setupResultHandlers() {
        handleResult<ShippingPackageSelectorResult>(ShippingPackageSelectorFragment.SELECTED_PACKAGE_RESULT) { result ->
            viewModel.onPackageSelected(result.position, result.selectedPackage)
        }
    }

    private fun setupObservers(binding: FragmentEditShippingLabelPackagesBinding) {
        viewModel.viewStateData.observe(viewLifecycleOwner) { old, new ->
            new.shippingLabelPackages.takeIfNotEqualTo(old?.shippingLabelPackages) {
                packagesAdapter.shippingLabelPackages = it
            }

            new.showSkeletonView.takeIfNotEqualTo(old?.showSkeletonView) {
                showSkeleton(it, binding)
            }

            new.isDataValid.takeIfNotEqualTo(old?.isDataValid) {
                if (::doneMenuItem.isInitialized) {
                    doneMenuItem.isVisible = it
                }
            }
        }
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is OpenPackageSelectorEvent -> {
                    val action = EditShippingLabelPackagesFragmentDirections
                        .actionEditShippingLabelPackagesFragmentToShippingPackageSelectorFragment(
                            position = event.position,
                            availablePackages = viewModel.availablePackages
                        )

                    findNavController().navigateSafely(action)
                }
                is Exit -> findNavController().navigateUp()
                else -> event.isHandled = false
            }
        }
    }

    fun showSkeleton(show: Boolean, binding: FragmentEditShippingLabelPackagesBinding) {
        if (show) {
            skeletonView.show(binding.packagesList, R.layout.skeleton_shipping_label_package_details, delayed = false)
        } else {
            skeletonView.hide()
        }
    }

    override fun onRequestAllowBackPress(): Boolean {
        navigateBackWithNotice(EDIT_PACKAGES_CLOSED)
        return false
    }
}
