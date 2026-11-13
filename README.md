# YouTube AI Clipper (Android Native - Kotlin)

Aplikasi Android untuk mengubah video YouTube panjang menjadi video pendek vertikal 9:16 (Shorts/TikTok/Reels) secara otomatis menggunakan integrasi **Google NotebookLM (Gemini 1.5 Pro)** dan **Google MediaPipe Face Tracking**.

---

## 🌟 Fitur Utama

1. **Auto-Scan Viral Segments via Google NotebookLM:**
   - Cukup login akun Google sekali di dalam aplikasi (via in-app WebView).
   - Ingest link YouTube secara instan ke backend NotebookLM Google.
   - LLM otomatis menganalisis seluruh transkrip dan menentukan 2–4 segmen paling berpotensi viral lengkap dengan skor viralitas, hook pembuka, dan timestamp `[start, end]`.
   - **Hemat memori & gratis:** Transkripsi dilakukan di server Google, tanpa perlu download Whisper lokal yang berat di HP pengguna.

2. **Smart Face Tracking & Smart Cropping (MediaPipe BlazeFace):**
   - **Mode Solo (1 Orang):** Kamera vertikal 9:16 otomatis mengikuti wajah pembicara dengan filter smoothing (*Exponential Moving Average*) agar gerakan kamera halus.
   - **Mode Podcast (2 Orang):** Layar otomatis dibagi dua secara vertikal (**Split-Screen 9:16** — atas untuk Pembicara 1, bawah untuk Pembicara 2) sehingga kedua subjek tetap terlihat jelas.

3. **Dynamic Animated Subtitles (Gaya Alex Hormozi / TikTok):**
   - Transkrip dipecah menjadi frasa 3 kata ritmis.
   - Kata yang sedang diucapkan menyala (*highlight* warna kuning neon) dengan teks tebal berbingkai kontras tinggi.

4. **Hardware Accelerated Export (AndroidX Media3 Transformer):**
   - Menggunakan `androidx.media3.transformer` dan `MediaCodec` bawaan chipset Android (Snapdragon/Dimensity/Exynos) untuk rendering cepat dan hemat baterai ke format MP4 1080x1920.

5. **Direct Share dari Aplikasi YouTube:**
   - Pengguna bisa langsung menekan tombol **"Bagikan / Share"** di aplikasi resmi YouTube dan memilih **AI Clipper** untuk memproses langsung.

---

## 📂 Struktur Modul

```
app/src/main/java/com/clipper/app/
├── auth/
│   ├── GoogleLoginWebView.kt     # In-app WebView untuk login Google & Cookie/CSRF sniffer
│   └── SessionManager.kt         # Penyimpan session terenkripsi (EncryptedSharedPreferences)
├── nlm/
│   ├── NlmClient.kt              # HTTP Client OkHttp untuk memanggil internal API NotebookLM
│   ├── NlmParser.kt              # Parser JSON/Markdown segmen viral dari respon LLM
│   └── models/NlmModels.kt       # Model data ViralSegment, CropMode, NotebookSession
├── downloader/
│   └── YouTubeStreamExtractor.kt # Handler resolusi stream MP4 dan buffer segmen
├── vision/
│   ├── FaceTracker.kt            # Wrapper Google MediaPipe Tasks Vision (BlazeFace)
│   └── SmartCropStrategy.kt      # Kalkulasi koordinat crop 9:16 (Solo & Podcast Split)
├── subtitle/
│   └── SubtitleEngine.kt         # Logika timestamp kata per kata & styling karaoke
├── video/
│   └── VideoCompositor.kt        # AndroidX Media3 Transformer untuk burn video MP4
└── ui/
    ├── MainViewModel.kt          # State management (Idle, Processing, Ready, Export)
    ├── screens/
    │   ├── HomeScreen.kt         # Input link YouTube, status session Google
    │   ├── ProcessingScreen.kt   # Progress bar multi-tahap
    │   └── EditorPreviewScreen.kt# Preview video 9:16, toggle split-screen, subtitle overlay
    └── theme/                    # Dark mode theme dengan aksen Neon Yellow & YouTube Red
```

---

## 🚀 Cara Menjalankan

1. Buka proyek ini di Android Studio (Iguana / Koala / Ladybug atau lebih baru).
2. Sinkronkan Gradle (`Sync Project with Gradle Files`).
3. Jalankan pada perangkat Android fisik (disarankan) atau Emulator dengan Android 8.0+ (API 26 ke atas).
