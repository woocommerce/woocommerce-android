## Warning
If reading this you find anything is not up-to-date, please fix it or report to the team if not sure how to do that

## Dictionary
* **IAP** - In-App Payments. Payments proceeded by Google/Apple and were done directly via the app. Usually performed in exchange for digital goods.
* **Mobile Pay** - Build by A8C solution to handle purchases done via IAP, will be referred as *backend*. Usually, after payment is processed, it unlocks access to premium features.
* **Purchase acknowledgment** - IAP is a two-step process. The first step is to get money from a user and the second is to confirm that digital goods is released. In our case acknowledgment is performed via Mobile Pay
* **WP.com plan** - the only plan we currently sell, the one that allows a user to install a WooCommerce plugin and start its store

## Overview
The module is a wrapper around google's [billing library](https://developer.android.com/google/play/billing/integrate).
The goal of it is to provide easy-to-use and specific for our current needs implementation of the IAP

Currently, it supports just one hardcoded in the code product

The easiest way to get started with the module is to look into the showcase located in `com.woocommerce.android.iapshowcase` package.

## Structure
To get started, you need to understand the structure of the module.

![test](/docs/images/iap-module-diagram.webp)

The module is divided into 2 main parts: *public* and *internal*

### Public
The exposed (public) part of the module contains the interface and its parameters.

Currently, there are 5 integration points:

#### 1. `val purchaseWpComPlanResult: Flow<WPComPurchaseResult>`

This flow is used to get the result of the purchase. It can be used to show a success or error message to the user.
Notice, that purchase can be published here even if `purchaseWPComPlan` was not called in the current lifecycle, so you should be subscribed to the flow even if `purchaseWPComPlan` was not invoked.

#### 2. `suspend fun purchaseWPComPlan(activityWrapper: IAPActivityWrapper)`

`IAPActivityWrapper` is a simple wrapper around activity to make ViewModel unit testable

This method:
* Checks if the purchase was already done, and if it was but it's not acknowledged, then the backend is called and the success result is returned via `purchaseWpComPlanResult`
* Fetches product details via google billing library. Product detail is needed to start the flow
* If all preceding was successful, then it kicks in the purchase UI flow
* If the result from the UI flow was successful then we hit the backend and its release to a user and acknowledges the purchase on the google side
* Success result is returned via `purchaseWpComPlanResult`

#### 3. `suspend fun fetchWPComPlanProduct(): WPComProductResult`

This method returns information (title, description, price, and currency) about a product that is on sale

#### 4. `suspend fun isIAPSupported(): IAPSupportedResult`

Returns if IAP is possible. IAP might be not possible for many reasons, including:
* The device does not support IAP due to a lack of google play services or not up to date version of it
* The user is not logged in to google play services
* We do not support IAP in the user country/currency
* etc


#### 5. `suspend fun isWPComPlanPurchased(): WPComIsPurchasedResult`

Returns if the plan has been purchased and/or acknowledged already.

### Internal

Internally the module is splitting into 3 main parts:
#### 1. WP Com Plan purhase layer

This layer contains the logic that is specific to our current needs, for instance:
* It determines what product we work with
* What currency is supported
* Communicates with Mobile Pay to confirm the purchase
* Communicates with the layer below which is a generic wrapper around the google billing library

#### 2. Wrapper around billing library
Wrapper that makes the API a bit more user-friendly. Also, it wraps types exposed by the library so it'll be easier to replace/update it when needed

The layer also handles different edge cases, e.g. when the purchase result is not delivered via callback. It polls the result periodically in the background

#### 3. Network layer
The layer that communicates to Mobile Pay, using Fluxc

## Proposed flow

* `isIAPSupported` has to be called first to determine if IAP flow can be used. If an error is returned, then depending on it we might consider showing to a user a specific UI, suggesting different steps. E.g. if a version of Google Service is outdated then we might need to ask a user to update them
* Subscribe to the flow exposed by `purchaseWpComPlanResult` to get up-to-date information about the purchase status
* We might need to check `isWPComPlanPurchased`. If purchased and acknowledged then we might let a user in right away. If purchased, but not acknowledged, then consider calling `purchaseWpComPlanResult`, so it will hit the backend internally and the backend on its own will release the purchase and acknowledge the purchase
* If the plan is not purchased yet, then call `fetchWPComPlanProduct` to get information about the product. Show this information to a user with the "purchase" button
* Click on the button should invoke the `purchaseWPComPlan` method. It will start the IAP flow
