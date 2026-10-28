package com.starled.remote.hk320df

import android.content.Context
import android.content.SharedPreferences

enum class CommandStatus {
    UNTESTED,
    CANDIDATE,
    VERIFIED,
    UNSUPPORTED
}

data class RemoteButton(
    val id: String,
    val labelEn: String,
    val labelAr: String,
    val defaultAddress: Int,
    val defaultCommand: Int,
    val isExtended: Boolean = false,
    var status: CommandStatus = CommandStatus.CANDIDATE
)

data class RemoteProfile(
    val id: String,
    val name: String,
    val chassis: String,
    val description: String,
    val defaultAddress: Int,
    val isExtended: Boolean,
    val buttons: Map<String, RemoteButton>
)

class RemoteCommandRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("starled_hk320df_prefs", Context.MODE_PRIVATE)

    companion object {
        const val PREF_ACTIVE_PROFILE = "active_profile_id"
        const val PREF_CONFIRMED_PREFIX = "confirmed_code_"
    }

    private val candidateProfiles: List<RemoteProfile> = listOf(
        createProfile(
            id = "starled_cvte_0x04",
            name = "Profile 1: StarLED HK320DF (CVTE Primary)",
            chassis = "CVTE TP.SK506.PB801 / DLED 32\"",
            description = "Most common factory profile for 32\" StarLED TVs with HK320DF remote marking. Standard NEC 0x04 address at 38kHz.",
            defaultAddress = 0x04,
            isExtended = false,
            powerCmd = 0x08,
            muteCmd = 0x0A,
            volUpCmd = 0x02,
            volDownCmd = 0x03,
            chUpCmd = 0x00,
            chDownCmd = 0x01,
            menuCmd = 0x0E,
            inputCmd = 0x0B
        ),
        createProfile(
            id = "starled_cvte_0x00",
            name = "Profile 2: StarLED Smart AOSP (CVTE 0x00)",
            chassis = "CVTE TP.MS338.PB801 / Smart 32\"",
            description = "Found in 32-inch Android smart TVs utilizing CVTE TP.MS338/MS358 series mainboards. Standard NEC 0x00 address.",
            defaultAddress = 0x00,
            isExtended = false,
            powerCmd = 0x12,
            muteCmd = 0x10,
            volUpCmd = 0x1A,
            volDownCmd = 0x1E,
            chUpCmd = 0x1B,
            chDownCmd = 0x1F,
            menuCmd = 0x13,
            inputCmd = 0x0F
        ),
        createProfile(
            id = "starled_ext_0xbf00",
            name = "Profile 3: StarLED Extended NEC (0xBF00)",
            chassis = "CVTE Extended 16-bit / OEM Haier-StarLED 32\"",
            description = "16-bit extended address format utilized by certain export batches with anti-crosstalk remote controllers.",
            defaultAddress = 0xBF00,
            isExtended = true,
            powerCmd = 0x12,
            muteCmd = 0x10,
            volUpCmd = 0x1A,
            volDownCmd = 0x1E,
            chUpCmd = 0x1B,
            chDownCmd = 0x1F,
            menuCmd = 0x13,
            inputCmd = 0x0F
        ),
        createProfile(
            id = "starled_mstar_0x01",
            name = "Profile 4: StarLED MStar Chassis (0x01)",
            chassis = "MStar MSD6A338 / 32\" DLED",
            description = "Found in classic StarLED non-smart & entry LED 32-inch TVs using MStar controller firmware.",
            defaultAddress = 0x01,
            isExtended = false,
            powerCmd = 0x48,
            muteCmd = 0x40,
            volUpCmd = 0x41,
            volDownCmd = 0x42,
            chUpCmd = 0x43,
            chDownCmd = 0x44,
            menuCmd = 0x0F,
            inputCmd = 0x0D
        )
    )

    fun getProfiles(): List<RemoteProfile> = candidateProfiles

    fun getActiveProfile(): RemoteProfile {
        val savedId = prefs.getString(PREF_ACTIVE_PROFILE, candidateProfiles.first().id)
        return candidateProfiles.find { it.id == savedId } ?: candidateProfiles.first()
    }

    fun setActiveProfile(profileId: String) {
        prefs.edit().putString(PREF_ACTIVE_PROFILE, profileId).apply()
    }

    fun saveVerifiedCommand(profileId: String, buttonId: String, confirmed: Boolean) {
        val key = "${PREF_CONFIRMED_PREFIX}${profileId}_${buttonId}"
        prefs.edit().putBoolean(key, confirmed).apply()
    }

    fun isCommandVerified(profileId: String, buttonId: String): Boolean {
        val key = "${PREF_CONFIRMED_PREFIX}${profileId}_${buttonId}"
        return prefs.getBoolean(key, false)
    }

    private fun createProfile(
        id: String,
        name: String,
        chassis: String,
        description: String,
        defaultAddress: Int,
        isExtended: Boolean,
        powerCmd: Int,
        muteCmd: Int,
        volUpCmd: Int,
        volDownCmd: Int,
        chUpCmd: Int,
        chDownCmd: Int,
        menuCmd: Int,
        inputCmd: Int
    ): RemoteProfile {
        val map = mutableMapOf<String, RemoteButton>()

        fun add(bId: String, en: String, ar: String, cmd: Int) {
            map[bId] = RemoteButton(bId, en, ar, defaultAddress, cmd, isExtended)
        }

        // Top Row
        add("POWER", "POWER", "تشغيل", powerCmd)
        add("MUTE", "MUTE", "كتم", muteCmd)

        // Numpad
        add("NUM_1", "1", "١", 0x10)
        add("NUM_2", "2", "٢", 0x11)
        add("NUM_3", "3", "٣", 0x12)
        add("NUM_4", "4", "٤", 0x14)
        add("NUM_5", "5", "٥", 0x15)
        add("NUM_6", "6", "٦", 0x16)
        add("NUM_7", "7", "٧", 0x18)
        add("NUM_8", "8", "٨", 0x19)
        add("NUM_9", "9", "٩", 0x1A)
        add("DASH", "-/--", "-/--", 0x1E)
        add("NUM_0", "0", "٠", 0x1D)
        add("HOME", "HOME", "الرئيسية", 0x5A)

        // Volume / Channel
        add("VOL_UP", "VOL +", "صوت +", volUpCmd)
        add("VOL_DOWN", "VOL -", "صوت -", volDownCmd)
        add("P_MODE", "P.MODE", "وضع الصورة", 0x44)
        add("S_MODE", "S.MODE", "وضع الصوت", 0x45)
        add("CH_UP", "CH +", "قناة +", chUpCmd)
        add("CH_DOWN", "CH -", "قناة -", chDownCmd)

        // Additional Controls
        add("ASPECT", "ASPECT", "أبعاد الشاشة", 0x48)
        add("INPUT", "INPUT", "المصدر", inputCmd)
        add("RECORD", "REC", "تسجيل", 0x51)
        add("DISPLAY", "DISPLAY", "عرض", 0x0F)

        // D-Pad
        add("NAV_UP", "UP", "أعلى", 0x40)
        add("NAV_DOWN", "DOWN", "أسفل", 0x41)
        add("NAV_LEFT", "LEFT", "يسار", 0x42)
        add("NAV_RIGHT", "RIGHT", "يمين", 0x43)
        add("ENTER", "ENTER", "موافق", 0x47)
        add("SLEEP", "SLEEP", "مؤقت النوم", 0x49)
        add("MENU", "MENU", "القائمة", menuCmd)
        add("EXIT", "EXIT", "خروج", 0x46)

        // Teletext
        add("COLOR_RED", "TTX/CC", "نص", 0x4C)
        add("COLOR_GREEN", "SIZE", "حجم", 0x4D)
        add("COLOR_YELLOW", "REVEAL", "كشف", 0x4E)
        add("COLOR_BLUE", "SUBPAGE", "صفحة", 0x4F)

        // Media
        add("MEDIA_PLAY_PAUSE", "PLAY", "تشغيل", 0x52)
        add("MEDIA_REWIND", "REW", "ترجيع", 0x53)
        add("MEDIA_FAST_FORWARD", "FF", "تقديم", 0x54)
        add("MEDIA_STOP", "STOP", "إيقاف", 0x55)
        add("MEDIA_PREV", "PREV", "السابق", 0x56)
        add("MEDIA_NEXT", "NEXT", "التالي", 0x57)

        // Bottom
        add("APP_YOUTUBE", "YouTube", "يوتيوب", 0x58)
        add("APP_EPG", "EPG", "دليل", 0x59)
        add("APP_NETFLIX", "Netflix", "نتفليكس", 0x5B)

        return RemoteProfile(
            id = id,
            name = name,
            chassis = chassis,
            description = description,
            defaultAddress = defaultAddress,
            isExtended = isExtended,
            buttons = map
        )
    }
}
