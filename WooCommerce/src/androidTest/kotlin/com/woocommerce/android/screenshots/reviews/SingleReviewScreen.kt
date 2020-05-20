package com.woocommerce.android.screenshots.reviews

import com.woocommerce.android.R
import com.woocommerce.android.screenshots.util.Screen

class SingleReviewScreen : Screen {
    companion object {
        const val PRODUCT_NAME_LABEL = R.id.review_product_name
    }

    constructor() : super(PRODUCT_NAME_LABEL)

    fun goBackToReviewsScreen(): ReviewsListScreen {
        pressBack()
        return ReviewsListScreen()
    }
}
