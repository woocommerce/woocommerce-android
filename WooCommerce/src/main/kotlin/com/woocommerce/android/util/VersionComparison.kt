package com.woocommerce.android.util

/*
 * Plugin version comparison logic is based on the WordPress PHP implementation.
 */
@Suppress("CyclomaticComplexMethod", "LoopWithTooManyJumpStatements", "MagicNumber")
fun String?.isGreaterThanPluginVersion(v2: String?): Boolean {
    if (this == null || v2 == null) return false

    val vm = mapOf(
        "dev" to -6,
        "alpha" to -5,
        "a" to -5,
        "beta" to -4,
        "b" to -4,
        "RC" to -3,
        "rc" to -3,
        "#" to -2,
        "p" to 1,
        "pl" to 1
    )

    fun prepVersion(v: String): List<String> {
        var version = v.replace(Regex("[_\\-+]"), ".")
        version = version.replace(Regex("([^\\.\\d]+)"), ".$1.").replace(Regex("\\.{2,}"), ".")
        return if (version.isEmpty()) listOf("-8") else version.split(".")
    }

    fun numVersion(v: String): Int {
        return if (v.isEmpty()) 0 else vm[v] ?: v.toIntOrNull() ?: -7
    }

    val version1 = prepVersion(this).map { numVersion(it) }
    val version2 = prepVersion(v2).map { numVersion(it) }
    val x = maxOf(version1.size, version2.size)
    var compare = 0

    for (i in 0 until x) {
        if (version1.getOrNull(i) == version2.getOrNull(i)) {
            continue
        }
        val numV1 = version1.getOrNull(i) ?: 0
        val numV2 = version2.getOrNull(i) ?: 0
        compare = when {
            numV1 < numV2 -> -1
            numV1 > numV2 -> 1
            else -> 0
        }
        if (compare != 0) break
    }

    return compare > 0
}
