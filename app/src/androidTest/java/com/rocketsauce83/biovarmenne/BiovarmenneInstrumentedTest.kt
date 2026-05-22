package com.rocketsauce83.biovarmenne

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*

@RunWith(AndroidJUnit4::class)
class BiovarmenneInstrumentedTest {

    @Test
    fun appContext_packageName_isCorrect() {
        val appContext = InstrumentationRegistry
            .getInstrumentation().targetContext
        assertEquals(
            "com.rocketsauce83.biovarmenne",
            appContext.packageName
        )
    }

    @Test
    fun secureStorage_saveAndRetrievePin_returnsCorrectPin() {
        val appContext = InstrumentationRegistry
            .getInstrumentation().targetContext
        val storage = SecurePinStorage(appContext)

        storage.savePin("1234")
        assertEquals("1234", storage.getPin())

        // Cleanup
        storage.clearPin()
    }

    @Test
    fun secureStorage_hasPin_returnsTrueAfterSave() {
        val appContext = InstrumentationRegistry
            .getInstrumentation().targetContext
        val storage = SecurePinStorage(appContext)

        storage.savePin("5678")
        assertEquals(true, storage.hasPin())

        // Cleanup
        storage.clearPin()
    }

    @Test
    fun secureStorage_hasPin_returnsFalseAfterClear() {
        val appContext = InstrumentationRegistry
            .getInstrumentation().targetContext
        val storage = SecurePinStorage(appContext)

        storage.savePin("1234")
        storage.clearPin()
        assertEquals(false, storage.hasPin())
    }

    @Test
    fun secureStorage_getPin_returnsNullAfterClear() {
        val appContext = InstrumentationRegistry
            .getInstrumentation().targetContext
        val storage = SecurePinStorage(appContext)

        storage.savePin("1234")
        storage.clearPin()
        assertNull(storage.getPin())
    }
}