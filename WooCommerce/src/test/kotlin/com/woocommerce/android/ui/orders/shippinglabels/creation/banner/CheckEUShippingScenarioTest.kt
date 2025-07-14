package com.woocommerce.android.ui.orders.shippinglabels.creation.banner

import com.woocommerce.android.model.Address
import com.woocommerce.android.model.Location
import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.State.Idle
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.State.WaitingForInput
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.StateMachineData
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Step.CarrierStep
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Step.CustomsStep
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Step.OriginAddressStep
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Step.PackagingStep
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Step.PaymentsStep
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Step.ShippingAddressStep
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.StepStatus
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.TestScope
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import java.util.Date

@OptIn(ExperimentalCoroutinesApi::class)
internal class CheckEUShippingScenarioTest : BaseUnitTest() {
    lateinit var sut: CheckEUShippingScenario

    @Before
    fun setUp() {
        sut = CheckEUShippingScenario()
    }

    @Test
    fun `when origin country is NOT US, then emit false`() = testBlocking {
        // Given
        val transitions = generateExpectedTransitions(
            originCountryCode = "DE",
            destinationCountryCodePair = Pair("AT", "AUT")
        )
        val results = mutableListOf<Boolean>()

        // When
        sut.invoke(transitions)
            .onEach { results.add(it) }
            .launchIn(this)

        // Then
        assertThat(results).hasSize(2)
        assertThat(results[0]).isFalse
        assertThat(results[1]).isFalse
    }

    @Test
    fun `when origin country is US and destination country is not in EU new custom rules, then emit false`() = testBlocking {
        // Given
        val transitions = generateExpectedTransitions(
            originCountryCode = "DE",
            destinationCountryCodePair = Pair("BR", "BRA")
        )
        val results = mutableListOf<Boolean>()

        // When
        sut.invoke(transitions)
            .onEach { results.add(it) }
            .launchIn(this)

        // Then
        assertThat(results).hasSize(2)
        assertThat(results[0]).isFalse
        assertThat(results[1]).isFalse
    }

    @Test
    fun `when transition state is not WaitingForInput, then emit false`() = testBlocking {
        // Given
        val results = mutableListOf<Boolean>()

        // When
        sut.invoke(flowOf(ShippingLabelsStateMachine.Transition(Idle, null)))
            .onEach { results.add(it) }
            .launchIn(this)

        // Then
        assertThat(results).hasSize(1)
        assertThat(results[0]).isFalse
    }

    @Test
    fun `when origin country is US and destination country is AT-AUT then emit true`() = testBlocking {
        assertCheckSucceedsFor(Pair("AT", "AUT"))
    }

    @Test
    fun `when origin country is US and destination country is BE-BEL then emit true`() = testBlocking {
        assertCheckSucceedsFor(Pair("BE", "BEL"))
    }

    @Test
    fun `when origin country is US and destination country is BG-BGR then emit true`() = testBlocking {
        assertCheckSucceedsFor(Pair("BG", "BGR"))
    }

    @Test
    fun `when origin country is US and destination country is HR-HRV then emit true`() = testBlocking {
        assertCheckSucceedsFor(Pair("HR", "HRV"))
    }

    @Test
    fun `when origin country is US and destination country is CY-CYP then emit true`() = testBlocking {
        assertCheckSucceedsFor(Pair("CY", "CYP"))
    }

    @Test
    fun `when origin country is US and destination country is CZ-CZE then emit true`() = testBlocking {
        assertCheckSucceedsFor(Pair("CZ", "CZE"))
    }

    @Test
    fun `when origin country is US and destination country is DK-DNK then emit true`() = testBlocking {
        assertCheckSucceedsFor(Pair("DK", "DNK"))
    }

    @Test
    fun `when origin country is US and destination country is EE-EST then emit true`() = testBlocking {
        assertCheckSucceedsFor(Pair("EE", "EST"))
    }

    @Test
    fun `when origin country is US and destination country is FI-FIN then emit true`() = testBlocking {
        assertCheckSucceedsFor(Pair("FI", "FIN"))
    }

    @Test
    fun `when origin country is US and destination country is FR-FRA then emit true`() = testBlocking {
        assertCheckSucceedsFor(Pair("FR", "FRA"))
    }

    @Test
    fun `when origin country is US and destination country is DE-DEU then emit true`() = testBlocking {
        assertCheckSucceedsFor(Pair("DE", "DEU"))
    }

    @Test
    fun `when origin country is US and destination country is GR-GRC then emit true`() = testBlocking {
        assertCheckSucceedsFor(Pair("GR", "GRC"))
    }

    @Test
    fun `when origin country is US and destination country is HU-HUN then emit true`() = testBlocking {
        assertCheckSucceedsFor(Pair("HU", "HUN"))
    }

    @Test
    fun `when origin country is US and destination country is IE-IRL then emit true`() = testBlocking {
        assertCheckSucceedsFor(Pair("IE", "IRL"))
    }

    @Test
    fun `when origin country is US and destination country is IT-ITA then emit true`() = testBlocking {
        assertCheckSucceedsFor(Pair("IT", "ITA"))
    }

    @Test
    fun `when origin country is US and destination country is LV-LVA then emit true`() = testBlocking {
        assertCheckSucceedsFor(Pair("LV", "LVA"))
    }

    @Test
    fun `when origin country is US and destination country is LT-LTU then emit true`() = testBlocking {
        assertCheckSucceedsFor(Pair("LT", "LTU"))
    }

    @Test
    fun `when origin country is US and destination country is LU-LUX then emit true`() = testBlocking {
        assertCheckSucceedsFor(Pair("LU", "LUX"))
    }

    @Test
    fun `when origin country is US and destination country is MT-MLT then emit true`() = testBlocking {
        assertCheckSucceedsFor(Pair("MT", "MLT"))
    }

    @Test
    fun `when origin country is US and destination country is NL-NLD then emit true`() = testBlocking {
        assertCheckSucceedsFor(Pair("NL", "NLD"))
    }

    @Test
    fun `when origin country is US and destination country is NO-NOR then emit true`() = testBlocking {
        assertCheckSucceedsFor(Pair("NO", "NOR"))
    }

    @Test
    fun `when origin country is US and destination country is PL-POL then emit true`() = testBlocking {
        assertCheckSucceedsFor(Pair("PL", "POL"))
    }

    @Test
    fun `when origin country is US and destination country is PT-PRT then emit true`() = testBlocking {
        assertCheckSucceedsFor(Pair("PT", "PRT"))
    }

    @Test
    fun `when origin country is US and destination country is RO-ROU then emit true`() = testBlocking {
        assertCheckSucceedsFor(Pair("RO", "ROU"))
    }

    @Test
    fun `when origin country is US and destination country is SK-SVK then emit true`() = testBlocking {
        assertCheckSucceedsFor(Pair("SK", "SVK"))
    }

    @Test
    fun `when origin country is US and destination country is SI-SVN then emit true`() = testBlocking {
        assertCheckSucceedsFor(Pair("SI", "SVN"))
    }

    @Test
    fun `when origin country is US and destination country is ES-ESP then emit true`() = testBlocking {
        assertCheckSucceedsFor(Pair("ES", "ESP"))
    }

    @Test
    fun `when origin country is US and destination country is SE-SWE then emit true`() = testBlocking {
        assertCheckSucceedsFor(Pair("SE", "SWE"))
    }

    @Test
    fun `when origin country is US and destination country is CH-CHE then emit true`() = testBlocking {
        assertCheckSucceedsFor(Pair("CH", "CHE"))
    }

    private fun TestScope.assertCheckSucceedsFor(
        countryCodes: Pair<String, String>
    ) {
        // Given
        val transitions = generateExpectedTransitions(
            originCountryCode = "US",
            destinationCountryCodePair = countryCodes
        )
        val results = mutableListOf<Boolean>()

        // When
        sut.invoke(transitions)
            .onEach { results.add(it) }
            .launchIn(this)

        // Then
        assertThat(results).hasSize(2)
        assertThat(results[0]).isTrue
        assertThat(results[1]).isTrue
    }

    private fun generateExpectedTransitions(
        originCountryCode: String,
        destinationCountryCodePair: Pair<String, String>
    ) = flowOf(
        generateTransitionState(originCountryCode, destinationCountryCodePair.first),
        generateTransitionState(originCountryCode, destinationCountryCodePair.second)
    )

    private fun generateTransitionState(
        originCountryCode: String,
        destinationCountryCode: String
    ) = WaitingForInput(
        data = StateMachineData(
            order = Order.getEmptyOrder(Date(), Date()),
            stepsState = emptyStepState.copy(
                originAddressStep = OriginAddressStep(
                    StepStatus.READY,
                    Address.EMPTY.copy(country = Location.EMPTY.copy(code = originCountryCode))
                ),
                shippingAddressStep = ShippingAddressStep(
                    StepStatus.READY,
                    Address.EMPTY.copy(country = Location.EMPTY.copy(code = destinationCountryCode))
                )
            )
        )
    ).let { ShippingLabelsStateMachine.Transition(it, null) }

    private val emptyStepState = ShippingLabelsStateMachine.StepsState(
        originAddressStep = OriginAddressStep(StepStatus.NOT_READY, Address.EMPTY),
        shippingAddressStep = ShippingAddressStep(StepStatus.NOT_READY, Address.EMPTY),
        packagingStep = PackagingStep(StepStatus.NOT_READY, emptyList()),
        customsStep = CustomsStep(StepStatus.NOT_READY, false, null),
        carrierStep = CarrierStep(StepStatus.NOT_READY, emptyList()),
        paymentsStep = PaymentsStep(StepStatus.NOT_READY, null)
    )
}
