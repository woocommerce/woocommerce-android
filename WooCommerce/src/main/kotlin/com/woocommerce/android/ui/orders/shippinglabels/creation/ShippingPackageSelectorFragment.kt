package com.woocommerce.android.ui.orders.shippinglabels.creation

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentShippingPackagesSelectorBinding
import com.woocommerce.android.extensions.navigateBackWithResult
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingPackageSelectorViewModel.ShowCreatePackageScreen
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ShippingPackageSelectorFragment : BaseFragment(R.layout.fragment_shipping_packages_selector) {
    companion object {
        const val SELECTED_PACKAGE_RESULT = "selected-package"
    }

    @Inject lateinit var uiMessageResolver: UIMessageResolver
    val viewModel: ShippingPackageSelectorViewModel by viewModels()

    private val packagesAdapter by lazy {
        ShippingPackagesAdapter(
            viewModel.dimensionUnit,
            viewModel::onPackageSelected
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentShippingPackagesSelectorBinding.bind(view)

        with(binding.packagesList) {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
            adapter = packagesAdapter
        }

        binding.packagesCreateNewButton.setOnClickListener {
            viewModel.onCreateNewPackageButtonClicked()
        }
        setupObservers(binding)
    }

    private fun setupObservers(binding: FragmentShippingPackagesSelectorBinding) {
        viewModel.viewStateData.observe(viewLifecycleOwner) { old, new ->
            new.packagesList.takeIfNotEqualTo(old?.packagesList) { list ->
                packagesAdapter.updatePackages(list)
            }
            new.isLoading.takeIfNotEqualTo(old?.isLoading) { isLoading ->
                binding.loadingProgress.isVisible = isLoading
                binding.packagesList.isVisible = !isLoading
            }
        }
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is ShowSnackbar -> uiMessageResolver.showSnack(event.message)
                is ExitWithResult<*> -> navigateBackWithResult(SELECTED_PACKAGE_RESULT, event.data)
                is Exit -> findNavController().navigateUp()
                is ShowCreatePackageScreen -> {
                    val action = ShippingPackageSelectorFragmentDirections
                        .actionShippingPackageSelectorFragmentToShippingLabelCreatePackageFragment(
                            event.position
                        )
                    findNavController().navigateSafely(action)
                }
                else -> event.isHandled = false
            }
        }
    }

    override fun getFragmentTitle() = getString(R.string.shipping_label_package_selector_title)
}
