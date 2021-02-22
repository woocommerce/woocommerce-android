package com.woocommerce.android.ui.orders.shippinglabels.creation

import com.nhaarman.mockitokotlin2.spy
import com.woocommerce.android.model.PackageDimensions
import com.woocommerce.android.model.ShippingLabelPackage
import com.woocommerce.android.model.ShippingLabelPackage.Item
import com.woocommerce.android.model.ShippingPackage
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelAddressValidator.AddressType.ORIGIN
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Data
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Event
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.FlowStep
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.SideEffect
import com.woocommerce.android.util.CoroutineTestRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runBlockingTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class ShippingLabelsStateMachineTest {
    private lateinit var stateMachine: ShippingLabelsStateMachine

    private val orderId = "123"
    private val originAddress = CreateShippingLabelTestUtils.generateAddress()
    private val shippingAddress = originAddress.copy(company = "McDonald's")
    private val data = Data(originAddress, shippingAddress, emptyList(), setOf(FlowStep.ORIGIN_ADDRESS))

    @get:Rule
    var coroutinesTestRule = CoroutineTestRule()

    @Before
    fun stateMachine() {
        stateMachine = spy(ShippingLabelsStateMachine())
    }

    @Test
    fun `Test the data login sequence of events after start`() = coroutinesTestRule.testDispatcher.runBlockingTest {
        val expectedSideEffectCount = 3 // necessary to terminate the flow
        var sideEffect: SideEffect? = null
        launch {
            stateMachine.transitions.take(expectedSideEffectCount).collect {
                sideEffect = it.sideEffect
            }
        }

        assertThat(sideEffect).isEqualTo(SideEffect.NoOp)

        stateMachine.start(orderId)

        assertThat(sideEffect).isEqualTo(SideEffect.LoadData(orderId))

        stateMachine.handleEvent(Event.DataLoaded(originAddress, shippingAddress))

        assertThat(sideEffect).isEqualTo(SideEffect.UpdateViewState(data))
    }

    @Test
    fun `Test successful address verification`() = coroutinesTestRule.testDispatcher.runBlockingTest {
        val expectedSideEffectCount = 5 // necessary to terminate the flow
        var sideEffect: SideEffect? = null
        launch {
            stateMachine.transitions.take(expectedSideEffectCount).collect {
                sideEffect = it.sideEffect
            }
        }

        stateMachine.start(orderId)
        stateMachine.handleEvent(Event.DataLoaded(originAddress, shippingAddress))
        stateMachine.handleEvent(Event.OriginAddressValidationStarted)

        assertThat(sideEffect).isEqualTo(SideEffect.ValidateAddress(data.originAddress, ORIGIN))

        val newData = data.copy(
            originAddress = data.originAddress,
            flowSteps = data.flowSteps + FlowStep.SHIPPING_ADDRESS
        )
        stateMachine.handleEvent(Event.AddressValidated(data.originAddress))

        assertThat(sideEffect).isEqualTo(SideEffect.UpdateViewState(newData))
    }

    @Test
    fun `test show packages step`() = coroutinesTestRule.testDispatcher.runBlockingTest {
        val packagesList = listOf(
            ShippingLabelPackage(
                selectedPackage = ShippingPackage(
                    "id",
                    "title",
                    false,
                    "provider",
                    PackageDimensions(10.0, 10.0, 10.0)
                ),
                weight = 10.0,
                items = listOf(Item(10L, "item", "attributes", "10kgs"))
            )
        )

        stateMachine.start(orderId)
        stateMachine.handleEvent(Event.DataLoaded(originAddress, shippingAddress))
        stateMachine.handleEvent(Event.PackageSelectionStarted)

        assertThat(stateMachine.transitions.value.sideEffect).isEqualTo(SideEffect.ShowPackageOptions(emptyList()))

        stateMachine.handleEvent(Event.PackagesSelected(packagesList))

        val newData = data.copy(
            shippingPackages = packagesList,
            flowSteps = data.flowSteps + FlowStep.CUSTOMS
        )

        assertThat(stateMachine.transitions.value.sideEffect).isEqualTo(SideEffect.UpdateViewState(newData))
    }
}
