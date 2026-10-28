# StarLED HK320DF Smart Infrared Remote (Native Android & Kotlin)

A production-grade, native Android application engineered for **Infinix HOT 60 Pro+** and compatible Android smartphones to control **StarLED 32-inch Smart Televisions** running with the **HK320DF** physical remote control.

---

## 1. Target Hardware Specifications

| Property | Value |
|---|---|
| **Target Smartphone** | Infinix HOT 60 Pro+ (Android 14 / XOS) |
| **TV Brand** | StarLED |
| **Physical Remote Marking** | `HK320DF` |
| **TV Screen Size** | 32 Inches (DLED HD / Smart Android AOSP) |
| **Common Mainboard / Chassis** | CVTE `TP.SK506.PB801`, `TP.MS338.PB801`, MStar `MSD6A338` |
| **Modulation Protocol** | NEC & Extended NEC at 38.0 kHz carrier |
| **Android Subsystem** | `android.hardware.ConsumerIrManager` |

---

## 2. Infrared Command Architecture

```
   [ UI Remote Screen / Discovery Wizard ]
                     │
                     ▼
       [ RemoteCommandRepository ]  ◄── (Stores Verified vs Candidate mappings)
                     │
                     ▼
          [ InfraredProtocol ]      ◄── (Converts Hex Address/Cmd into microsecond pulses)
                     │
                     ▼
     [ StarLEDInfraredController ]  ◄── (Queries ConsumerIrManager, verifies 38kHz)
                     │
                     ▼
   [ Infinix HOT 60 Pro+ IR Emitter ]
                     │
             38 kHz IR Burst
                     │
                     ▼
          [ StarLED 32" TV Sensor ]
```

### Protocol Timing Specifications (NEC Standard)
* **Carrier Frequency:** `38,000 Hz` (38 kHz)
* **AGC Leader Pulse:** `9,000 µs` HIGH (mark), `4,500 µs` LOW (space)
* **Logical 0:** `562.5 µs` mark + `562.5 µs` space (Total: 1,125 µs)
* **Logical 1:** `562.5 µs` mark + `1,687.5 µs` space (Total: 2,250 µs)
* **Stop Bit:** `562.5 µs` mark
* **Total Pulses per Frame:** `67` elements (`IntArray` fed directly to `ConsumerIrManager.transmit`)

> **CRITICAL RULE:** The app **never** sends raw hex values directly. All commands are converted into physical microsecond timing arrays before passing to Android hardware.

---

## 3. Remote Button Layout (1:1 Physical Match)

1. **Top Row:**
   * `POWER` (Red, top left)
   * `MUTE` (Green, top right)
2. **Numeric Keypad:**
   * `1`, `2`, `3`
   * `4`, `5`, `6`
   * `7`, `8`, `9`
   * `-/--`, `0`, `HOME`
3. **Volume and Channel Controls:**
   * Vertical Volume Rocker: `VOL +` and `VOL -`
   * Center Function Keys: `P.MODE` (Picture Mode), `S.MODE` (Sound Mode)
   * Vertical Channel Rocker: `CH +` and `CH -`
4. **Additional Controls:**
   * `ASPECT`, `INPUT`, `REC`, `DISPLAY`
   * D-Pad Ring: `UP`, `DOWN`, `LEFT`, `RIGHT`, `ENTER` (Center)
   * `SLEEP`, `MENU`, `EXIT`
5. **Colored Teletext Buttons:**
   * Red: `TTX/CC`
   * Green: `SIZE`
   * Yellow: `REVEAL`
   * Blue: `SUBPAGE`
6. **Media Keys:**
   * `PLAY/PAUSE`, `REW`, `FF`, `STOP`, `PREV`, `NEXT`
7. **Bottom Row:**
   * `YouTube`, `EPG`, `Netflix`

---

## 4. Infrared Code Discovery Module

Due to differences between StarLED television revisions (chassis revisions like CVTE vs MStar), the app provides a **Discovery Wizard**:

1. **Step 1:** Hardware detection — Verifies if device has `PackageManager.FEATURE_CONSUMER_IR` and `ConsumerIrManager.hasIrEmitter()`.
2. **Step 2:** Carrier frequency validation — Queries `getCarrierFrequencies()` to ensure 38 kHz is within supported range.
3. **Step 3:** Candidate code selection — Tests candidate profiles:
   * **Profile 1:** StarLED HK320DF CVTE Primary (`Address: 0x04`)
   * **Profile 2:** StarLED Smart AOSP CVTE (`Address: 0x00`)
   * **Profile 3:** StarLED Extended 16-bit NEC (`Address: 0xBF00`)
   * **Profile 4:** StarLED MStar Classic (`Address: 0x01`)
4. **Step 4:** Physical confirmation dialog — Never assumes transmission implies TV reception. Asks user: *"Did the TV turn ON/OFF or adjust volume?"*
5. **Step 5:** Persistence — Only user-confirmed codes are marked `VERIFIED` and preserved in persistent local storage.

---

## 5. Building & Deploying with Android Studio

### Prerequisites
* Android Studio Iguana / Jellyfish / Koala or later
* JDK 17
* Android SDK 35 (API 35, Android 15)
* Target Infinix phone with USB Debugging enabled

### Build Steps
```bash
# Clone or open the /android folder in Android Studio
cd android

# Build debug APK
./gradlew assembleDebug

# Run unit tests for Infrared protocol timing
./gradlew testDebugUnitTest

# Install directly to connected Infinix phone
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## 6. Permissions in AndroidManifest.xml
```xml
<uses-permission android:name="android.permission.TRANSMIT_IR" />
<uses-permission android:name="android.permission.VIBRATE" />
<uses-feature android:name="android.hardware.consumerir" android:required="false" />
```
*(Marked `required="false"` so the app can still be installed, inspected, and used with external IR blasters on devices without internal hardware).*
