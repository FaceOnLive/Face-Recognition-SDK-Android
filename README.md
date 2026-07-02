# FaceOnLive — Face Recognition & Liveness SDK for Android

On-device face detection, **face liveness (anti-spoofing)**, and 1:1 / 1:N face matching for Android. All processing runs locally — no image ever leaves the device.

> Part of the [FaceOnLive](https://faceonlive.com) on-premises biometric SDK suite.

## Features
- Face detection with landmarks and a **liveness score** (blocks photos, videos, masks).
- Face template extraction and similarity comparison for verification & identification.
- Fully on-device and offline; camera and image input.

## Requirements
| | |
|---|---|
| Min Android SDK | 24 (Android 7.0) |
| Compile / Target SDK | 34 |
| Language | Kotlin / Java |
| Permissions | `CAMERA` |

## Setup
1. Open the project in **Android Studio**.
2. Get a license key — free trial at **https://faceonlive.com**.
3. In `app/src/main/java/com/bio/facerecognition/MainActivity.kt`, replace `<YOUR_LICENSE_KEY>` in `FaceSDK.setActivation(...)` with your key.
4. Build and run on a device.

## Quick start
```kotlin
import com.bio.facesdk.FaceSDK

if (FaceSDK.setActivation("<YOUR_LICENSE_KEY>") == FaceSDK.SDK_SUCCESS) {
    FaceSDK.init(assets)
}

val faces = FaceSDK.faceDetection(bitmap)                 // detect + liveness
val t1 = FaceSDK.templateExtraction(bitmap1, faces1[0])
val t2 = FaceSDK.templateExtraction(bitmap2, faces2[0])
val similarity = FaceSDK.similarityCalculation(t1, t2)    // 0.0 – 1.0
```

## API reference
| Method | Description | Returns |
|---|---|---|
| `setActivation(license)` | Activate with your license key. | `SDK_SUCCESS` or error |
| `init(assets)` | Load the face models. | `SDK_SUCCESS` or error |
| `faceDetection(bitmap)` | Detect faces (box, landmarks, liveness score). | list of faces |
| `templateExtraction(bitmap, face)` | Extract a face's feature template. | template |
| `similarityCalculation(t1, t2)` | Compare two templates. | `0.0–1.0` |
| `yuv2Bitmap(frame, w, h, rot)` | Camera YUV frame → Bitmap. | Bitmap |

**Status codes:** `SDK_SUCCESS`, `SDK_LICENSE_KEY_ERROR`, `SDK_LICENSE_APPID_ERROR`, `SDK_LICENSE_EXPIRED`, `SDK_NO_ACTIVATED`, `SDK_INIT_ERROR`.

## License & support
Requires a valid license key — get one at **[faceonlive.com](https://faceonlive.com)**. Never commit your key. Questions: contact@faceonlive.com

## 📦 Full SDK download
This repository contains the source/demo code only. Download the complete SDK — engine libraries and models, with full project structure — from the [Releases](../../releases) page and extract it over this project.
