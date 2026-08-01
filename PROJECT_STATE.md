# PROJECT_STATE.md — PromptNotepad

> Wajib dibaca AI di awal setiap sesi baru sebelum melanjutkan proyek ini.
> Riwayat insiden bersifat kronologis dan TIDAK BOLEH dihapus — hanya ditambah.

## Status Terakhir
- **Versi:** `versionCode 3` / `versionName "1.1.0"`
- **Batch terakhir selesai:** Batch 1 dari daftar evaluasi lanjutan (14 item, 5 kategori)
- **Batch berikutnya (menunggu keputusan user):** Batch 2 (auto-save timer, optimasi recomposition, buffer file besar)

## Riwayat Insiden Kronologis (jangan dihapus)

1. **[Setup awal]** Proyek dibangun dari spesifikasi awal (Kotlin + Jetpack Compose, penyimpanan `.txt`/`.md` lokal tanpa DB). `release.keystore` asli dibuat + `secrets.txt` untuk GitHub Actions secrets.
2. **[Build gagal — v1.0.0]** CI (`compileReleaseKotlin`) gagal. Penyebab & perbaikan:
   - `ShortcutBar.kt`: import `Modifier` salah dari `androidx.compose.foundation.layout` (seharusnya `androidx.compose.ui.Modifier`)
   - `TextEditor.kt`: import `TransformedText` salah package (seharusnya `androidx.compose.ui.text.input.TransformedText`)
   - `PremiumLayout.kt`: import eksplisit `androidx.compose.foundation.layout.weight` bentrok dengan simbol internal — dihapus (weight di-resolve otomatis dari `ColumnScope`)
   - `MainActivity.kt`: `TopAppBar`/`TopAppBarDefaults` butuh `@OptIn(ExperimentalMaterial3Api::class)`
3. **[Stabilitas — v1.0.1]** User mengirim evaluasi 4 prioritas (dari log CI + review manual): cegah data loss (rememberSaveable tab), I/O async (Dispatchers.IO/Default, write dispatcher paralelisme-1 agar urutan auto-save terjamin), debounce highlighting 250ms, safety-net `runCatching` + Snackbar. Semua diimplementasikan.
4. **[Evaluasi lanjutan 14 item — Batch 1 → v1.1.0]** User mengirim daftar 14 item lintas 5 kategori (state resilience, performa editor, SAF/scoped storage, exception safety, fitur produktivitas). Dipecah 4 batch; user pilih Batch 1 saja:
   - Unsaved Changes Indicator (`TabItem.isDirty` + titik penanda + dialog konfirmasi tutup tab)
   - Encoding UTF-8 eksplisit + normalisasi newline (`\r\n`/`\r` → `\n`)
   - Proteksi timeout regex (2 detik, dispatcher terisolasi 1 thread) — **catatan: mitigasi terbaik yang mungkin di JVM/Android, bukan jaminan 100%** karena backtracking regex tidak selalu responsif terhadap cancellation
   - Cek `canWrite()` sebelum tulis (proteksi read-only/file terkunci)
   - Batas 12 tab + eviction FIFO otomatis (konten aman krn sudah auto-save)
5. **[Dokumentasi wajib — file ini]** `PROJECT_STATE.md` dan `FILE_MANIFEST.txt` ternyata belum pernah dibuat sejak awal proyek (luput dari batch-batch sebelumnya). Dibuat sekarang, retroaktif mencakup riwayat di atas.
6. **[README.md luput diupdate]** `README.md` masih berisi daftar fitur v1.0.0 meski sudah 2 rilis berjalan (v1.0.1, v1.1.0) — tidak pernah disinkronkan. Diperbaiki: sekarang mencantumkan seluruh fitur per kategori rilis + link ke `CHANGELOG.md`/`PROJECT_STATE.md`. **Pengingat untuk sesi berikutnya: README.md WAJIB ikut diperbarui di setiap rilis, bukan hanya `CHANGELOG.md`/`FILE_MANIFEST.txt`.**

## Keputusan Arsitektur Utama
- **Penyimpanan:** `java.io.File` langsung ke `filesDir/notes` (internal storage app-specific, tidak perlu permission). **Belum** migrasi ke Storage Access Framework/`Uri` — itu Batch 3, butuh konfirmasi eksplisit dulu karena mengubah `FileUtils`, `TabItem`, `TabManager` hampir menyeluruh.
- **Concurrency:** `Dispatchers.IO.limitedParallelism(1)` khusus untuk write (urutan auto-save terjamin, no race). `Dispatchers.Default.limitedParallelism(1)` khusus untuk regex (isolasi agar pola "meledak" tidak menyita thread pool lain).
- **State persistence:** `rememberSaveable` dengan `Saver` kustom untuk `TabManager` (bukan `ViewModel`/`SavedStateHandle` formal — itu bagian dari Batch 3).
- **Auto-save:** instan per keystroke (bukan debounce timer) — write dilakukan async tapi tetap dipicu tiap karakter berubah.

## Struktur Modul Singkat
```
app/src/main/java/com/promptnotepad/app/
├── MainActivity.kt        # Entry point, wiring seluruh state & UI
├── model/                 # TabItem (+ isDirty), TodoTask
├── state/                 # TabManager (tab list, active index, eviction)
├── ui/                    # TextEditor, TabBar, ShortcutBar, PremiumLayout,
│                           # MarkdownViewer, TodoHighlighter, theme/
└── util/                  # FileUtils (I/O async), RegexUtils (async+timeout), TodoParser
```

## Belum Dikerjakan (menunggu instruksi)
- Batch 2: auto-save timer-debounce (⚠️ berpotensi kontradiksi dgn auto-save instan yang sudah ada — perlu konfirmasi), `derivedStateOf` untuk recomposition, buffer file besar
- Batch 3: migrasi SAF/`Uri`, `Persistable URI Permission`, `SavedStateHandle`/ViewModel formal — **wajib tanya dulu** fitur/behavior apa yang harus tetap sama persis
- Batch 4: Undo/Redo stack, Hardware keyboard shortcuts (`onKeyEvent`)
