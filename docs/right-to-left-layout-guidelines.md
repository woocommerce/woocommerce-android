Supporting Right-to-Left text is quite straightforward as most issues are handled automatically by the Android OS or are guarded by lint.

## Animations and Drawables

Whenever you add a new animation/drawable, consider whether it needs to be mirrored in RTL mode.

## Images/Icons

Writing direction also affects time flow direction -> some asymmetric images/icons, such as `reply` or `back`, need to be mirrored.

## Text alignment

Android automatically mirrors layouts in RtL mode. In rare cases the default text alignment (which is derived from the text language) needs to be overridden to keep the UI look consistent.

> **Example**: Lists can possibly contain items in both English and Hebrew. When the title `TextView` width is set to `wrap_content`, everything is handled correctly (see Img 1). But if the title `TextView` width is set to `match_parent`, the UI can become disarranged (see Img 2).

[[docs/images/Right-to-left/TextAlignment.png|width=600px]]

* To fix this issue set the text alignment explicitly to `viewStart` -> `android:textAlignment="viewStart"`.

* If using `ConstraintLayout`, set the view to `wrap_content`, and then use `app:layout_constrainedWidth="true"` along with `app:layout_constraintHorizontal_bias="0.0"`.
