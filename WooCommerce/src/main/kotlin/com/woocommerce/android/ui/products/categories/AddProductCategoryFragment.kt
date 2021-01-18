package com.woocommerce.android.ui.products.categories

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.databinding.FragmentAddProductCategoryBinding
import com.woocommerce.android.extensions.navigateBackWithResult
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.extensions.setHtmlText
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.main.MainActivity.Companion.BackPressListener
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowDialog
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ViewModelFactory
import com.woocommerce.android.widgets.CustomProgressDialog
import dagger.Lazy
import org.wordpress.android.util.ActivityUtils
import javax.inject.Inject

class AddProductCategoryFragment : BaseFragment(R.layout.fragment_add_product_category), BackPressListener {
    companion object {
        const val ARG_ADDED_CATEGORY = "arg-added-category"
    }

    private var doneMenuItem: MenuItem? = null

    private var progressDialog: CustomProgressDialog? = null

    @Inject lateinit var uiMessageResolver: UIMessageResolver
    @Inject lateinit var viewModelFactory: Lazy<ViewModelFactory>

    private var _binding: FragmentAddProductCategoryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AddProductCategoryViewModel
        by navGraphViewModels(R.id.nav_graph_add_product_category) { viewModelFactory.get() }

    override fun getFragmentTitle() = getString(R.string.product_add_category)

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    override fun onStop() {
        super.onStop()
        activity?.let { ActivityUtils.hideKeyboard(it) }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
        inflater.inflate(R.menu.menu_done, menu)
        super.onCreateOptionsMenu(menu, inflater)
        doneMenuItem = menu.findItem(R.id.menu_done)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_done -> {
                AnalyticsTracker.track(Stat.ADD_PRODUCT_CATEGORY_SAVE_TAPPED)
                viewModel.addProductCategory(getCategoryName())
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentAddProductCategoryBinding.bind(view)

        setHasOptionsMenu(true)
        setupObservers(viewModel)

        binding.productCategoryName.setOnTextChangedListener {
            if (binding.productCategoryName.hasFocus()) {
                viewModel.onCategoryNameChanged(it.toString())
            }
        }

        with(binding.productCategoryParent) {
            viewModel.getSelectedParentCategoryName()?.let { post { setHtmlText(it) } }
            setClickListener {
                val action = AddProductCategoryFragmentDirections
                    .actionAddProductCategoryFragmentToParentCategoryListFragment(
                        viewModel.getSelectedParentId()
                    )
                findNavController().navigateSafely(action)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onRequestAllowBackPress(): Boolean {
        return viewModel.onBackButtonClicked(getCategoryName(), binding.productCategoryParent.getText())
    }

    private fun setupObservers(viewModel: AddProductCategoryViewModel) {
        viewModel.addProductCategoryViewStateData.observe(viewLifecycleOwner) { old, new ->
            new.categoryNameErrorMessage?.takeIfNotEqualTo(old?.categoryNameErrorMessage) {
                displayCategoryNameError(it)
            }
            new.displayProgressDialog?.takeIfNotEqualTo(old?.displayProgressDialog) { showProgressDialog(it) }
        }

        viewModel.event.observe(viewLifecycleOwner, Observer { event ->
            when (event) {
                is ShowSnackbar -> uiMessageResolver.showSnack(event.message)
                is Exit -> requireActivity().onBackPressed()
                is ShowDialog -> event.showDialog()
                is ExitWithResult<*> -> navigateBackWithResult(ARG_ADDED_CATEGORY, event.data)
                else -> event.isHandled = false
            }
        })
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

    private fun getCategoryName() = binding.productCategoryName.getText()
}
