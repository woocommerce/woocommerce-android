package com.woocommerce.android.extensions

@Suppress("UNCHECKED_CAST")
fun <K, V : Any> Map<out K, V?>.filterNotNull(): Map<K, V> = filterValues { it != null } as Map<K, V>
