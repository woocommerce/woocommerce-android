package com.woocommerce.android.ui.orders.shippinglabels.creation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.woocommerce.android.R
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.viewmodel.ViewModelFactory
import javax.inject.Inject

class CreateShippingLabelFragment  : BaseFragment() {
    @Inject lateinit var uiMessageResolver: UIMessageResolver

    @Inject lateinit var viewModelFactory: ViewModelFactory
    val viewModel: CreateShippingLabelViewModel by viewModels { viewModelFactory }

    override fun getFragmentTitle() = getString(R.string.shipping_label_create_title)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewModel.initCreationFlow()
        return inflater.inflate(R.layout.fragment_create_shipping_label, container, false)
    }
}
