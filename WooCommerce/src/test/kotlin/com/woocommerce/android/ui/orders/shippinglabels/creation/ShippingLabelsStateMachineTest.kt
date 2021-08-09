package com.woocommerce.android.ui.orders.shippinglabels.creation

import com.nhaarman.mockitokotlin2.spy
import com.woocommerce.android.model.PackageDimensions
import com.woocommerce.android.model.ShippingLabelPackage
import com.woocommerce.android.model.ShippingLabelPackage.Item
import com.woocommerce.android.model.ShippingPackage
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.ui.orders.OrderTestUtils
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Event
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Event.DataLoaded
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.SideEffect
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.State
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.State.DataLoading
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.StateMachineData
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Step.CarrierStep
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Step.CustomsStep
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Step.OriginAddressStep
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Step.PackagingStep
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Step.PaymentsStep
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Step.ShippingAddressStep
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.StepStatus.DONE
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.StepStatus.NOT_READY
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.StepStatus.READY
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.StepsState
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Transition
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowDialog
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runBlockingTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

@ExperimentalCoroutinesApi
class ShippingLabelsStateMachineTest : BaseUnitTest() {
    private lateinit var stateMachine: ShippingLabelsStateMachine

    private val order = OrderTestUtils.generateOrder().toAppModel()
    private val originAddress = CreateShippingLabelTestUtils.generateAddress()
    private val shippingAddress = originAddress.copy(company = "McDonald's")
    private val data = StateMachineData(
        order,
        StepsState(
            originAddressStep = OriginAddressStep(READY, originAddress),
            shippingAddressStep = ShippingAddressStep(NOT_READY, shippingAddress),
            packagingStep = PackagingStep(NOT_READY, emptyList()),
            customsStep = CustomsStep(
                NOT_READY,
                isVisible = originAddress.country != shippingAddress.country,
                data = null
            ),
            carrierStep = CarrierStep(NOT_READY, emptyList()),
            paymentsStep = PaymentsStep(NOT_READY, null)
        )
    )

    @Before
    fun stateMachine() {
        stateMachine = spy(ShippingLabelsStateMachine())
    }

    @Test
    fun `Test the data login sequence of events after start`() = coroutinesTestRule.testDispatcher.runBlockingTest {
        val expectedSideEffectCount = 3 // necessary to terminate the flow
        var transition: Transition? = null
        launch {
            stateMachine.transitions.take(expectedSideEffectCount).collect {
                transition = it
            }
        }

        assertThat(transition?.sideEffect).isEqualTo(SideEffect.NoOp)

        stateMachine.start(order.remoteId.toString())

        assertThat(transition?.state).isEqualTo(State.DataLoading(order.remoteId.toString()))

        stateMachine.handleEvent(
            Event.DataLoaded(
                order,
                originAddress,
                shippingAddress,
                null
            )
        )

        assertThat(transition?.state).isEqualTo(State.WaitingForInput(data))
    }

    @Test
    fun `Test successful address verification`() = coroutinesTestRule.testDispatcher.runBlockingTest {
        val expectedSideEffectCount = 5 // necessary to terminate the flow
        var transition: Transition? = null
        launch {
            stateMachine.transitions.take(expectedSideEffectCount).collect {
                transition = it
            }
        }

        stateMachine.start(order.remoteId.toString())
        stateMachine.handleEvent(Event.DataLoaded(order, originAddress, shippingAddress, null))
        stateMachine.handleEvent(Event.OriginAddressValidationStarted)

        assertThat(transition?.state).isEqualTo(State.OriginAddressValidation(data))

        val newStepsState = data.stepsState.copy(
            originAddressStep = data.stepsState.originAddressStep.copy(status = DONE),
            shippingAddressStep = data.stepsState.shippingAddressStep.copy(status = READY)
        )
        val newData = data.copy(stepsState = newStepsState)
        stateMachine.handleEvent(Event.AddressValidated(data.stepsState.originAddressStep.data))

        assertThat(transition?.state).isEqualTo(State.WaitingForInput(newData))
    }

    @Test
    fun `test show packages step`() = coroutinesTestRule.testDispatcher.runBlockingTest {
        val packagesList = listOf(
            ShippingLabelPackage(
                position = 1,
                selectedPackage = ShippingPackage(
                    "id",
                    "title",
                    false,
                    "provider",
                    PackageDimensions(10.0f, 10.0f, 10.0f),
                    1f
                ),
                weight = 10.0f,
                items = listOf(Item(10L, "item", "attributes", 1, 10f, BigDecimal.valueOf(20)))
            )
        )

        stateMachine.start(order.toString())
        stateMachine.handleEvent(Event.DataLoaded(order, originAddress, shippingAddress, null))
        stateMachine.handleEvent(Event.OriginAddressValidationStarted)
        stateMachine.handleEvent(Event.AddressValidated(originAddress))
        stateMachine.handleEvent(Event.ShippingAddressValidationStarted)
        stateMachine.handleEvent(Event.AddressValidated(shippingAddress))
        stateMachine.handleEvent(Event.PackageSelectionStarted)

        assertThat(stateMachine.transitions.value.sideEffect).isEqualTo(SideEffect.ShowPackageOptions(emptyList()))

        stateMachine.handleEvent(Event.PackagesSelected(packagesList))

        val newStepsState = data.stepsState.copy(
            originAddressStep = data.stepsState.originAddressStep.copy(status = DONE),
            shippingAddressStep = data.stepsState.shippingAddressStep.copy(status = DONE),
            packagingStep = data.stepsState.packagingStep.copy(status = DONE, data = packagesList),
            carrierStep = data.stepsState.carrierStep.copy(status = READY)
        )
        val newData = data.copy(stepsState = newStepsState)

        assertThat(stateMachine.transitions.value.state).isEqualTo(State.WaitingForInput(newData))
    }

    @Test
    fun `when the origin address is a US military state, then require a customs form`() = testBlocking {
        val originAddress = originAddress.copy(
            state = "AA",
            country = "US"
        )
        stateMachine.start(order.identifier)
        stateMachine.handleEvent(DataLoaded(order, originAddress, shippingAddress, null))

        val machineState = stateMachine.transitions.value.state
        assertThat(machineState.data?.stepsState?.customsStep?.isVisible).isTrue
    }

    @Test
    fun `when the shipping address is a US military state, then require a customs form`() = testBlocking {
        val shippingAddress = shippingAddress.copy(
            state = "AA",
            country = "US"
        )
        stateMachine.start(order.identifier)
        stateMachine.handleEvent(DataLoaded(order, originAddress, shippingAddress, null))

        val machineState = stateMachine.transitions.value.state
        assertThat(machineState.data?.stepsState?.customsStep?.isVisible).isTrue
    }

    @Test
    fun `when the origin and shipping address have same country, then don't require a customs form`() = testBlocking {
        val originAddress = originAddress.copy(
            country = "UK"
        )
        val shippingAddress = shippingAddress.copy(
            country = "UK"
        )
        stateMachine.start(order.identifier)
        stateMachine.handleEvent(DataLoaded(order, originAddress, shippingAddress, null))

        val machineState = stateMachine.transitions.value.state
        assertThat(machineState.data?.stepsState?.customsStep?.isVisible).isFalse()
    }

    @Test
    fun `when the origin and shipping address have different countries, then require a customs form`() = testBlocking {
        val originAddress = originAddress.copy(
            country = "US"
        )
        val shippingAddress = shippingAddress.copy(
            country = "UK"
        )
        stateMachine.start(order.identifier)
        stateMachine.handleEvent(DataLoaded(order, originAddress, shippingAddress, null))

        val machineState = stateMachine.transitions.value.state
        assertThat(machineState.data?.stepsState?.customsStep?.isVisible).isTrue
    }
}
