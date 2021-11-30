package com.woocommerce.android.cardreader.payments

sealed class CardPaymentStatus {
    object InitializingPayment : CardPaymentStatus()
    object CollectingPayment : CardPaymentStatus()
    object WaitingForInput : CardPaymentStatus()
    object ProcessingPayment : CardPaymentStatus()
    object CapturingPayment : CardPaymentStatus()
    data class PaymentCompleted(val receiptUrl: String) : CardPaymentStatus()

    data class PaymentFailed(
        val type: CardPaymentStatusErrorType,
        val paymentDataForRetry: PaymentData?,
        val errorMessage: String
    ) : CardPaymentStatus()

    sealed class CardPaymentStatusErrorType {
        object CardReadTimeOut : CardPaymentStatusErrorType()
        object NoNetwork : CardPaymentStatusErrorType()
        object Server : CardPaymentStatusErrorType()
        object Generic : CardPaymentStatusErrorType()

        sealed class DeclinedByStripeApiError(val errorCodes: Array<String>) : CardPaymentStatusErrorType() {
            /**
             * The specified amount is less than the minimum amount allowed (50 cents at the moment)
             */
            object AmountTooSmall : DeclinedByStripeApiError(
                arrayOf(
                    "amount_too_small"
                )
            )

            /**
             * Declined by stripe api with unknown reason
             */
            object Unknown : DeclinedByStripeApiError(emptyArray())

            sealed class CardDeclined(val declineErrorCodes: Array<String>) : DeclinedByStripeApiError(emptyArray()) {
                companion object {
                    const val ERROR_CODE = "card_declined"
                }

                /**
                 * A possibly temporary error caused the decline (e.g. the issuing
                 * bank's servers could not be contacted.) Tell the user this and prompt
                 * them to try again with the same (or another) payment method.
                 */
                object Temporary : CardDeclined(
                    arrayOf(
                        "approve_with_id",
                        "issuer_not_available",
                        "processing_error",
                        "reenter_transaction",
                        "try_again_later",
                    )
                )

                /**
                 * The card has been reported lost or stolen. Don't reveal this
                 * to the user. Treat it like you would a generic decline.
                 */
                object Fraud : CardDeclined(
                    arrayOf(
                        "call_issuer",
                        "card_velocity_exceeded",
                        "do_not_honor",
                        "do_not_try_again",
                        "fraudulent",
                        "lost_card",
                        "merchant_blacklist",
                        "pickup_card",
                        "restricted_card",
                        "revocation_of_all_authorizations",
                        "revocation_of_authorization",
                        "security_violation",
                        "stolen_card",
                        "stop_payment_order",
                    )
                )

                /**
                 * The payment was declined for an unspecified reason. Ask
                 * the user to try another payment method.
                 */
                object Generic : CardDeclined(
                    arrayOf(
                        "generic_decline",
                        "no_action_taken",
                        "not_permitted",
                        "service_not_allowed",
                        "transaction_not_allowed",
                    )
                )

                /**
                 * The card, or the account it connects to, is not valid. Ask
                 * the user to try another payment method.
                 */
                object InvalidAccount : CardDeclined(
                    arrayOf(
                        "invalid_account",
                        "new_account_information_available",
                    )
                )

                /**
                 * The card presented is not supported. Tell the user this and
                 * ask them to try another payment method.
                 */
                object CardNotSupported : CardDeclined(
                    arrayOf(
                        "card_not_supported",
                    )
                )

                /**
                 * The currency is not supported by the card presented. Tell the
                 * user this and ask them to try another payment method.
                 */
                object CurrencyNotSupported : CardDeclined(
                    arrayOf(
                        "currency_not_supported",
                    )
                )

                /**
                 * An identical transaction was just completed for the card presented.
                 * Tell the user this and ask them to try another payment method if they
                 * really want to do this.
                 */
                object DuplicateTransaction : CardDeclined(
                    arrayOf(
                        "duplicate_transaction",
                    )
                )

                /**
                 * The card presented has expired. Tell the user this and ask them
                 * to try another payment method.
                 */
                object ExpiredCard : CardDeclined(
                    arrayOf(
                        "expired_card",
                    )
                )

                /**
                 * The card presented has a different ZIP/postal code than was
                 * used to place the order. Tell the user this and ask them
                 * to try another payment method (or correct the order.)
                 */
                object IncorrectPostalCode : CardDeclined(
                    arrayOf(
                        "incorrect_zip",
                    )
                )

                /**
                 * The card presented has insufficient funds for the purchase.
                 * Tell the user this and ask them to try another payment method.
                 */
                object InsufficientFunds : CardDeclined(
                    arrayOf(
                        "insufficient_funds",
                        "withdrawal_count_limit_exceeded",
                    )
                )

                /**
                 * The card presented does not allow purchases of the amount
                 * given. Tell the user this and ask them to try another payment method.
                 */
                object InvalidAmount : CardDeclined(
                    arrayOf(
                        "invalid_amount",
                    )
                )

                /**
                 * The card presented requires a PIN and the device doesn't support
                 * PIN entry. Tell the user this and ask them to try another payment method.
                 */
                object PinRequired : CardDeclined(
                    arrayOf(
                        "invalid_pin",
                        "offline_pin_required",
                        "online_or_offline_pin_required",
                    )
                )

                /**
                 * The card presented has had an incorrect PIN entered too many times.
                 * Tell the user this and ask them to try another payment method.
                 */
                object TooManyPinTries : CardDeclined(
                    arrayOf(
                        "pin_try_exceeded",
                    )
                )

                /**
                 * The card presented is a system test card and cannot be used to
                 * process a payment. Tell the user this and ask them to try another
                 * payment method.
                 */
                object TestCard : CardDeclined(
                    arrayOf(
                        "testmode_decline",
                    )
                )

                /**
                 * The card presented is a live card, but the store/account is in test
                 * mode. Tell the user this and ask them to use a system test card instead.
                 */
                object TestModeLiveCard : CardDeclined(
                    arrayOf(
                        "test_mode_live_card"
                    )
                )
            }
        }
    }

    enum class AdditionalInfoType {
        RETRY_CARD,
        INSERT_CARD,
        INSERT_OR_SWIPE_CARD,
        SWIPE_CARD,
        REMOVE_CARD,
        MULTIPLE_CONTACTLESS_CARDS_DETECTED,
        TRY_ANOTHER_READ_METHOD,
        TRY_ANOTHER_CARD,
        CHECK_MOBILE_DEVICE,
    }
}

interface PaymentData
