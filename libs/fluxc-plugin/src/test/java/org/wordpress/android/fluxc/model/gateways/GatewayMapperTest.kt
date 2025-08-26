package org.wordpress.android.fluxc.model.gateways

import com.google.gson.Gson
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.network.rest.wpcom.wc.gateways.GatewayRestClient.GatewayResponse
import org.wordpress.android.fluxc.persistence.entity.GatewayEntity

class GatewayMapperTest {
    private lateinit var sut: GatewayMapper

    @Before
    fun setUp() {
        sut = GatewayMapper(Gson())
    }

    @Test
    fun `given fields are present, when toModel, then maps all `() {
        val response = GatewayResponse(
            gatewayId = "cod",
            title = "Cash on delivery",
            description = "Pay in person",
            order = "5",
            enabled = true,
            methodTitle = "COD",
            methodDescription = "Cash method",
            features = listOf("refunds", "products")
        )

        val model = sut.toModel(response)

        assertThat(model.id).isEqualTo("cod")
        assertThat(model.title).isEqualTo("Cash on delivery")
        assertThat(model.description).isEqualTo("Pay in person")
        assertThat(model.order).isEqualTo(5)
        assertThat(model.isEnabled).isTrue()
        assertThat(model.methodTitle).isEqualTo("COD")
        assertThat(model.methodDescription).isEqualTo("Cash method")
        assertThat(model.features).containsExactly("refunds", "products")
    }

    @Test
    fun `given fields are null, when toModel, then applies defaults`() {
        val response = GatewayResponse(
            gatewayId = "stripe",
            title = null,
            description = null,
            order = null,
            enabled = null,
            methodTitle = null,
            methodDescription = null,
            features = null
        )

        val model = sut.toModel(response)

        assertThat(model.id).isEqualTo("stripe")
        assertThat(model.title).isEmpty()
        assertThat(model.description).isEmpty()
        assertThat(model.order).isEqualTo(0)
        assertThat(model.isEnabled).isFalse()
        assertThat(model.methodTitle).isEmpty()
        assertThat(model.methodDescription).isEmpty()
        assertThat(model.features).isEmpty()
    }

    @Test
    fun `given non-numeric values, when toModel, then order parsing handles them as zero`() {
        val response = GatewayResponse(
            gatewayId = "dummy",
            title = null,
            description = null,
            order = "not_a_number",
            enabled = null,
            methodTitle = null,
            methodDescription = null,
            features = null
        )

        val model = sut.toModel(response)

        assertThat(model.order).isEqualTo(0)
    }

    @Test
    fun `given round trip, when toEntity and toModel(entity), then data is preserved`() {
        val siteId = LocalId(7)
        val response = GatewayResponse(
            gatewayId = "cod",
            title = "Cash on delivery",
            description = "Pay in person",
            order = "2",
            enabled = true,
            methodTitle = "COD",
            methodDescription = "Cash method",
            features = listOf("refunds")
        )

        val entity = sut.toEntity(siteId, response)
        // Sanity checks on entity structure
        assertThat(entity.siteId).isEqualTo(siteId)
        assertThat(entity.gatewayId).isEqualTo("cod")
        assertThat(entity.data).isNotEmpty()

        val modelFromEntity = sut.toModel(entity)
        val modelFromResponse = sut.toModel(response)

        assertThat(modelFromEntity).isEqualTo(modelFromResponse)
    }

    @Test
    fun `when toEntities, then maps list of responses`() {
        val siteId = LocalId(3)
        val responses = listOf(
            GatewayResponse(
                gatewayId = "cod",
                title = "Cash on delivery",
                description = null,
                order = null,
                enabled = true,
                methodTitle = null,
                methodDescription = null,
                features = null
            ),
            GatewayResponse(
                gatewayId = "stripe",
                title = "Stripe",
                description = null,
                order = "10",
                enabled = false,
                methodTitle = null,
                methodDescription = null,
                features = listOf("refunds")
            )
        )

        val entities = sut.toEntities(siteId, responses)

        assertThat(entities).hasSize(2)
        assertThat(entities.map { it.siteId }).containsOnly(siteId)
        assertThat(entities.map { it.gatewayId }).containsExactlyInAnyOrder("cod", "stripe")
        assertThat(entities.map { it.data }).allMatch { (it as String).isNotEmpty() }

        // Validate that models constructed from entities match models from responses
        val modelsFromEntities = entities.map { sut.toModel(it) }
        val modelsFromResponses = responses.map { sut.toModel(it) }
        assertThat(modelsFromEntities).isEqualTo(modelsFromResponses)
    }

    @Test
    fun `given JSON saved in GatewayEntity, when toModel(entity), then parses it successfully`() {
        val response = GatewayResponse(
            gatewayId = "custom",
            title = "Custom Title",
            description = "Desc",
            order = "1",
            enabled = true,
            methodTitle = "MethodTitle",
            methodDescription = "MethodDesc",
            features = listOf("feature1", "feature2")
        )
        val entity = GatewayEntity(
            siteId = LocalId(1),
            gatewayId = response.gatewayId,
            data = Gson().toJson(response)
        )

        val model = sut.toModel(entity)

        assertThat(model).isEqualTo(sut.toModel(response))
    }
}
