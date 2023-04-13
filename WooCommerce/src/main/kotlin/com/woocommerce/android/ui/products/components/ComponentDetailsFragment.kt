package com.woocommerce.android.ui.products.components

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentComponentDetailsBinding
import com.woocommerce.android.di.GlideApp
import com.woocommerce.android.ui.base.BaseFragment
import org.wordpress.android.util.PhotonUtils

class ComponentDetailsFragment : BaseFragment(R.layout.fragment_component_details) {
    private var _binding: FragmentComponentDetailsBinding? = null
    private val binding get() = _binding!!

    override fun getFragmentTitle() = resources.getString(R.string.product_component_settings)

    private val navArgs: ComponentDetailsFragmentArgs by navArgs()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentComponentDetailsBinding.bind(view)

        val component = navArgs.component

        binding.componentTitle.text = component.title
        binding.componentDescription.text = component.description
        showComponentImage(component.thumbnailUrl)
    }

    private fun showComponentImage(imageUrl: String?) {
        val imageSize = resources.getDimensionPixelSize(R.dimen.image_major_120)
        val padding: Int
        when {
            imageUrl.isNullOrEmpty() -> {
                padding = resources.getDimensionPixelSize(R.dimen.major_200)
                binding.componentImage.setImageResource(R.drawable.ic_product)
            }
            else -> {
                padding = 0
                val photonUrl = PhotonUtils.getPhotonImageUrl(imageUrl, imageSize, imageSize)
                GlideApp.with(requireContext()).load(photonUrl)
                    .transform(CenterCrop()).placeholder(R.drawable.ic_product)
                    .into(binding.componentImage)
            }
        }
        binding.componentImage.setPadding(padding, padding, padding, padding)
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}
