package ru.kino.dev.welcome.pres.masterpassword

sealed interface MasterPasswordSideEffect {

    /** "Save as" for the database, with the name to suggest. */
    data class PickDatabase(val suggestedName: String) : MasterPasswordSideEffect
}
