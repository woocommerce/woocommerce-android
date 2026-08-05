package com.woocommerce.android.cardreader

private const val DEFAULT_MAX_CAUSE_DEPTH = 5
private const val CAUSE_SEPARATOR = " <- caused by: "

/**
 * Renders a throwable and its cause chain into one line for an analytics `error_description`.
 * `toString()` alone drops the cause, which is usually where the real reason lives. Bounded so a
 * deep or self-referencing chain cannot produce an unbounded property value.
 */
fun Throwable.describeWithCauses(maxDepth: Int = DEFAULT_MAX_CAUSE_DEPTH): String {
    val descriptions = mutableListOf<String>()
    val visited = mutableListOf<Throwable>()
    var current: Throwable? = this

    while (current != null && descriptions.size < maxDepth && visited.none { it === current }) {
        visited.add(current)
        descriptions.add(current.toString())
        current = current.cause
    }

    return descriptions.joinToString(CAUSE_SEPARATOR)
}
