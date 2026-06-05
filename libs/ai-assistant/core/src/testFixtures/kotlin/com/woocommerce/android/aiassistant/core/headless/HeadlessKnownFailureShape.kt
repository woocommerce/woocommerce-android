package com.woocommerce.android.aiassistant.core.headless

fun HeadlessScenarioRunResult.failingSampleHardCheckResults(): List<List<HeadlessHardCheckResult>> {
    val sampleChecks = sampleResults
        .takeIf { it.isNotEmpty() }
        ?.map { sample -> sample.status to sample.hardCheckResults }
        ?: listOf(status to hardCheckResults)

    return sampleChecks
        .filter { (status, _) -> status == HeadlessScenarioStatus.FAIL }
        .map { (_, hardCheckResults) -> hardCheckResults }
}

fun HeadlessScenarioRunResult.matchesKnownFailureShape(
    knownFailure: HeadlessKnownFailure,
): Boolean {
    val expectedFailedHardChecks = knownFailure.expectedFailedHardChecks.toSet()
    val failingSamples = failingSampleHardCheckResults()

    return failingSamples.isNotEmpty() &&
        failingSamples.all { hardCheckResults -> hardCheckResults.failedHardChecks() == expectedFailedHardChecks }
}

fun List<HeadlessHardCheckResult>.failedHardChecks(): Set<HeadlessHardCheck> =
    filterNot { it.passed }
        .map { it.check }
        .toSet()
