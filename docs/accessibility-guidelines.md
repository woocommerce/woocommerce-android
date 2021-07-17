## Activity Titles

When an Activity comes to the foreground, TalkBack announces it’s title. When the activity has no title, TalkBack announces the name of the application which might confuse the user -> **_set a title to all visible activities_**, either in `AndroidManifest` or using `Activity.setTitle()` method.

## Images

* Illustrative **images** such as `ImageView`'s should have `importantForAccessibility` set to “no” -> `android:importantForAccessibility="no"`.

* **Buttons** such as `ImageButton`'s with labels should have `contentDescription` set to null. _Setting importanceForAccessibility to “no” makes them unfocusable in the accessibility mode_.

* `ImageButton`s without text labels **must** set a valid `contentDescription`. **Exclude the element type from the `contentDescription`**.
  * Most accessibility services, such as **TalkBack** and **BrailleBack**, automatically announce an elements type after announcing its label.

> _Example: The background image on the login prologue screen (below) does not require additional context because the text announces the intent of the view. The same is true of the JetPack login button._

[[/images/Accessibility/ImportantForAccessibility.png|width=300px]]

## Labels & Hints

* When a UI element is just a label for another element, set the `labelFor` attribute.

* When labeling editable elements, such as `EditText` objects, use the `android:hint` XML attribute for static elements and `setHint()` for dynamic elements.

## Grouping Content

* If users should treat a set of elements as a single unit of information, you can group these elements in a focusable container (use android:focusable=”true”).

> _Example: The **email note to customer** component was specifically built to better communicate the purpose of this settings by using a focusable container_

[[/images/Accessibility/grouping_content.png|width=300px]]

## Custom Views

* Make sure that custom views are accessible with both [Switch Access](https://support.google.com/accessibility/android/answer/6122836?hl=en) and [TalkBack](https://support.google.com/accessibility/android/answer/6283677?hl=en). Learn more about both of these accessibility features in the [Android developer docs](https://developer.android.com/guide/topics/ui/accessibility). Consider implementing accessibility functionality for them using [ExploreByTouchHelper](https://developer.android.com/reference/android/support/v4/widget/ExploreByTouchHelper).
