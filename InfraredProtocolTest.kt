package com.starled.remote.hk320df

import org.junit.Assert.*
import org.junit.Test

class InfraredProtocolTest {

    @Test
    fun testNecEncodingProducesExact67PulsePattern() {
        val signal = InfraredProtocol.encodeNec(address = 0x04, command = 0x08, isExtended = false)

        assertEquals("Carrier frequency must be 38000 Hz", 38000, signal.carrierFrequency)
        assertEquals("Standard NEC must produce exactly 67 timing pulses", 67, signal.pattern.size)
        assertEquals("Leader mark must be 9000 us", 9000, signal.pattern[0])
        assertEquals("Leader space must be 4500 us", 4500, signal.pattern[1])
        assertEquals("Stop mark must be 562 us", 562, signal.pattern[66])
        assertEquals(ProtocolType.NEC, signal.protocol)
        assertEquals("0x04", signal.addressHex)
        assertEquals("0x08", signal.commandHex)
    }

    @Test
    fun testExtendedNecEncodes16BitAddress() {
        val signal = InfraredProtocol.encodeNec(address = 0xBF00, command = 0x12, isExtended = true)

        assertEquals(ProtocolType.EXTENDED_NEC, signal.protocol)
        assertEquals(67, signal.pattern.size)
        assertEquals("0xBF00", signal.addressHex)
        assertEquals("0x12", signal.commandHex)
    }

    @Test
    fun testInvalidCommandCodeThrowsException() {
        try {
            InfraredProtocol.encodeNec(address = 0x04, command = 0x1FF)
            fail("Expected IllegalArgumentException for out-of-range command 0x1FF")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("8-bit value") == true)
        }
    }
}
