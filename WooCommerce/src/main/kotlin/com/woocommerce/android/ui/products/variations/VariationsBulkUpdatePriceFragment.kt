package com.woocommerce.android.ui.products.variations

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.fragment.app.viewModels
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentVariationsBulkUpdatePriceBinding
import com.woocommerce.android.ui.base.BaseFragment

class VariationsBulkUpdatePriceFragment : BaseFragment(R.layout.fragment_variations_bulk_update_price) {
    private val viewModel: VariationsBulkUpdatePriceViewModel by viewModels()

    private var _binding: FragmentVariationsBulkUpdatePriceBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setHasOptionsMenu(true)
        _binding = FragmentVariationsBulkUpdatePriceBinding.bind(view)
        binding.price.setOnTextChangedListener {
            val price = it.toString().toBigDecimalOrNull()
            viewModel.onPriceEntered(price)
        }
        binding.price.showKeyboard()
    }

    override fun getFragmentTitle(): String {
        return getString(R.string.variations_bulk_update_regular_price)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
        inflater.inflate(R.menu.menu_variations_bulk_update, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.done -> {
                viewModel.onDoneClicked()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
