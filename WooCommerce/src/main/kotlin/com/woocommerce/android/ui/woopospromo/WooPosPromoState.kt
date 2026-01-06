package com.woocommerce.android.ui.woopospromo

import androidx.annotation.StringRes
import com.woocommerce.android.R

data class WooPosPromoState(
    val imageRes: Int = R.drawable.img_woo_pos_promo,
    val pages: List<CarouselPage> = listOf(
        CarouselPage(
            titleRes = R.string.woo_pos_promo_page1_title,
            descriptionRes = R.string.woo_pos_promo_page1_description,
        ),
        CarouselPage(
            titleRes = R.string.woo_pos_promo_page2_title,
            descriptionRes = R.string.woo_pos_promo_page2_description,
        ),
        CarouselPage(
            titleRes = R.string.woo_pos_promo_page3_title,
            descriptionRes = R.string.woo_pos_promo_page3_description,
        ),
        CarouselPage(
            titleRes = R.string.woo_pos_promo_page4_title,
            descriptionRes = R.string.woo_pos_promo_page4_description,
        ),
        CarouselPage(
            titleRes = R.string.woo_pos_promo_page5_title,
            descriptionRes = R.string.woo_pos_promo_page5_description,
        ),
    ),
)

data class CarouselPage(
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
)