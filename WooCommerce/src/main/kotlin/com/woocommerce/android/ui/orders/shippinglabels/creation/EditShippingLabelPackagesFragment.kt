package com.woocommerce.android.ui.orders.shippinglabels.creation

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.content.res.AppCompatResources
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentEditShippingLabelPackagesBinding
import com.woocommerce.android.extensions.handleDialogResult
import com.woocommerce.android.extensions.handleResult
import com.woocommerce.android.extensions.navigateBackWithNotice
import com.woocommerce.android.extensions.navigateBackWithResult
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.ui.main.MainActivity.Companion.BackPressListener
import com.woocommerce.android.ui.orders.shippinglabels.creation.EditShippingLabelPackagesViewModel.OpenHazmatCategorySelector
import com.woocommerce.android.ui.orders.shippinglabels.creation.EditShippingLabelPackagesViewModel.OpenPackageCreatorEvent
import com.woocommerce.android.ui.orders.shippinglabels.creation.EditShippingLabelPackagesViewModel.OpenPackageSelectorEvent
import com.woocommerce.android.ui.orders.shippinglabels.creation.EditShippingLabelPackagesViewModel.OpenURL
import com.woocommerce.android.ui.orders.shippinglabels.creation.EditShippingLabelPackagesViewModel.ShowMoveItemDialog
import com.woocommerce.android.ui.orders.shippinglabels.creation.MoveShippingItemViewModel.MoveItemResult
import com.woocommerce.android.util.ChromeCustomTabUtils
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.widgets.SkeletonView
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

typealias OnHazmatCategorySelected = (ShippingLabelHazmatCategory) -> Unit

@AndroidEntryPoint
class EditShippingLabelPackagesFragment :
    BaseFragment(R.layout.fragment_edit_shipping_label_packages),
    BackPressListener {
    companion object {
        const val EDIT_PACKAGES_CLOSED = "edit_packages_closed"
        const val EDIT_PACKAGES_RESULT = "edit_packages_result"
        const val KEY_HAZMAT_CATEGORY_SELECTOR_RESULT = "hazmat_category_selector_result"
    }

    @Inject lateinit var uiMessageResolver: UIMessageResolver
    val viewModel: EditShippingLabelPackagesViewModel by viewModels()

    private lateinit var doneMenuItem: MenuItem

    override val activityAppBarStatus: AppBarStatus
        get() = AppBarStatus.Hidden

    private val packagesAdapter: ShippingLabelPackagesAdapter by lazy {
        ShippingLabelPackagesAdapter(
            viewModel.siteParameters,
            viewModel::onWeightEdited,
            viewModel::onExpandedChanged,
            viewModel::onPackageSpinnerClicked,
            viewModel::onMoveButtonClicked,
            viewModel::onHazmatCategoryClicked,
            viewModel::onContainsHazmatChanged,
            viewModel::onURLClicked
        )
    }

    private val skeletonView: SkeletonView = SkeletonView()

    fun onMenuItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_done -> {
                viewModel.onDoneButtonClicked()
                true
            }
            else -> {
                false
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = FragmentEditShippingLabelPackagesBinding.bind(view)
        setupToolbar(binding)
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

    private fun setupToolbar(binding: FragmentEditShippingLabelPackagesBinding) {
        binding.toolbar.title = getString(R.string.orderdetail_shipping_label_item_package_info)
        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            onMenuItemSelected(menuItem)
        }
        binding.toolbar.navigationIcon = AppCompatResources.getDrawable(
            requireActivity(),
            R.drawable.ic_back_24dp
        )
        binding.toolbar.setNavigationOnClickListener {
            onRequestAllowBackPress()
        }
        binding.toolbar.inflateMenu(R.menu.menu_done)
        doneMenuItem = binding.toolbar.menu.findItem(R.id.menu_done)
        doneMenuItem.isVisible = viewModel.viewStateData.liveData.value?.isDataValid ?: false
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
                is OpenHazmatCategorySelector -> showHazmatCategoryPicker(
                    event.packagePosition,
                    event.currentSelection,
                    event.onHazmatCategorySelected
                )
                is OpenURL -> ChromeCustomTabUtils.launchUrl(requireContext(), event.url)
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

    private fun showHazmatCategoryPicker(
        packagePosition: Int,
        currentSelection: ShippingLabelHazmatCategory?,
        onHazmatCategorySelected: OnHazmatCategorySelected
    ) {
        handleDialogResult<String>(
            key = KEY_HAZMAT_CATEGORY_SELECTOR_RESULT,
            entryId = R.id.editShippingLabelPackagesFragment
        ) { hazmatSelection ->
            val selectedCategory = ShippingLabelHazmatCategory.valueOf(hazmatSelection)
            viewModel.onHazmatCategorySelected(selectedCategory, packagePosition)
            onHazmatCategorySelected(selectedCategory)
        }
        EditShippingLabelPackagesFragmentDirections
            .actionEditShippingLabelPaymentFragmentToHazmatCategorySelector(
                title = getString(R.string.shipping_label_package_details_hazmat_select_category_action),
                requestKey = KEY_HAZMAT_CATEGORY_SELECTOR_RESULT,
                keys = ShippingLabelHazmatCategory.values()
                    .map { getString(it.stringResourceID) }
                    .toTypedArray(),
                values = ShippingLabelHazmatCategory.values()
                    .map { it.toString() }
                    .toTypedArray(),
                selectedItem = currentSelection.toString()
            ).let { findNavController().navigateSafely(it) }
    }
}
