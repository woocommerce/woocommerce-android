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

    private fun orderEntity(giftCards: String) = OrderEntity(
        localSiteId = LocalId(1),
        orderId = 1L,
        giftCards = giftCards,
    )
}
