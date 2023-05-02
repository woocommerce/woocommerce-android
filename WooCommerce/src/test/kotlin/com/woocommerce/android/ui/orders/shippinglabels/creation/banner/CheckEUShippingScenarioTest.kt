package com.woocommerce.android.ui.orders.shippinglabels.creation.banner

import com.woocommerce.android.model.Address
import com.woocommerce.android.model.Location
import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine
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
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class CheckEUShippingScenarioTest: BaseUnitTest() {
    lateinit var sut: CheckEUShippingScenario

    @Before
    fun setUp() {
        sut = CheckEUShippingScenario()
    }

    @Test
    fun `when origin country is US and destination country is AT then emit true`() = testBlocking {
        // Given
        val transitions = generateExpectedTransitions(
            originCountryCode = "US",
            destinationCountryCodePair = Pair("AT", "AUT")
        )
        val results = mutableListOf<Boolean>()

        // When
        sut.invoke(transitions)
            .onEach { results.add(it) }
            .launchIn(this)

        // Then
        assertThat(results).hasSize(2)
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
                order = Order.EMPTY,
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
