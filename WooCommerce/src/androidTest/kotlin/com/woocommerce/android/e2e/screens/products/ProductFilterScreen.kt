package com.woocommerce.android.e2e.screens.products

import androidx.test.espresso.Espresso
import androidx.test.espresso.matcher.ViewMatchers
import com.woocommerce.android.R
import com.woocommerce.android.e2e.helpers.util.Screen
import org.hamcrest.Matchers

class ProductFilterScreen : Screen {

    companion object {
        const val TOOLBAR = R.id.toolbar
    }

    constructor() : super(TOOLBAR)

    fun filterByPropertyAndValue(property: String, value: String): ProductFilterScreen {
        clickByTextAndId(property, R.id.filterItemName)
        clickByTextAndId(value, R.id.filterOptionItem_name)
        return this
    }

    fun clearFilters(): ProductFilterScreen {
        clickOn(R.id.menu_clear)
        return this
    }

    fun tapShowProducts(): ProductListScreen {
        val showProductButton = Espresso.onView(
            Matchers.allOf(
                Matchers.anyOf(
                    ViewMatchers.withId(R.id.filterList_btnShowProducts),
                    ViewMatchers.withId(R.id.filterOptionList_btnShowProducts)
                ),
                ViewMatchers.isCompletelyDisplayed(),
            )
        )

        clickOn(showProductButton)
        return ProductListScreen()
    }

    fun leaveFilterScreen(): ProductListScreen {
        if (isElementDisplayed(R.id.filterList) || isElementDisplayed(R.id.filterOptionList)) {
            tapShowProducts()
        }

        return ProductListScreen()
    }
}
