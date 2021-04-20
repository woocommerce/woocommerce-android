package com.woocommerce.android.ui.orders.cardreader

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.Window
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentCardReaderPaymentBinding
import com.woocommerce.android.databinding.FragmentSettingsCardReaderConnectBinding
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.prefs.cardreader.connect.CardReaderConnectViewModel
import com.woocommerce.android.ui.prefs.cardreader.connect.CardReaderConnectViewModel.NavigationTarget.CardReaderScanScreen
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

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        return dialog
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = FragmentCardReaderPaymentBinding.bind(view)

        initViews(binding)
        initObservers()
    }

    private fun initViews(binding: FragmentCardReaderPaymentBinding) {
        // TODO cardreader remove this
        binding.testingText.setText("Hardcoded: Card Reader Payment Fragment")
    }

    private fun initObservers() {
        // TODO cardreader remove this
        viewModel.foo()
    }

    override fun androidInjector(): AndroidInjector<Any> = androidInjector
}
