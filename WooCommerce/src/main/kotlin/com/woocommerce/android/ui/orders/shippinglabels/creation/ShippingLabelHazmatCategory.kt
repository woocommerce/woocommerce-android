package com.woocommerce.android.ui.orders.shippinglabels.creation

import com.woocommerce.android.R
import org.wordpress.android.fluxc.model.shippinglabels.WCShippingLabelModel.HazmatCategory

enum class ShippingLabelHazmatCategory(val stringResourceID: Int) {
    AIR_ELIGIBLE_ETHANOL(R.string.shipping_label_hazmat_option_air_eligible_ethanol),
    CLASS_1(R.string.shipping_label_hazmat_option_class_1),
    CLASS_3(R.string.shipping_label_hazmat_option_class_3),
    CLASS_4(R.string.shipping_label_hazmat_option_class_4),
    CLASS_5(R.string.shipping_label_hazmat_option_class_5),
    CLASS_6(R.string.shipping_label_hazmat_option_class_6),
    CLASS_7(R.string.shipping_label_hazmat_option_class_7),
    CLASS_8_CORROSIVE(R.string.shipping_label_hazmat_option_class_8_corrosive),
    CLASS_8_WET_BATTERY(R.string.shipping_label_hazmat_option_class_8_wet_battery),
    CLASS_9_NEW_LITHIUM_INDIVIDUAL(R.string.shipping_label_hazmat_option_class_9_new_lithium_individual),
    CLASS_9_USED_LITHIUM(R.string.shipping_label_hazmat_option_class_9_used_lithium),
    CLASS_9_NEW_LITHIUM_DEVICE(R.string.shipping_label_hazmat_option_class_9_new_lithium_device),
    CLASS_9_DRY_ICE(R.string.shipping_label_hazmat_option_class_9_dry_ice),
    CLASS_9_UNMARKED_LITHIUM(R.string.shipping_label_hazmat_option_class_9_unmarked_lithium),
    CLASS_9_MAGNETIZED(R.string.shipping_label_hazmat_option_class_9_magnetized),
    DIVISION_4_1(R.string.shipping_label_hazmat_option_division_4_1),
    DIVISION_5_1(R.string.shipping_label_hazmat_option_division_5_1),
    DIVISION_5_2(R.string.shipping_label_hazmat_option_division_5_2),
    DIVISION_6_1(R.string.shipping_label_hazmat_option_division_6_1),
    DIVISION_6_2(R.string.shipping_label_hazmat_option_division_6_2),
    EXCEPTED_QUANTITY_PROVISION(R.string.shipping_label_hazmat_option_excepted_quantity_provision),
    GROUND_ONLY(R.string.shipping_label_hazmat_option_ground_only),
    ID8000(R.string.shipping_label_hazmat_option_id8000),
    LIGHTERS(R.string.shipping_label_hazmat_option_lighters),
    LIMITED_QUANTITY(R.string.shipping_label_hazmat_option_limited_quantity),
    SMALL_QUANTITY_PROVISION(R.string.shipping_label_hazmat_option_small_quantity_provision);

    @Suppress("ComplexMethod")
    fun toHazmatCategory() = when (this) {
        AIR_ELIGIBLE_ETHANOL -> HazmatCategory.AIR_ELIGIBLE_ETHANOL
        CLASS_1 -> HazmatCategory.CLASS_1
        CLASS_3 -> HazmatCategory.CLASS_3
        CLASS_4 -> HazmatCategory.CLASS_4
        CLASS_5 -> HazmatCategory.CLASS_5
        CLASS_6 -> HazmatCategory.CLASS_6
        CLASS_7 -> HazmatCategory.CLASS_7
        CLASS_8_CORROSIVE -> HazmatCategory.CLASS_8_CORROSIVE
        CLASS_8_WET_BATTERY -> HazmatCategory.CLASS_8_WET_BATTERY
        CLASS_9_NEW_LITHIUM_INDIVIDUAL -> HazmatCategory.CLASS_9_NEW_LITHIUM_INDIVIDUAL
        CLASS_9_USED_LITHIUM -> HazmatCategory.CLASS_9_USED_LITHIUM
        CLASS_9_NEW_LITHIUM_DEVICE -> HazmatCategory.CLASS_9_NEW_LITHIUM_DEVICE
        CLASS_9_DRY_ICE -> HazmatCategory.CLASS_9_DRY_ICE
        CLASS_9_UNMARKED_LITHIUM -> HazmatCategory.CLASS_9_UNMARKED_LITHIUM
        CLASS_9_MAGNETIZED -> HazmatCategory.CLASS_9_MAGNETIZED
        DIVISION_4_1 -> HazmatCategory.DIVISION_4_1
        DIVISION_5_1 -> HazmatCategory.DIVISION_5_1
        DIVISION_5_2 -> HazmatCategory.DIVISION_5_2
        DIVISION_6_1 -> HazmatCategory.DIVISION_6_1
        DIVISION_6_2 -> HazmatCategory.DIVISION_6_2
        EXCEPTED_QUANTITY_PROVISION -> HazmatCategory.EXCEPTED_QUANTITY_PROVISION
        GROUND_ONLY -> HazmatCategory.GROUND_ONLY
        ID8000 -> HazmatCategory.ID8000
        LIGHTERS -> HazmatCategory.LIGHTERS
        LIMITED_QUANTITY -> HazmatCategory.LIMITED_QUANTITY
        SMALL_QUANTITY_PROVISION -> HazmatCategory.SMALL_QUANTITY_PROVISION
    }

    val requestFieldValue
        get() = toHazmatCategory().toString()
}
