package com.woocommerce.android.ui.orders

import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.action.ViewActions.click
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.contrib.RecyclerViewActions
import android.support.test.espresso.matcher.BoundedMatcher
import android.support.test.espresso.matcher.ViewMatchers
import android.support.test.espresso.matcher.ViewMatchers.Visibility
import android.support.test.espresso.matcher.ViewMatchers.Visibility.VISIBLE
import android.support.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import android.support.test.espresso.matcher.ViewMatchers.withId
import android.support.test.espresso.matcher.ViewMatchers.withText
import android.support.test.filters.LargeTest
import android.support.test.runner.AndroidJUnit4
import android.support.v7.widget.RecyclerView
import android.view.View
import com.woocommerce.android.R
import com.woocommerce.android.R.string
import com.woocommerce.android.helpers.WCMatchers
import com.woocommerce.android.ui.TestBase
import com.woocommerce.android.widgets.sectionedrecyclerview.SectionedRecyclerViewAdapter
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class AddOrderTrackingProviderListTest : TestBase() {
    @Rule
    @JvmField var activityTestRule = AddShipmentTrackingActivityTestRule()

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

    @Test
    fun verifyProviderListSectionsDisplayedCorrectly() {
        // launch activity with empty provider name
        activityTestRule.launchAddShipmentActivityWithIntent(mockWCOrderModel.getIdentifier(), "")

        // inject mock data to the provider list screen
        activityTestRule.setOrderProviderListWithMockData()

        // click on select provider field to redirect to provider list screen
        onView(withId(R.id.addTracking_editCarrier)).perform(click())

        // the section count matches the country count from the mock data + custom provider section
        val expectedSectionCount = mockShipmentTrackingProviders.groupBy { it.country }.keys.size + 1
        onView(ViewMatchers.isAssignableFrom(RecyclerView::class.java))
                .check(matches(WCMatchers.withSectionCount(expectedSectionCount)))

        // verify the there is 1 item in the `Custom` section
        onView(ViewMatchers.isAssignableFrom(RecyclerView::class.java))
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
        // launch activity with empty provider name
        activityTestRule.launchAddShipmentActivityWithIntent(mockWCOrderModel.getIdentifier(), "")

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
        onView(ViewMatchers.isAssignableFrom(RecyclerView::class.java))
                .check(matches(withProviderSectionItemCount(
                        sectionTitle, 3
                )))
    }

    @Test
    fun verifyStoreCountryDisplayedFirstForIndia() {
        // launch activity with empty provider name
        activityTestRule.launchAddShipmentActivityWithIntent(mockWCOrderModel.getIdentifier(), "")

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
        onView(ViewMatchers.isAssignableFrom(RecyclerView::class.java))
                .check(matches(withProviderSectionItemCount(
                        sectionTitle, 1
                )))
    }

    @Test
    fun verifyStoreCountryNotDisplayedFirstForHongKong() {
        // launch activity with empty provider name
        activityTestRule.launchAddShipmentActivityWithIntent(mockWCOrderModel.getIdentifier(), "")

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
        onView(ViewMatchers.isAssignableFrom(RecyclerView::class.java))
                .check(matches(withProviderSectionItemCount(
                        sectionTitle, 1
                )))
    }

    @Test
    fun verifySelectDefaultProviderFromList() {
        // launch activity with empty provider name
        activityTestRule.launchAddShipmentActivityWithIntent(mockWCOrderModel.getIdentifier(), "")

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
        // launch activity with empty provider name
        activityTestRule.launchAddShipmentActivityWithIntent(mockWCOrderModel.getIdentifier(), "")

        // verify that the select provider field is empty
        onView(withId(R.id.addTracking_editCarrier)).check(matches(withText("")))

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
        // launch activity with empty provider name
        val providerName = "US Provider 1"
        activityTestRule.launchAddShipmentActivityWithIntent(mockWCOrderModel.getIdentifier(), providerName)

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
