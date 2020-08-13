package com.woocommerce.android.ui.products

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import com.woocommerce.android.R
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.main.MainActivity.Companion.BackPressListener
import com.woocommerce.android.viewmodel.ViewModelFactory
import kotlinx.android.synthetic.main.fragment_product_detail.*
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

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        showDefaultProductDetails()
    }

    private fun showDefaultProductDetails() {
        imageGallery.isVisible = false
        addImageContainer.isVisible = true
        productDetail_addMoreContainer.isVisible = true
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()

        inflater.inflate(R.menu.menu_product_detail_fragment, menu)

        menu.findItem(R.id.menu_product_settings).isVisible = true
        menu.findItem(R.id.menu_done).apply {
            title = getString(R.string.product_add_tool_bar_menu_button_done)
            isVisible = true
        }

        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_done -> {
                Toast.makeText(this@ProductAddFragment.context, "Publish clicked!", Toast.LENGTH_SHORT).show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onRequestAllowBackPress(): Boolean {
        // todo
        return true
    }

    override fun getFragmentTitle() = getString(R.string.product_add_tool_bar_title)
}

