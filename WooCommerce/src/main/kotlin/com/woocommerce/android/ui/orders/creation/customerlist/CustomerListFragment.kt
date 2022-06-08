package com.woocommerce.android.ui.orders.creation.customerlist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.viewModels
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentCustomerListBinding
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CustomerListFragment : BaseFragment(R.layout.fragment_customer_list) {
    private val viewModel by viewModels<CustomerListViewModel>()

    private var searchMenuItem: MenuItem? = null
    private var searchView: SearchView? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        setHasOptionsMenu(true)
        val binding = FragmentCustomerListBinding.inflate(inflater, container, false)

        binding.customerComposeView.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                WooThemeWithBackground {
                    CustomerListScreen(
                        onCustomerClick = viewModel::onCustomerClick
                    )
                }
            }
        }

        return binding.root
    }

    override fun getFragmentTitle() = getString(R.string.order_creation_customer_search_title)

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_search, menu)

        searchMenuItem = menu.findItem(R.id.menu_search)
        searchView = searchMenuItem?.actionView as SearchView?
        searchView?.queryHint = getString(R.string.order_creation_customer_search_hint)
        searchView?.setIconifiedByDefault(false)

        super.onCreateOptionsMenu(menu, inflater)
    }
}
