package com.starled.remote.hk320df

/**
 * Infrared Code Discovery & Candidate Testing Module
 *
 * Implements candidate code evaluation, step-by-step verification flows for essential controls
 * (Power, Vol+, Vol-, Mute, Menu, Input, Navigation), and diagnostic logging.
 *
 * Adheres strictly to the principle:
 * "Never report successful transmission as proof that the television received or executed a command.
 * Only mark as VERIFIED once the human user explicitly confirms physical TV response."
 */
class HK320DFDiscoveryModule(
    private val controller: StarLEDInfraredController,
    private val repository: RemoteCommandRepository
) {

    data class CandidateTestItem(
        val buttonId: String,
        val displayName: String,
        val testInstruction: String,
        val candidateProfiles: List<CandidateEntry>
    )

    data class CandidateEntry(
        val profileId: String,
        val profileName: String,
        val address: Int,
        val command: Int,
        val isExtended: Boolean,
        val carrierFrequency: Int = InfraredProtocol.DEFAULT_CARRIER_FREQ,
        val hexRepr: String = String.format(
            if (isExtended) "Addr 0x%04X | Cmd 0x%02X" else "Addr 0x%02X | Cmd 0x%02X",
            address, command
        )
    )

    sealed class TestState {
        object Idle : TestState()
        data class Transmitting(val candidate: CandidateEntry) : TestState()
        data class AwaitingUserConfirmation(
            val candidate: CandidateEntry,
            val signal: EncodedSignal,
            val message: String
        ) : TestState()
        data class Confirmed(val candidate: CandidateEntry) : TestState()
        data class Rejected(val candidate: CandidateEntry, val reason: String) : TestState()
        data class Error(val errorMsg: String) : TestState()
    }

    /**
     * Builds test targets for essential remote controls.
     */
    fun getEssentialTestTargets(): List<CandidateTestItem> {
        val profiles = repository.getProfiles()
        val targets = listOf(
            Triple("POWER", "Power / Standby", "Point phone at StarLED TV. Press Test and check if screen turns ON/OFF."),
            Triple("MUTE", "Mute Audio", "Check if TV audio mutes or unmutes."),
            Triple("VOL_UP", "Volume Up (+)", "Check if on-screen volume slider increases."),
            Triple("VOL_DOWN", "Volume Down (-)", "Check if on-screen volume slider decreases."),
            Triple("MENU", "Main Menu", "Check if TV settings or main menu appears on screen."),
            Triple("INPUT", "Input / Source", "Check if input source list (HDMI/AV/TV) appears."),
            Triple("NAV_UP", "Navigation Up", "Check if cursor moves up on screen."),
            Triple("ENTER", "Enter / OK", "Check if selected menu item activates.")
        )

        return targets.map { (btnId, name, instruction) ->
            val candidates = profiles.mapNotNull { prof ->
                val btn = prof.buttons[btnId]
                if (btn != null) {
                    CandidateEntry(
                        profileId = prof.id,
                        profileName = prof.name,
                        address = btn.defaultAddress,
                        command = btn.defaultCommand,
                        isExtended = btn.isExtended
                    )
                } else null
            }
            CandidateTestItem(btnId, name, instruction, candidates)
        }
    }

    /**
     * Executes test transmission for a specific candidate entry.
     */
    fun testCandidate(
        candidate: CandidateEntry,
        onResult: (TestState) -> Unit
    ) {
        onResult(TestState.Transmitting(candidate))

        val result = controller.transmitCommand(
            address = candidate.address,
            command = candidate.command,
            isExtended = candidate.isExtended
        )

        when (result) {
            is StarLEDInfraredController.TransmissionResult.Success -> {
                val diagnosticMsg = "Physical pulse modulated at ${result.signal.carrierFrequency / 1000} kHz " +
                        "(${result.signal.pattern.size} pulses, ${result.signal.totalDurationUs / 1000.0} ms). " +
                        "Awaiting user visual/audio confirmation."
                onResult(TestState.AwaitingUserConfirmation(candidate, result.signal, diagnosticMsg))
            }
            is StarLEDInfraredController.TransmissionResult.HardwareUnavailable -> {
                onResult(TestState.Error("IR Blaster not detected: ${result.message}"))
            }
            is StarLEDInfraredController.TransmissionResult.FrequencyNotSupported -> {
                onResult(TestState.Error("Carrier frequency ${result.requestedFreq} Hz not supported by phone emitter."))
            }
            is StarLEDInfraredController.TransmissionResult.Failure -> {
                onResult(TestState.Error("Transmission failed: ${result.error}"))
            }
        }
    }

    /**
     * User confirmed whether the television responded.
     */
    fun confirmResult(
        buttonId: String,
        candidate: CandidateEntry,
        didTvRespond: Boolean,
        onComplete: (TestState) -> Unit
    ) {
        if (didTvRespond) {
            repository.saveVerifiedCommand(candidate.profileId, buttonId, true)
            repository.setActiveProfile(candidate.profileId)
            onComplete(TestState.Confirmed(candidate))
        } else {
            repository.saveVerifiedCommand(candidate.profileId, buttonId, false)
            onComplete(TestState.Rejected(candidate, "User reported TV did not respond."))
        }
    }
}
