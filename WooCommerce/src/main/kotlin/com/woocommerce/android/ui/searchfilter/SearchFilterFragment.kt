package com.woocommerce.android.ui.searchfilter

import androidx.navigation.fragment.navArgs
import com.woocommerce.android.R
import com.woocommerce.android.ui.base.BaseFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SearchFilterFragment : BaseFragment(R.layout.fragment_search_filter) {
    companion object {
        val TAG: String = SearchFilterFragment::class.java.simpleName
    }

    private val navArgs: SearchFilterFragmentArgs by navArgs()

    private val selectedItem: String?
        get() = navArgs.selectedItem

    private val searchTitle: String
        get() = navArgs.title

    private val searchHint: String
        get() = navArgs.hint

    private val searchFilterItems: Array<SearchFilterItem>
        get() = navArgs.items

    private val requestKey: String
        get() = navArgs.requestKey
}
