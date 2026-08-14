package com.woocommerce.android.cardreader.connection

import com.woocommerce.android.cardreader.connection.ReaderType.BuildInReader
import com.woocommerce.android.cardreader.connection.ReaderType.ExternalReader

sealed class ReaderType(val name: String) {
    sealed class ExternalReader(
        extReaderName: String,
        val isInternetReader: Boolean = false,
    ) : ReaderType(extReaderName) {
        object Chipper2X : ExternalReader("CHIPPER_2X")
        object StripeM2 : ExternalReader("STRIPE_M2")
        object VerifoneP400 : ExternalReader("VERIFONE_P400", isInternetReader = true)
        object WisePade3 : ExternalReader("WISEPAD_3")
        object WisePadeE : ExternalReader("WISEPOS_E", isInternetReader = true)
        object StripeS700 : ExternalReader("STRIPE_S700", isInternetReader = true)
        object StripeS710 : ExternalReader("STRIPE_S710", isInternetReader = true)
        object StripeT600 : ExternalReader("STRIPE_T600", isInternetReader = true)
        object StripeT610 : ExternalReader("STRIPE_T610", isInternetReader = true)
    }

    sealed class BuildInReader(buildInReaderName: String) : ReaderType(buildInReaderName) {
        object TapToPayDevice : BuildInReader("TAP_TO_PAY_DEVICE")
    }

    object Unknown : ReaderType("UNKNOWN")

    companion object {
        private fun fromName(name: String): ReaderType =
            when (name.uppercase()) {
                "CHIPPER_2X" -> ExternalReader.Chipper2X
                "STRIPE_M2" -> ExternalReader.StripeM2
                "VERIFONE_P400" -> ExternalReader.VerifoneP400
                "WISEPAD_3" -> ExternalReader.WisePade3
                "WISEPOS_E" -> ExternalReader.WisePadeE
                "STRIPE_S700" -> ExternalReader.StripeS700
                "STRIPE_S710" -> ExternalReader.StripeS710
                "STRIPE_T600" -> ExternalReader.StripeT600
                "STRIPE_T610" -> ExternalReader.StripeT610
                "TAP_TO_PAY_DEVICE" -> BuildInReader.TapToPayDevice
                else -> Unknown
            }

        fun isExternalReaderType(name: String?): Boolean = name?.let { fromName(name) is ExternalReader } ?: false

        fun isBuiltInReaderType(name: String?): Boolean = name?.let { fromName(name) is BuildInReader } ?: false
    }
}

sealed class CardReaderTypesToDiscover {
    sealed class SpecificReaders(val readers: List<ReaderType>) : CardReaderTypesToDiscover() {
        data class ExternalReaders(val externalReaders: List<ExternalReader>) : SpecificReaders(externalReaders)
        data class BuiltInReaders(val builtInReaders: List<BuildInReader>) : SpecificReaders(builtInReaders)
    }

    object UnspecifiedReaders : CardReaderTypesToDiscover()
}
