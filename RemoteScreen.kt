package com.starled.remote.hk320df.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.starled.remote.hk320df.InfraredProtocol
import com.starled.remote.hk320df.RemoteCommandRepository
import com.starled.remote.hk320df.StarLEDInfraredController
import com.starled.remote.hk320df.ui.theme.*

@Composable
fun RemoteScreen(
    controller: StarLEDInfraredController,
    repository: RemoteCommandRepository,
    onNavigateToDiscovery: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isArabic by remember { mutableStateOf(false) }
    var lastTransmittedInfo by remember { mutableStateOf<String?>(null) }
    var isLedActive by remember { mutableStateOf(false) }
    val activeProfile = repository.getActiveProfile()
    val hwState = remember { controller.checkHardwareState() }

    LaunchedEffect(isLedActive) {
        if (isLedActive) {
            kotlinx.coroutines.delay(120)
            isLedActive = false
        }
    }

    fun handlePress(buttonId: String) {
        val btn = activeProfile.buttons[buttonId]
        if (btn != null) {
            isLedActive = true
            val result = controller.transmitCommand(
                address = btn.defaultAddress,
                command = btn.defaultCommand,
                isExtended = btn.isExtended
            )
            lastTransmittedInfo = when (result) {
                is StarLEDInfraredController.TransmissionResult.Success -> {
                    "Sent ${btn.labelEn} (${result.signal.addressHex}:${result.signal.commandHex})"
                }
                is StarLEDInfraredController.TransmissionResult.HardwareUnavailable -> {
                    "Simulated (No IR Blaster): ${btn.labelEn}"
                }
                is StarLEDInfraredController.TransmissionResult.FrequencyNotSupported -> {
                    "Frequency error: 38kHz unsupported"
                }
                is StarLEDInfraredController.TransmissionResult.Failure -> {
                    "Error: ${result.error}"
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CharcoalBackground)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top App Bar & Quick Settings
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "StarLED HK320DF",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = activeProfile.name,
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                FilterChip(
                    selected = isArabic,
                    onClick = { isArabic = !isArabic },
                    label = { Text(if (isArabic) "AR" else "EN", fontSize = 11.sp) },
                    modifier = Modifier.padding(end = 6.dp)
                )

                IconButton(onClick = onNavigateToDiscovery) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "IR Code Discovery Wizard",
                        tint = AccentAmber
                    )
                }
            }
        }

        // Hardware Status Banner
        if (hwState is StarLEDInfraredController.HardwareState.NoHardware) {
            Surface(
                color = Color(0xFF332020),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = PowerRed,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Phone lacks Consumer IR emitter. Interactive preview & timing generation active.",
                        color = Color(0xFFFFD4D4),
                        fontSize = 11.sp
                    )
                }
            }
        }

        // Diagnostic Feedback Line
        AnimatedVisibility(visible = lastTransmittedInfo != null) {
            Text(
                text = lastTransmittedInfo ?: "",
                color = AccentAmber,
                fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // THE PHYSICAL REMOTE CASING
        Box(
            modifier = Modifier
                .width(320.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF262A34),
                            Color(0xFF1C1F26),
                            Color(0xFF181A20)
                        )
                    )
                )
                .border(1.5.dp, Color(0xFF363B48), RoundedCornerShape(32.dp))
                .padding(horizontal = 16.dp, vertical = 20.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Top Transmitter Diode & Brand Plate
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(if (isLedActive) Color(0xFFFF2A85) else Color(0xFF3A4150))
                        .border(1.dp, Color(0xFF555F73), CircleShape)
                )

                Text(
                    text = "StarLED",
                    color = Color(0xFFE2E8F0),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )

                // 1. TOP ROW: Power (Red) & Mute (Green)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    RemoteKeyButton(
                        label = if (isArabic) "تشغيل" else "POWER",
                        icon = Icons.Default.PowerSettingsNew,
                        color = PowerRed,
                        textColor = Color.White,
                        onClick = { handlePress("POWER") },
                        modifier = Modifier.width(90.dp)
                    )

                    RemoteKeyButton(
                        label = if (isArabic) "كتم" else "MUTE",
                        icon = Icons.Default.VolumeMute,
                        color = MuteGreen,
                        textColor = Color.White,
                        onClick = { handlePress("MUTE") },
                        modifier = Modifier.width(90.dp)
                    )
                }

                // 2. NUMERIC KEYPAD
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        NumpadButton("1", if (isArabic) "١" else "1") { handlePress("NUM_1") }
                        NumpadButton("2", if (isArabic) "٢" else "2") { handlePress("NUM_2") }
                        NumpadButton("3", if (isArabic) "٣" else "3") { handlePress("NUM_3") }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        NumpadButton("4", if (isArabic) "٤" else "4") { handlePress("NUM_4") }
                        NumpadButton("5", if (isArabic) "٥" else "5") { handlePress("NUM_5") }
                        NumpadButton("6", if (isArabic) "٦" else "6") { handlePress("NUM_6") }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        NumpadButton("7", if (isArabic) "٧" else "7") { handlePress("NUM_7") }
                        NumpadButton("8", if (isArabic) "٨" else "8") { handlePress("NUM_8") }
                        NumpadButton("9", if (isArabic) "٩" else "9") { handlePress("NUM_9") }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        SmallFuncButton("-/--", if (isArabic) "-/--" else "-/--") { handlePress("DASH") }
                        NumpadButton("0", if (isArabic) "٠" else "0") { handlePress("NUM_0") }
                        SmallFuncButton("HOME", if (isArabic) "رئيسية" else "HOME") { handlePress("HOME") }
                    }
                }

                Divider(color = Color(0xFF2C3240), thickness = 1.dp)

                // 3. VOLUME & CHANNEL CONTROLS WITH P.MODE & S.MODE
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Vertical VOL rocker
                    Column(
                        modifier = Modifier
                            .width(64.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF2B313E))
                            .border(1.dp, Color(0xFF3B4354), RoundedCornerShape(14.dp))
                    ) {
                        RockerHalf(label = "VOL +", icon = Icons.Default.Add) { handlePress("VOL_UP") }
                        Divider(color = Color(0xFF384050), thickness = 1.dp)
                        RockerHalf(label = "VOL -", icon = Icons.Default.Remove) { handlePress("VOL_DOWN") }
                    }

                    // Center: P.MODE & S.MODE
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PillButton(label = if (isArabic) "وضع الصورة" else "P.MODE") { handlePress("P_MODE") }
                        PillButton(label = if (isArabic) "وضع الصوت" else "S.MODE") { handlePress("S_MODE") }
                    }

                    // Vertical CH rocker
                    Column(
                        modifier = Modifier
                            .width(64.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF2B313E))
                            .border(1.dp, Color(0xFF3B4354), RoundedCornerShape(14.dp))
                    ) {
                        RockerHalf(label = "CH +", icon = Icons.Default.KeyboardArrowUp) { handlePress("CH_UP") }
                        Divider(color = Color(0xFF384050), thickness = 1.dp)
                        RockerHalf(label = "CH -", icon = Icons.Default.KeyboardArrowDown) { handlePress("CH_DOWN") }
                    }
                }

                // 4. ADDITIONAL CONTROLS: ASPECT, INPUT, RECORD, DISPLAY
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MiniButton(if (isArabic) "أبعاد" else "ASPECT") { handlePress("ASPECT") }
                    MiniButton(if (isArabic) "مصدر" else "INPUT") { handlePress("INPUT") }
                    MiniButton(if (isArabic) "تسجيل" else "REC", textColor = PowerRed) { handlePress("RECORD") }
                    MiniButton(if (isArabic) "عرض" else "DISP") { handlePress("DISPLAY") }
                }

                // 5. NAVIGATION D-PAD
                Box(
                    modifier = Modifier
                        .size(190.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF242933))
                        .border(1.dp, Color(0xFF384052), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    // UP
                    IconButton(
                        onClick = { handlePress("NAV_UP") },
                        modifier = Modifier.align(Alignment.TopCenter).padding(top = 4.dp)
                    ) {
                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Up", tint = TextPrimary)
                    }

                    // DOWN
                    IconButton(
                        onClick = { handlePress("NAV_DOWN") },
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp)
                    ) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Down", tint = TextPrimary)
                    }

                    // LEFT
                    IconButton(
                        onClick = { handlePress("NAV_LEFT") },
                        modifier = Modifier.align(Alignment.CenterStart).padding(start = 4.dp)
                    ) {
                        Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Left", tint = TextPrimary)
                    }

                    // RIGHT
                    IconButton(
                        onClick = { handlePress("NAV_RIGHT") },
                        modifier = Modifier.align(Alignment.CenterEnd).padding(end = 4.dp)
                    ) {
                        Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Right", tint = TextPrimary)
                    }

                    // CENTER ENTER
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF323948))
                            .border(1.dp, Color(0xFF475266), CircleShape)
                            .clickable { handlePress("ENTER") },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isArabic) "موافق" else "ENTER",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }

                // SLEEP, MENU, EXIT Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MiniButton(if (isArabic) "نوم" else "SLEEP") { handlePress("SLEEP") }
                    MiniButton(if (isArabic) "قائمة" else "MENU") { handlePress("MENU") }
                    MiniButton(if (isArabic) "خروج" else "EXIT") { handlePress("EXIT") }
                }

                // 6. COLORED TELETEXT BUTTONS
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    ColorButton(Color(0xFFDC2626), "TTX/CC") { handlePress("COLOR_RED") }
                    ColorButton(Color(0xFF16A34A), "SIZE") { handlePress("COLOR_GREEN") }
                    ColorButton(Color(0xFFEAB308), "REVEAL") { handlePress("COLOR_YELLOW") }
                    ColorButton(Color(0xFF2563EB), "SUBPAGE") { handlePress("COLOR_BLUE") }
                }

                // 7. MEDIA BUTTONS
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MediaIconButton(Icons.Default.PlayArrow) { handlePress("MEDIA_PLAY_PAUSE") }
                    MediaIconButton(Icons.Default.FastRewind) { handlePress("MEDIA_REWIND") }
                    MediaIconButton(Icons.Default.FastForward) { handlePress("MEDIA_FAST_FORWARD") }
                    MediaIconButton(Icons.Default.Stop) { handlePress("MEDIA_STOP") }
                    MediaIconButton(Icons.Default.SkipPrevious) { handlePress("MEDIA_PREV") }
                    MediaIconButton(Icons.Default.SkipNext) { handlePress("MEDIA_NEXT") }
                }

                // 8. BOTTOM ROW: YouTube, EPG, Netflix
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    AppButton("YouTube", Color(0xFFDC2626)) { handlePress("APP_YOUTUBE") }
                    AppButton("EPG", Color(0xFF3B4354)) { handlePress("APP_EPG") }
                    AppButton("Netflix", Color(0xFFE50914)) { handlePress("APP_NETFLIX") }
                }

                Text(
                    text = "HK320DF",
                    color = Color(0xFF64748B),
                    fontSize = 11.sp,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun NumpadButton(num: String, label: String, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.92f else 1f, label = "press")

    Box(
        modifier = Modifier
            .size(76.dp, 40.dp)
            .scale(scale)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF292E3A))
            .border(1.dp, Color(0xFF384050), RoundedCornerShape(8.dp))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text = label, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SmallFuncButton(tag: String, label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(76.dp, 40.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF232732))
            .border(1.dp, Color(0xFF343B4A), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text = label, color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun RockerHalf(label: String, icon: ImageVector, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(18.dp))
            Text(label, color = TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun PillButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(80.dp, 36.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF2A303D))
            .border(1.dp, Color(0xFF3E475A), RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun MiniButton(label: String, textColor: Color = TextPrimary, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(62.dp, 32.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF252A35))
            .border(1.dp, Color(0xFF363D4E), RoundedCornerShape(6.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = textColor, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun ColorButton(bg: Color, text: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(64.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp, 16.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(bg)
                .clickable(onClick = onClick)
        )
        Text(text, color = TextSecondary, fontSize = 8.sp, textAlign = TextAlign.Center)
    }
}

@Composable
fun MediaIconButton(icon: ImageVector, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(Color(0xFF262B37))
            .border(1.dp, Color(0xFF394154), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(16.dp))
    }
}

@Composable
fun AppButton(name: String, brandColor: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(88.dp, 34.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1E222B))
            .border(1.dp, brandColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(name, color = brandColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun RemoteKeyButton(
    label: String,
    icon: ImageVector? = null,
    color: Color,
    textColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.94f else 1f, label = "press")

    Box(
        modifier = modifier
            .height(44.dp)
            .scale(scale)
            .clip(RoundedCornerShape(10.dp))
            .background(color)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = textColor, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(label, color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}
