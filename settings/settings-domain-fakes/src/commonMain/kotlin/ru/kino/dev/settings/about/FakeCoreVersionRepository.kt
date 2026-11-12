package ru.kino.dev.settings.about

/** The version of the core in memory; [fail] - the core does not answer. */
class FakeCoreVersionRepository(
    private val version: String = "1.0.0",
    private val fail: Boolean = false,
) : CoreVersionRepository {

    override suspend fun coreVersion(): String {
        if (fail) error("the core does not answer")
        return version
    }
}
