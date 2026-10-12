package bible.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val base = Typography()

private val typography = Typography(
    displayLarge = base.displayLarge.copy(fontFamily = Literata),
    headlineMedium = base.headlineMedium.copy(fontFamily = Literata),
    titleLarge = base.titleLarge.copy(fontFamily = Golos),
    titleMedium = base.titleMedium.copy(fontFamily = Golos),
    titleSmall = base.titleSmall.copy(fontFamily = Golos),
    bodyLarge = base.bodyLarge.copy(fontFamily = Golos),
    bodyMedium = base.bodyMedium.copy(fontFamily = Golos),
    bodySmall = base.bodySmall.copy(fontFamily = Golos),
    labelLarge = base.labelLarge.copy(fontFamily = Golos),
    labelMedium = base.labelMedium.copy(fontFamily = Golos),
    labelSmall = base.labelSmall.copy(fontFamily = Golos),
)

@Composable
fun BibleTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = BibleColors.Gold,
            background = BibleColors.Background,
            surface = BibleColors.Background,
            onBackground = BibleColors.Ink,
            onSurface = BibleColors.Ink,
            onSurfaceVariant = BibleColors.Muted,
            outlineVariant = BibleColors.Line,
            secondaryContainer = BibleColors.Tab,
        ),
        typography = typography,
        content = content,
    )
}
