# LLM_CONTEXT — PromptNotepad

Ringkasan ini dibuat agar sesi Claude manapun (chat baru) bisa langsung paham
konteks proyek tanpa perlu penjelasan ulang. Upload/paste isi file ini di awal
sesi troubleshooting atau penambahan fitur.

## Arsitektur Singkat
UI (Jetpack Compose) → State (`TabManager`) → Utility (`FileUtils`,
`TodoParser`, `RegexUtils`). Alur data satu arah: aksi UI memanggil fungsi di
State/Utility, hasilnya dikembalikan sebagai `State`/`Result` yang di-observe
oleh UI.

## Peta Berkas Utama
| Berkas | Tanggung Jawab |
|---|---|
| `MainActivity.kt` | Entry point, komposisi layar utama, orkestrasi antara TabManager, FileUtils, dan komponen UI |
| `state/TabManager.kt` | Manajemen daftar tab & tab aktif (state murni, tanpa I/O) |
| `util/FileUtils.kt` | Semua operasi baca/tulis/list berkas, async di `Dispatchers.IO`, dibungkus `runCatching` |
| `util/TodoParser.kt` | Parsing sintaks todo.txt (prioritas, context, project) — Pure Kotlin |
| `util/RegexUtils.kt` | Cari & ganti berbasis regex, aman dari pola invalid — Pure Kotlin |
| `ui/TextEditor.kt` | Komponen editor teks utama + debounce highlight (250ms) |
| `ui/TodoHighlighter.kt` | Styling visual sintaks todo.txt |
| `ui/TabBar.kt`, `ui/ShortcutBar.kt`, `ui/PremiumLayout.kt` | Komponen UI pendukung (tab bar, shortcut, kerangka layout) |
| `ui/MarkdownViewer.kt` | Render preview Markdown ringan (tanpa WebView) |
| `model/TabItem.kt`, `model/TodoTask.kt` | Data class murni |

## Aturan Utama Proyek
- 100% offline native, tanpa library jaringan.
- I/O dan pemrosesan regex/parsing wajib di-offload dari UI thread.
- Pemisahan logika ketat: `TodoParser.kt` dan `RegexUtils.kt` murni Kotlin,
  tidak boleh mengimpor Android SDK (agar mudah di-unit test).
- Auto-save berjalan per keystroke (via `onContentChange`), ditulis dengan
  `Dispatchers.IO.limitedParallelism(1)` agar urutan write terjamin.
- Tab & index aktif dipulihkan dari saved-instance-state (`rememberSaveable`)
  agar tidak hilang saat proses di-kill OS.
- Semua kegagalan I/O ditangani via `Result` + Snackbar, tidak pernah crash.

## Konvensi Logging (Logcat)
Filter tag `PN_` di Logcat untuk langsung tahu di layer mana masalah terjadi:
- `PN_UI` — interaksi antarmuka (klik tombol, aksi pengguna)
- `PN_STATE` — perubahan tab/draf di `TabManager`
- `PN_IO` — baca/tulis/list berkas di `FileUtils`

## Unit Test yang Tersedia
Jalankan sebelum menganggap perubahan aman (2-3 detik, tanpa build APK):
- `TodoParserTest` — parsing checklist & metadata todo.txt
- `RegexUtilsTest` — kebenaran & keamanan cari-ganti regex
- `TabManagerTest` — alur buka, tutup, ganti, restore tab

## Status Versi Saat Ini
- Async I/O, anti data-loss (restore tab), debounce highlight, safety-net
  error handling — sudah stabil.
- Sedang berjalan roadmap 3 batch: (A) maintainability/logging/test — batch
  ini, (B) user-friendly untuk pengguna awam, (C) UI/UX "ultra native premium
  feel". Urutan dikerjakan A → B → C.
