package com.woocommerce.android.ui.orders.shippinglabels.creation

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentShippingCustomsBinding
import com.woocommerce.android.extensions.navigateBackWithNotice
import com.woocommerce.android.extensions.navigateBackWithResult
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.main.MainActivity.Companion.BackPressListener
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ShippingCustomsFragment : BaseFragment(R.layout.fragment_shipping_customs), BackPressListener {
    companion object {
        const val EDIT_CUSTOMS_CLOSED = "edit_customs_closed"
        const val EDIT_CUSTOMS_RESULT = "edit_customs_result"
    }

    @Inject lateinit var uiMessageResolver: UIMessageResolver
    private val viewModel: ShippingCustomsViewModel by viewModels()
    private lateinit var doneMenuItem: MenuItem

    private val customsAdapter: ShippingCustomsAdapter by lazy {
        ShippingCustomsAdapter(
            weightUnit = viewModel.weightUnit,
            currencyUnit = viewModel.currencyUnit,
            countries = viewModel.countries.toTypedArray(),
            listener = viewModel
        )
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)

        inflater.inflate(R.menu.menu_done, menu)
        doneMenuItem = menu.findItem(R.id.menu_done)
        doneMenuItem.isVisible = viewModel.viewStateData.liveData.value?.canSubmitForm ?: false
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

    override fun getFragmentTitle(): String = getString(R.string.shipping_label_create_customs)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)
        val binding = FragmentShippingCustomsBinding.bind(view)
        binding.packagesList.apply {
            this.adapter = customsAdapter
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
            itemAnimator = DefaultItemAnimator().apply {
                // Disable change animations to avoid duplicating viewholders
                supportsChangeAnimations = false
            }
        }

        setupObservers(binding)
    }

    private fun setupObservers(binding: FragmentShippingCustomsBinding) {
        viewModel.viewStateData.observe(
            viewLifecycleOwner,
            { old, new ->
                new.customsPackages.takeIfNotEqualTo(old?.customsPackages) { customsPackages ->
                    customsAdapter.customsPackages = customsPackages
                }
                new.canSubmitForm.takeIfNotEqualTo(old?.canSubmitForm) { canSubmitForm ->
                    if (::doneMenuItem.isInitialized) {
                        doneMenuItem.isVisible = canSubmitForm
                    }
                }
                new.isProgressViewShown.takeIfNotEqualTo(old?.isProgressViewShown) { show ->
                    binding.progressView.isVisible = show
                    binding.packagesList.isVisible = !show
                }
            }
        )
        viewModel.event.observe(
            viewLifecycleOwner,
            { event ->
                when (event) {
                    is ExitWithResult<*> -> navigateBackWithResult(EDIT_CUSTOMS_RESULT, event.data)
                    is Exit -> navigateBackWithNotice(EDIT_CUSTOMS_CLOSED)
                    is ShowSnackbar -> uiMessageResolver.showSnack(event.message)
                    else -> event.isHandled = false
                }
            }
        )
    }

    override fun onRequestAllowBackPress(): Boolean {
        viewModel.onBackButtonClicked()
        return false
    }
}
