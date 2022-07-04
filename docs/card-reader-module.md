## Warning
If reading this you find anything is not up-to-date, please fix it or report to the team if not sure how to do that

## Dictionary
* **CPP** - Card Present Payments
* **Stripe** - Third party company we use as payments processing for In-Person Payments
* **WCPay** - Or WooCommerce Payments is a plugin for WooCommerce (which itself is a plugin for Wordpress) that contains integrations with different payments platform including Stripe
* **Stripe Extension/Plugin** - is another plugin for WooCommerce which can be used to accept payments using Stripe Payment Gateway.
* **Card Reader** - At the moment the app supports *BBPOS Chipper 2X BT*. We are working on adding support for *Stripe M2* (ask Aaron to order your own reader)
* **COD** - Cash On Delivery. Paying with a card is also "cash", so CPP is COD 🤷 (Cash On Delivery option can be renamed to "Pay On Delivery" on the site, but internally COD is used for both)
* **POS** - Point Of Sale
* **KYC** - Know Your Customer
* **Card Present Payments / In-Person Payments / Card Reader Payments** - are interchangeable terms in our internal documentation and code, but only “In-Person Payments” is used in user facing features.

## Useful Links To Get Started
* Store Setup for Card Present Payment Testing - P91TBi-4BH-p2
* Stripe terminal In-Person Payments - https://stripe.com/docs/terminal
* Our very own P2 - dfdoF-p2
* Before creating of taptopay P2 we were using general WooMobile P2 with tag #card-present 91TBi-p2
* WCPay Server repo. If something goes wrong with the API most probably issue has to be created there - https://github.com/Automattic/woocommerce-payments-server
* Stripe Android SDK github repo - https://github.com/stripe/stripe-android
* Designs - vaw3DvewbUwuQnPXpdNGGH-fi-8%3A0

## CardReaderModule Overview

This section is aimed for developers who want to understand how the CardReaderModule works so they can start modifying it or integrate it with another app.

The module provides an abstraction from a provider-specific SDK to connect and accept payments using card readers. The module currently supports only Stripe Terminal SDK for Android and most of this documentation is specific only to this SDK. However, the module tries to hide as many details as possible from the client app.

#### Public Interfaces
`CardReaderManager` - The main interface for communicating with a card reader from the client application.
`CardReaderStore` - Abstraction of some network requests made by the `CardReaderModule` (only some since Stripe Terminal SDK performs some requests internally). The abstraction was introduced so the client app can decide how to handle network requests (WCAndroid delegates all requests to FluxC).
`CardReaderManagerFactory` - Simple implementation of a factory which creates a concrete implementation of CardReaderManager.
#### Model Objects
`CardReader` - Abstraction of a card reader with basic info such as an id and a battery level.
`CardReaderDiscoveryEvents` - Events that can be emitted during bluetooth discovery.
`CardPaymentStatus` - Events that can be emitted when accepting a payment.
`CardReaderStatus` - Current state of a connection status to a card reader.
`PaymentInfo` - Model containing data necessary to initiate a payment.
`SoftwareUpdateAvailability` - Current state of software update availability.
`SoftwareUpdateStatus` - Events that can be emitted during an ongoing software update.

## Usage of existing UI flows implemented in WCAndroid
This section is aimed for developer who want to add IPP into another section of the app without necessarily understanding how the module and communication between the app and the module actually works.

There are several sections/screens/flows related to In-Person-Payments (IPP) already implemented in WCAndroid and can be reused to access IPP from a new section of the app.

#### Onboarding Flow
`CardReaderOnboardingChecker` is a class used to check eligibility of a store for IPP. It can be used from any class to check store's eligibility. However, when the store is not eligible, it is recommended to navigate to `CardReaderOnboardingFragment` instead of handling the errors on each screen separately.

In general, the UI related to In-Person-Payments (eg. row in settings, collect payment button on order detail etc) is visible on all stores. When a user attempts to access one of the IPP sections with a store which is not eligible for IPP, the app displays `CardReaderOnboardingFragment` with an explanation what makes the store not-eligible.

#### Hub screen
`CardReaderHubFragment` is an "intersection/hub" with links to different flows - eg. Order Card Reader, Open Card Reader Manual, Manage Card Reader, etc.

#### Connection Flow
`CardReaderConnectDialogFragment` encapsulates the connection flow. Displays each step of the flow in a dialog. Returns a boolean flag (KEY_CONNECT_TO_READER_RESULT = "key_connect_to_reader_result") indicating whether the connection was successfully established to the original destination the flow was started from.

#### Payment Flow
`CardReaderPaymentDialogFragment` encapsulates the payment flow. Displays each step of the flow in a dialog. A card reader needs to be already connected before this flow is started, otherwise finishes immediately and displays a Snackbar with an error message.

`CardReaderPaymentCollectibilityChecker` should be used to verify whether an Order meets the requirements for IPP. If the app navigates to `CardReaderPaymentDialogFragment` for an order which is not eligible for IPP, the payment might fail with a generic error.

## CardReaderModule usage flow
This section explains the happy flow of accepting payment by a merchant using CardReaderModule. This section is useful if you want to implement your own UI with the usage of the solution's built-in in the module

#### Initialization
The first step a client app needs to take is initialize the CardReaderModule. `CardReaderManager` provides a simple `initialize(..)` method which takes care of the initialization.

#### Connecting to a card reader
The `CardReaderManager` provides `discoverReaders(..)` method that starts a discovery of nearby readers. The discovery process is mandatory no matter if the app was connected to a card reader before or not.

When a card reader is discovered, the client app is informed and can invoke `startConnectionToReader(..)` method to connect to the card reader. This method will work only when the discovery is still in progress and a recently found reader is provided (it is not allowed to cache any discovered readers by the client app).

Each card reader needs to be assigned to a physical location. The `startConnectionToReader()` accepts a mandatory location id and it's up to the client app to provide an id which is registered to the corresponding Stripe's account. The `CardReader` object contains a cached location id which can be used if it's not empty. More info about registering a new location can be found on the following link - https://stripe.com/docs/terminal/fleet/locations.

When a successful connection to a card reader is established, the SDK invokes `CardReaderStore.getConnectionToken(..)`. The client app is responsible for obtaining the token and passing it back to the SDK - this is done through `CardReaderStore` which is provided to CardReaderManager during initialization. The token is required to pair the reader with the user’s Stripe account.

Sometimes, the Stripe Terminal SDK might start updating the app during the connection flow. This happens when there is a required update available. The client app cannot skip the update therefore the card reader cannot be used to accept payments until it's updated. Most software updates for card readers are optional at first and they become required later.

The card reader is ready to accept payments now.

Most of the related code is encapsulated in ConnectionManager and its dependencies.

#### Installing optional updates
The client app needs to observe `CardReaderManager.softwareUpdateAvailability` in order to receive current state of SoftwareUpdateAvailability. When an update is available, the client app can invoke `CardReaderManager.startAsyncSoftwareUpdate()` to start the update process and observe `CardReaderManager.softwareUpdateStatus` in order to receive updates.

#### Accepting Payments
The payment process is consisted of 4 steps:
1. *Create PaymentIntent* - the payment data is set and the payment is initialized.
2. *Collect Payment* - The user needs to tap/swipe/insert a card into the card reader.
3. *Process Payment* - The Stripe Terminal SDK verifies the payment and processes it. The SDK internally passes the PaymentIntent into Stripe’s backend. The owner of the card should see the payment in “on-hold” state until the payment is captured or canceled (or it times out after +- N days).
4. *Capture Payment* - the payment is captured and the user gets charged. Offline payments are not currently supported.

Most of the related code is encapsulated in PaymentManager and its dependencies.

#### Switching accounts or stores
In order to switch accounts or to just reset the integration with the Stripe Terminal SDK it is necessary to first disconnect from connected readers, via a call to `disconnect()` and, after that call is completed, call `clear()`.

The method clear() will reset the integration, clear all the caches, and will make the Stripe Terminal ready to fetch a new connection token.

###### This document was inspired by and some parts even copied from https://github.com/woocommerce/woocommerce-ios/blob/develop/docs/HARDWARE.md.
