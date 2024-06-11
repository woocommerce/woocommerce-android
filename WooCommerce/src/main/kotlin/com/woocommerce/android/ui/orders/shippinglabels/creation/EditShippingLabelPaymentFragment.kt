package com.woocommerce.android.ui.orders.shippinglabels.creation

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.woocommerce.android.AppUrls
import com.woocommerce.android.AppUrls.FETCH_PAYMENT_METHOD_URL_PATH
import com.woocommerce.android.NavGraphOrdersDirections
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentEditShippingLabelPaymentBinding
import com.woocommerce.android.extensions.handleNotice
import com.woocommerce.android.extensions.navigateBackWithNotice
import com.woocommerce.android.extensions.navigateBackWithResult
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.common.wpcomwebview.WPComWebViewFragment
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.ui.main.MainActivity.Companion.BackPressListener
import com.woocommerce.android.ui.orders.shippinglabels.creation.EditShippingLabelPaymentViewModel.AddPaymentMethod
import com.woocommerce.android.ui.orders.shippinglabels.creation.EditShippingLabelPaymentViewModel.DataLoadState.Error
import com.woocommerce.android.ui.orders.shippinglabels.creation.EditShippingLabelPaymentViewModel.DataLoadState.Loading
import com.woocommerce.android.ui.orders.shippinglabels.creation.EditShippingLabelPaymentViewModel.DataLoadState.Success
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.widgets.CustomProgressDialog
import com.woocommerce.android.widgets.SkeletonView
import com.woocommerce.android.widgets.WCEmptyView
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class EditShippingLabelPaymentFragment :
    BaseFragment(
        R.layout.fragment_edit_shipping_label_payment
    ),
    BackPressListener {
    companion object {
        const val EDIT_PAYMENTS_CLOSED = "edit_payments_closed"
        const val EDIT_PAYMENTS_RESULT = "edit_payments_result"
    }

    @Inject lateinit var uiMessageResolver: UIMessageResolver

    private val skeletonView = SkeletonView()

    private val viewModel: EditShippingLabelPaymentViewModel by viewModels()

    private val paymentMethodsAdapter by lazy { ShippingLabelPaymentMethodsAdapter(viewModel::onPaymentMethodSelected) }

    private lateinit var doneMenuItem: MenuItem
    private var progressDialog: CustomProgressDialog? = null

    override val activityAppBarStatus: AppBarStatus
        get() = AppBarStatus.Hidden

    override fun getFragmentTitle() = getString(R.string.orderdetail_shipping_label_item_payment)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = FragmentEditShippingLabelPaymentBinding.bind(view)
        setupToolbar(binding)
        binding.paymentMethodsList.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
            adapter = paymentMethodsAdapter
        }
        binding.emailReceiptsCheckbox.setOnCheckedChangeListener { _, isChecked ->
            viewModel.onEmailReceiptsCheckboxChanged(isChecked)
        }
        binding.addPaymentMethodButton.setOnClickListener {
            viewModel.onAddPaymentMethodClicked()
        }
        binding.addFirstPaymentMethodButton.setOnClickListener {
            viewModel.onAddPaymentMethodClicked()
        }
        setupObservers(binding)
        setupResultHandlers()
    }

    private fun setupToolbar(binding: FragmentEditShippingLabelPaymentBinding) {
        binding.toolbar.title = getString(R.string.orderdetail_shipping_label_item_payment)
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
        doneMenuItem.isVisible = viewModel.viewStateData.liveData.value?.canSave ?: false
    }

    private fun onMenuItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_done -> {
                viewModel.onDoneButtonClicked()
                true
            }
            else -> false
        }
    }

    override fun onRequestAllowBackPress(): Boolean {
        viewModel.onBackButtonClicked()
        return false
    }

    private fun setupObservers(binding: FragmentEditShippingLabelPaymentBinding) {
        viewModel.viewStateData.observe(viewLifecycleOwner) { old, new ->
            new.dataLoadState?.takeIfNotEqualTo(old?.dataLoadState) { uiState ->
                when (uiState) {
                    Loading -> {
                        showSkeleton(binding)
                        binding.errorView.hide()
                    }
                    Error -> {
                        skeletonView.hide()
                        binding.contentLayout.isVisible = false
                        binding.errorView.show(
                            type = WCEmptyView.EmptyViewType.NETWORK_ERROR,
                            onButtonClick = { viewModel.refreshData() }
                        )
                    }
                    Success -> {
                        skeletonView.hide()
                        binding.errorView.hide()
                    }
                }
            }
            new.canManagePayments.takeIfNotEqualTo(old?.canManagePayments) { canManagePayments ->
                binding.editWarningBanner.isVisible = !canManagePayments
                paymentMethodsAdapter.isEditingEnabled = canManagePayments
                binding.paymentMethodsSectionTitle.isEnabled = canManagePayments
            }
            new.paymentMethods.takeIfNotEqualTo(old?.paymentMethods) {
                paymentMethodsAdapter.submitList(it)
                it.isEmpty().let { isListEmpty ->
                    binding.paymentMethodsSectionTitle.isVisible = !isListEmpty
                    binding.paymentMethodsList.isVisible = !isListEmpty
                }
            }
            new.showAddFirstPaymentButton.takeIfNotEqualTo(old?.showAddFirstPaymentButton) {
                binding.addFirstPaymentMethodButton.isVisible = it
            }
            new.showAddPaymentButton.takeIfNotEqualTo(old?.showAddPaymentButton) {
                binding.addPaymentMethodButton.isVisible = it
            }
            new.canEditSettings.takeIfNotEqualTo(old?.canEditSettings) { canEditSettings ->
                binding.emailReceiptsCheckbox.isEnabled = canEditSettings
            }
            new.emailReceipts.takeIfNotEqualTo(binding.emailReceiptsCheckbox.isChecked) {
                binding.emailReceiptsCheckbox.isChecked = it
            }
            new.storeOwnerDetails?.takeIfNotEqualTo(old?.storeOwnerDetails) { details ->
                binding.editWarningBanner.message = getString(
                    R.string.shipping_label_payments_cant_edit_warning,
                    details.name,
                    details.wpcomUserName
                )
                binding.paymentsInfo.text = getString(
                    R.string.shipping_label_payments_account_info,
                    details.wpcomUserName,
                    details.wpcomEmail
                )
                binding.emailReceiptsCheckbox.text = getString(
                    R.string.shipping_label_payments_email_receipts_checkbox,
                    details.name.ifEmpty { details.userName },
                    details.userName,
                    details.wpcomEmail
                )
            }
            new.canSave.takeIfNotEqualTo(old?.canSave) {
                if (::doneMenuItem.isInitialized) {
                    doneMenuItem.isVisible = it
                }
            }
            new.showSavingProgressDialog.takeIfNotEqualTo(old?.showSavingProgressDialog) { show ->
                if (show) {
                    showSavingProgressDialog()
                } else {
                    hideProgressDialog()
                }
            }
        }

        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is AddPaymentMethod -> {
                    findNavController().navigateSafely(
                        NavGraphOrdersDirections.actionGlobalWPComWebViewFragment(
                            urlToLoad = AppUrls.WPCOM_ADD_PAYMENT_METHOD,
                            urlsToTriggerExit = arrayOf(FETCH_PAYMENT_METHOD_URL_PATH),
                            title = getFragmentTitle()
                        )
                    )
                }
                is ShowSnackbar -> uiMessageResolver.showSnack(event.message)
                is ExitWithResult<*> -> navigateBackWithResult(EDIT_PAYMENTS_RESULT, event.data)
                is Exit -> navigateBackWithNotice(EDIT_PAYMENTS_CLOSED)
                else -> event.isHandled = false
            }
        }
    }

    private fun setupResultHandlers() {
        handleNotice(WPComWebViewFragment.WEBVIEW_RESULT) {
            viewModel.onPaymentMethodAdded()
        }
    }

    fun showSkeleton(binding: FragmentEditShippingLabelPaymentBinding) {
        skeletonView.show(binding.contentLayout, R.layout.skeleton_shipping_label_payment_list, delayed = false)
    }

    private fun showSavingProgressDialog() {
        hideProgressDialog()
        progressDialog = CustomProgressDialog.show(
            title = getString(R.string.shipping_label_payments_saving_dialog_title),
            message = getString(R.string.shipping_label_payments_saving_dialog_message)
        ).also { it.show(parentFragmentManager, CustomProgressDialog.TAG) }
        progressDialog?.isCancelable = false
    }

    private fun hideProgressDialog() {
        progressDialog?.dismiss()
        progressDialog = null
    }
}
