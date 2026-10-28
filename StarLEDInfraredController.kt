package com.starled.remote.hk320df

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.ConsumerIrManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log

/**
 * Controller component responsible for hardware abstraction, ConsumerIrManager interaction,
 * carrier frequency validation, and safe transmission of physical infrared signals.
 */
class StarLEDInfraredController(private val context: Context) {

    private val tag = "StarLEDIrController"
    private val consumerIrManager: ConsumerIrManager? =
        context.getSystemService(Context.CONSUMER_IR_SERVICE) as? ConsumerIrManager

    sealed class HardwareState {
        object Ready : HardwareState()
        data class NoHardware(val reason: String) : HardwareState()
        data class UnsupportedFrequency(val targetFreq: Int) : HardwareState()
    }

    sealed class TransmissionResult {
        data class Success(val signal: EncodedSignal) : TransmissionResult()
        data class HardwareUnavailable(val message: String) : TransmissionResult()
        data class FrequencyNotSupported(val requestedFreq: Int) : TransmissionResult()
        data class Failure(val error: String) : TransmissionResult()
    }

    /**
     * Inspects whether the target Android device (e.g. Infinix HOT 60 Pro+) has a functional IR emitter.
     */
    fun checkHardwareState(): HardwareState {
        val hasFeature = context.packageManager.hasSystemFeature(PackageManager.FEATURE_CONSUMER_IR)
        if (!hasFeature || consumerIrManager == null) {
            return HardwareState.NoHardware(
                "Device does not declare android.hardware.consumerir system feature."
            )
        }

        if (!consumerIrManager.hasIrEmitter()) {
            return HardwareState.NoHardware(
                "ConsumerIrManager reports no active infrared emitter is physically present."
            )
        }

        val supports38k = isFrequencySupported(InfraredProtocol.DEFAULT_CARRIER_FREQ)
        if (!supports38k) {
            return HardwareState.UnsupportedFrequency(InfraredProtocol.DEFAULT_CARRIER_FREQ)
        }

        return HardwareState.Ready
    }

    /**
     * Validates if the carrier frequency (e.g. 38,000 Hz) falls within the device's emitter ranges.
     */
    fun isFrequencySupported(frequency: Int): Boolean {
        if (consumerIrManager == null || !consumerIrManager.hasIrEmitter()) return false
        val ranges = consumerIrManager.carrierFrequencies ?: return true // If null, assume standard 38kHz supported
        for (range in ranges) {
            if (frequency in range.minFrequency..range.maxFrequency) {
                return true
            }
        }
        return false
    }

    /**
     * Formats supported frequency ranges into human-readable text for the diagnostics screen.
     */
    fun getSupportedFrequencySummary(): String {
        if (consumerIrManager == null || !consumerIrManager.hasIrEmitter()) {
            return "None (IR hardware absent)"
        }
        val ranges = consumerIrManager.carrierFrequencies ?: return "Default 30 kHz - 60 kHz"
        return ranges.joinToString(", ") { "${it.minFrequency / 1000}kHz - ${it.maxFrequency / 1000}kHz" }
    }

    /**
     * Transmits a pulse pattern only when valid encoded timing data is available.
     * Never transmits raw hexadecimal directly.
     */
    fun transmitCommand(
        address: Int,
        command: Int,
        isExtended: Boolean = false,
        triggerHaptic: Boolean = true
    ): TransmissionResult {
        if (triggerHaptic) {
            performHapticClick()
        }

        val hwState = checkHardwareState()
        if (hwState is HardwareState.NoHardware) {
            Log.w(tag, "IR transmission cancelled: ${hwState.reason}")
            return TransmissionResult.HardwareUnavailable(hwState.reason)
        }
        if (hwState is HardwareState.UnsupportedFrequency) {
            Log.w(tag, "IR transmission cancelled: Carrier ${hwState.targetFreq} Hz not supported")
            return TransmissionResult.FrequencyNotSupported(hwState.targetFreq)
        }

        return try {
            val encoded = InfraredProtocol.encodeNec(address, command, isExtended)
            consumerIrManager?.transmit(encoded.carrierFrequency, encoded.pattern)

            Log.i(
                tag,
                "IR Transmitted: Addr=${encoded.addressHex}, Cmd=${encoded.commandHex}, " +
                        "Pulses=${encoded.pattern.size}, Duration=${encoded.totalDurationUs / 1000.0}ms"
            )

            TransmissionResult.Success(encoded)
        } catch (e: Exception) {
            Log.e(tag, "Error during IR transmission", e)
            TransmissionResult.Failure(e.localizedMessage ?: "Unknown hardware transmission error")
        }
    }

    /**
     * Provides tactile haptic feedback for physical remote feel.
     */
    private fun performHapticClick() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                val vibrator = vibratorManager?.defaultVibrator
                vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(25, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(25)
                }
            }
        } catch (_: Exception) {
            // Ignore haptic failures gracefully
        }
    }
}
