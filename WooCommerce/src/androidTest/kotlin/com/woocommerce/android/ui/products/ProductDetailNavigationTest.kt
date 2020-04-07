package com.woocommerce.android.ui.products

import android.app.Activity
import android.app.Instrumentation.ActivityResult
import android.content.Intent
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers
import androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.Visibility.GONE
import androidx.test.espresso.matcher.ViewMatchers.Visibility.VISIBLE
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import com.woocommerce.android.R
import com.woocommerce.android.helpers.WCMatchers
import com.woocommerce.android.ui.TestBase
import com.woocommerce.android.ui.main.MainActivityTestRule
import com.woocommerce.android.ui.orders.WcOrderTestUtils
import com.woocommerce.android.ui.products.ProductType.GROUPED
import org.hamcrest.CoreMatchers
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.Matcher
import org.hamcrest.Matchers.equalToIgnoringCase
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.wordpress.android.fluxc.model.SiteModel

@RunWith(AndroidJUnit4::class)
@LargeTest
class ProductDetailNavigationTest : TestBase() {
    @Rule
    @JvmField var activityTestRule = MainActivityTestRule()

    private val mockWCOrderModel = WcOrderTestUtils.generateOrderDetail(orderStatus = "Completed")
    private val mockProductModel = WcProductTestUtils.generateProductDetail()

    private fun chooser(matcher: Matcher<Intent>): Matcher<Intent> {
        return allOf(IntentMatchers.hasAction(Intent.ACTION_CHOOSER), hasExtra(`is`(Intent.EXTRA_INTENT), matcher))
    }

    @Before
    override fun setup() {
        super.setup()
        Intents.init()

        // By default Espresso Intents does not stub any Intents. Stubbing needs to be setup before
        // every test run. In this case all external Intents will be blocked.
        Intents.intending(CoreMatchers.not(IntentMatchers.isInternal()))
                .respondWith(ActivityResult(Activity.RESULT_OK, null))

        // Bypass login screen and display dashboard
        activityTestRule.launchMainActivityLoggedIn(null, SiteModel())

        // Make sure the bottom navigation view is showing
        activityTestRule.activity.showBottomNav()

        // add mock data to order list screen
        activityTestRule.setOrderListWithMockData()

        // Click on Orders tab in the bottom bar
        onView(withId(R.id.orders)).perform(click())

        // inject mock data to order detail page
        activityTestRule.setOrderDetailWithMockData(mockWCOrderModel)

        // redirect to order detail page
        onView(withId(R.id.ordersList))
                .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(1, click()))

        // inject mock data to order product list page
        activityTestRule.setOrderProductListWithMockData(mockWCOrderModel)

        // click on the Details button in product list card
        onView(withId(R.id.productList_btnDetails)).perform(WCMatchers.scrollTo(), click())
    }

    @After
    fun tearDown() {
        Intents.release()
    }

    @Test
    fun verifyProductListItemClickRedirectsToProductDetail() {
        // inject mock data to product detail
        activityTestRule.setOrderProductDetailWithMockData(mockProductModel)

        // click on the first item in product list page
        onView(withId(R.id.productList_products))
                .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click()))

        // verify that toolbar title is changed to the product detail name
        onView(withId(R.id.toolbar)).check(matches(WCMatchers.withToolbarTitle(
                equalToIgnoringCase(mockProductModel.name))))

        // verify that close button is visible
        onView(withContentDescription(R.string.abc_action_bar_up_description))
                .check(matches(ViewMatchers.withEffectiveVisibility(VISIBLE)))

        openActionBarOverflowOrOptionsMenu(getInstrumentation().targetContext)

        // verify that share button is visible
        onView(withText(R.string.share))
                .check(matches(ViewMatchers.withEffectiveVisibility(VISIBLE)))

        pressBack()

        // Clicking the "up" button in product detail screen returns the user to the product list screen.
        // The Toolbar title changes to "Order #1"
        onView(withContentDescription(R.string.abc_action_bar_up_description)).perform(click())

        onView(withId(R.id.toolbar)).check(matches(WCMatchers.withToolbarTitle(equalToIgnoringCase(
                appContext.getString(R.string.orderdetail_orderstatus_ordernum, "1")
        ))))
    }

    @Test
    fun verifyProductDetailListItemDisplayedSuccessfully() {
        // inject mock data to product detail
        activityTestRule.setOrderProductDetailWithMockData(mockProductModel)

        // click on the first item in product list page
        onView(withId(R.id.productList_products))
                .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click()))

        // since image is available, the imageView should be visible
        onView(withId(R.id.imageGallery)).check(matches(ViewMatchers.withEffectiveVisibility(VISIBLE)))

        // verify that review label is displayed correctly
        onView(WCMatchers.matchesWithIndex(withId(R.id.textPropertyName), 1))
                .check(matches(withText(appContext.getString(R.string.product_reviews))))

        // verify that product title is displayed correctly
        onView(withId(R.id.editText)).check(matches(withText(mockProductModel.name)))
    }

    @Test
    fun verifyProductDetailForVariationProductsDisplayedCorrectly() {
        // inject mock data to product detail
        mockProductModel.type = ProductType.VARIATION.name
        mockProductModel.reviewsAllowed = true
        activityTestRule.setOrderProductDetailWithMockData(mockProductModel)

        // click on the first item in product list page
        onView(withId(R.id.productList_products))
                .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click()))

        // verify that reviews view is not displayed for variation products
        onView(WCMatchers.matchesWithIndex(withId(R.id.textPropertyValue), 3)).check(doesNotExist())
        onView(WCMatchers.matchesWithIndex(withId(R.id.textPropertyName), 3)).check(doesNotExist())
    }

    @Test
    fun verifyProductDetailStoreButtonOpensWebView() {
        // inject mock data to product detail
        activityTestRule.setOrderProductDetailWithMockData(mockProductModel)

        // click on the first item in product list page
        onView(withId(R.id.productList_products))
                .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click()))

        openActionBarOverflowOrOptionsMenu(getInstrumentation().targetContext)

        // click the view product on store menu button
        onView(withText(R.string.product_view_in_store)).perform(click())

        // check if webview intent is opened for the given url
        Intents.intended(allOf(IntentMatchers.hasAction(Intent.ACTION_VIEW),
                IntentMatchers.hasData(mockProductModel.permalink)))
    }

    @Test
    fun verifyProductDetailReviewsDisplayedOnlyIfAllowed() {
        // inject mock data to product detail
        mockProductModel.reviewsAllowed = false
        activityTestRule.setOrderProductDetailWithMockData(mockProductModel)

        // click on the first item in product list page
        onView(withId(R.id.productList_products))
                .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click()))

        // verify that reviews view is hidden
        onView(WCMatchers.matchesWithIndex(withId(R.id.textPropertyValue), 4)).check(doesNotExist())
        onView(WCMatchers.matchesWithIndex(withId(R.id.textPropertyName), 4)).check(doesNotExist())
    }

    @Test
    fun verifyProductDetailReviewsDisplayedCorrectlyWhenAllowed() {
        // inject mock data to product detail
        mockProductModel.reviewsAllowed = true
        mockProductModel.ratingCount = 30
        mockProductModel.averageRating = "4"
        activityTestRule.setOrderProductDetailWithMockData(mockProductModel)

        // click on the first item in product list page
        onView(withId(R.id.productList_products))
                .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click()))

        // verify that reviews view is displayed correctly
        onView(WCMatchers.matchesWithIndex(withId(R.id.textPropertyName), 1))
                .check(matches(withText(appContext.getString(R.string.product_reviews))))

        onView(WCMatchers.matchesWithIndex(withId(R.id.textPropertyValue), 4))
                .check(matches(withText(mockProductModel.ratingCount.toString())))

        // verify the rating bar displays correct value
        onView(WCMatchers.matchesWithIndex(withId(R.id.ratingBar), 4))
                .check(matches(WCMatchers.matchesRating(mockProductModel.averageRating.toFloat())))
    }

    @Test
    fun verifyProductDetailStatusBadgeDisplayedForUnpublishedProducts() {
        // inject mock data to product detail
        mockProductModel.status = ProductStatus.DRAFT.name
        activityTestRule.setOrderProductDetailWithMockData(mockProductModel)

        // click on the first item in product list page
        onView(withId(R.id.productList_products))
                .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click()))

        // verify that badge view is displayed
        onView(withId(R.id.frameStatusBadge)).check(matches(ViewMatchers.withEffectiveVisibility(VISIBLE)))

        // verify badge text is displayed correctly
        onView(withId(R.id.textStatusBadge)).check(matches(withText(ProductStatus.DRAFT.toString(appContext))))
    }

    @Test
    fun verifyProductDetailShareButtonOpensShareIntent() {
        // inject mock data to product detail
        activityTestRule.setOrderProductDetailWithMockData(mockProductModel)

        // click on the first item in product list page
        onView(withId(R.id.productList_products))
                .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click()))

        openActionBarOverflowOrOptionsMenu(getInstrumentation().targetContext)

        // click the share button
        onView(withText(R.string.share)).perform(click())

        // check if share intent is opened with the given product details
        Intents.intended(chooser(allOf(
                IntentMatchers.hasAction(Intent.ACTION_SEND),
                hasExtra(Intent.EXTRA_TEXT, mockProductModel.permalink)
        )))
    }

    @Test
    fun verifyProductDetailPricingInfoDisplayedCorrectlyWhenNoPriceInfoAvailable() {
        // inject mock data to product detail
        mockProductModel.price = ""
        mockProductModel.salePrice = ""
        mockProductModel.taxClass = ""
        mockProductModel.sku = "Blah"
        activityTestRule.setOrderProductDetailWithMockData(mockProductModel)

        // click on the first item in product list page
        onView(withId(R.id.productList_products))
                .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click()))

        // verify caption is displayed correctly
        onView(WCMatchers.matchesWithIndex(withId(R.id.textPropertyName), 3))
                .check(matches(withText(appContext.getString(R.string.product_inventory))))

        // verify that the pricing card sku text is displayed correctly
        onView(WCMatchers.matchesWithIndex(withId(R.id.textPropertyValue), 2))
                .check(matches(withText(
                        appContext.getString(R.string.product_sku) + ": " + mockProductModel.sku
                )))
    }

    @Test
    fun verifyProductDetailPricingInfoDisplayedCorrectlyWhenPriceInfoAvailable() {
        // inject mock data to product detail
        mockProductModel.price = "10"
        mockProductModel.salePrice = ""
        mockProductModel.taxClass = "2"
        mockProductModel.sku = "Blah"
        activityTestRule.setOrderProductDetailWithMockData(mockProductModel)

        // click on the first item in product list page
        onView(withId(R.id.productList_products))
                .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click()))

        // verify caption is displayed correctly
        onView(WCMatchers.matchesWithIndex(withId(R.id.textPropertyName), 3))
                .check(matches(withText(appContext.getString(R.string.product_inventory))))

        // verify that the pricing card sku text is displayed correctly
        onView(WCMatchers.matchesWithIndex(withId(R.id.textPropertyValue), 2))
                .check(matches(withText(
                        appContext.getString(R.string.product_sku) + ": " + mockProductModel.sku
                )))
    }

    @Test
    fun verifyProductDetailPricingInfoDisplayedCorrectlyWhenSalePriceNotAvailable() {
        // inject mock data to product detail
        mockProductModel.price = "10.00"
        mockProductModel.salePrice = ""
        mockProductModel.taxClass = "2"
        mockProductModel.sku = "Blah"
        activityTestRule.setOrderProductDetailWithMockData(mockProductModel)

        // click on the first item in product list page
        onView(withId(R.id.productList_products))
                .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click()))

        // verify that the pricing card property label = R.string.product_price
        onView(WCMatchers.matchesWithIndex(withId(R.id.textPropertyName), 2))
                .check(matches(withText(appContext.getString(R.string.product_price))))

        // verify that the pricing card pricing text is displayed correctly
        onView(WCMatchers.matchesWithIndex(withId(R.id.textPropertyValue), 1))
                .check(matches(withText("\$${mockProductModel.price}")))
    }

    @Test
    fun verifyProductDetailPricingInfoDisplayedCorrectlyWhenSalePriceAvailable() {
        // inject mock data to product detail
        mockProductModel.price = "10.00"
        mockProductModel.regularPrice = "10.00"
        mockProductModel.salePrice = "15.00"
        mockProductModel.taxClass = "2"
        mockProductModel.sku = "Blah"
        activityTestRule.setOrderProductDetailWithMockData(mockProductModel)

        // click on the first item in product list page
        onView(withId(R.id.productList_products))
                .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click()))

        // verify that the pricing card property label = R.string.product_price
        onView(WCMatchers.matchesWithIndex(withId(R.id.textPropertyName), 2))
                .check(matches(withText(appContext.getString(R.string.product_price))))

        // verify that the pricing card pricing text is displayed correctly
        val regularPrice = "${appContext.getString(R.string.product_regular_price)}: \$${mockProductModel.regularPrice}"
        val salesPrice = "${appContext.getString(R.string.product_sale_price)}: \$${mockProductModel.salePrice}"
        onView(WCMatchers.matchesWithIndex(withId(R.id.textPropertyValue), 1))
                .check(matches(withText("$regularPrice\n$salesPrice")))
    }

    @Test
    fun verifyProductDetailShippingInfoDisplayedCorrectlyWhenWidthHeightLengthAvailable() {
        // inject mock data to product detail
        mockProductModel.weight = "4"
        mockProductModel.width = "2"
        mockProductModel.height = "3"
        mockProductModel.length = "1"
        mockProductModel.shippingClass = "5"
        mockProductModel.shippingClassId = 5
        activityTestRule.setOrderProductDetailWithMockData(mockProductModel)

        // click on the first item in product list page
        onView(withId(R.id.productList_products))
                .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click()))

        // verify that the shipping card property label = R.string.product_shipping
        onView(WCMatchers.matchesWithIndex(withId(R.id.textPropertyName), 4))
                .check(matches(withText(appContext.getString(R.string.product_shipping))))

        // verify that the shipping card pricing text is displayed correctly
        val weight = "${appContext.getString(R.string.product_weight)}: ${mockProductModel.weight}oz"
        val size = "${appContext.getString(R.string.product_dimensions)}: " +
                "${mockProductModel.length} x ${mockProductModel.width} x ${mockProductModel.height} in"
        val shippingClass = "${appContext.getString(R.string.product_shipping_class)}: " +
                mockProductModel.shippingClass

        onView(WCMatchers.matchesWithIndex(withId(R.id.textPropertyValue), 3))
                .check(matches(withText("$weight\n$size\n$shippingClass")))
    }

    @Test
    fun verifyProductDetailShippingInfoDisplayedCorrectlyWhenWidthHeightOnlyAvailable() {
        // inject mock data to product detail
        val mockProductModel = WcProductTestUtils.generateProductDetail(productType = GROUPED)
        mockProductModel.weight = "4"
        mockProductModel.width = "2"
        mockProductModel.height = "3"
        mockProductModel.shippingClass = "5"
        mockProductModel.shippingClassId = 5
        activityTestRule.setOrderProductDetailWithMockData(mockProductModel)

        // click on the first item in product list page
        onView(withId(R.id.productList_products))
                .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click()))

        // verify caption is displayed correctly
        onView(WCMatchers.matchesWithIndex(withId(R.id.cardCaptionText), 2))
                .check(matches(withText(appContext.getString(R.string.product_purchase_details))))

        // verify that the shipping card property label = R.string.product_shipping
        onView(WCMatchers.matchesWithIndex(withId(R.id.textPropertyName), 3))
                .check(matches(withText(appContext.getString(R.string.product_shipping))))

        // verify that the shipping card pricing text is displayed correctly
        val weight = "${appContext.getString(R.string.product_weight)}: ${mockProductModel.weight}oz"
        val size = "${appContext.getString(R.string.product_size)}: " +
                "${mockProductModel.width} x ${mockProductModel.height} in"
        val shippingClass = "${appContext.getString(R.string.product_shipping_class)}: " +
                mockProductModel.shippingClass

        onView(WCMatchers.matchesWithIndex(withId(R.id.textPropertyValue), 2))
                .check(matches(withText("$weight\n$size\n$shippingClass")))
    }

    @Test
    fun verifyProductDetailPurchaseInfoForDownloadableProductsDisplayedCorrectly() {
        // inject mock data to product detail
        mockProductModel.downloadable = true
        mockProductModel.downloadLimit = 10
        mockProductModel.downloadExpiry = 2
        activityTestRule.setOrderProductDetailWithMockData(mockProductModel)

        // click on the first item in product list page
        onView(withId(R.id.productList_products))
                .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click()))

        onView(WCMatchers.matchesWithIndex(withId(R.id.cardCaptionText), 2))
                .check(matches(withText(appContext.getString(R.string.product_purchase_details))))

        // verify that the download card property label = R.string.product_downloads
        onView(WCMatchers.matchesWithIndex(withId(R.id.textPropertyName), 5))
                .check(matches(withText(appContext.getString(R.string.product_downloads))))

        // verify that the shipping card pricing text is displayed correctly
        val downloadableFiles = mockProductModel.getDownloadableFiles()
        val count = "${appContext.getString(R.string.product_downloadable_files)}: ${downloadableFiles.size}"
        val limit = "${appContext.getString(R.string.product_download_limit)}: ${String.format(
                appContext.getString(R.string.product_download_limit_count),
                mockProductModel.downloadLimit
        )}"
        val expiry = "${appContext.getString(R.string.product_download_expiry)}: ${String.format(
                appContext.getString(R.string.product_download_expiry_days),
                mockProductModel.downloadExpiry
        )}"
        onView(WCMatchers.matchesWithIndex(withId(R.id.textPropertyValue), 4))
                .check(matches(withText("$count\n$limit\n$expiry")))
    }

    @Test
    fun verifyProductDetailPurchaseInfoForDownloadableProductsWithoutLimitAndExpiryDisplayedCorrectly() {
        // inject mock data to product detail
        mockProductModel.downloadable = true
        activityTestRule.setOrderProductDetailWithMockData(mockProductModel)

        // click on the first item in product list page
        onView(withId(R.id.productList_products))
                .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click()))

        onView(WCMatchers.matchesWithIndex(withId(R.id.cardCaptionText), 2))
                .check(matches(withText(appContext.getString(R.string.product_purchase_details))))

        // verify that the download card property label = R.string.product_downloads
        onView(WCMatchers.matchesWithIndex(withId(R.id.textPropertyName), 5))
                .check(matches(withText(appContext.getString(R.string.product_downloads))))

        // verify that the shipping card pricing text is displayed correctly
        val downloadableFiles = mockProductModel.getDownloadableFiles()
        val count = "${appContext.getString(R.string.product_downloadable_files)}: ${downloadableFiles.size}"
        onView(WCMatchers.matchesWithIndex(withId(R.id.textPropertyValue), 4))
                .check(matches(withText(count)))
    }

    @Test
    fun verifyProductDetailPurchaseNoteDisplayedCorrectlyWhenAvailable() {
        // inject mock data to product detail
        mockProductModel.purchaseNote = "Purchase Note"
        activityTestRule.setOrderProductDetailWithMockData(mockProductModel)

        // click on the first item in product list page
        onView(withId(R.id.productList_products))
                .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click()))

        onView(WCMatchers.matchesWithIndex(withId(R.id.cardCaptionText), 2))
                .check(matches(withText(appContext.getString(R.string.product_purchase_details))))

        // verify that the purchase note property label = R.string.product_purchase_note
        onView(WCMatchers.matchesWithIndex(withId(R.id.textCaption), 0))
                .check(matches(withText(appContext.getString(R.string.product_purchase_note))))

        // verify that the purchase note card pricing text is displayed correctly
        onView(WCMatchers.matchesWithIndex(withId(R.id.textContent), 0))
                .check(matches(withText(mockProductModel.purchaseNote)))

        // verify that the purchase note read more is not displayed
        onView(WCMatchers.matchesWithIndex(withId(R.id.textReadMore), 0))
                .check(matches(ViewMatchers.withEffectiveVisibility(GONE)))
    }

    @Test
    fun verifyProductDetailReallyLongPurchaseNoteDisplayedCorrectlyWhenAvailable() {
        // inject mock data to product detail
        mockProductModel.purchaseNote = "Lorem ipsum dolor sit amet, consectetur adipisicing elit. " +
                "Eligendi non quis exercitationem culpa nesciunt nihil aut nostrum explicabo reprehenderit " +
                "optio amet ab temporibus asperiores quasi cupiditate. Voluptatum ducimus voluptates voluptas?"
        activityTestRule.setOrderProductDetailWithMockData(mockProductModel)

        // click on the first item in product list page
        onView(withId(R.id.productList_products))
                .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click()))

        onView(WCMatchers.matchesWithIndex(withId(R.id.cardCaptionText), 2))
                .check(matches(withText(appContext.getString(R.string.product_purchase_details))))

        // verify that the purchase note property label = R.string.product_purchase_note
        onView(WCMatchers.matchesWithIndex(withId(R.id.textCaption), 0))
                .check(matches(withText(appContext.getString(R.string.product_purchase_note))))

        // verify that the purchase note card pricing text is displayed correctly
        onView(WCMatchers.matchesWithIndex(withId(R.id.textContent), 0))
                .check(matches(withText(mockProductModel.purchaseNote)))

        // verify that the purchase note read more is displayed
        onView(withId(R.id.textReadMore)).check(matches(ViewMatchers.withEffectiveVisibility(VISIBLE)))

        // verify dialog is displayed when read more button is clicked
        onView(withId(R.id.textReadMore)).perform(WCMatchers.scrollTo(), click())

        onView(withText(appContext.getString(R.string.product_purchase_note)))
                .inRoot(isDialog()).check(matches(isDisplayed()))
    }
}
