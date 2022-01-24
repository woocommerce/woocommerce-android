package com.woocommerce.android.ui.orders.shippinglabels.creation

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentEditShippingLabelPackagesBinding
import com.woocommerce.android.extensions.handleResult
import com.woocommerce.android.extensions.navigateBackWithNotice
import com.woocommerce.android.extensions.navigateBackWithResult
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.main.MainActivity.Companion.BackPressListener
import com.woocommerce.android.ui.orders.shippinglabels.creation.EditShippingLabelPackagesViewModel.OpenPackageCreatorEvent
import com.woocommerce.android.ui.orders.shippinglabels.creation.EditShippingLabelPackagesViewModel.OpenPackageSelectorEvent
import com.woocommerce.android.ui.orders.shippinglabels.creation.EditShippingLabelPackagesViewModel.ShowMoveItemDialog
import com.woocommerce.android.ui.orders.shippinglabels.creation.MoveShippingItemViewModel.MoveItemResult
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.widgets.SkeletonView
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class EditShippingLabelPackagesFragment :
    BaseFragment(R.layout.fragment_edit_shipping_label_packages),
    BackPressListener {
    companion object {
        const val EDIT_PACKAGES_CLOSED = "edit_packages_closed"
        const val EDIT_PACKAGES_RESULT = "edit_packages_result"
    }

    @Inject lateinit var uiMessageResolver: UIMessageResolver
    val viewModel: EditShippingLabelPackagesViewModel by viewModels()

    private lateinit var doneMenuItem: MenuItem

    private val packagesAdapter: ShippingLabelPackagesAdapter by lazy {
        ShippingLabelPackagesAdapter(
            viewModel.siteParameters,
            viewModel::onWeightEdited,
            viewModel::onExpandedChanged,
            viewModel::onPackageSpinnerClicked,
            viewModel::onMoveButtonClicked
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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_done -> {
                viewModel.onDoneButtonClicked()
                true
            }
            else -> {
                super.onOptionsItemSelected(item)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentEditShippingLabelPackagesBinding.bind(view)
        with(binding.packagesList) {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
            adapter = packagesAdapter
            itemAnimator = DefaultItemAnimator().apply {
                // Disable change animations to avoid duplicating viewholders
                supportsChangeAnimations = false
            }
        }

        setupObservers(binding)
        setupResultHandlers()
    }

    private fun setupResultHandlers() {
        handleResult<ShippingPackageSelectorResult>(ShippingPackageSelectorFragment.SELECTED_PACKAGE_RESULT) { result ->
            viewModel.onPackageSelected(result.position, result.selectedPackage)
        }
        handleResult<MoveItemResult>(MoveShippingItemDialog.MOVE_ITEM_RESULT) { result ->
            viewModel.handleMoveItemResult(result)
        }
    }

    private fun setupObservers(binding: FragmentEditShippingLabelPackagesBinding) {
        viewModel.viewStateData.observe(viewLifecycleOwner) { old, new ->
            new.packagesUiModels.takeIfNotEqualTo(old?.packagesUiModels) {
                packagesAdapter.uiModels = it
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
                            position = event.position
                        )

                    findNavController().navigateSafely(action)
                }
                is OpenPackageCreatorEvent -> {
                    val action = EditShippingLabelPackagesFragmentDirections
                        .actionEditShippingLabelPackagesFragmentToShippingLabelCreatePackageFragment(
                            position = event.position
                        )

                    findNavController().navigateSafely(action)
                }

                is ShowMoveItemDialog -> {
                    val action = EditShippingLabelPackagesFragmentDirections
                        .actionEditShippingLabelPackagesFragmentToMoveShippingItemDialog(
                            item = event.item,
                            currentPackage = event.currentPackage,
                            packagesList = event.packagesList.toTypedArray()
                        )

                    findNavController().navigateSafely(action)
                }
                is ShowSnackbar -> uiMessageResolver.showSnack(event.message)
                is ExitWithResult<*> -> navigateBackWithResult(EDIT_PACKAGES_RESULT, event.data)
                is Exit -> navigateBackWithNotice(EDIT_PACKAGES_CLOSED)
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
        viewModel.onBackButtonClicked()
        return false
    }
}
