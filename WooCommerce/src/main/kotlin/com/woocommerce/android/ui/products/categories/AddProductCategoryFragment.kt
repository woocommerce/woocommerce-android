package com.woocommerce.android.ui.products.categories

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.Toolbar
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
        const val ARG_ADDED_CATEGORY = "arg-added-category"
    }

    private var doneMenuItem: MenuItem? = null

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
        toolbar.inflateMenu(R.menu.menu_done)
        doneMenuItem = toolbar.menu.findItem(R.id.menu_done)
    }

    private fun onMenuItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_done -> {
                AnalyticsTracker.track(AnalyticsEvent.ADD_PRODUCT_CATEGORY_SAVE_TAPPED)
                viewModel.addProductCategory(binding.productCategoryName.text)
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
            new.displayProgressDialog?.takeIfNotEqualTo(old?.displayProgressDialog) { showProgressDialog(it) }
            new.categoryName.takeIfNotEqualTo(old?.categoryName) { binding.productCategoryName.text = it }
            new.selectedParentId.takeIfNotEqualTo(old?.selectedParentId) {
                val parentCategoryName = viewModel.getSelectedParentCategoryName()
                parentCategoryName?.let { binding.productCategoryParent.setText(it) }
            }
        }

        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is ShowSnackbar -> uiMessageResolver.showSnack(event.message)
                is Exit -> requireActivity().onBackPressedDispatcher.onBackPressed()
                is ShowDialog -> event.showDialog()
                is ExitWithResult<*> -> navigateBackWithResult(ARG_ADDED_CATEGORY, event.data)
                else -> event.isHandled = false
            }
        }
    }

    private fun displayCategoryNameError(messageId: Int) {
        if (messageId != 0) {
            binding.productCategoryName.error = getString(messageId)
            showDoneMenuItem(false)
        } else {
            binding.productCategoryName.clearError()
            showDoneMenuItem(true)
        }
    }

    private fun showDoneMenuItem(show: Boolean) {
        doneMenuItem?.isVisible = show
    }

    private fun showProgressDialog(show: Boolean) {
        if (show) {
            hideProgressDialog()
            progressDialog = CustomProgressDialog.show(
                getString(R.string.product_add_category_dialog_title),
                getString(R.string.product_add_category_dialog_message)
            ).also { it.show(parentFragmentManager, CustomProgressDialog.TAG) }
            progressDialog?.isCancelable = false
        } else {
            hideProgressDialog()
        }
    }

    private fun hideProgressDialog() {
        progressDialog?.dismiss()
        progressDialog = null
    }
}
