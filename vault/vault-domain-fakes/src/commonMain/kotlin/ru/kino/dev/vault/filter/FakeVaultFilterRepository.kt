package ru.kino.dev.vault.filter

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.IOException

/**
 * [VaultFilterRepository] in memory, for tests of what is built on it. [saved] is every filter saved, in
 * order; with [failSaving] a save throws, as a device that does not take it would.
 */
class FakeVaultFilterRepository(initial: VaultFilter = VaultFilter.DEFAULT) : VaultFilterRepository {

    private val current = MutableStateFlow(initial)

    val saved = mutableListOf<VaultFilter>()

    var failSaving = false

    override val filter: Flow<VaultFilter> = current

    override suspend fun save(filter: VaultFilter) {
        if (failSaving) throw IOException("the device does not take it")
        saved += filter
        current.value = filter
    }
}
