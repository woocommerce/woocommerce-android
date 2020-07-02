package com.woocommerce.android.screenshots.reviews

import com.woocommerce.android.R
import com.woocommerce.android.screenshots.util.Screen

class SingleReviewScreen : Screen(PRODUCT_NAME_LABEL) {
    companion object {
        const val PRODUCT_NAME_LABEL = R.id.review_product_name
    }

    fun goBackToReviewsScreen(): ReviewsListScreen {
        pressBack()
        return ReviewsListScreen()
    }
}
