package org.wordpress.android.fluxc.network.rest.wpcom.wc.order

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.AdditionalAnswers
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.wordpress.android.fluxc.model.LocalOrRemoteId
import org.wordpress.android.fluxc.persistence.entity.OrderEntity

internal class OrderDtoMapperTest {
    private val stripOrder: StripOrder = mock {
        on { invoke(any()) }.then(AdditionalAnswers.returnsFirstArg<OrderEntity>())
    }
    private val stripOrderMetaData: StripOrderMetaData = mock()

    val localSiteId = LocalOrRemoteId.LocalId(0)
    val sut = OrderDtoMapper(stripOrder, stripOrderMetaData)

    @Test
    fun `when is_editable is NULL and the status is an editable status the order in Editable`() = runTest {
        for (status in OrderDtoMapper.EDITABLE_STATUSES) {
            val oldOrderDTO = getEditableOrder(
                isEditable = null,
                status = status
            )
            val (orderEntity, _) = sut.toDatabaseEntity(oldOrderDTO, localSiteId)
            assertThat(orderEntity.isEditable).isEqualTo(true)
        }
    }

    @Test
    fun `when is_editable is NULL and the status is not an editable status the order is not Editable`() = runTest {
        val oldOrderDTO = getEditableOrder(isEditable = null, status = "")
        val (orderEntity, _) = sut.toDatabaseEntity(oldOrderDTO, localSiteId)
        assertThat(orderEntity.isEditable).isEqualTo(false)
    }

    @Test
    fun `when is_editable field is true the order is Editable`() = runTest {
        val oldOrderDTO = getEditableOrder(isEditable = true, status = "")
        val (orderEntity, _) = sut.toDatabaseEntity(oldOrderDTO, localSiteId)
        assertThat(orderEntity.isEditable).isEqualTo(true)
    }

    @Test
    fun `when is_editable field is false the order is not Editable`() = runTest {
        // We only check for editable statuses if the is_editable field is null
        for (status in OrderDtoMapper.EDITABLE_STATUSES) {
            val oldOrderDTO = getEditableOrder(isEditable = false, status = status)
            val (orderEntity, _) = sut.toDatabaseEntity(oldOrderDTO, localSiteId)
            assertThat(orderEntity.isEditable).isEqualTo(false)
        }
    }

    private fun getEditableOrder(
        isEditable: Boolean?,
        status: String = "auto-draft"
    ): OrderDto {
        val json = JsonObject().apply {
            addProperty("status", status)
            isEditable?.let { value -> addProperty("is_editable", value) }
        }
        return Gson().fromJson(json, OrderDto::class.java)
    }

    @Test
    fun `when needs_payment is not in json the order dto needs_payment property is null`() = runTest {
        val json = JsonObject()
        val orderDto = Gson().fromJson(json, OrderDto::class.java)
        val (orderEntity, _) = sut.toDatabaseEntity(orderDto, localSiteId)

        assertThat(orderEntity.needsPayment).isNull()
    }

    @Test
    fun `when needs_payment is in json the order dto needs_payment property is parsed`() = runTest {
        val json = JsonObject().apply {
            addProperty("needs_payment", "true")
        }
        val orderDto = Gson().fromJson(json, OrderDto::class.java)
        val (orderEntity, _) = sut.toDatabaseEntity(orderDto, localSiteId)

        assertThat(orderEntity.needsPayment).isTrue()
    }

    @Test
    fun `when gift_cards is in json, then the order entity stores the gift cards json`() = runTest {
        val giftCard = JsonObject().apply {
            addProperty("id", 4)
            addProperty("code", "NZR8-BMP8-XJZ2-ZKS9")
            addProperty("amount", 18)
        }
        val json = JsonObject().apply {
            add("gift_cards", JsonArray().apply { add(giftCard) })
        }
        val orderDto = Gson().fromJson(json, OrderDto::class.java)
        val (orderEntity, _) = sut.toDatabaseEntity(orderDto, localSiteId)

        assertThat(orderEntity.giftCards).contains("NZR8-BMP8-XJZ2-ZKS9")
    }

    @Test
    fun `when needs_processing is not in json the order dto needs_processing property is null`() = runTest {
        val json = JsonObject()
        val orderDto = Gson().fromJson(json, OrderDto::class.java)
        val (orderEntity, _) = sut.toDatabaseEntity(orderDto, localSiteId)

        assertThat(orderEntity.needsProcessing).isNull()
    }

    @Test
    fun `when needs_processing is in json the order dto needs_processing property is parsed`() = runTest {
        val json = JsonObject().apply {
            addProperty("needs_processing", "true")
        }
        val orderDto = Gson().fromJson(json, OrderDto::class.java)
        val (orderEntity, _) = sut.toDatabaseEntity(orderDto, localSiteId)

        assertThat(orderEntity.needsProcessing).isTrue()
    }

    @Test
    fun `given shipping_tax is absent in json when mapping then orderEntity shippingTax is empty string`() = runTest {
        val json = JsonObject()
        val orderDto = Gson().fromJson(json, OrderDto::class.java)
        val (orderEntity, _) = sut.toDatabaseEntity(orderDto, localSiteId)

        assertThat(orderEntity.shippingTax).isEmpty()
    }

    @Test
    fun `given shipping_tax is present in json when mapping then orderEntity shippingTax is parsed`() = runTest {
        val json = JsonObject().apply {
            addProperty("shipping_tax", "10.00")
        }
        val orderDto = Gson().fromJson(json, OrderDto::class.java)
        val (orderEntity, _) = sut.toDatabaseEntity(orderDto, localSiteId)

        assertThat(orderEntity.shippingTax).isEqualTo("10.00")
    }

    @Test
    fun `given created_via is absent in json when mapping then orderEntity createdVia is empty string`() = runTest {
        val json = JsonObject()
        val orderDto = Gson().fromJson(json, OrderDto::class.java)
        val (orderEntity, _) = sut.toDatabaseEntity(orderDto, localSiteId)

        assertThat(orderEntity.createdVia).isEmpty()
    }

    @Test
    fun `given created_via is present in json when mapping then orderEntity createdVia is parsed`() = runTest {
        val json = JsonObject().apply {
            addProperty("created_via", "pos-rest-api")
        }
        val orderDto = Gson().fromJson(json, OrderDto::class.java)
        val (orderEntity, _) = sut.toDatabaseEntity(orderDto, localSiteId)

        assertThat(orderEntity.createdVia).isEqualTo("pos-rest-api")
    }

    @Test
    fun `given created_via is rest-api in json when mapping then orderEntity createdVia is parsed`() = runTest {
        val json = JsonObject().apply {
            addProperty("created_via", "rest-api")
        }
        val orderDto = Gson().fromJson(json, OrderDto::class.java)
        val (orderEntity, _) = sut.toDatabaseEntity(orderDto, localSiteId)

        assertThat(orderEntity.createdVia).isEqualTo("rest-api")
    }
}
