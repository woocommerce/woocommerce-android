package com.woocommerce.android.ui.orders

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.BoundedMatcher
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.Visibility
import androidx.test.espresso.matcher.ViewMatchers.Visibility.VISIBLE
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.R
import com.woocommerce.android.R.string
import com.woocommerce.android.helpers.WCMatchers
import com.woocommerce.android.ui.TestBase
import com.woocommerce.android.ui.main.MainActivityTestRule
import com.woocommerce.android.widgets.sectionedrecyclerview.SectionedRecyclerViewAdapter
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.wordpress.android.fluxc.model.SiteModel

@RunWith(AndroidJUnit4::class)
@LargeTest
class AddOrderTrackingProviderListTest : TestBase() {
    @Rule
    @JvmField var activityTestRule = MainActivityTestRule()

    private val mockWCOrderModel = WcOrderTestUtils.generateOrderDetail(orderStatus = "processing")
    private val mockShipmentTrackingProviders = WcOrderTestUtils.generateShipmentTrackingProviderList()

    /**
     * Custom matcher to check if the [SectionedRecyclerViewAdapter] section count matches
     * the incoming count value
     */
    private fun withProviderSectionItemCount(title: String, itemsCount: Int): Matcher<View> {
        return object : BoundedMatcher<View, RecyclerView>(RecyclerView::class.java) {
            override fun describeTo(description: Description) {
                description.appendText("with number of items: $itemsCount")
            }

            override fun matchesSafely(recyclerView: RecyclerView): Boolean {
                val adapter = recyclerView.adapter as? AddOrderTrackingProviderListAdapter
                return adapter?.getSectionItemsTotal(title) == itemsCount
            }
        }
    }

    @Before
    override fun setup() {
        super.setup()
        // Bypass login screen and display dashboard
        activityTestRule.launchMainActivityLoggedIn(null, SiteModel())

        // Make sure the bottom navigation view is showing
        activityTestRule.activity.showBottomNav()

        // add mock data to order list screen
        activityTestRule.setOrderListWithMockData()

        // Click on Orders tab in the bottom bar
        onView(withId(R.id.orderListFragment)).perform(click())

        // add mock data to order detail screen
        activityTestRule.setOrderDetailWithMockData(order = mockWCOrderModel)

        // click on a single order from the list
        onView(withId(R.id.ordersList))
                .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(1, click()))
    }

    @Test
    fun verifyProviderListSectionsDisplayedCorrectly() {
        // setting the default provider value to empty here
        AppPrefs.setSelectedShipmentTrackingProviderName("")

        // scroll to bottom and click on add tracking button and redirect to Add shipment tracking fragnent
        onView(withId(R.id.shipmentTrack_btnAddTracking)).perform(WCMatchers.scrollTo(), click())

        // inject mock data to the provider list screen
        activityTestRule.setOrderProviderListWithMockData()

        // click on select provider field to redirect to provider list screen
        onView(withId(R.id.addTracking_editCarrier)).perform(click())

        // the section count matches the country count from the mock data + custom provider section
        val expectedSectionCount = mockShipmentTrackingProviders.groupBy { it.country }.keys.size + 1
        onView(ViewMatchers.isAssignableFrom(androidx.recyclerview.widget.RecyclerView::class.java))
                .check(matches(WCMatchers.withSectionCount(expectedSectionCount)))

        // verify the there is 1 item in the `Custom` section
        onView(ViewMatchers.isAssignableFrom(androidx.recyclerview.widget.RecyclerView::class.java))
                .check(matches(withProviderSectionItemCount(
                        appContext.getString(R.string.order_shipment_tracking_custom_provider_section_title),
                        1
                )))

        // verify that the item text in the "Custom" section = "Custom Provider"
        onView(WCMatchers.withRecyclerView(R.id.addTrackingProviderList).atPositionOnView(
                5, R.id.addShipmentTrackingProviderListItem_name
        )).check(matches(withText(appContext.getString(R.string.order_shipment_tracking_custom_provider_section_name))))
    }

    @Test
    fun verifyStoreCountryDisplayedFirstForUnitedStates() {
        // setting the default provider value to empty here
        AppPrefs.setSelectedShipmentTrackingProviderName("")

        // scroll to bottom and click on add tracking button and redirect to Add shipment tracking fragnent
        onView(withId(R.id.shipmentTrack_btnAddTracking)).perform(WCMatchers.scrollTo(), click())

        // inject mock data to the provider list screen
        activityTestRule.setOrderProviderListWithMockData()

        // click on select provider field to redirect to provider list screen
        onView(withId(R.id.addTracking_editCarrier)).perform(click())

        // verify the the first section in the list is has title United States
        val sectionTitle = appContext.getString(R.string.country_mapping_US)
        onView(WCMatchers.withRecyclerView(R.id.addTrackingProviderList).atPositionOnView(
                0, R.id.providerListHeader
        )).check(matches(withText(sectionTitle)))

        // verify the the first section in the list is has section count 3
        onView(ViewMatchers.isAssignableFrom(androidx.recyclerview.widget.RecyclerView::class.java))
                .check(matches(withProviderSectionItemCount(
                        sectionTitle, 3
                )))
    }

    @Test
    fun verifyStoreCountryDisplayedFirstForIndia() {
        // setting the default provider value to empty here
        AppPrefs.setSelectedShipmentTrackingProviderName("")

        // scroll to bottom and click on add tracking button and redirect to Add shipment tracking fragnent
        onView(withId(R.id.shipmentTrack_btnAddTracking)).perform(WCMatchers.scrollTo(), click())

        // inject mock data to the provider list screen
        activityTestRule.setOrderProviderListWithMockData(storeCountry = "IN")

        // click on select provider field to redirect to provider list screen
        onView(withId(R.id.addTracking_editCarrier)).perform(click())

        // verify the the first section in the list is has title India
        val sectionTitle = appContext.getString(R.string.country_mapping_IN)
        onView(WCMatchers.withRecyclerView(R.id.addTrackingProviderList).atPositionOnView(
                0, R.id.providerListHeader
        )).check(matches(withText(sectionTitle)))

        // verify the the first section in the list is has section count 1
        onView(ViewMatchers.isAssignableFrom(androidx.recyclerview.widget.RecyclerView::class.java))
                .check(matches(withProviderSectionItemCount(
                        sectionTitle, 1
                )))
    }

    @Test
    fun verifyStoreCountryNotDisplayedFirstForHongKong() {
        // setting the default provider value to empty here
        AppPrefs.setSelectedShipmentTrackingProviderName("")

        // scroll to bottom and click on add tracking button and redirect to Add shipment tracking fragnent
        onView(withId(R.id.shipmentTrack_btnAddTracking)).perform(WCMatchers.scrollTo(), click())

        // inject mock data to the provider list screen
        activityTestRule.setOrderProviderListWithMockData(storeCountry = "HK")

        // click on select provider field to redirect to provider list screen
        onView(withId(R.id.addTracking_editCarrier)).perform(click())

        // verify the the first section in the list is Custom provider since there is no provider for the store
        // country, the list will be displayed alphabetically after displaying the custom section
        val sectionTitle = appContext.getString(R.string.order_shipment_tracking_custom_provider_section_title)
        onView(WCMatchers.withRecyclerView(R.id.addTrackingProviderList).atPositionOnView(
                0, R.id.providerListHeader
        )).check(matches(withText(sectionTitle)))

        // verify the the first section in the list is has section count 3
        onView(ViewMatchers.isAssignableFrom(androidx.recyclerview.widget.RecyclerView::class.java))
                .check(matches(withProviderSectionItemCount(
                        sectionTitle, 1
                )))
    }

    @Test
    fun verifySelectDefaultProviderFromList() {
        // setting the default provider value to empty here
        AppPrefs.setSelectedShipmentTrackingProviderName("")

        // scroll to bottom and click on add tracking button and redirect to Add shipment tracking fragnent
        onView(withId(R.id.shipmentTrack_btnAddTracking)).perform(WCMatchers.scrollTo(), click())

        // verify that the select provider field is empty
        onView(withId(R.id.addTracking_editCarrier)).check(matches(withText("")))

        // inject mock data to the provider list screen
        activityTestRule.setOrderProviderListWithMockData()

        // click on select provider field to redirect to provider list screen
        onView(withId(R.id.addTracking_editCarrier)).perform(click())

        // select the first item on the first section of the list: under United States
        onView(withId(R.id.addTrackingProviderList))
                .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(1, click()))

        // verify if redirected to previous screen
        onView(withId(R.id.toolbar)).check(matches(WCMatchers.withToolbarTitle(
                Matchers.equalToIgnoringCase(appContext.getString(string.order_shipment_tracking_toolbar_title))
        )))

        // verify that the select provider field now has the value selected
        onView(withId(R.id.addTracking_editCarrier)).check(matches(withText("US Provider 1")))

        // verify that custom provider name field is not displayed
        onView(withId(R.id.addTracking_custom_provider_name)).check(matches(withEffectiveVisibility(Visibility.GONE)))

        // verify that custom provider tracking url field is not displayed
        onView(withId(R.id.addTracking_custom_provider_url)).check(matches(withEffectiveVisibility(Visibility.GONE)))
    }

    @Test
    fun verifySelectCustomProviderFromList() {
        AppPrefs.setSelectedShipmentTrackingProviderName("")
        AppPrefs.setIsSelectedShipmentTrackingProviderNameCustom(true)

        // scroll to bottom and click on add tracking button and redirect to Add shipment tracking fragnent
        onView(withId(R.id.shipmentTrack_btnAddTracking)).perform(WCMatchers.scrollTo(), click())

        // verify that the select provider field is empty
        onView(withId(R.id.addTracking_custom_provider_name)).check(matches(withText("")))

        // inject mock data to the provider list screen
        activityTestRule.setOrderProviderListWithMockData()

        // click on select provider field to redirect to provider list screen
        onView(withId(R.id.addTracking_editCarrier)).perform(click())

        // select the Custom provider list item
        onView(withId(R.id.addTrackingProviderList))
                .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(5, click()))

        // verify if redirected to previous screen
        onView(withId(R.id.toolbar)).check(matches(WCMatchers.withToolbarTitle(
                Matchers.equalToIgnoringCase(appContext.getString(string.order_shipment_tracking_toolbar_title))
        )))

        // verify that the select provider field now has the value selected
        onView(withId(R.id.addTracking_editCarrier)).check(matches(withText(
                appContext.getString(R.string.order_shipment_tracking_custom_provider_section_name)
        )))

        // verify that custom provider name field is displayed
        onView(withId(R.id.addTracking_custom_provider_name)).check(
                matches(withEffectiveVisibility(Visibility.VISIBLE))
        )

        // verify that custom provider tracking url field is displayed
        onView(withId(R.id.addTracking_custom_provider_url)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
    }

    @Test
    fun verifyAlreadySelectedProviderNameDisplayedCorrectly() {
        val providerName = "US Provider 1"
        AppPrefs.setSelectedShipmentTrackingProviderName(providerName)
        AppPrefs.setIsSelectedShipmentTrackingProviderNameCustom(false)

        // scroll to bottom and click on add tracking button and redirect to Add shipment tracking fragnent
        onView(withId(R.id.shipmentTrack_btnAddTracking)).perform(WCMatchers.scrollTo(), click())

        // verify that the select provider field is not empty
        onView(withId(R.id.addTracking_editCarrier)).check(matches(withText(providerName)))

        // inject mock data to the provider list screen
        activityTestRule.setOrderProviderListWithMockData()

        // click on select provider field to redirect to provider list screen
        onView(withId(R.id.addTracking_editCarrier)).perform(click())

        // verify that tick is displayed against the selected provider item
        onView(WCMatchers.withRecyclerView(R.id.addTrackingProviderList)
                .atPositionOnView(1, R.id.addShipmentTrackingProviderListItem_tick))
                .check(matches(withEffectiveVisibility(VISIBLE)))
    }
}
