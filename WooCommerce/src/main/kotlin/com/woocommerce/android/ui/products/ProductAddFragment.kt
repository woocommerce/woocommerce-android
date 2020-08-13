package com.woocommerce.android.ui.products

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.woocommerce.android.R
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.main.MainActivity.Companion.BackPressListener
import com.woocommerce.android.viewmodel.ViewModelFactory
import javax.inject.Inject

class ProductAddFragment : BaseFragment(), BackPressListener {
    @Inject lateinit var viewModelFactory: ViewModelFactory
    private val viewModel: ProductAddViewModel by viewModels { viewModelFactory }

    private fun initializeViewModel() {
        // todo
    }

    private fun setupObservers() {
        // todo
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        setHasOptionsMenu(true)
        return inflater.inflate(R.layout.fragment_product_detail, container, false)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()

        inflater.inflate(R.menu.menu_product_detail_fragment, menu)

        menu.findItem(R.id.menu_product_settings).isVisible = true
        menu.findItem(R.id.menu_done).title = getString(R.string.product_add_tool_bar_menu_button_done)

        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onRequestAllowBackPress(): Boolean {
        // todo
        return true
    }

    override fun getFragmentTitle() = getString(R.string.product_add_tool_bar_title)
}

