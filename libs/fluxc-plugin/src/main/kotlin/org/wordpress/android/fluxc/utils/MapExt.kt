package org.wordpress.android.fluxc.utils

fun <T> MutableMap<T, String>.putIfNotEmpty(vararg pairs: Pair<T, String?>) = apply {
    pairs.forEach { pair ->
        pair.second?.takeIf { it.isNotEmpty() }
                ?.let { put(pair.first, it) }
    }
}

fun <T, K> MutableMap<T, K>.putIfNotNull(vararg pairs: Pair<T, K?>) = apply {
    pairs.forEach { pair ->
        pair.second?.let { put(pair.first, it) }
    }
}
