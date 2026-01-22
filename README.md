# Yuyin (余音)

**Yuyin** is an Android application that performs real-time speech-to-text (ASR) on internal audio captured from other applications. It leverages the power of **WeNet** for end-to-end speech recognition and **PyTorch Mobile** for on-device inference, allowing for offline transcription.

The app features a **floating window** that overlays on top of other apps, displaying the transcribed text in real-time as you watch videos, attend meetings, or listen to audio.

## Features

-   **Internal Audio Capture**: Records audio directly from other apps (requires Android 10+).
-   **Real-time ASR**: Uses WeNet pre-trained models for fast and accurate speech recognition.
-   **Offline Inference**: All processing is done locally on the device using PyTorch Mobile; no internet connection is required for transcription.
-   **Floating Overlay**: A draggable floating window displays subtitles/captions over other apps.
-   **Multi-language Support**: Designed to support Chinese (`zh`) and English (`en`) models.

## Screenshots

<!-- Add screenshots of the app here, e.g., the main interface and the floating window in action -->
| Main Interface | Floating Window | Settings |
|:---:|:---:|:---:|
| ![Main](path/to/screenshot1.png) | ![Float](path/to/screenshot2.png) | ![Settings](path/to/screenshot3.png) |

## Prerequisites

-   **Android Device**: Running Android 10 (API level 29) or higher (required for internal audio capture).
-   **Architecture**: Supports `arm64-v8a` (tested).

## Model Setup

To keep the application size manageable, the ASR models might not be included in the repository or the base APK. You need to ensure the model files are present for the app to function correctly.

1.  **Download WeNet Models**: Obtain the pre-trained WeNet models (TorchScript format).
    *   You generally need a `final.zip` (the model) and `units.txt` (the vocabulary).
2.  **Rename & Place in Assets**:
    The app expects specific file names in the `app/src/main/assets/` directory (or customized via settings). Based on the default configuration:
    *   **Chinese Model**:
        *   Model: `final_zh.zip`
        *   Dictionary: `words_zh.txt`
    *   **English Model**:
        *   Model: `final_en.zip`
        *   Dictionary: `words_en.txt`

    *Note: The `words_*.txt` files are currently present in the repository, but you must provide the matching `*.zip` model files.*

## Build Instructions

1.  **Clone the Repository**:
    ```bash
    git clone https://github.com/your-username/yuyin.git
    cd yuyin
    ```

2.  **Open in Android Studio**:
    Open the project folder in Android Studio.

3.  **Sync Gradle**:
    Allow Android Studio to download dependencies and sync the project.

4.  **Build and Run**:
    Connect your Android device and run the `app` configuration.

    *Note: Ensure your NDK version matches the one specified in `build.gradle` (currently `21.1.6352462`), or update the configuration to match your installed NDK.*

## Usage

1.  **Permissions**:
    Upon first launch, grant the necessary permissions:
    *   **Microphone**: To capture audio (even for internal audio, this permission group is required).
    *   **Notifications**: To show the foreground service status.
    *   **Display over other apps**: To show the floating subtitle window.

2.  **Start Recognition**:
    *   Select the desired language (Chinese/English) in the settings if available.
    *   Tap the **Start** button (or "Capture") to begin the foreground service.
    *   Accept the Android system dialog for "Start recording or casting with Yuyin?".

3.  **Floating Window**:
    *   The floating window will appear.
    *   Open the target app (e.g., a video player).
    *   The text will appear in the floating window as audio is played.

## Architecture & Credits

This project stands on the shoulders of giants:

*   **[WeNet](https://github.com/wenet-e2e/wenet)**: Production First and Production Ready End-to-End Speech Recognition Toolkit.
*   **[PyTorch Mobile](https://pytorch.org/mobile/home/)**: For running the model inference on Android.
*   **[EasyFloat](https://github.com/princekin-f/EasyFloat)**: For the floating window implementation.
*   **[EasyPermissions](https://github.com/googlesamples/easypermissions)**: For simplified permission logic.

## License

This project is licensed under the **GNU General Public License v3.0 (GPLv3)**. See the [LICENSE](LICENSE) file for details.
