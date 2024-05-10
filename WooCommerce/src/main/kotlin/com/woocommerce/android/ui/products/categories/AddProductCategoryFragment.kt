package com.woocommerce.android.ui.products.categories

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.annotation.StringRes
import androidx.appcompat.widget.Toolbar
import androidx.core.text.parseAsHtml
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.FragmentAddProductCategoryBinding
import com.woocommerce.android.extensions.navigateBackWithResult
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.ui.main.MainActivity.Companion.BackPressListener
import com.woocommerce.android.ui.products.categories.AddProductCategoryViewModel.ProgressDialog
import com.woocommerce.android.ui.products.categories.AddProductCategoryViewModel.ProgressDialog.CreatingCategory
import com.woocommerce.android.ui.products.categories.AddProductCategoryViewModel.ProgressDialog.DeletingCategory
import com.woocommerce.android.ui.products.categories.AddProductCategoryViewModel.ProgressDialog.Hidden
import com.woocommerce.android.ui.products.categories.AddProductCategoryViewModel.ProgressDialog.UpdatingCategory
import com.woocommerce.android.util.setupTabletSecondPaneToolbar
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowDialog
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.fixedHiltNavGraphViewModels
import com.woocommerce.android.widgets.CustomProgressDialog
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.util.ActivityUtils
import javax.inject.Inject

@AndroidEntryPoint
class AddProductCategoryFragment :
    BaseFragment(R.layout.fragment_add_product_category),
    BackPressListener {
    companion object {
        const val ARG_CATEGORY_UPDATE_RESULT = "arg-category-update-result"
    }

    private var doneMenuItem: MenuItem? = null
    private var deleteMenuItem: MenuItem? = null

    private var progressDialog: CustomProgressDialog? = null

    @Inject lateinit var uiMessageResolver: UIMessageResolver

    private var _binding: FragmentAddProductCategoryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AddProductCategoryViewModel by fixedHiltNavGraphViewModels(
        navGraphId = R.id.nav_graph_add_product_category
    )

    override val activityAppBarStatus: AppBarStatus
        get() = AppBarStatus.Hidden

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    override fun onStop() {
        super.onStop()
        activity?.let { ActivityUtils.hideKeyboard(it) }
    }

    private fun onCreateMenu(toolbar: Toolbar) {
        toolbar.inflateMenu(R.menu.menu_product_category_detail)
        doneMenuItem = toolbar.menu.findItem(R.id.menu_item_done)
        deleteMenuItem = toolbar.menu.findItem(R.id.menu_item_delete)
    }

    private fun onMenuItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_item_done -> {
                AnalyticsTracker.track(AnalyticsEvent.ADD_PRODUCT_CATEGORY_SAVE_TAPPED)
                viewModel.saveProductCategory(binding.productCategoryName.text)
                true
            }

            R.id.menu_item_delete -> {
                AnalyticsTracker.track(AnalyticsEvent.ADD_PRODUCT_CATEGORY_DELETE_TAPPED)
                viewModel.onDeletedCategory()
                true
            }

            else -> false
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentAddProductCategoryBinding.bind(view)

        setupObservers(viewModel)

        binding.productCategoryName.setOnTextChangedListener {
            if (binding.productCategoryName.hasFocus()) {
                viewModel.onCategoryNameChanged(it.toString())
            }
        }

        binding.clearParentCategory.setOnClickListener {
            viewModel.onClearParentCategoryClicked()
        }

        with(binding.productCategoryParent) {
            viewModel.getSelectedParentCategoryName()?.let { setText(it) }
            setClickListener {
                val action = AddProductCategoryFragmentDirections
                    .actionAddProductCategoryFragmentToParentCategoryListFragment(
                        viewModel.getSelectedParentId()
                    )
                findNavController().navigateSafely(action)
            }
        }

        setupTabletSecondPaneToolbar(
            title = getString(R.string.product_add_category),
            onMenuItemSelected = ::onMenuItemSelected,
            onCreateMenu = { toolbar ->
                toolbar.setNavigationOnClickListener {
                    if (viewModel.onBackButtonClicked(
                            binding.productCategoryName.text,
                            binding.productCategoryParent.getText()
                        )
                    ) {
                        findNavController().navigateUp()
                    }
                }
                onCreateMenu(toolbar)
            }
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onRequestAllowBackPress(): Boolean {
        return viewModel.onBackButtonClicked(
            binding.productCategoryName.text,
            binding.productCategoryParent.getText()
        )
    }

    private fun setupObservers(viewModel: AddProductCategoryViewModel) {
        viewModel.addProductCategoryViewStateLiveData.observe(viewLifecycleOwner) { old, new ->
            new.categoryNameErrorMessage?.takeIfNotEqualTo(old?.categoryNameErrorMessage) {
                displayCategoryNameError(it)
            }
            new.displayProgressDialog.takeIfNotEqualTo(old?.displayProgressDialog) { showProgressDialog(it) }
            new.categoryName.takeIfNotEqualTo(old?.categoryName) {
                if (it != binding.productCategoryName.text) {
                    binding.productCategoryName.text = it.parseAsHtml().toString()
                }
            }
            new.selectedParentId.takeIfNotEqualTo(old?.selectedParentId) {
                val parentCategoryName = viewModel.getSelectedParentCategoryName()
                if (parentCategoryName != null) {
                    binding.productCategoryParent.setHtmlText(parentCategoryName)
                } else {
                    binding.productCategoryParent.setHtmlText("")
                }
            }
            new.isEditingMode.takeIfNotEqualTo(old?.isEditingMode) {
                deleteMenuItem?.isVisible = it
            }
            new.selectedParentId.takeIfNotEqualTo(old?.selectedParentId) {
                binding.clearParentCategory.isEnabled = it != 0L && it != null
            }
        }

        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is ShowSnackbar -> uiMessageResolver.showSnack(event.message)
                is Exit -> requireActivity().onBackPressedDispatcher.onBackPressed()
                is ShowDialog -> event.showDialog()
                is ExitWithResult<*> -> navigateBackWithResult(ARG_CATEGORY_UPDATE_RESULT, event.data)
                else -> event.isHandled = false
            }
        }
    }

    private fun displayCategoryNameError(messageId: Int) {
        if (messageId != 0) {
            binding.productCategoryName.error = getString(messageId)
            doneMenuItem?.isVisible = false
        } else {
            binding.productCategoryName.clearError()
            doneMenuItem?.isVisible = true
        }
    }

    private fun showProgressDialog(progressDialog: ProgressDialog) {
        hideProgressDialog()
        when (progressDialog) {
            CreatingCategory -> showProgressDialog(R.string.product_add_category_dialog_title)
            UpdatingCategory -> showProgressDialog(R.string.product_update_category_dialog_title)
            DeletingCategory -> showProgressDialog(R.string.product_removing_category_dialog_title)
            Hidden -> hideProgressDialog()
        }
    }

    private fun showProgressDialog(
        @StringRes title: Int,
        @StringRes messageId: Int = R.string.product_add_category_dialog_message
    ) {
        this.progressDialog = CustomProgressDialog.show(getString(title), getString(messageId))
            .also {
                it.show(parentFragmentManager, CustomProgressDialog.TAG)
                it.isCancelable = false
            }
    }

    private fun hideProgressDialog() {
        progressDialog?.dismiss()
        progressDialog = null
    }
}
