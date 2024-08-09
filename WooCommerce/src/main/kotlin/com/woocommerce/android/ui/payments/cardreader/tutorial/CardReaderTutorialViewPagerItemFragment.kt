package com.woocommerce.android.ui.payments.cardreader.tutorial

import android.os.Bundle
import android.view.View
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentCardReaderTutorialViewpagerItemBinding
import com.woocommerce.android.extensions.hide
import com.woocommerce.android.util.UiHelpers
import org.wordpress.android.util.DisplayUtils

/**
 * Displays a single image and text label in the card reader tutorial view pager
 */
class CardReaderTutorialViewPagerItemFragment :
    Fragment(R.layout.fragment_card_reader_tutorial_viewpager_item) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        arguments?.let { args ->
            val binding = FragmentCardReaderTutorialViewpagerItemBinding.bind(view)
            binding.labelTextView.setText(args.getInt(ARG_LABEL_ID))
            binding.detailTextView.setText(args.getInt(ARG_DETAIL_ID))

            UiHelpers.setImageOrHideInLandscapeOnCompactScreenHeightSizeClass(
                binding.imageView,
                args.getInt(ARG_DRAWABLE_ID)
            )
        }
    }

    companion object {
        private const val ARG_DRAWABLE_ID = "drawable_id"
        private const val ARG_LABEL_ID = "label_id"
        private const val ARG_DETAIL_ID = "detail_id"

        fun newInstance(
            @DrawableRes drawableId: Int,
            @StringRes labelId: Int,
            @StringRes detailId: Int
        ): CardReaderTutorialViewPagerItemFragment {
            CardReaderTutorialViewPagerItemFragment().also { fragment ->
                fragment.arguments = Bundle().also { bundle ->
                    bundle.putInt(ARG_DRAWABLE_ID, drawableId)
                    bundle.putInt(ARG_LABEL_ID, labelId)
                    bundle.putInt(ARG_DETAIL_ID, detailId)
                }
                return fragment
            }
        }
    }
}
