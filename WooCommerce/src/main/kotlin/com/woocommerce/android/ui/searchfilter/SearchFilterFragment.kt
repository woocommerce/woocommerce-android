package com.woocommerce.android.ui.searchfilter

import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.SearchView.OnQueryTextListener
import androidx.navigation.fragment.navArgs
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentSearchFilterBinding
import com.woocommerce.android.ui.base.BaseFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SearchFilterFragment : BaseFragment(R.layout.fragment_search_filter), OnQueryTextListener {
    companion object {
        val TAG: String = SearchFilterFragment::class.java.simpleName
    }

    private val navArgs: SearchFilterFragmentArgs by navArgs()

    private val searchTitle: String
        get() = navArgs.title

    private val searchHint: String
        get() = navArgs.hint

    private val searchFilterItems: Array<SearchFilterItem>
        get() = navArgs.items

    private val requestKey: String
        get() = navArgs.requestKey

    private var _binding: FragmentSearchFilterBinding? = null
    private val binding: FragmentSearchFilterBinding
        get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentSearchFilterBinding.bind(view)
        setupTitle()
        setupSearch()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupTitle() {
        activity?.title = searchTitle
    }

    private fun setupSearch() {
        binding.searchView.apply {
            queryHint = searchHint
            setOnQueryTextListener(this@SearchFilterFragment)
        }
    }

    override fun onQueryTextSubmit(query: String): Boolean {
        return false
    }

    override fun onQueryTextChange(newText: String): Boolean {
        return false
    }
}
