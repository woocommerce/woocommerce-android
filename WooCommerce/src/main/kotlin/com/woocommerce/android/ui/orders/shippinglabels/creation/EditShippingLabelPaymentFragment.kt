package com.woocommerce.android.ui.orders.shippinglabels.creation

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.observe
import androidx.recyclerview.widget.LinearLayoutManager
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentEditShippingLabelPaymentBinding
import com.woocommerce.android.extensions.navigateBackWithNotice
import com.woocommerce.android.extensions.navigateBackWithResult
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.main.MainActivity.Companion.BackPressListener
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ViewModelFactory
import com.woocommerce.android.widgets.CustomProgressDialog
import javax.inject.Inject

class EditShippingLabelPaymentFragment : BaseFragment(R.layout.fragment_edit_shipping_label_payment), BackPressListener {
    companion object {
        const val EDIT_PAYMENTS_CLOSED = "edit_payments_closed"
        const val EDIT_PAYMENTS_RESULT = "edit_payments_result"
    }

    @Inject lateinit var uiMessageResolver: UIMessageResolver
    @Inject lateinit var viewModelFactory: ViewModelFactory

    private val viewModel: EditShippingLabelPaymentViewModel by viewModels { viewModelFactory }

    private val paymentMethodsAdapter by lazy { ShippingLabelPaymentMethodsAdapter(viewModel::onPaymentMethodSelected) }

    private lateinit var doneMenuItem: MenuItem
    private var progressDialog: CustomProgressDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_done, menu)
        doneMenuItem = menu.findItem(R.id.menu_done)
        doneMenuItem.isVisible = viewModel.viewStateData.liveData.value?.hasChanges ?: false
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentEditShippingLabelPaymentBinding.bind(view)
        binding.paymentMethodsList.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
            adapter = paymentMethodsAdapter
        }
        binding.emailReceiptsCheckbox.setOnCheckedChangeListener { _, isChecked ->
            viewModel.onEmailReceiptsCheckboxChanged(isChecked)
        }
        setupObservers(binding)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_done -> {
                viewModel.saveSettings()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onRequestAllowBackPress(): Boolean {
        viewModel.onBackButtonClicked()
        return false
    }

    private fun setupObservers(binding: FragmentEditShippingLabelPaymentBinding) {
        viewModel.viewStateData.observe(viewLifecycleOwner) { old, new ->
            new.isLoading.takeIfNotEqualTo(old?.isLoading) { isLoading ->
                binding.loadingProgress.isVisible = isLoading
                binding.contentLayout.isVisible = !isLoading
            }
            new.paymentMethods.takeIfNotEqualTo(old?.paymentMethods) {
                paymentMethodsAdapter.items = it
            }
            new.emailReceipts.takeIfNotEqualTo(binding.emailReceiptsCheckbox.isChecked) {
                binding.emailReceiptsCheckbox.isChecked = it
            }
            new.storeOwnerDetails?.takeIfNotEqualTo(old?.storeOwnerDetails) { details ->
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
            new.hasChanges.takeIfNotEqualTo(old?.hasChanges) {
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
            when(event) {
                is ShowSnackbar -> uiMessageResolver.showSnack(event.message)
                is ExitWithResult<*> -> navigateBackWithResult(EDIT_PAYMENTS_RESULT, event.data)
                is Exit -> navigateBackWithNotice(EDIT_PAYMENTS_CLOSED)
                else -> event.isHandled = false
            }
        }
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
