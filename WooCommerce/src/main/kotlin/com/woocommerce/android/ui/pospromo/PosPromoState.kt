package com.woocommerce.android.ui.pospromo

import androidx.annotation.StringRes
import com.woocommerce.android.R

data class PosPromoState(
    val imageRes: Int = R.drawable.img_woo_pos_promo,
    val titleRes: Int = R.string.woo_pos_promo_title,
    val currentPage: Int = 0,
    val pages: List<CarouselPage> = listOf(
        CarouselPage(descriptionRes = R.string.woo_pos_promo_page1_description),
        CarouselPage(descriptionRes = R.string.woo_pos_promo_page2_description),
        CarouselPage(descriptionRes = R.string.woo_pos_promo_page3_description),
        CarouselPage(descriptionRes = R.string.woo_pos_promo_page4_description),
        CarouselPage(descriptionRes = R.string.woo_pos_promo_page5_description),
    ),
)

data class CarouselPage(@StringRes val descriptionRes: Int)
