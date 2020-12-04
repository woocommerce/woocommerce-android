package com.woocommerce.android.ui.orders.shippinglabels.creation

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.FragmentEditShippingLabelAddressBinding
import com.woocommerce.android.extensions.navigateBackWithResult
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.model.Address
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ViewModelFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.wordpress.android.util.ActivityUtils
import javax.inject.Inject

@ExperimentalCoroutinesApi
class EditShippingLabelAddressFragment : BaseFragment() {
    @Inject lateinit var uiMessageResolver: UIMessageResolver
    @Inject lateinit var viewModelFactory: ViewModelFactory

    private var _binding: FragmentEditShippingLabelAddressBinding? = null
    private val binding get() = _binding!!

    val viewModel: EditShippingLabelAddressViewModel by viewModels { viewModelFactory }

    private var screenTitle = ""
        set(value) {
            field = value
            updateActivityTitle()
        }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        setHasOptionsMenu(true)

        _binding = FragmentEditShippingLabelAddressBinding.inflate(inflater, container, false)
        return binding.root
    }

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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        subscribeObservers()
        initializeViews()
    }

    override fun getFragmentTitle() = screenTitle

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)

        inflater.inflate(R.menu.menu_done, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_done -> {
                ActivityUtils.hideKeyboard(activity)
                viewModel.onDoneButtonClicked(gatherData())
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun subscribeObservers() {
        viewModel.viewStateData.observe(viewLifecycleOwner) { old, new ->
            new.address?.takeIfNotEqualTo(old?.address) {
                binding.company.setText(it.company)
                binding.name.setText("${it.firstName} ${it.lastName}")
                binding.phone.setText(it.phone)
                binding.address1.setText(it.address1)
                binding.address2.setText(it.address2)
                binding.zip.setText(it.postcode)
                binding.state.setText(it.state)
                binding.city.setText(it.city)
                binding.country.setText(it.country)
            }
            new.title?.takeIfNotEqualTo(old?.title) {
                screenTitle = getString(it)
            }
        }

        viewModel.event.observe(viewLifecycleOwner, Observer { event ->
            when (event) {
                is ShowSnackbar -> uiMessageResolver.showSnack(event.message)
                is ExitWithResult<*> -> navigateBackWithResult(
                    CreateShippingLabelFragment.KEY_EDIT_ADDRESS_DIALOG_RESULT,
                    event.data
                )
                else -> event.isHandled = false
            }
        })
    }

    private fun initializeViews() {
        binding.btnUsAddressAsIs.setOnClickListener {
            viewModel.onUseAddressAsIsButtonClicked(gatherData())
        }
    }

    private fun gatherData(): Address {
        return Address(
            company = binding.company.toString(),
            firstName = binding.name.toString(),
            lastName = "",
            phone = binding.phone.toString(),
            address1 = binding.address1.toString(),
            address2 = binding.address2.toString(),
            postcode = binding.zip.toString(),
            state = binding.state.toString(),
            city = binding.city.toString(),
            country = binding.country.toString(),
            email = ""
        )
    }
}
