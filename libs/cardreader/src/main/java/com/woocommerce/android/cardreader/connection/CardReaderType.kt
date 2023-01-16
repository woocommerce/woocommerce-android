package com.woocommerce.android.cardreader.connection

import com.woocommerce.android.cardreader.connection.ReaderType.BuildInReader
import com.woocommerce.android.cardreader.connection.ReaderType.ExternalReader

sealed class ReaderType(val name: String) {
    sealed class ExternalReader(extReaderName: String) : ReaderType(extReaderName) {
        object Chipper2X : ExternalReader("CHIPPER_2X")
        object StripeM2 : ExternalReader("STRIPE_M2")
        object VerifoneP400 : ExternalReader("VERIFONE_P400")
        object WisePade3 : ExternalReader("WISEPAD_3")
        object WisePadeE : ExternalReader("WISEPOS_E")
    }

    sealed class BuildInReader(buildInReaderName: String) : ReaderType(buildInReaderName) {
        object CotsDevice : BuildInReader("COTS_DEVICE")
    }
}

sealed class CardReaderTypesToDiscover {
    sealed class SpecificReaders(val readers: List<ReaderType>) : CardReaderTypesToDiscover() {
        data class ExternalReaders(val externalReaders: List<ExternalReader>) : SpecificReaders(externalReaders)
        data class BuiltInReaders(val builtInReaders: List<BuildInReader>) : SpecificReaders(builtInReaders)
    }

    object UnspecifiedReaders : CardReaderTypesToDiscover()
}
