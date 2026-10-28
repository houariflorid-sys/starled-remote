package com.starled.remote.hk320df

/**
 * Infrared Protocol Timing and Modulation Engine
 *
 * Implements the standard NEC protocol (38 kHz) and Extended NEC (16-bit address)
 * specifications used by StarLED 32-inch television chassis (CVTE, MStar, OEM).
 *
 * Conforms to Android's ConsumerIrManager.transmit(carrierFrequency, pattern) API,
 * where pattern is an alternating array of mark (pulse on) and space (pulse off) durations in microseconds.
 */
enum class ProtocolType {
    NEC,
    EXTENDED_NEC
}

data class EncodedSignal(
    val protocol: ProtocolType,
    val carrierFrequency: Int,
    val pattern: IntArray,
    val totalDurationUs: Long,
    val addressHex: String,
    val commandHex: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as EncodedSignal
        return protocol == other.protocol &&
                carrierFrequency == other.carrierFrequency &&
                pattern.contentEquals(other.pattern) &&
                totalDurationUs == other.totalDurationUs
    }

    override fun hashCode(): Int {
        var result = protocol.hashCode()
        result = 31 * result + carrierFrequency
        result = 31 * result + pattern.contentHashCode()
        result = 31 * result + totalDurationUs.hashCode()
        return result
    }
}

object InfraredProtocol {

    const val DEFAULT_CARRIER_FREQ = 38000 // 38 kHz
    private const val LEADER_MARK = 9000   // 9 ms burst
    private const val LEADER_SPACE = 4500  // 4.5 ms space
    private const val BIT_MARK = 562       // 562 µs carrier burst
    private const val ZERO_SPACE = 562     // 562 µs space for logical 0
    private const val ONE_SPACE = 1688     // 1688 µs space for logical 1
    private const val STOP_MARK = 562      // 562 µs trailing burst

    /**
     * Converts raw address and command parameters into an alternating microsecond timing pattern.
     *
     * @param address 8-bit standard address or 16-bit extended address
     * @param command 8-bit command byte
     * @param isExtended true to encode 16-bit address without byte inversion
     */
    fun encodeNec(
        address: Int,
        command: Int,
        isExtended: Boolean = false,
        carrierFrequency: Int = DEFAULT_CARRIER_FREQ
    ): EncodedSignal {
        require(command in 0x00..0xFF) { "Command must be an 8-bit value (0x00..0xFF), was: $command" }

        val addressLow: Int
        val addressHigh: Int

        if (isExtended) {
            require(address in 0x0000..0xFFFF) { "Extended address must be 16-bit (0x0000..0xFFFF)" }
            addressLow = address and 0xFF
            addressHigh = (address shr 8) and 0xFF
        } else {
            require(address in 0x00..0xFF) { "Standard address must be 8-bit (0x00..0xFF)" }
            addressLow = address and 0xFF
            addressHigh = (addressLow.inv()) and 0xFF // Inverted address byte
        }

        val commandByte = command and 0xFF
        val commandInverted = (commandByte.inv()) and 0xFF

        // NEC frame bytes transmitted LSB first: [AddressLow, AddressHigh, Command, ~Command]
        val bytes = intArrayOf(addressLow, addressHigh, commandByte, commandInverted)

        // 1 leader mark + 1 leader space + (32 bits * 2) + 1 stop mark = 67 elements
        val pattern = IntArray(67)
        var idx = 0

        pattern[idx++] = LEADER_MARK
        pattern[idx++] = LEADER_SPACE

        var totalUs = (LEADER_MARK + LEADER_SPACE).toLong()

        for (b in bytes) {
            for (bit in 0 until 8) {
                val bitValue = (b shr bit) and 1
                pattern[idx++] = BIT_MARK
                val space = if (bitValue == 1) ONE_SPACE else ZERO_SPACE
                pattern[idx++] = space
                totalUs += (BIT_MARK + space)
            }
        }

        pattern[idx] = STOP_MARK
        totalUs += STOP_MARK

        val addrHex = if (isExtended) {
            String.format("0x%04X", address)
        } else {
            String.format("0x%02X", address)
        }
        val cmdHex = String.format("0x%02X", command)

        return EncodedSignal(
            protocol = if (isExtended) ProtocolType.EXTENDED_NEC else ProtocolType.NEC,
            carrierFrequency = carrierFrequency,
            pattern = pattern,
            totalDurationUs = totalUs,
            addressHex = addrHex,
            commandHex = cmdHex
        )
    }
}
