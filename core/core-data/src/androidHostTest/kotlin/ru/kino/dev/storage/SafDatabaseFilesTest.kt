package ru.kino.dev.storage

import android.content.pm.ProviderInfo
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import ru.kino.dev.storage.TestDocumentProvider.Behaviour

// A database that got smaller must not keep the tail of the longer one, whatever the provider does with "wt"
// (pass-docs, Step 112)
@RunWith(RobolectricTestRunner::class)
class SafDatabaseFilesTest {

    @get:Rule
    val folder = TemporaryFolder()

    private lateinit var provider: TestDocumentProvider
    private val files = SafDatabaseFiles(RuntimeEnvironment.getApplication())

    @Before
    fun setUp() {
        provider = Robolectric.buildContentProvider(TestDocumentProvider::class.java)
            .create(ProviderInfo().apply { authority = AUTHORITY })
            .get()
        provider.file = folder.newFile().apply { writeBytes(LONGER) }
    }

    @Test
    fun `a provider that truncates keeps only the new bytes`() = runTest {
        provider.behaviour = Behaviour.Truncates

        files.writeReplacing(URI, SHORTER)

        assertArrayEquals(SHORTER, provider.file.readBytes())
    }

    @Test
    fun `a provider that ignores the truncate of wt keeps only the new bytes`() = runTest {
        provider.behaviour = Behaviour.IgnoresTruncate

        files.writeReplacing(URI, SHORTER)

        assertArrayEquals(SHORTER, provider.file.readBytes())
    }

    private companion object {
        const val AUTHORITY = "ru.kino.dev.test.documents"
        const val URI = "content://$AUTHORITY/document/base.kdbx"
        val LONGER = ByteArray(64) { 1 }
        val SHORTER = ByteArray(16) { 2 }
    }
}
