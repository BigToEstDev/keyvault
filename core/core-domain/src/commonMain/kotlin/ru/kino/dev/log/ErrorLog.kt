package ru.kino.dev.log

/**
 * Where failures nobody expected go: the "something went wrong" case of a screen, with its cause.
 *
 * An interface so that processors stay testable on the jvm, where `android.util.Log` is not there. In the domain
 * since Step 86 cmp (pass-docs): the data modules write into it too - the lock of `vault` - and they are below pres.
 * Nothing secret is written: the failures of the core carry no credentials (pass-docs, Step 31).
 */
interface ErrorLog {

    fun unexpected(where: String, error: Throwable)
}
