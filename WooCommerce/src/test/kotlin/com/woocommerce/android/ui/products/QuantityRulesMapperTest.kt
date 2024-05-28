package com.woocommerce.android.ui.products

import com.google.gson.Gson
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class QuantityRulesMapperTest : BaseUnitTest() {
    private val sut = QuantityRulesMapper(Gson())

    @Test
    fun `when product metadata has valid quantity rules keys then a QuantityRules is returned`() {
        val result = sut.toAppModelFromProductMetadata(successProductMetadata)
        Assertions.assertThat(result).isNotNull
        result?.let { quantityRules ->
            Assertions.assertThat(quantityRules.min).isEqualTo(4)
            Assertions.assertThat(quantityRules.max).isEqualTo(20)
            Assertions.assertThat(quantityRules.groupOf).isEqualTo(2)
        }
    }

    @Test
    fun `when product metadata has no quantity rules keys then null is returned`() {
        val result = sut.toAppModelFromProductMetadata(noQuantityRulesKeysMetadata)
        Assertions.assertThat(result).isNull()
    }

    @Test
    fun `when product metadata miss some keys then a QuantityRules is returned using null to fill missing values`() {
        val result = sut.toAppModelFromProductMetadata(successProductMetadataPartial)
        Assertions.assertThat(result).isNotNull
        result?.let { quantityRules ->
            Assertions.assertThat(quantityRules.min).isNull()
            Assertions.assertThat(quantityRules.max).isEqualTo(20)
            Assertions.assertThat(quantityRules.groupOf).isNull()
        }
    }

    @Test
    fun `when variation metadata has valid quantity rules keys then a QuantityRules is returned`() {
        val result = sut.toAppModelFromVariationMetadata(successVariationMetadata)
        Assertions.assertThat(result).isNotNull
        result?.let { quantityRules ->
            Assertions.assertThat(quantityRules.min).isEqualTo(6)
            Assertions.assertThat(quantityRules.max).isEqualTo(30)
            Assertions.assertThat(quantityRules.groupOf).isEqualTo(3)
        }
    }

    @Test
    fun `when variation metadata has no quantity rules keys then null is returned`() {
        val result = sut.toAppModelFromVariationMetadata(noQuantityRulesKeysMetadata)
        Assertions.assertThat(result).isNull()
    }

    @Test
    fun `when variation metadata miss some keys then a QuantityRules is returned using null to fill missing values`() {
        val result = sut.toAppModelFromVariationMetadata(successVariationMetadataPartial)
        Assertions.assertThat(result).isNotNull
        result?.let { quantityRules ->
            Assertions.assertThat(quantityRules.min).isEqualTo(2)
            Assertions.assertThat(quantityRules.max).isEqualTo(24)
            Assertions.assertThat(quantityRules.groupOf).isNull()
        }
    }

    /**
     * Product metadata
     *  groupOf = 2,
     *  min = 4,
     *  max = 20,
     */
    private val successProductMetadata = """ [ {
                "id": 14054,
                "key": "group_of_quantity",
                "value": "2"
            },
            {
                "id": 14055,
                "key": "minimum_allowed_quantity",
                "value": "4"
            },
            {
                "id": 14056,
                "key": "maximum_allowed_quantity",
                "value": "20"
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
     *  Metadata with no quantity rules keys
     */
    private val noQuantityRulesKeysMetadata = """ [ {
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
     * Product metadata
     *  groupOf = ,
     *  min = ,
     *  max = 20,
     */
    private val successProductMetadataPartial = """ [ {
                "id": 14056,
                "key": "maximum_allowed_quantity",
                "value": "20"
            },
            {
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
     * Variation metadata
     *  groupOf = 3,
     *  min = 6,
     *  max = 30,
     */
    private val successVariationMetadata = """ [{
                "id": 14056,
                "key": "maximum_allowed_quantity",
                "value": "20"
            },
            {
                "id": 14245,
                "key": "variation_group_of_quantity",
                "value": "3"
            },
            {
                "id": 14246,
                "key": "variation_minimum_allowed_quantity",
                "value": "6"
            },
            {
                "id": 5187,
                "key": "trial_length",
                "value": "2"
            },
            {
                "id": 14247,
                "key": "variation_maximum_allowed_quantity",
                "value": "30"
            }]
            """

    /**
     * Variation metadata
     *  groupOf = ,
     *  min = 2,
     *  max = 24,
     */
    private val successVariationMetadataPartial = """ [{
                "id": 14056,
                "key": "maximum_allowed_quantity",
                "value": "20"
            },
            {
                "id": 14246,
                "key": "variation_minimum_allowed_quantity",
                "value": "2"
            },
            {
                "id": 5187,
                "key": "trial_length",
                "value": "2"
            },
            {
                "id": 14247,
                "key": "variation_maximum_allowed_quantity",
                "value": "24"
            }]
            """
}
