package org.wordpress.android.fluxc.persistence.entity

import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId

class OrderEntityTest {
    @Test
    fun `when giftCards column holds a json array, then getGiftCardList parses it`() = runTest {
        val entity = orderEntity(giftCards = """[{"id":4,"code":"NZR8-BMP8-XJZ2-ZKS9","amount":18}]""")

        val result = entity.getGiftCardList()

        assertThat(result).hasSize(1)
        assertThat(result.first().code).isEqualTo("NZR8-BMP8-XJZ2-ZKS9")
        assertThat(result.first().amount).isEqualTo("18")
    }

    /**
     * Pre-existing rows get the empty-string default for [OrderEntity.giftCards] after the
     * auto-migration that adds the column. Parsing that must return an empty list without throwing
     * (so it can't spam logs or fail mapping on upgrade).
     */
    @Test
    fun `when giftCards column is empty (migration default), then getGiftCardList returns empty list`() = runTest {
        val result = orderEntity(giftCards = "").getGiftCardList()

        assertThat(result).isEmpty()
    }

    @Test
    fun `when giftCards column is the null literal, then getGiftCardList returns empty list`() = runTest {
        val result = orderEntity(giftCards = "null").getGiftCardList()

        assertThat(result).isEmpty()
    }

    @Test
    fun `given a line item tax id is a non-numeric string, when getLineItemList, then all line items are returned with a null rate id`() = runTest {
        val entity = orderEntity(
            lineItems = """[
                {"id":1,"total":"10.00","taxes":[{"id":"AVATAX-TX-STATE-TAX","total":"0.63"}]},
                {"id":2,"total":"20.00","taxes":[{"id":1,"total":"1.25"}]}
            ]""",
        )

        val result = entity.getLineItemList()

        assertThat(result).hasSize(2)
        assertThat(result.map { it.total }).containsExactly("10.00", "20.00")
        assertThat(result[0].taxes?.map { it.rateId }).containsExactly(null)
        assertThat(result[0].taxes?.first()?.total).isEqualByComparingTo("0.63")
        assertThat(result[1].taxes?.map { it.rateId }).containsExactly(1L)
    }

    @Test
    fun `given a line item tax id is a numeric string, when getLineItemList, then the rate id is parsed`() = runTest {
        val entity = orderEntity(lineItems = """[{"id":1,"taxes":[{"id":"5","total":"1.00"}]}]""")

        val result = entity.getLineItemList()

        assertThat(result.single().taxes?.single()?.rateId).isEqualTo(5L)
    }

    @Test
    fun `given a shipping line tax id is a non-numeric string, when getShippingLineList, then the shipping line is returned`() = runTest {
        val entity = orderEntity(
            shippingLines = """[{"id":3,"total":"5.00","taxes":[{"id":"AVATAX-TX-STATE-TAX","total":"0.31"}]}]""",
        )

        val result = entity.getShippingLineList()

        assertThat(result).hasSize(1)
        assertThat(result.single().total).isEqualTo("5.00")
        assertThat(result.single().taxes?.single()?.rateId).isNull()
    }

    @Test
    fun `given a fee line tax id is a non-numeric string, when getFeeLineList, then the fee line is returned`() = runTest {
        val entity = orderEntity(
            feeLines = """[{"id":4,"total":"2.00","taxes":[{"id":"AVATAX-TX-STATE-TAX","total":"0.13"}]}]""",
        )

        val result = entity.getFeeLineList()

        assertThat(result).hasSize(1)
        assertThat(result.single().total).isEqualTo("2.00")
        assertThat(result.single().taxes?.single()?.rateId).isNull()
    }

    private fun orderEntity(
        giftCards: String = "",
        lineItems: String = "",
        shippingLines: String = "",
        feeLines: String = "",
    ) = OrderEntity(
        localSiteId = LocalId(1),
        orderId = 1L,
        giftCards = giftCards,
        lineItems = lineItems,
        shippingLines = shippingLines,
        feeLines = feeLines,
    )
}
