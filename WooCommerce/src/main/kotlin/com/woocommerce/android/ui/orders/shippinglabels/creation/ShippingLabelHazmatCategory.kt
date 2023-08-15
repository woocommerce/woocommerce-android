package com.woocommerce.android.ui.orders.shippinglabels.creation

import com.woocommerce.android.R

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
    SMALL_QUANTITY_PROVISION(R.string.shipping_label_hazmat_option_small_quantity_provision)
}
