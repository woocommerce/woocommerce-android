package com.woocommerce.android.ui.orders.shippinglabels.creation

import com.nhaarman.mockitokotlin2.spy
import com.woocommerce.android.model.Address
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
    private val originAddress = Address(
        company = "KFC",
        firstName = "Harland",
        lastName = "Sanders",
        phone = "12345678",
        country = "US",
        state = "KY",
        address1 = "123 Main St.",
        address2 = "",
        city = "Lexington",
        postcode = "11222",
        email = "boss@kfc.com"
    )
    private val shippingAddress = originAddress.copy(company = "McDonald's")
    private val data = Data(originAddress, shippingAddress, setOf(FlowStep.ORIGIN_ADDRESS))

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
            stateMachine.effects.take(expectedSideEffectCount).collect {
                sideEffect = it
            }
        }

        assertThat(sideEffect).isEqualTo(SideEffect.NoOp)

        stateMachine.start(orderId)

        assertThat(sideEffect).isEqualTo(SideEffect.LoadData(orderId))

        stateMachine.handleEvent(Event.DataLoaded(originAddress, shippingAddress))

        assertThat(sideEffect).isEqualTo(SideEffect.UpdateViewState(data))
    }
}
