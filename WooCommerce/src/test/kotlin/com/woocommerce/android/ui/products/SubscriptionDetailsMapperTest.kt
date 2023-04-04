package com.woocommerce.android.ui.products

import com.woocommerce.android.model.SubscriptionDetailsMapper
import com.woocommerce.android.model.SubscriptionPeriod
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
class SubscriptionDetailsMapperTest : BaseUnitTest() {

    @Test
    fun `when metadata is valid then a SubscriptionDetails is returned`() {
        val result = SubscriptionDetailsMapper.toAppModel(successMetadata)
        assertThat(result).isNotNull
        result?.let { subscription ->
            assertThat(subscription.price).isEqualTo(BigDecimal.valueOf(60))
            assertThat(subscription.period).isEqualTo(SubscriptionPeriod.fromValue("month"))
            assertThat(subscription.periodInterval).isEqualTo(1)
            assertThat(subscription.length).isNull()
            assertThat(subscription.signUpFee).isEqualTo(BigDecimal.valueOf(5))
            assertThat(subscription.trialPeriod).isEqualTo(SubscriptionPeriod.fromValue("day"))
            assertThat(subscription.trialLength).isEqualTo(2)
            assertThat(subscription.oneTimeShipping).isEqualTo(true)
        }
    }

    @Test
    fun `when metadata has no subscription keys then null is returned`() {
        val result = SubscriptionDetailsMapper.toAppModel(noSubscriptionKeysMetadata)
        assertThat(result).isNull()
    }

    @Test
    fun `when metadata miss some keys then a SubscriptionDetails is returned using default values`() {
        val result = SubscriptionDetailsMapper.toAppModel(successMetadataPartial)
        assertThat(result).isNotNull
        result?.let { subscription ->
            assertThat(subscription.price).isEqualTo(BigDecimal.valueOf(60))
            assertThat(subscription.period).isEqualTo(SubscriptionPeriod.fromValue(""))
            assertThat(subscription.periodInterval).isEqualTo(0)
            assertThat(subscription.length).isNull()
            assertThat(subscription.signUpFee).isEqualTo(BigDecimal.valueOf(5))
            assertThat(subscription.trialPeriod).isNull()
            assertThat(subscription.trialLength).isEqualTo(2)
            assertThat(subscription.oneTimeShipping).isEqualTo(false)
        }
    }

    /**
     *  price = 60,
     *  period = month,
     *  periodInterval = 1,
     *  length = 0,
     *  signUpFee = 5,
     *  trialPeriod = day,
     *  trialLength = 2,
     *  oneTimeShipping = yes
     */
    private val successMetadata = """ [ {
                "id": 5182,
                "key": "_subscription_payment_sync_date",
                "value": "0"
            },
            {
                "id": 5183,
                "key": "_subscription_price",
                "value": "60"
            },
            {
                "id": 5187,
                "key": "_subscription_trial_length",
                "value": "2"
            },
            {
                "id": 5188,
                "key": "_subscription_sign_up_fee",
                "value": "5"
            },
            {
                "id": 5189,
                "key": "_subscription_period",
                "value": "month"
            },
            {
                "id": 5190,
                "key": "_subscription_period_interval",
                "value": "1"
            },
            {
                "id": 5191,
                "key": "_subscription_length",
                "value": "0"
            },
            {
                "id": 5192,
                "key": "_subscription_trial_period",
                "value": "day"
            },
            {
                "id": 5193,
                "key": "_subscription_limit",
                "value": "no"
            },
            {
                "id": 5194,
                "key": "_subscription_one_time_shipping",
                "value": "yes"
            } ]
            """

    /**
     *  Metadata with no subscription key
     */
    private val noSubscriptionKeysMetadata = """ [ {
                "id": 5182,
                "key": "sync_date",
                "value": "0"
            },
            {
                "id": 5183,
                "key": "price",
                "value": "60"
            },
            {
                "id": 5187,
                "key": "trial_length",
                "value": "2"
            }]
            """

    /**
     *  price = 60,
     *  period = ,
     *  periodInterval = ,
     *  length = ,
     *  signUpFee = 5,
     *  trialPeriod = ,
     *  trialLength = 2,
     *  oneTimeShipping =
     */
    private val successMetadataPartial = """ [ {
                "id": 5182,
                "key": "_subscription_payment_sync_date",
                "value": "0"
            },
            {
                "id": 5183,
                "key": "_subscription_price",
                "value": "60"
            },
            {
                "id": 5187,
                "key": "_subscription_trial_length",
                "value": "2"
            },
            {
                "id": 5188,
                "key": "_subscription_sign_up_fee",
                "value": "5"
            }]
            """
}
