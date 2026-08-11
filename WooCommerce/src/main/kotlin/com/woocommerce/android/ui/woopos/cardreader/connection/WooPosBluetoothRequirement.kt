package com.woocommerce.android.ui.woopos.cardreader.connection

sealed interface WooPosBluetoothRequirement {
    data object MissingBluetoothPermission : WooPosBluetoothRequirement
    data object BluetoothOff : WooPosBluetoothRequirement
    data object MissingLocationPermission : WooPosBluetoothRequirement
    data object LocationOff : WooPosBluetoothRequirement
}
