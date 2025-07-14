package com.woocommerce.android.ui.products.components

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentComponentListBinding
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.model.Component
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.util.setupTabletSecondPaneToolbar
import com.woocommerce.android.widgets.AlignedDividerDecoration
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ComponentListFragment :
    BaseFragment(R.layout.fragment_component_list),
    ComponentsListAdapter.OnComponentClickListener {
    val viewModel: ComponentListViewModel by viewModels()

    override val activityAppBarStatus: AppBarStatus
        get() = AppBarStatus.Hidden

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentComponentListBinding.bind(view)
        val componentsListAdapter = ComponentsListAdapter(this)

        binding.productsRecycler.run {
            layoutManager = LinearLayoutManager(requireActivity())
            adapter = componentsListAdapter
            isMotionEventSplittingEnabled = false
            if (itemDecorationCount == 0) {
                addItemDecoration(
                    AlignedDividerDecoration(
                        context,
                        DividerItemDecoration.VERTICAL,
                        R.id.componentInformation
                    )
                )
            }
        }

        viewModel.componentList.observe(viewLifecycleOwner) {
            componentsListAdapter.submitList(it)
        }
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is ViewComponentDetails -> {
                    ComponentListFragmentDirections.actionCompositeProductFragmentToComponentDetailsFragment(
                        event.component
                    ).let { findNavController().navigateSafely(it) }
                }
                else -> event.isHandled = false
            }
        }

        setupTabletSecondPaneToolbar(
            title = getString(R.string.product_components),
            onMenuItemSelected = { _ -> false },
            onCreateMenu = { toolbar ->
                toolbar.setNavigationOnClickListener {
                    findNavController().navigateUp()
                }
            }
        )
    }

    override fun onComponentClickListener(component: Component) {
        viewModel.onComponentSelected(component)
    }
}
