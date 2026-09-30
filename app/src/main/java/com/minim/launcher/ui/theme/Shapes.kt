package com.minim.launcher.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Nothing OS: background radius equal to Android 16 as requested.
val NothingShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp)
)

// Android 16 / Material 3 Expressive: large, confident, continuous-feeling
// corners and a bigger jump between sizes.
val Android16Shapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp)
)

// iOS 26 Liquid Glass: soft continuous rounding on every surface, closer to
// Apple's squircle than Material's circular rounding, approximated here with
// a slightly smaller radius set so panels don't look over-rounded when
// translucent.
val GlassShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(22.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

fun shapesFor(designLanguage: DesignLanguage): Shapes = when (designLanguage) {
    DesignLanguage.NOTHING -> NothingShapes
    DesignLanguage.ANDROID_16 -> Android16Shapes
    DesignLanguage.GLASS -> GlassShapes
}

/**
 * App icon shape is a separate, user-controlled setting rather than tied
 * to design language — someone running Nothing's sharp-cornered look may
 * still prefer circular icons, and vice versa.
 */
fun iconShapeFor(name: String) = when (name) {
    "squircle" -> androidx.compose.foundation.shape.RoundedCornerShape(30)
    "roundedSquare" -> RoundedCornerShape(10.dp)
    else -> androidx.compose.foundation.shape.CircleShape
}
