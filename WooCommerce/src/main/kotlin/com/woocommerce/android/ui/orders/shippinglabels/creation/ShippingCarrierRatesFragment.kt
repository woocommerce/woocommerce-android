package com.woocommerce.android.ui.orders.shippinglabels.creation

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.FragmentShippingCarrierRatesBinding
import com.woocommerce.android.extensions.navigateBackWithNotice
import com.woocommerce.android.extensions.navigateBackWithResult
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.main.MainActivity.Companion.BackPressListener
import com.woocommerce.android.util.DateUtils
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.widgets.SkeletonView
import com.woocommerce.android.widgets.WCEmptyView.EmptyViewType.SHIPPING_LABEL_CARRIER_RATES
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.util.ActivityUtils
import javax.inject.Inject

@AndroidEntryPoint
class ShippingCarrierRatesFragment :
    BaseFragment(R.layout.fragment_shipping_carrier_rates),
    BackPressListener,
    MenuProvider {
    companion object {
        const val SHIPPING_CARRIERS_CLOSED = "shipping_carriers_closed"
        const val SHIPPING_CARRIERS_RESULT = "shipping_carriers_result"
    }

    @Inject lateinit var uiMessageResolver: UIMessageResolver
    @Inject lateinit var resourceProvider: ResourceProvider
    @Inject lateinit var dateUtils: DateUtils

    private var doneMenuItem: MenuItem? = null

    private var _binding: FragmentShippingCarrierRatesBinding? = null
    private val binding get() = _binding!!

    private val skeletonView: SkeletonView = SkeletonView()

    val viewModel: ShippingCarrierRatesViewModel by viewModels()

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    override fun onStop() {
        super.onStop()
        activity?.let {
            ActivityUtils.hideKeyboard(it)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().addMenuProvider(this, viewLifecycleOwner)

        _binding = FragmentShippingCarrierRatesBinding.bind(view)

        initializeViewModel()
        initializeViews()
    }

    private fun initializeViewModel() {
        subscribeObservers()
    }

    private fun initializeViews() {
        binding.carrierRates.apply {
            adapter = binding.carrierRates.adapter ?: ShippingCarrierRatesAdapter(
                onRateSelected = viewModel::onShippingRateSelected,
                dateUtils = dateUtils
            )
            layoutManager = LinearLayoutManager(context)
            itemAnimator = DefaultItemAnimator().apply {
                supportsChangeAnimations = false
            }
        }
    }

    override fun getFragmentTitle() = getString(R.string.shipping_label_shipping_carriers_title)

    override fun onCreateMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_done, menu)
        doneMenuItem = menu.findItem(R.id.menu_done)
        doneMenuItem?.isVisible = viewModel.viewStateData.liveData.value?.isDoneButtonVisible ?: false
    }

    override fun onMenuItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_done -> {
                ActivityUtils.hideKeyboard(activity)
                viewModel.onDoneButtonClicked()
                true
            }
            else -> false
        }
    }

    @SuppressLint("SetTextI18n")
    private fun subscribeObservers() {
        viewModel.shippingRates.observe(viewLifecycleOwner) { rates ->
            (binding.carrierRates.adapter as? ShippingCarrierRatesAdapter)?.items = rates
        }

        viewModel.viewStateData.observe(viewLifecycleOwner) { old, new ->
            new.bannerMessage.takeIfNotEqualTo(old?.bannerMessage) { message ->
                binding.infoBanner.isVisible = !message.isNullOrEmpty()
                binding.infoBannerMessage.text = message
            }
            new.isSkeletonVisible.takeIfNotEqualTo(old?.isSkeletonVisible) { isVisible ->
                showSkeleton(isVisible)
            }
            new.isEmptyViewVisible.takeIfNotEqualTo(old?.isEmptyViewVisible) { isVisible ->
                showEmptyView(isVisible)
            }
            new.isDoneButtonVisible.takeIfNotEqualTo(old?.isDoneButtonVisible) { isVisible ->
                doneMenuItem?.isVisible = isVisible
            }
        }

        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is ShowSnackbar -> uiMessageResolver.showSnack(event.message)
                is ExitWithResult<*> -> navigateBackWithResult(SHIPPING_CARRIERS_RESULT, event.data)
                is Exit -> navigateBackWithNotice(SHIPPING_CARRIERS_CLOSED)
                else -> event.isHandled = false
            }
        }
    }

    private fun showEmptyView(isVisible: Boolean) {
        if (isVisible) {
            binding.emptyView.show(SHIPPING_LABEL_CARRIER_RATES)
        } else {
            binding.emptyView.hide()
        }
    }

    fun showSkeleton(show: Boolean) {
        if (show) {
            skeletonView.show(binding.carrierRates, R.layout.skeleton_shipping_label_carrier_list, delayed = false)
        } else {
            skeletonView.hide()
        }
    }

    // Let the ViewModel know the user is attempting to close the screen
    override fun onRequestAllowBackPress(): Boolean {
        return (viewModel.event.value == Exit).also { if (it.not()) viewModel.onExit() }
    }
}
