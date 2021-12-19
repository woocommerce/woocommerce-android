package com.woocommerce.android.ui.orders.filters

import io.kotest.common.ExperimentalKotest
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import kotlin.time.ExperimentalTime

@ExperimentalKotest
@ExperimentalTime
class OrderFilterCategoriesViewModelTestKotest : BehaviorSpec() {
    init {
        testCoroutineDispatcher = true
        TestCoroutineDispatcher().runBlockingTest {

        }
        given("Given a string") {
            val string = "hello"
            `when`("checking string legnth") {
                then("returns string length") {
                    string.length shouldBe 5
                }
            }
        }
    }

//    suspend fun givenOrderStatusOptionsAvailable() {
//        whenever(getOrderStatusFilterOptions.invoke()).thenReturn(
//            OrderTestUtils.generateOrderStatusOptions()
//                .map {
//                    OrderStatusOption(
//                        key = it.statusKey,
//                        label = it.label,
//                        statusCount = it.statusCount,
//                        isSelected = false
//                    )
//                }
//        )
//    }
//
//    fun givenDateRangeFiltersAvailable() {
//        whenever(getDateRangeFilterOptions.invoke()).thenReturn(
//            listOf(DateRange.TODAY, DateRange.LAST_2_DAYS, DateRange.LAST_7_DAYS, DateRange.LAST_30_DAYS)
//                .map {
//                    DateRangeFilterOption(
//                        dateRange = it,
//                        isSelected = false,
//                        startDate = 0,
//                        endDate = 0
//                    )
//                }
//        )
//    }
//
//    fun givenResourceProviderReturnsNonEmptyStrings() {
//        whenever(resourceProvider.getString(any())).thenReturn("AnyString")
//        whenever(resourceProvider.getString(any(), any(), any())).thenReturn("AnyString")
//    }
}

fun testBlocking(block: suspend TestCoroutineScope.() -> Unit) =
    TestCoroutineDispatcher().runBlockingTest {
        block()
    }
