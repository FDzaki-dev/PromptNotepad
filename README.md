# PromptNotepad

Aplikasi catatan native Android (Kotlin + Jetpack Compose), 100% offline, tanpa database eksternal — file mentah `.txt`/`.md` disimpan langsung di penyimpanan internal aplikasi.

**Versi saat ini:** `1.2.0` (`versionCode 4`). Lihat [`CHANGELOG.md`](CHANGELOG.md) untuk riwayat rilis dan [`PROJECT_STATE.md`](PROJECT_STATE.md) untuk konteks arsitektur & riwayat insiden.

## Fitur

### Editor & Produktivitas (sejak v1.0.0)
- Multi-tab (buka beberapa file sekaligus)
- QuickNote otomatis terbuka saat aplikasi dijalankan
- Auto-save instan per karakter
- Cari & ganti dengan Regex
- Shortcut bar (`#`, `- [ ]`, `()`, Tab, Timestamp)
- Markdown viewer offline (heading, bullet, checkbox)
- Deteksi sintaksis Todo.txt (`(A)`, `@konteks`, `+proyek`) dengan highlight warna
- Tema Dark AMOLED Ultra Premium, font monospace untuk editor

### Stabilitas (sejak v1.0.1)
- I/O file & regex asinkron (`Dispatchers.IO`/`Default`), tidak lagi membekukan UI
- Pemulihan tab otomatis via `rememberSaveable` jika proses aplikasi di-kill OS
- Debounce highlighting todo.txt (250ms) agar tidak lag di file besar
- Safety-net `runCatching` + notifikasi Snackbar saat operasi file gagal

### Resiliensi & Batas Wajar (sejak v1.1.0)
- Indikator perubahan belum tersimpan (titik penanda tab) + konfirmasi sebelum menutup tab yang dirty
- Encoding UTF-8 eksplisit + normalisasi newline (`\r\n`/`\r` → `\n`)
- Proteksi timeout regex (2 detik) untuk mitigasi pola yang menyebabkan *catastrophic backtracking*
- Cek `canWrite()` sebelum menulis (proteksi file read-only/terkunci)
- Batas 12 tab terbuka + eviction otomatis (FIFO, konten tetap aman karena sudah auto-save)

### Performa (sejak v1.2.0)
- Batas ukuran file 2MB saat dibuka di editor (cegah OOM/UI freeze pada file sangat besar)
- Optimasi recomposition: state teks yang diedit diisolasi dari `TopAppBar`/`Scaffold`, sehingga mengetik/menggerakkan kursor tidak memicu recompose seluruh layar

## Build lokal
```
./gradlew assembleRelease
```
Signing config release membaca dari `keystore.properties` (lokal) atau environment variable `ANDROID_KEYSTORE_*` (CI/GitHub Actions).

## Struktur Proyek
```
app/src/main/java/com/promptnotepad/app/
├── MainActivity.kt        # Entry point, wiring seluruh state & UI
├── model/                 # TabItem (+ isDirty), TodoTask
├── state/                 # TabManager (tab list, active index, eviction)
├── ui/                    # TextEditor, TabBar, ShortcutBar, PremiumLayout,
│                           # MarkdownViewer, TodoHighlighter, theme/
└── util/                  # FileUtils (I/O async), RegexUtils (async+timeout), TodoParser
```

## Roadmap (belum dikerjakan)
- Batch 2: auto-save timer-debounce, optimasi recomposition, buffer file besar
- Batch 3: migrasi Storage Access Framework/`Uri`, `SavedStateHandle`/ViewModel formal
- Batch 4: Undo/Redo stack, Hardware keyboard shortcuts

