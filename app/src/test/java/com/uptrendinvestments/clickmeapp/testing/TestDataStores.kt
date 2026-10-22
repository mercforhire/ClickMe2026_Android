package com.uptrendinvestments.clickmeapp.testing

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.CoroutineScope
import org.junit.rules.TemporaryFolder

/**
 * Creates a real Preferences DataStore backed by a file in [folder], driven by [scope] (use `backgroundScope`
 * inside `runTest` so the store is cancelled when the test ends). Pass a distinct [fileName] to create a second,
 * independent store in the same [folder].
 */
fun testPreferencesDataStore(
    scope: CoroutineScope,
    folder: TemporaryFolder,
    fileName: String = "test.preferences_pb",
): DataStore<Preferences> {
    val file = folder.newFile(fileName)
    // DataStore expects to create the file itself; an existing empty file is not a valid preferences proto.
    file.delete()
    return PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })
}
