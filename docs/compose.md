<h1 align="center"><img src="https://user-images.githubusercontent.com/2663464/157550609-1b8b4781-409c-432a-a5e8-aca134c4913a.png" width="500"><br>Guidelines for WooCommerce Android</h1>


⚠️ **Disclaimer:** This guide is not a tutorial on how to work with Compose and assumes some basic knowledge of the framework. 

## Content

1. [Code style](#code-style)
2. [Theming and Styling](#theming-and-styling)
3. [File Structure](#file-structure)
4. [Managing state](#managing-state)
5. [Composable Functions Best Practices](#functions-best-practices)
6. [Navigation](#navigation)
7. [Accessibility](#accessibility)
8. [UI Tests in Compose](#ui-tests-in-compose)

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

Compose enables you to define your own set of `colors`, `typography`, and `shapes`. Currently, we are using [MDC-Android Compose Adapter](https://material-components.github.io/material-components-android-compose-theme-adapter/) in order to bridge/reuse the current colors and text appearances we have defined in our `type.xml` and `colors.xml` files. Inside `com.woocommerce.android.ui.compose.theme` package you'll find the defined `WooTheme`. Using it is as simple as wrapping your Compose screen with the theme: 

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

Managing state properly in Compose is key to updating the UI as expected and making composable functions as reusable as possible. Some key take to managing state: 

- Recommended talk on Compose [state](https://www.youtube.com/watch?v=rmv2ug-wW4U&ab_channel=AndroidDevelopers)
- Best practices on handling state in `@Composable` functions: 
	- Apply https://developer.android.com/jetpack/compose/state#state-hoisting whenever possible. State hoisting is basically moving all private state out of the @Composable functions to make composable functions stateless. Ideally, delegate data manipulation to the ViewModel or at least to the parent function that is calling the @Composable function.
	- When using state inside a composable function prefer to use property delegates such `by` to avoid having to access the `mutableState.value` all the time. For example: `var foo : Int by rememberSaveable {mutableStateOf(1)}`
	- Always mutate state outside the composable function scope. Like for example onClick{} lambdas passed as parameters.
	- Pass immutable values to composable functions to respect the single source of truth.
	- Composable functions should be side effects free. However, when they need to mutate the state of the app, they should be called from a controlled environment that is aware of the lifecycle of the composable. More on that in the [Compose side effects guides](https://developer.android.com/jetpack/compose/side-effects#state-effect-use-cases)

# Composable Functions Best Practices ✅ <a name="functions-best-practices"></a>

This section shares some principles we should aim for when creating our `@Composable` functions:

- Always provide a `Modifier` parameter to any composable function that emits layout. The reasons for this are well explained [here](https://chris.banes.dev/always-provide-a-modifier/). In essence, the parent composable function should always be telling the child composable (through a `Modifier` param with layout attributes such as height, width, margins, etc) how to measure and be laid out, not the other way around. The child composable should only think about its own content. We will adopt [official guidelines](https://github.com/androidx/androidx/blob/androidx-main/compose/docs/compose-api-guidelines.md#elements-accept-and-respect-a-modifier-parameter) on how to pass the `Modifier` as parameter: "MUST be named "modifier" and MUST appear as the **first optional parameter** in the element function's parameter list". Example:

```kotlin
@Composable
fun MyComposable(
    name: String,
    foo: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(start = 16.dp)
            .fillMaxWidth()
    ) {
        Text(name)
        Text(foo)
    }
}
```

- Composable functions should not emit content at their top level. 

❌

``` kotlin
@Composable
fun MyComposable() {
	Text("Hello")
	Text("Foo")
}
```
This composable function will behave differently depending on its parent composable (if it's a Row, a Column, a Box, etc). Always "scope" the content inside a container: 

✅

```kotlin
@Composable
fun MyComposable() {
	Column(...){
		Text("Hello")
		Text("Foo")
	}
}

or 

@Composable
fun ColumnScope.MyComposable() {
	Text("Hello")
	Text("Foo")
}
```

- Composable functions that emit content should always return unit
- Don't acquire the viewModel inside a composable function, this will make testing harder. Inject it as a parameter and provide a default value to facilitate reusability: 

❌

``` kotlin
@Composable
fun MyComposable() {
	val viewModel by viewModel<MyViewModel>()
	...
}
```

✅

``` kotlin
@Composable
fun MyComposable(viewModel : MyViewModel = getViewModel()) {
	...
}
```

- Don't pass mutable types (mutableList, mutableState, etc) as parameters to @Composable functions. Always use immutable types. 
- Always "remember" `mutableStateOf / derivedStateOf` inside a composable function


# Navigation 🗺 <a name="navigation"></a>

Currently, we are using Compose through `ComposeView` nested inside a `Fragment` as the root view in a 1:1 relationship. With this kind of usage, Navigation remains unchanged, we can keep using the existing navitation_graphs.xml. 
There is one thing to keep in mind when using this `ComposeView` approach. Compose views involve ongoing work and registering the composition with external event sources. These registrations can cause the composition to remain live and ineligible for garbage collection for long after the host View may have been abandoned. To avoid any leaks Android provides [ViewCompositionStrategy](https://developer.android.com/reference/kotlin/androidx/compose/ui/platform/ViewCompositionStrategy)  for disposing the composition automatically at an appropriate time. The recommended strategy for the `Fragment` <--> `ComposeView` approach is [DisposeOnViewTreeLifecycleDestroyed](https://developer.android.com/reference/kotlin/androidx/compose/ui/platform/ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

We can add the `ComposeView` to the fragment directly from the code. That way we avoid creating unnecessary layout xml files. An example of how to do this can be found [here](https://developer.android.com/jetpack/compose/interop/interop-apis#:~:text=You%20can%20also%20include%20a%20ComposeView%20directly%20in%20a%20fragment). Just add `ComposeView` from the fragment's `onCreateView` function: 

```kotlin
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            // Dispose of the Composition when the view's LifecycleOwner
            // is destroyed
            setViewCompositionStrategy(DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                WooThemeWithBackground {
                    // Compose code here.
                }
            }
        }
    }
```


# Accessibility ♿️ <a name="accessibility"></a>

Most of the rules that apply for Android's view system apply for Compose UI. But is worth highlighting a few concepts and tools to handle accessibility nicely. 

The key idea to make Composable components accessible is [Semantics](https://developer.android.com/jetpack/compose/semantics). Accessibility services use the `Semantics tree` to provide information to the people using the services (like talkback), and the UI testing framework uses it to make assertions. To keep your Composable UI accessible: 
- For composables and modifiers from the Compose foundation and material library, the Semantics tree is automatically filled and generated for you
- When adding custom low-level composables, you will have to manually provide its semantics
- Set `contentDescriptions` for relevant icons and images. Describe the meaning of the icon, not what it is. 
- Use `Modifier.semantics(mergeDescendants = true)` to group content and facilitate navigating through UI components with "TalkBack" tool.
-  Accessibility services such as TalkBack provide navigation shortcuts. One of those is jumping between headers to skip to the content the user is interested in. You can inform accessibility services that something is a header using `Modifier.semantics { heading() }`
-  You can also add state descriptions to inform if an item is at a certain state. For example, selected or unselected: 
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

[Testing UI in Compose](https://developer.android.com/jetpack/compose/testing) is pretty similar to testing UI with Espresso. Compose provides a set of testing APIs to find elements, verify their attributes and perform user actions. They also include advanced features such as time manipulation. 
To interact with compose UI elements the tests need to add `ComposetestRule`
```kotlin 

class MyComposeTest {

    @get:Rule
    val composeTestRule = createComposeRule()
    
    ...
}
```
That will enable the following interactions with elements in the UI: 
- **Finders** let you select one or multiple elements (or nodes in the Semantics tree) to make assertions or perform actions on them.
- **Assertions** are used to verify that the elements exist or have certain attributes.
- **Actions** inject simulated user events on the elements, such as clicks or other gestures.

**Synchronization**

Expresso tests offer idling resources to deal with "waiting" for data loaded in the background to be available on the screen before the test proceeds to make validations. We are currently not using `idling` resources for this in our view based UI tests. We are using `Thread.sleep()` in some places which is not recommended as it makes UI tests slower than they should be and leads to flaky tests sometimes.
```kotlin
    fun idleFor(milliseconds: Int) {
        try {
            Thread.sleep(milliseconds.toLong())
        } catch (ex: Exception) {
            // do nothing
        }
    }
```

In Compose UI tests we can easily avoid this kind of workaround. The [recommended way](https://medium.com/androiddevelopers/alternatives-to-idling-resources-in-compose-tests-8ae71f9fc473#:~:text=Option%203%3A%20Waiting%20for%20things%20the%20right%20way!) to deal with waiting for background tasks to run and update the UI is using `waitUntil`. Example: 
```kotlin 
composeTestRule.waitUntil {
    composeTestRule
        .onAllNodesWithText("Welcome")
        .fetchSemanticsNodes().size == 1
}
```



