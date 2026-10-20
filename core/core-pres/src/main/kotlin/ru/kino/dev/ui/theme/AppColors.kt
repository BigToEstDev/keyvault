package ru.kino.dev.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * The colors of the app that the scheme of Material 3 has no role for - the service colors of `Color.kt`,
 * one set per theme; [KeyVaultTheme] provides the one of its theme.
 *
 * @param passwordDigit the digits of a secret shown in clear (pass-docs, Step 58)
 * @param passwordSymbol everything of a secret that is neither a letter nor a digit
 */
@Immutable
data class AppColors(
    val passwordDigit: Color,
    val passwordSymbol: Color,
)

internal val LightAppColors = AppColors(
    passwordDigit = PasswordDigitLight1565C0,
    passwordSymbol = PasswordSymbolLightD84315,
)

internal val DarkAppColors = AppColors(
    passwordDigit = PasswordDigitDark82B1FF,
    passwordSymbol = PasswordSymbolDarkFFAB91,
)

/** The [AppColors] of the current theme. */
val LocalAppColors = staticCompositionLocalOf { LightAppColors }
