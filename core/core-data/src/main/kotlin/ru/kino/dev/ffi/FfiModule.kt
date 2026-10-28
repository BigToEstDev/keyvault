package ru.kino.dev.ffi

import org.koin.dsl.module
import ru.kino.dev.core.NativeCore

/**
 * Binds the native implementation of the core.
 *
 * Single is not a convenience here: the native library holds the open databases in process global state
 * keyed by `db_key`, so there is exactly one core per process no matter how many callers there are.
 */
internal val ffiModule = module {
    single<NativeCore> { NativeCoreFfi() }
}
