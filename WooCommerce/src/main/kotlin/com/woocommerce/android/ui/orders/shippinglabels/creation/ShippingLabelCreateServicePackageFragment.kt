package com.woocommerce.android.ui.orders.shippinglabels.creation

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentShippingLabelCreateServicePackageBinding
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.util.ActivityUtils
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelCreateServicePackageViewModel.PackageSuccessfullyMadeEvent
import com.woocommerce.android.widgets.SkeletonView
import javax.inject.Inject

@AndroidEntryPoint
class ShippingLabelCreateServicePackageFragment :
    BaseFragment(R.layout.fragment_shipping_label_create_service_package) {
    @Inject lateinit var uiMessageResolver: UIMessageResolver
    private val skeletonView: SkeletonView = SkeletonView()

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
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentShippingLabelCreateServicePackageBinding.bind(view)
        val packagesAdapter = ShippingLabelServicePackageAdapter(
            viewModel::onPackageSelected
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
            new.uiModels.takeIfNotEqualTo(old?.uiModels) { uiModels ->
                adapter.updateData(uiModels)
            }

            new.isLoading.takeIfNotEqualTo(old?.isLoading) {
                showSkeleton(it, binding)
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

    fun showSkeleton(show: Boolean, binding: FragmentShippingLabelCreateServicePackageBinding) {
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
}
