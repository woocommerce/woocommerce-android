<h1 align="center"><img src="https://user-images.githubusercontent.com/2663464/157550609-1b8b4781-409c-432a-a5e8-aca134c4913a.png" width="500"><br>Guidelines for WooCommerce Android</h1>


⚠️ **Disclaimer:** This guide is not a tutorial on how to work with Compose and assumes some basic understanding of the framework. 

## Content

1. [Code style](#code-style)
2. [Theming and Styling](#theming-and-styling)
3. [File Structure](#file-structure)
4. [Managing state](#managing-state)
5. [Navigation](#navigation)
6. [Accessibility](#accessibility)

 # Code Style

For **Compose App development** we will follow the official styling guidelines that can be found [here](https://github.com/androidx/androidx/blob/androidx-main/compose/docs/compose-api-guidelines.md#api-guidelines-for-jetpack-compose). Note that the guidelines differentiate between 3 levels of restriction:
* Compose Framework Development
* Compose Library Development
* Compose App development

We will apply the **App development** guidelines which are more flexible. 

Many of the rules from official guidelines are already integrated with Lint, so it will be easier to comply with them. 

Any exception to those code style guidelines should be described here: 

* Currently there are no exceptions. 

A few things to **highlight** from the Compose official guidelines: 

* Name any @Composable function that returns Unit using `PascalCase`
* Name of @Composable functions must be a noun, not a verb or verb phrase, nor a nouned preposition, adjective, or adverb. Nouns MAY be prefixed by descriptive adjectives. [Why?](https://github.com/androidx/androidx/blob/androidx-main/compose/docs/compose-api-guidelines.md#why-2)
* @Composable functions that return values SHOULD follow the standard [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html#function-names) for the naming of functions.
* Any @composable function that internally `remember {}`s and returns a mutable object should add the prefix `remember`.
* @Composable functions should either emit content into the composition or return a value, but not both. [Why](https://github.com/androidx/androidx/blob/androidx-main/compose/docs/compose-api-guidelines.md#why-6)

# Theming and Styling

Compose enables to define your own set of `colors`, `typography` and `shapes`. Currently we are going to make use of [MDC-Android Compose Adapter](https://material-components.github.io/material-components-android-compose-theme-adapter/) in order to bridge/reuse the current colors and textAppearances we have defined in our `type.xml` and `colors.xml` files. Inside `com.woocommerce.android.ui.compose.theme` package you'll finde the defined `WooTheme`. Using it is as simple as wrapping your compose content with the theme like for example in `MoreMenuFragment.kt`: 

```kotlin
setContent {
    WooTheme {
        MoreMenu(viewModel)
    }
}
```

# File Structure



Compose UI files should follow 

# Managing State

Managing state properly in Compose is key to updating the UI as expected and making composable functions as reusable as possible. Some key take to managing state properly: 

- Recommended talk on Compose [state](https://www.youtube.com/watch?v=rmv2ug-wW4U&ab_channel=AndroidDevelopers)
- Key points: 
	- Use property delegates `by` to avoid having to access the `foo.value` all the time. 
	- Mutate state outside the composable function scope
	- Pass immutable val to composable function to respect the single source of truth. Only modify data in one place. 
	- State hoisting -> move the private state out of composable functions to make composable functions stateless, the idea is composable. Delegate data manipulation to the viewModel

	Side effects in Compose: https://developer.android.com/jetpack/compose/side-effects
	

Use  `var quantity: Int by remember/rememberSaveable{ MutableState<Int> = mutableStateOf(1) } to remember state across configuration changes you

# Navigation

//TODO

# Accessibility

//TODO
