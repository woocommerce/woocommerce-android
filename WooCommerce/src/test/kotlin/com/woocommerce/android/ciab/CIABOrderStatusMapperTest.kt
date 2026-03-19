package com.woocommerce.android.ciab

import com.woocommerce.android.R
import com.woocommerce.android.model.Order.OrderStatus
import com.woocommerce.android.ui.orders.filters.data.OrderStatusOption
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.given
import org.wordpress.android.fluxc.model.WCOrderStatusModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.CoreOrderStatus

@OptIn(ExperimentalCoroutinesApi::class)
class CIABOrderStatusMapperTest : BaseUnitTest() {

    private val ciabSiteGateKeeper: CIABSiteGateKeeper = mock()
    private val resourceProvider: ResourceProvider = mock()

    private lateinit var sut: CIABOrderStatusMapper

    private val openLabel = "Open"

    private val defaultStatusMap = mapOf(
        CoreOrderStatus.PENDING.value to WCOrderStatusModel(statusKey = "pending", label = "Pending"),
        CoreOrderStatus.PROCESSING.value to WCOrderStatusModel(statusKey = "processing", label = "Processing"),
        CoreOrderStatus.ON_HOLD.value to WCOrderStatusModel(statusKey = "on-hold", label = "On hold"),
        CoreOrderStatus.COMPLETED.value to WCOrderStatusModel(statusKey = "completed", label = "Completed"),
        CoreOrderStatus.CANCELLED.value to WCOrderStatusModel(statusKey = "cancelled", label = "Cancelled"),
        CoreOrderStatus.REFUNDED.value to WCOrderStatusModel(statusKey = "refunded", label = "Refunded"),
        CoreOrderStatus.FAILED.value to WCOrderStatusModel(statusKey = "failed", label = "Failed"),
    )

    private val defaultFilterOptions = listOf(
        OrderStatusOption(key = "pending", label = "Pending", statusCount = 3, isSelected = false),
        OrderStatusOption(key = "processing", label = "Processing", statusCount = 5, isSelected = false),
        OrderStatusOption(key = "on-hold", label = "On hold", statusCount = 2, isSelected = false),
        OrderStatusOption(key = "failed", label = "Failed", statusCount = 1, isSelected = false),
        OrderStatusOption(key = "completed", label = "Completed", statusCount = 10, isSelected = false),
        OrderStatusOption(key = "cancelled", label = "Cancelled", statusCount = 4, isSelected = false),
    )

    @Before
    fun setUp() {
        given(ciabSiteGateKeeper.isCurrentSiteCIAB()).willReturn(true)
        given(resourceProvider.getString(R.string.ciab_order_status_open)).willReturn(openLabel)

        sut = CIABOrderStatusMapper(ciabSiteGateKeeper, resourceProvider)
    }

    @Test
    fun `given site is not CIAB, when mapOrderStatusOptionsList, then returns input unchanged`() {
        // GIVEN
        given(ciabSiteGateKeeper.isCurrentSiteCIAB()).willReturn(false)

        // WHEN
        val result = sut.mapOrderStatusOptionsList(defaultStatusMap)

        // THEN
        assertThat(result).isEqualTo(defaultStatusMap)
    }

    @Test
    fun `given site is CIAB, when mapOrderStatusOptionsList, then remaps open statuses to open`() {
        // WHEN
        val result = sut.mapOrderStatusOptionsList(defaultStatusMap)

        // THEN
        for (key in CIABOrderStatusMapper.OPEN_CORE_KEYS) {
            val original = defaultStatusMap[key]!!
            assertThat(result[key]).isEqualTo(
                original.copy(statusKey = "open", label = openLabel)
            )
        }
    }

    @Test
    fun `given site is CIAB, when mapOrderStatusOptionsList, then leaves non-open statuses unchanged`() {
        // WHEN
        val result = sut.mapOrderStatusOptionsList(defaultStatusMap)

        // THEN
        assertThat(result["completed"]).isEqualTo(defaultStatusMap["completed"])
        assertThat(result["cancelled"]).isEqualTo(defaultStatusMap["cancelled"])
        assertThat(result["refunded"]).isEqualTo(defaultStatusMap["refunded"])
    }

    @Test
    fun `given site is not CIAB, when mapOrderStatus, then returns input unchanged`() {
        // GIVEN
        given(ciabSiteGateKeeper.isCurrentSiteCIAB()).willReturn(false)
        val status = OrderStatus(statusKey = "pending", label = "Pending")

        // WHEN
        val result = sut.mapOrderStatus(status)

        // THEN
        assertThat(result).isEqualTo(status)
    }

    @Test
    fun `given site is CIAB, when mapOrderStatus with pending status, then maps to Open`() {
        // GIVEN
        val status = OrderStatus(statusKey = "pending", label = "Pending")

        // WHEN
        val result = sut.mapOrderStatus(status)

        // THEN
        assertThat(result).isEqualTo(OrderStatus(statusKey = "open", label = openLabel))
    }

    @Test
    fun `given site is CIAB, when mapOrderStatus with completed status, then returns unchanged`() {
        // GIVEN
        val status = OrderStatus(statusKey = "completed", label = "Completed")

        // WHEN
        val result = sut.mapOrderStatus(status)

        // THEN
        assertThat(result).isEqualTo(status)
    }

    @Test
    fun `given site is not CIAB, when mapFilterOptions, then returns input unchanged`() {
        // GIVEN
        given(ciabSiteGateKeeper.isCurrentSiteCIAB()).willReturn(false)

        // WHEN
        val result = sut.mapFilterOptions(defaultFilterOptions)

        // THEN
        assertThat(result).isEqualTo(defaultFilterOptions)
    }

    @Test
    fun `given site is CIAB, when mapFilterOptions, then groups open statuses with summed counts`() {
        // WHEN
        val result = sut.mapFilterOptions(defaultFilterOptions)

        // THEN
        val openOption = result.first { it.key == "open" }
        assertThat(openOption.label).isEqualTo(openLabel)
        assertThat(openOption.statusCount).isEqualTo(3 + 5 + 2 + 1)
        assertThat(result.none { it.key in CIABOrderStatusMapper.OPEN_CORE_KEYS }).isTrue()
    }

    @Test
    fun `given site is CIAB and one open status is selected, when mapFilterOptions, then grouped option is selected`() {
        // GIVEN
        val options = defaultFilterOptions.map {
            if (it.key == "processing") it.copy(isSelected = true) else it
        }

        // WHEN
        val result = sut.mapFilterOptions(options)

        // THEN
        val openOption = result.first { it.key == "open" }
        assertThat(openOption.isSelected).isTrue()
    }

    @Test
    fun `given site is CIAB, when mapFilterOptions with checkout-draft, then excludes checkout-draft`() {
        // GIVEN
        val optionsWithDraft = defaultFilterOptions + OrderStatusOption(
            key = "checkout-draft",
            label = "Draft",
            statusCount = 2,
            isSelected = false
        )

        // WHEN
        val result = sut.mapFilterOptions(optionsWithDraft)

        // THEN
        assertThat(result.none { it.key == "checkout-draft" }).isTrue()
    }

    @Test
    fun `given site is not CIAB, when mapFilterOptions with checkout-draft, then keeps checkout-draft`() {
        // GIVEN
        given(ciabSiteGateKeeper.isCurrentSiteCIAB()).willReturn(false)
        val optionsWithDraft = defaultFilterOptions + OrderStatusOption(
            key = "checkout-draft",
            label = "Draft",
            statusCount = 2,
            isSelected = false
        )

        // WHEN
        val result = sut.mapFilterOptions(optionsWithDraft)

        // THEN
        assertThat(result.any { it.key == "checkout-draft" }).isTrue()
    }

    @Test
    fun `given site is not CIAB, when resolveFilterKeys, then returns input unchanged`() {
        // GIVEN
        given(ciabSiteGateKeeper.isCurrentSiteCIAB()).willReturn(false)
        val keys = listOf("open", "completed")

        // WHEN
        val result = sut.resolveFilterKeys(keys)

        // THEN
        assertThat(result).isEqualTo(keys)
    }

    @Test
    fun `given site is CIAB, when resolveFilterKeys with open key, then expands to core keys`() {
        // WHEN
        val result = sut.resolveFilterKeys(listOf("open"))

        // THEN
        assertThat(result).containsExactlyInAnyOrder("pending", "processing", "on-hold", "failed")
    }

    @Test
    fun `given site is CIAB, when resolveFilterKeys with non-open keys, then passes them through unchanged`() {
        // WHEN
        val result = sut.resolveFilterKeys(listOf("completed", "cancelled"))

        // THEN
        assertThat(result).containsExactly("completed", "cancelled")
    }
}
