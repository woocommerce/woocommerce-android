package com.woocommerce.android.ui.orders.shippinglabels.creation

import com.woocommerce.android.model.*
import com.woocommerce.android.model.ShippingLabelPackage.Item
import com.woocommerce.android.ui.orders.OrderTestUtils
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.*
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Event.DataLoaded
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Step.*
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.StepStatus.*
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runBlockingTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import java.math.BigDecimal

@ExperimentalCoroutinesApi
class ShippingLabelsStateMachineTest : BaseUnitTest() {
    private lateinit var stateMachine: ShippingLabelsStateMachine

    private val orderMapper = OrderMapper(
        getLocations = mock {
            on { invoke(any(), any()) } doReturn (Location.EMPTY to AmbiguousLocation.EMPTY)
        }
    )
    private val order = OrderTestUtils.generateOrder().let { orderMapper.toAppModel(it) }
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

        stateMachine.start(order.id)

        assertThat(transition?.state).isEqualTo(State.DataLoading(order.id))

        stateMachine.handleEvent(
            DataLoaded(
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

        stateMachine.start(order.id)
        stateMachine.handleEvent(DataLoaded(order, originAddress, shippingAddress, null))
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

        stateMachine.start(order.id)
        stateMachine.handleEvent(DataLoaded(order, originAddress, shippingAddress, null))
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
            state = AmbiguousLocation.Raw("AA"),
            country = Location("US", "US")
        )
        stateMachine.start(order.id)
        stateMachine.handleEvent(DataLoaded(order, originAddress, shippingAddress, null))

        val machineState = stateMachine.transitions.value.state
        assertThat(machineState.data?.stepsState?.customsStep?.isVisible).isTrue
    }

    @Test
    fun `when the shipping address is a US military state, then require a customs form`() = testBlocking {
        val shippingAddress = shippingAddress.copy(
            state = AmbiguousLocation.Raw("AA"),
            country = Location("US", "US")
        )
        stateMachine.start(order.id)
        stateMachine.handleEvent(DataLoaded(order, originAddress, shippingAddress, null))

        val machineState = stateMachine.transitions.value.state
        assertThat(machineState.data?.stepsState?.customsStep?.isVisible).isTrue
    }

    @Test
    fun `when the origin and shipping address have same country, then don't require a customs form`() = testBlocking {
        val originAddress = originAddress.copy(
            country = Location("UK", "UK")
        )
        val shippingAddress = shippingAddress.copy(
            country = Location("UK", "UK")
        )
        stateMachine.start(order.id)
        stateMachine.handleEvent(DataLoaded(order, originAddress, shippingAddress, null))

        val machineState = stateMachine.transitions.value.state
        assertThat(machineState.data?.stepsState?.customsStep?.isVisible).isFalse()
    }

    @Test
    fun `when the origin and shipping address have different countries, then require a customs form`() = testBlocking {
        val originAddress = originAddress.copy(
            country = Location("US", "US")
        )
        val shippingAddress = shippingAddress.copy(
            country = Location("UK", "UK")
        )
        stateMachine.start(order.id)
        stateMachine.handleEvent(DataLoaded(order, originAddress, shippingAddress, null))

        val machineState = stateMachine.transitions.value.state
        assertThat(machineState.data?.stepsState?.customsStep?.isVisible).isTrue
    }
}
