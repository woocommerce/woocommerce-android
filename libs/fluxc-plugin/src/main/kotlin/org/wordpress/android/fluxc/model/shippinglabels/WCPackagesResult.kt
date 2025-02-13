package org.wordpress.android.fluxc.model.shippinglabels

data class WCPackagesResult(
    val customPackages: List<CustomPackage>,
    val predefinedOptions: List<PredefinedOption>
) {
    data class CustomPackage(
        val title: String,
        val isLetter: Boolean,
        val dimensions: String,
        val boxWeight: Float
    )

    data class PredefinedOption(
        val title: String,
        val carrier: String,
        val predefinedPackages: List<PredefinedPackage>
    ) {
        data class PredefinedPackage(
            val id: String,
            val title: String,
            val isLetter: Boolean,
            val dimensions: String,
            val boxWeight: Float
        )
    }
}
