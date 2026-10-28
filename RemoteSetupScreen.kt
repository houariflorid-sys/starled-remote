package com.starled.remote.hk320df.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.starled.remote.hk320df.HK320DFDiscoveryModule
import com.starled.remote.hk320df.RemoteCommandRepository
import com.starled.remote.hk320df.StarLEDInfraredController
import com.starled.remote.hk320df.ui.theme.*

@Composable
fun RemoteSetupScreen(
    controller: StarLEDInfraredController,
    repository: RemoteCommandRepository,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val discoveryModule = remember { HK320DFDiscoveryModule(controller, repository) }
    val testTargets = remember { discoveryModule.getEssentialTestTargets() }
    var selectedTargetIndex by remember { mutableIntStateOf(0) }
    var testState by remember { mutableStateOf<HK320DFDiscoveryModule.TestState>(HK320DFDiscoveryModule.TestState.Idle) }
    val hwState = remember { controller.checkHardwareState() }
    val activeProfile = repository.getActiveProfile()

    val currentTarget = testTargets.getOrNull(selectedTargetIndex)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CharcoalBackground)
            .statusBarsPadding()
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "IR Code Discovery Wizard",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "StarLED HK320DF (32-Inch Smart TV)",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Hardware Diagnostics Card
        Card(
            colors = CardDefaults.cardColors(
                containerColor = when (hwState) {
                    is StarLEDInfraredController.HardwareState.Ready -> Color(0xFF132E22)
                    else -> Color(0xFF2E1A1A)
                }
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (hwState is StarLEDInfraredController.HardwareState.Ready) Icons.Default.CheckCircle else Icons.Default.Info,
                        contentDescription = null,
                        tint = if (hwState is StarLEDInfraredController.HardwareState.Ready) MuteGreen else PowerRed,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (hwState is StarLEDInfraredController.HardwareState.Ready) "ConsumerIrManager: Emitter Active" else "Hardware Notice: No Internal IR",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 13.sp
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (hwState is StarLEDInfraredController.HardwareState.Ready) {
                        "Supported Carrier: ${controller.getSupportedFrequencySummary()}"
                    } else {
                        "Infinix HOT 60 Pro+ / Android preview mode. All NEC timing patterns are generated accurately for verification."
                    },
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Target Control Selector Tabs
        Text(
            text = "STEP 1: SELECT CONTROL TO VERIFY",
            color = AccentAmber,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )

        ScrollableTabRow(
            selectedTabIndex = selectedTargetIndex,
            containerColor = Color.Transparent,
            contentColor = TextPrimary,
            edgePadding = 0.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            testTargets.forEachIndexed { index, target ->
                Tab(
                    selected = selectedTargetIndex == index,
                    onClick = {
                        selectedTargetIndex = index
                        testState = HK320DFDiscoveryModule.TestState.Idle
                    },
                    text = { Text(target.displayName, fontSize = 12.sp) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Target details & candidates list
        if (currentTarget != null) {
            Text(
                text = currentTarget.testInstruction,
                color = TextSecondary,
                fontSize = 12.sp,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(currentTarget.candidateProfiles) { candidate ->
                    val isVerified = repository.isCommandVerified(candidate.profileId, currentTarget.buttonId)

                    Surface(
                        color = Color(0xFF222732),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                1.dp,
                                if (isVerified) MuteGreen else Color(0xFF333B4D),
                                RoundedCornerShape(10.dp)
                            )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = candidate.profileName,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = "${candidate.hexRepr} | Carrier: ${candidate.carrierFrequency / 1000}kHz",
                                        color = AccentAmber,
                                        fontSize = 11.sp
                                    )
                                }

                                if (isVerified) {
                                    Text(
                                        text = "VERIFIED",
                                        color = MuteGreen,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Button(
                                onClick = {
                                    discoveryModule.testCandidate(candidate) { state ->
                                        testState = state
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333D50)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Sensors, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Transmit Candidate Pattern", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Test Confirmation Panel
            when (val state = testState) {
                is HK320DFDiscoveryModule.TestState.AwaitingUserConfirmation -> {
                    Surface(
                        color = Color(0xFF2A2E3B),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                            .border(1.dp, AccentAmber, RoundedCornerShape(12.dp))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Did the StarLED TV respond to this signal?",
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontSize = 13.sp
                            )
                            Text(
                                text = state.message,
                                color = TextSecondary,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        discoveryModule.confirmResult(
                                            buttonId = currentTarget.buttonId,
                                            candidate = state.candidate,
                                            didTvRespond = true
                                        ) { res -> testState = res }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MuteGreen),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Yes, Responded")
                                }

                                Button(
                                    onClick = {
                                        discoveryModule.confirmResult(
                                            buttonId = currentTarget.buttonId,
                                            candidate = state.candidate,
                                            didTvRespond = false
                                        ) { res -> testState = res }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A5568)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("No Response")
                                }
                            }
                        }
                    }
                }
                is HK320DFDiscoveryModule.TestState.Confirmed -> {
                    Surface(
                        color = Color(0xFF143324),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    ) {
                        Text(
                            text = "Success: Working code saved to local profile!",
                            color = MuteGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
                is HK320DFDiscoveryModule.TestState.Rejected -> {
                    Surface(
                        color = Color(0xFF332020),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    ) {
                        Text(
                            text = "Code marked as non-functional for your TV.",
                            color = PowerRed,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
                is HK320DFDiscoveryModule.TestState.Error -> {
                    Text(
                        text = state.errorMsg,
                        color = PowerRed,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                else -> {}
            }
        }
    }
}
