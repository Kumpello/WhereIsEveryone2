package com.kumpello.whereiseveryone.main.map.presentation

import com.kumpello.whereiseveryone.common.domain.manager.PreferencesKey
import com.kumpello.whereiseveryone.common.domain.manager.PreferencesManager
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException

class LocationSharingGateTest {
    private val preferences = mockk<PreferencesManager>()
    private var savedEnabled: Boolean? = true

    private fun gate(): LocationSharingGate {
        coEvery { preferences.get(PreferencesKey.LocationSharingEnabled) } coAnswers { savedEnabled }
        coEvery { preferences.save(PreferencesKey.LocationSharingEnabled, any()) } coAnswers {
            savedEnabled = secondArg()
        }
        return LocationSharingGate(preferences)
    }

    @Test
    fun `pause survives repeated start requests and service recreation until explicit resume`() = runTest {
        val gate = gate()
        assertEquals(true, gate.readEnabledForStart())
        gate.saveEnabled(false)
        repeat(3) {
            gate.invalidateStarts() // Lifecycle/mode changes stop the previous request.
            assertEquals(false, gate.readEnabledForStart())
        }
        val recreated = LocationSharingGate(preferences)
        assertEquals(false, recreated.readEnabledForStart())
        recreated.saveEnabled(true)
        assertEquals(true, recreated.readEnabledForStart())
    }

    @Test
    fun `stop invalidates a preference read already in flight`() = runTest {
        val gate = gate()
        val read = CompletableDeferred<Boolean?>()
        coEvery { preferences.get(PreferencesKey.LocationSharingEnabled) } coAnswers { read.await() }
        val start = async(start = CoroutineStart.UNDISPATCHED) { gate.readEnabledForStart() }
        gate.invalidateStarts()
        read.complete(true)
        assertNull(start.await())
    }

    @Test
    fun `starts remain blocked while stop preference is being saved`() = runTest {
        val gate = gate()
        val write = CompletableDeferred<Unit>()
        coEvery { preferences.save(PreferencesKey.LocationSharingEnabled, false) } coAnswers {
            write.await()
            savedEnabled = false
        }
        val stop = async(start = CoroutineStart.UNDISPATCHED) { gate.saveEnabled(false) }
        assertNull(gate.readEnabledForStart())
        write.complete(Unit)
        stop.await()
        assertEquals(false, gate.readEnabledForStart())
    }

    @Test
    fun `failed persistence fails closed until an explicit successful write`() = runTest {
        val gate = gate()
        coEvery { preferences.save(PreferencesKey.LocationSharingEnabled, false) } throws IOException("Disk failure")
        try {
            gate.saveEnabled(false)
            fail("Expected write failure")
        } catch (_: IOException) {
            assertNull(gate.readEnabledForStart())
        }
        gate.saveEnabled(true)
        assertEquals(true, gate.readEnabledForStart())
    }

    @Test
    fun `new installation retains existing default sharing behavior`() = runTest {
        val gate = gate()
        savedEnabled = null
        assertEquals(true, gate.readEnabledForStart())
    }
}
