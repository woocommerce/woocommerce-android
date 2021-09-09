package com.woocommerce.android.cardreader.connection

sealed class SpecificReader(val name: String) {
    object Chipper2X : SpecificReader("CHIPPER_2X")
    object StripeM2 : SpecificReader("STRIPE_M2")
    object CotsDevice : SpecificReader("COTS_DEVICE")
    object VerifoneP400 : SpecificReader("VERIFONE_P400")
    object WisePade3 : SpecificReader("WISEPAD_3")
    object WisePadeE : SpecificReader("WISEPOS_E")
}

sealed class CardReaderTypesToDiscover {
    data class SpecificReaders(val readers: List<SpecificReader>) : CardReaderTypesToDiscover()
    object UnspecifiedReaders : CardReaderTypesToDiscover()
}
