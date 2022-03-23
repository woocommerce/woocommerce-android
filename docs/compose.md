<h1 align="center"><img src="https://user-images.githubusercontent.com/2663464/157550609-1b8b4781-409c-432a-a5e8-aca134c4913a.png" width="500"><br>Guidelines for WooCommerce Android</h1>


⚠️ **Disclaimer:** This guide is not a tutorial on how to work with Compose and assumes some basic knowledge of the framework. 

## Content

1. [Code style](#code-style)
2. [Theming and Styling](#theming-and-styling)
3. [File Structure](#file-structure)
4. [Managing state](#managing-state)
5. [Navigation](#navigation)
6. [Accessibility](#accessibility)
7. [UI Tests in Compose](#ui-tests-in-compose)

# Code Style ✍️ <a name="code-style"></a>

For **Compose App development** we will follow the official styling guidelines that can be found [here](https://github.com/androidx/androidx/blob/androidx-main/compose/docs/compose-api-guidelines.md#api-guidelines-for-jetpack-compose). Note that the guidelines differentiate between 3 levels of restriction:
* Compose Framework Development
* Compose Library Development
* Compose App development

We will apply the **App development** guidelines which are more flexible unless we are developing a Compose specific library, in which case Library development level should be applied. 

Many of the rules from official guidelines are already integrated with Lint, so it will be easier to comply with them. 

Any exception to those code style guidelines should be described here: 

* Guidelines suggest using `PascalCase` for constant values and enums, etc. We are currently using `UPPER_SNAKE_CASE` for this. There is no major reason for using `PascalCase` in Compose code and having different styles between Compose and non Compose code. We will keep using `UPPER_SNAKE_CASE `.  

A few things to **highlight** from the Compose official guidelines: 

* Name any @Composable function that returns Unit using `PascalCase`
* Name of @Composable functions must be a noun, not a verb or verb phrase, nor a nouned preposition, adjective, or adverb. Nouns MAY be prefixed by descriptive adjectives. [Why?](https://github.com/androidx/androidx/blob/androidx-main/compose/docs/compose-api-guidelines.md#why-2)
* @Composable functions that return values SHOULD follow the standard [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html#function-names) for the naming of functions.
* Any @composable function that internally `remember {}`s and returns a mutable object should add the prefix `remember`.
* @Composable functions should either emit content into the composition or return a value, but not both. [Why](https://github.com/androidx/androidx/blob/androidx-main/compose/docs/compose-api-guidelines.md#why-6)

# Theming and Styling 🎨 <a name="theming-and-styling"></a>

Compose enables you to define your own set of `colors`, `typography`, and `shapes`. Currently, we are using [MDC-Android Compose Adapter](https://material-components.github.io/material-components-android-compose-theme-adapter/) in order to bridge/reuse the current colors and text appearances we have defined in our `type.xml` and `colors.xml` files. Inside `com.woocommerce.android.ui.compose.theme` package you'll find the defined `WooTheme`. Using it is as simple as wrapping your compose content with the theme: 

```kotlin
    WooTheme {
        MoreMenu(viewModel)
    }
```

One important thing to keep in mind when setting the theme is to properly support light/dark modes. If the composable root function of the tree
does not support the use of `contentColor` then use `WooThemeWithBackground` to support light/dark colors out of the box. An example of this is `MoreMenuFragment.kt`: 

```kotlin
setContent {
    WooThemeWithBackground {
        MoreMenu(viewModel)
    }
}
```

# File Structure 🗃 <a name="file-structure"></a>

The file structure for Compose code should not differ much from how we organize files currently in the project. 

- `ui/compose/components`: common/generic components used by the entire app.
- `ui/compose/theme`: classes related to themes, colors, shapes.
- `ui/compose/animations`: common/generic animations that can be reused across multiple features.

In essence, anything inside `ui/compose` package should be Compose code that is reused across multiple features. Just common sense 🙂
Inside a specific feature, we can follow the same structure `ui/[feature]/components`, etc. 


# Managing State 👩‍💻 <a name="managing-state"></a>

Managing state properly in Compose is key to updating the UI as expected and making composable functions as reusable as possible. Some key takes to managing state: 

- Recommended talk on Compose [state](https://www.youtube.com/watch?v=rmv2ug-wW4U&ab_channel=AndroidDevelopers)
- Best practices on handling state in `@Composable` functions: 
	- Apply state hoisting whenever possible. State hoisting is basically moving all private state out of the @Composable functions to make composable functions stateless. Ideally, delegate data manipulation to the viewModel or at least to the parent function that is calling the @Composable function.
	- When using state inside a composable function prefer to use property delegates such `by` to avoid having to access the `mutableState.value` all the time. For example: `var foo : Int by rememberSaveable {mutableStateOf(1)}`
	- Always mutate state outside the composable function scope. Like for example onClick{} lambdas passed as parameter.
	- Pass immutable values to composable functions to respect the single source of truth.
	- Composable functions should be side effect free. However, when they need to mutate the state of the app, they should be called from a controlled environment that is aware of the lifecycle of the composable. More on that in the [Compose side effects guides](https://developer.android.com/jetpack/compose/side-effects#state-effect-use-cases)

# Navigation 🗺 <a name="navigation"></a>

Currently, we are using Compose through `ComposeView` nested inside a `Fragment` as the root in a 1:1 relationship. For this kind of usage, Navigation implementation remains the same, we can keep using the existing navitation_graphs.xml. 
There is one thing to keep in mind when using this `ComposeView` approach. Compose views involve ongoing work and registering the composition with external event sources. These registrations can cause the composition to remain live and ineligible for garbage collection for long after the host View may have been abandoned. To avoid any leaks Android provides [ViewCompositionStrategy](https://developer.android.com/reference/kotlin/androidx/compose/ui/platform/ViewCompositionStrategy)  for disposing the composition automatically at an appropriate time. The recommended strategy for the `Fragment` <--> `ComposeView` approach is [DisposeOnViewTreeLifecycleDestroyed](https://developer.android.com/reference/kotlin/androidx/compose/ui/platform/ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

//TODO Add guidelines to best practices when navigating between Composables. 


# Accessibility ♿️ <a name="accessibility"></a>

Most of the rules that apply for Android's view system apply for Compose UI. But is worth to highlight a few concepts and tools to handle accessibility nicely. 

The key idea to make Composable components accessible is [Semantics](https://developer.android.com/jetpack/compose/semantics). Accessibility services use the `Semantics tree` to provide information to the people using the services (like talkback), and the UI testing framework uses it to make assertions. Few key takes to keep your Composable UI accessible: 
- For composables and modifiers from the Compose foundation and material library, the Semantics tree is automatically filled and generated for you
- When adding custom low-level composables, you will have to manually provide its semantics
- Set `contentDescriptions` for relevant icons and images. Describe the meaning of the icon, not what it is. 
- Use `Modifier.semantics(mergeDescendants = true)` to group content and facilitate navigating through UI components with "TalkBack" tool.
-  Accessibility services such as TalkBack provide navigation shortcuts. One of those is jumping between headers to skip to the content the user is interested in. You can inform accessibility services that something is a header using `Modifier.semantics { heading() }`
-  You can also add state descriptions to inform if an item is at a certain state. For example selected or unselected: 
```kotlin 
val semanticsModifier =
  Modifier.semantics(mergeDescendants = true) {
    stateDescription = if (item.selected) {
      selectedDescription
    } else {
      unselectedDescription
    }
  }
```


# UI Tests in Compose 🧪 <a name="ui-tests-in-compose"></a>

//TODO
