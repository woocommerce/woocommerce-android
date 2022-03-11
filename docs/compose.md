<h1 align="center"><img src="https://user-images.githubusercontent.com/2663464/157550609-1b8b4781-409c-432a-a5e8-aca134c4913a.png" width="500"><br>Guidelines for WooCommerce Android</h1>


⚠️ **Disclaimer:** This guide is not a tutorial on how to work with Compose and assumes some basic understanding of the framework. 

## Content

1. [Code style](#code-style)
2. [Theming and Styling](#theming-and-styling)
3. [File Structure](#file-structure)
4. [Managing state](#managing-state)
5. [Navigation](#navigation)
6. [Accessibility](#accessibility)

# Code Style ✍️

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

# Theming and Styling 🎨

Compose enables to define your own set of `colors`, `typography` and `shapes`. Currently we are going to make use of [MDC-Android Compose Adapter](https://material-components.github.io/material-components-android-compose-theme-adapter/) in order to bridge/reuse the current colors and textAppearances we have defined in our `type.xml` and `colors.xml` files. Inside `com.woocommerce.android.ui.compose.theme` package you'll finde the defined `WooTheme`. Using it is as simple as wrapping your compose content with the theme like for example in `MoreMenuFragment.kt`: 

```kotlin
setContent {
    WooTheme {
        MoreMenu(viewModel)
    }
}
```

# File Structure 🗃

The file structure for Compose code should not differ much from how we organice files currently in the project. 

- `ui/compose/components`: common/generic components used by the entire app.
- `ui/compose/theme`: classes related to themes, colors, shapes.
- `ui/compose/animations`: common/generic animations that can be reused across multiple features.

In essence, anything inside `ui/compose` package should be compose code that is reused across multiple feature. Just common sense 🙂
Inside a specific feature we can follow the same structure `ui/[feature]/compoents`, etc. 


# Managing State 👩‍💻

Managing state properly in Compose is key to updating the UI as expected and making composable functions as reusable as possible. Some key takes to managing state properly: 

- Recommended talk on Compose [state](https://www.youtube.com/watch?v=rmv2ug-wW4U&ab_channel=AndroidDevelopers)
- Best practices on handling state in `@Composable` functions: 
	- Apply state hoisting whenever possible. State hoisting -> move private state out of composable functions to make composable functions stateless. Delegate data manipulation to the viewModel or at least to the parent function that is calling the composable function.
	- When using state inside a composable functions prefer to use property delegates such `by` to avoid having to access the `mutableState.value` all the time. For example: `var foo : Int by rememberSaveable {mutableStateOf(1)}`
	- Always mutate state outside the composable function scope.
	- Pass immutable values to composable functions to respect the single source of truth.
	- Composable functions should be side effect free. However, when they're necessary to mutate the state of the app, they should be called from a controlled environment that is aware of the lifecycle of the composable. More on that in the [Compose side effects guides](https://developer.android.com/jetpack/compose/side-effects#state-effect-use-cases)

# Navigation 🗺

//TODO

# Accessibility ♿️

//TODO
