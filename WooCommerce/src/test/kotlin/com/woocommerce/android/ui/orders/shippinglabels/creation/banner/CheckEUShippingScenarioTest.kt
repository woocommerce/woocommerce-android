package com.woocommerce.android.ui.orders.shippinglabels.creation.banner

import com.woocommerce.android.model.Address
import com.woocommerce.android.model.Location
import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine
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
import kotlinx.coroutines.flow.single
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
    fun `when origin country is US and destination country is EU then emit true`() = testBlocking {
        // Given
        val originCountry = "US"
        val destinationCountry = "AT"
        val state = ShippingLabelsStateMachine.State.WaitingForInput(
            data = ShippingLabelsStateMachine.StateMachineData(
                order = Order.EMPTY,
                stepsState = emptyStepState.copy(
                    originAddressStep = OriginAddressStep(
                        StepStatus.READY,
                        Address.EMPTY.copy(country = Location.EMPTY.copy(code = originCountry))
                    ),
                    shippingAddressStep = ShippingAddressStep(
                        StepStatus.READY,
                        Address.EMPTY.copy(country = Location.EMPTY.copy(code = destinationCountry))
                    )
                )
            )
        )

        // When
        val result = sut.invoke(
            flowOf(ShippingLabelsStateMachine.Transition(state, null))
        ).single()

        // Then
        assert(result)
    }

    private val emptyStepState = ShippingLabelsStateMachine.StepsState(
        originAddressStep = OriginAddressStep(StepStatus.NOT_READY, Address.EMPTY),
        shippingAddressStep = ShippingAddressStep(StepStatus.NOT_READY, Address.EMPTY),
        packagingStep = PackagingStep(StepStatus.NOT_READY, emptyList()),
        customsStep = CustomsStep(StepStatus.NOT_READY, false, null),
        carrierStep = CarrierStep(StepStatus.NOT_READY, emptyList()),
        paymentsStep = PaymentsStep(StepStatus.NOT_READY, null)
    )

    private val expectedIncludedCountries = listOf(
        Pair("AT", "AUT"),
        Pair("BE", "BEL"),
        Pair("BG", "BGR"),
        Pair("HR", "HRV"),
        Pair("CY", "CYP"),
        Pair("CZ", "CZE"),
        Pair("DK", "DNK"),
        Pair("EE", "EST"),
        Pair("FI", "FIN"),
        Pair("FR", "FRA"),
        Pair("DE", "DEU"),
        Pair("GR", "GRC"),
        Pair("HU", "HUN"),
        Pair("IE", "IRL"),
        Pair("IT", "ITA"),
        Pair("LV", "LVA"),
        Pair("LT", "LTU"),
        Pair("LU", "LUX"),
        Pair("MT", "MLT"),
        Pair("NL", "NLD"),
        Pair("NO", "NOR"),
        Pair("PL", "POL"),
        Pair("PT", "PRT"),
        Pair("RO", "ROU"),
        Pair("SK", "SVK"),
        Pair("SI", "SVN"),
        Pair("ES", "ESP"),
        Pair("SE", "SWE"),
        Pair("CH", "CHE")
    )

}
