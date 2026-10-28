package ru.kino.dev.settings.pres.common

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import org.orbitmvi.orbit.syntax.Syntax
import java.io.IOException

/**
 * A setting chosen on a screen of `settings`: shown at once, written in the background - only the store of
 * the app settings, not the database, so the screen does not wait and nothing is disabled meanwhile
 * (pass-docs, Step 56, item 4). Not written - shows what [kept] holds, the truth of the file rather than what
 * was shown before: of a few quick taps one may fail while another went through - and [notSaved] for the
 * snackbar. Nothing until the setting is read ([current] null), or when it is the same.
 */
internal suspend fun <S : Any, SE : Any, T : Any> Syntax<S, SE>.keepSetting(
    chosen: T,
    current: T?,
    kept: Flow<T>,
    show: S.(T) -> S,
    notSaved: S.() -> S,
    save: suspend (T) -> Unit,
) {
    if (current == null || chosen == current) return

    reduce { state.show(chosen) }
    try {
        save(chosen)
    } catch (_: IOException) {
        val actual = kept.first()
        reduce { state.show(actual).notSaved() }
    }
}
