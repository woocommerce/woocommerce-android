package com.woocommerce.android.ui.orders.cardreader

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.google.android.material.snackbar.BaseTransientBottomBar.LENGTH_SHORT
import com.google.android.material.snackbar.Snackbar
import com.woocommerce.android.R
import com.woocommerce.android.WooCommerce
import com.woocommerce.android.databinding.FragmentCardReaderPaymentBinding
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ViewModelFactory
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import dagger.android.support.AndroidSupportInjection
import javax.inject.Inject

class CardReaderPaymentDialog : DialogFragment(R.layout.fragment_card_reader_payment), HasAndroidInjector {
    @Inject lateinit var viewModelFactory: ViewModelFactory
    @Inject internal lateinit var androidInjector: DispatchingAndroidInjector<Any>

    val viewModel: CardReaderPaymentViewModel by viewModels { viewModelFactory }

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = FragmentCardReaderPaymentBinding.bind(view)

        initViews(binding)
        initObservers()
        initViewModel()
    }

    private fun initViewModel() {
        val manager = (requireActivity().application as WooCommerce).cardReaderManager
        // TODO card reader: remove !! when cardReaderManager is changed to a nonnullable type in WooCommerce
        viewModel.start(manager!!)
    }

    private fun initViews(binding: FragmentCardReaderPaymentBinding) {
        // TODO cardreader remove this
        binding.testingText.setText("Hardcoded: Card Reader Payment Fragment")
    }

    private fun initObservers() {
        viewModel.event.observe(viewLifecycleOwner, Observer { event ->
            // TODO cardreader Replace debug Snackbar with proper UI updates
            when (event) {
                is ShowSnackbar -> Snackbar.make(
                    requireView(),
                    String.format(getString(event.message), *event.args),
                    LENGTH_SHORT
                ).show()
            }
        })
    }

    override fun androidInjector(): AndroidInjector<Any> = androidInjector
}
