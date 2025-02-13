package org.wordpress.android.fluxc.network.rest.wpcom.wc.order

import com.google.gson.Gson
import com.google.gson.JsonObject
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.AdditionalAnswers
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.wordpress.android.fluxc.model.LocalOrRemoteId
import org.wordpress.android.fluxc.model.OrderEntity

internal class OrderDtoMapperTest {
    private val stripOrder: StripOrder = mock {
        on { invoke(any()) }.then(AdditionalAnswers.returnsFirstArg<OrderEntity>())
    }
    private val stripOrderMetaData: StripOrderMetaData = mock()

    val localSiteId = LocalOrRemoteId.LocalId(0)
    val sut = OrderDtoMapper(stripOrder, stripOrderMetaData)

    @Test
    fun `when is_editable is NULL and the status is an editable status the order in Editable`() {
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
    fun `when is_editable is NULL and the status is not an editable status the order is not Editable`() {
        val oldOrderDTO = getEditableOrder(isEditable = null, status = "")
        val (orderEntity, _) = sut.toDatabaseEntity(oldOrderDTO, localSiteId)
        assertThat(orderEntity.isEditable).isEqualTo(false)
    }

    @Test
    fun `when is_editable field is true the order is Editable`() {
        val oldOrderDTO = getEditableOrder(isEditable = true, status = "")
        val (orderEntity, _) = sut.toDatabaseEntity(oldOrderDTO, localSiteId)
        assertThat(orderEntity.isEditable).isEqualTo(true)
    }

    @Test
    fun `when is_editable field is false the order is not Editable`() {
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
    fun `when needs_payment is not in json the order dto needs_payment property is null`() {
        val json = JsonObject()
        val orderDto = Gson().fromJson(json, OrderDto::class.java)
        val (orderEntity, _) = sut.toDatabaseEntity(orderDto, localSiteId)

        assertThat(orderEntity.needsPayment).isNull()
    }

    @Test
    fun `when needs_payment is in json the order dto needs_payment property is parsed`() {
        val json = JsonObject().apply {
            addProperty("needs_payment", "true")
        }
        val orderDto = Gson().fromJson(json, OrderDto::class.java)
        val (orderEntity, _) = sut.toDatabaseEntity(orderDto, localSiteId)

        assertThat(orderEntity.needsPayment).isTrue()
    }

    @Test
    fun `when needs_processing is not in json the order dto needs_processing property is null`() {
        val json = JsonObject()
        val orderDto = Gson().fromJson(json, OrderDto::class.java)
        val (orderEntity, _) = sut.toDatabaseEntity(orderDto, localSiteId)

        assertThat(orderEntity.needsProcessing).isNull()
    }

    @Test
    fun `when needs_processing is in json the order dto needs_processing property is parsed`() {
        val json = JsonObject().apply {
            addProperty("needs_processing", "true")
        }
        val orderDto = Gson().fromJson(json, OrderDto::class.java)
        val (orderEntity, _) = sut.toDatabaseEntity(orderDto, localSiteId)

        assertThat(orderEntity.needsProcessing).isTrue()
    }

    @Test
    fun `given shipping_tax is absent in json when mapping then orderEntity shippingTax is empty string`() {
        val json = JsonObject()
        val orderDto = Gson().fromJson(json, OrderDto::class.java)
        val (orderEntity, _) = sut.toDatabaseEntity(orderDto, localSiteId)

        assertThat(orderEntity.shippingTax).isEmpty()
    }

    @Test
    fun `given shipping_tax is present in json when mapping then orderEntity shippingTax is parsed`() {
        val json = JsonObject().apply {
            addProperty("shipping_tax", "10.00")
        }
        val orderDto = Gson().fromJson(json, OrderDto::class.java)
        val (orderEntity, _) = sut.toDatabaseEntity(orderDto, localSiteId)

        assertThat(orderEntity.shippingTax).isEqualTo("10.00")
    }
}
