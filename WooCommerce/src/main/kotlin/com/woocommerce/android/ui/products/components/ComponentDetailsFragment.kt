package com.woocommerce.android.ui.products.components

import android.os.Bundle
import android.view.View
import androidx.core.text.HtmlCompat
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentComponentDetailsBinding
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.model.QueryType
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.ui.products.ComponentOptions
import com.woocommerce.android.util.setupTabletSecondPaneToolbar
import com.woocommerce.android.widgets.AlignedDividerDecoration
import com.woocommerce.android.widgets.SkeletonView
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.util.PhotonUtils

@AndroidEntryPoint
class ComponentDetailsFragment : BaseFragment(R.layout.fragment_component_details) {
    val viewModel: ComponentDetailsViewModel by viewModels()

    override val activityAppBarStatus: AppBarStatus
        get() = AppBarStatus.Hidden

    private val skeletonView = SkeletonView()

    private val componentsOptionsListAdapter: ComponentOptionsListAdapter by lazy { ComponentOptionsListAdapter() }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentComponentDetailsBinding.bind(view)

        binding.componentDetailsRefreshLayout.apply {
            // Set the scrolling view in the custom refresh SwipeRefreshLayout
            scrollUpChild = binding.componentDetails
            setOnRefreshListener { viewModel.pullToRefresh() }
        }

        binding.componentOptionsRecycler.run {
            layoutManager = LinearLayoutManager(requireActivity())
            adapter = componentsOptionsListAdapter
            isNestedScrollingEnabled = false
            isMotionEventSplittingEnabled = false
            if (itemDecorationCount == 0) {
                addItemDecoration(
                    AlignedDividerDecoration(
                        context,
                        DividerItemDecoration.VERTICAL,
                        R.id.componentOptionTitle
                    )
                )
            }
        }

        viewModel.componentDetails.observe(viewLifecycleOwner) { details ->
            binding.componentTitle.text = details.title
            binding.componentDescription.text = HtmlCompat.fromHtml(
                details.description,
                HtmlCompat.FROM_HTML_MODE_COMPACT
            )
            showComponentImage(details.imageUrl, binding)
        }

        viewModel.componentOptions.observe(viewLifecycleOwner) { componentOptions ->
            showComponentOptionsInfo(componentOptions, binding)
        }

        viewModel.componentDetailsViewStateData.observe(viewLifecycleOwner) { old, new ->
            new.isSkeletonShown.takeIfNotEqualTo(old?.isSkeletonShown) { showSkeleton(it, binding) }
            new.isRefreshing.takeIfNotEqualTo(old?.isRefreshing) {
                binding.componentDetailsRefreshLayout.isRefreshing = it
            }
        }

        setupTabletSecondPaneToolbar(
            title = getString(R.string.product_component_settings),
            onMenuItemSelected = { _ -> false },
            onCreateMenu = { toolbar ->
                toolbar.setNavigationOnClickListener {
                    findNavController().navigateUp()
                }
            }
        )
    }

    private fun showComponentOptionsInfo(componentOptions: ComponentOptions, binding: FragmentComponentDetailsBinding) {
        val noListInfo = componentOptions.options.isEmpty()
        val noDefaultOptionInfo = componentOptions.default == null
        val noComponentOptionInfo = noListInfo && noDefaultOptionInfo

        binding.componentOptionsSection.isVisible = noComponentOptionInfo.not()

        if (noComponentOptionInfo) return

        binding.componentOptionsType.text = when (componentOptions.type) {
            QueryType.PRODUCT -> getString(R.string.component_type_products)
            QueryType.CATEGORY -> getString(R.string.component_type_categories)
        }
        binding.componentOptionsType.isVisible = noListInfo.not()

        componentOptions.default?.let {
            binding.componentOptionsDefault.text = it
        } ?: run {
            binding.componentOptionsDefaultLabel.isVisible = false
            binding.componentOptionsDefault.isVisible = false
        }

        componentsOptionsListAdapter.submitList(componentOptions.options)
        binding.componentOptionsRecyclerDivider.isVisible = noListInfo.not()
        binding.componentOptionsRecycler.isVisible = noListInfo.not()
    }

    private fun showComponentImage(imageUrl: String?, binding: FragmentComponentDetailsBinding) {
        when {
            imageUrl.isNullOrEmpty() -> {
                binding.componentImage.isVisible = false
            }
            else -> {
                val imageSize = resources.getDimensionPixelSize(R.dimen.image_major_120)
                val photonUrl = PhotonUtils.getPhotonImageUrl(imageUrl, imageSize, imageSize)
                Glide.with(requireContext()).load(photonUrl)
                    .transform(CenterCrop()).placeholder(R.drawable.ic_product)
                    .into(binding.componentImage)
            }
        }
    }

    private fun showSkeleton(show: Boolean, binding: FragmentComponentDetailsBinding) {
        when (show) {
            true -> {
                skeletonView.show(binding.componentOptionsSection, R.layout.skeleton_component_options, delayed = true)
            }
            false -> skeletonView.hide()
        }
    }
}
