package com.woocommerce.android.ui.prefs.cardreader.tutorial

import android.os.Bundle
import android.view.View
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentCardReaderTutorialViewpagerItemBinding
import com.woocommerce.android.extensions.hide
import org.wordpress.android.util.DisplayUtils

/**
 * Displays a single image and text label in the card reader tutorial view pager
 */
class CardReaderTutorialViewPagerItemFragment : Fragment(R.layout.fragment_card_reader_tutorial_viewpager_item) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        arguments?.let { args ->
            val binding = FragmentCardReaderTutorialViewpagerItemBinding.bind(view)
            binding.labelTextView.setText(args.getInt(ARG_LABEL_ID))
            binding.detailTextView.setText(args.getInt(ARG_DETAIL_ID))

            val isLandscape = DisplayUtils.isLandscape(context)
            val isTablet = DisplayUtils.isTablet(context) || DisplayUtils.isXLargeTablet(context)

            // hide images in landscape unless this device is a tablet
            if (isLandscape && !isTablet) {
                binding.imageView.hide()
            } else {
                binding.imageView.setImageResource(args.getInt(ARG_DRAWABLE_ID))
            }

            // adjust the view sizes based on orientation
            val ratio = if (isLandscape) {
                (DisplayUtils.getDisplayPixelWidth() * RATIO_LANDSCAPE).toInt()
            } else {
                (DisplayUtils.getDisplayPixelWidth() * RATIO_PORTRAIT).toInt()
            }
            binding.labelTextView.layoutParams.width = ratio
            binding.detailTextView.layoutParams.width = ratio
            binding.imageView.layoutParams.width = ratio
        }
    }

    companion object {
        private const val ARG_DRAWABLE_ID = "drawable_id"
        private const val ARG_LABEL_ID = "label_id"
        private const val ARG_DETAIL_ID = "detail_id"

        private const val RATIO_PORTRAIT = 0.6f
        private const val RATIO_LANDSCAPE = 0.3f

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
