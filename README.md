# PromptNotepad

Aplikasi catatan native Android (Kotlin + Jetpack Compose), 100% offline, tanpa database eksternal — file mentah `.txt`/`.md` disimpan langsung di penyimpanan internal aplikasi.

## Fitur v1.0.0
- Multi-tab (buka beberapa file sekaligus)
- QuickNote otomatis terbuka saat aplikasi dijalankan
- Auto-save instan per karakter
- Cari & ganti dengan Regex
- Shortcut bar (`#`, `- [ ]`, `()`, Tab, Timestamp)
- Markdown viewer offline (heading, bullet, checkbox)
- Deteksi sintaksis Todo.txt (`(A)`, `@konteks`, `+proyek`) dengan highlight warna
- Tema Dark AMOLED Ultra Premium, font monospace untuk editor

## Build lokal
```
./gradlew assembleRelease
```
Signing config release membaca dari `keystore.properties` (lokal) atau environment variable `ANDROID_KEYSTORE_*` (CI/GitHub Actions).
