# PromptNotepad

Aplikasi catatan native Android (Kotlin + Jetpack Compose), 100% offline, tanpa database eksternal — file mentah `.txt`/`.md` disimpan langsung di penyimpanan internal aplikasi.

**Versi saat ini:** `1.6.0` (`versionCode 11`). Lihat [`CHANGELOG.md`](CHANGELOG.md) untuk riwayat rilis dan [`PROJECT_STATE.md`](PROJECT_STATE.md) untuk konteks arsitektur & riwayat insiden.

> **Catatan arah proyek:** sejak v1.6.0, tujuan utama adalah membuat UI/UX/layout & cara tangani berkas sedekat mungkin dengan app pembanding **TxtPad+** (termasuk warna/spacing). Fitur non-TxtPad+ (multi-tab, Markdown viewer, highlight Todo.txt, Cari & Ganti Regex) tetap ada tapi bukan prioritas.

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

### Integrasi Sistem (sejak v1.3.0)
- Muncul di menu **"Buka Dengan"** Android untuk file `.txt`/`.md` (dari file manager, aplikasi lain, dsb) — seperti TxtPad+
- Berkas eksternal otomatis diimpor & disinkron-balik ke berkas asal setiap auto-save
- Membuka file eksternal yang sama berulang kali tetap ter-mapping ke satu tab (bukan duplikat)

### Layout Minimal (sejak v1.4.0)
- TopAppBar disederhanakan jadi hanya judul aplikasi (bukan deretan ikon tanpa label)
- Bar bawah baru (di atas shortcut bar): ikon buka berkas + nama tab aktif + ikon file baru, langsung terlihat
- Menu **⋮**: Pratinjau Markdown, Cari/Ganti Regex, Undo/Redo, Cari di Berkas, Cetak, Gulir ke..., dan Info Berkas — seluruhnya fungsional penuh (sejak v1.4.1)
- Warna latar dilunakkan dari AMOLED hitam pekat ke abu-abu gelap agar lebih nyaman di mata

### Undo/Redo, Cari, Gulir, Cetak (sejak v1.4.1)
- Undo/Redo per-tab dengan checkpoint otomatis tiap jeda ketik
- Cari di Berkas (case-insensitive, wrap-around Berikutnya/Sebelumnya)
- Gulir ke Awal/Akhir/nomor baris tertentu
- Cetak lewat Android Print Framework bawaan (pilih printer/simpan sebagai PDF)

### Audit & Perbaikan Bug (sejak v1.4.2)
- Dispatcher Cari & Ganti Regex tidak lagi bisa macet permanen akibat pola bermasalah (thread pool terisolasi diganti dari 1 thread yang dipakai ulang ke cached pool)
- Item menu terkunci (jika ada di masa depan) kini benar-benar diblokir dari eksekusi, bukan cuma tampil abu-abu

### Pengaturan Tampilan (sejak v1.5.0)
- Ukuran font editor bisa diatur (stepper +/−, 11sp–26sp) lewat menu ⋮ → "Pengaturan Tampilan"
- Toggle tema terang/gelap (default tetap gelap) — pengaturan tersimpan otomatis, diterapkan langsung tanpa restart

### Daftar File & Pin (sejak v1.6.0 — redesain ala TxtPad+)
- **Layar utama sekarang Daftar File**, bukan langsung ke editor — daftar semua catatan dengan pratinjau baris pertama & tanggal ubah terakhir
- **Pin catatan penting** — selalu tampil di bagian paling atas daftar
- Pencarian nama berkas langsung dari layar utama
- Tombol (+) untuk membuat catatan baru dari layar utama
- Tombol back sistem & tombol panah di editor kembali ke Daftar File (bukan langsung keluar app)

## Build lokal
```
./gradlew assembleRelease
```
Signing config release membaca dari `keystore.properties` (lokal) atau environment variable `ANDROID_KEYSTORE_*` (CI/GitHub Actions). Nama file APK output dibuat otomatis mengikuti `versionName` + short commit SHA (mis. `PromptNotepad-1.5.0-a1b2c3d-release.apk`), bukan nama statis — sumber SHA dari env `ANDROID_COMMIT_SHA` (diisi CI) dengan fallback `git rev-parse --short HEAD` untuk build lokal/Termux.

## Struktur Proyek
```
app/src/main/java/com/promptnotepad/app/
├── MainActivity.kt        # Entry point, navigasi Daftar File <-> Editor, wiring state & UI,
│                           # handle Intent VIEW/EDIT, baca/tulis SettingsStore, dialog Pengaturan
├── model/                 # TabItem (+ isDirty, sourceUri), TodoTask
├── state/                 # TabManager (tab list, active index, eviction)
├── ui/                    # FileListScreen (layar utama, ala TxtPad+), TextEditor, TabBar,
│                           # ShortcutBar, PremiumLayout, BottomFileBar (menu ⋮), MarkdownViewer,
│                           # TodoHighlighter, theme/ (AppColors, LocalAppColors dinamis)
└── util/                  # FileUtils (I/O async + readSnippet), RegexUtils (async+timeout),
                            # ExternalFileUtils (impor & sinkron "Buka Dengan"), TodoParser,
                            # SettingsStore (font size + mode tema), PinStore (berkas di-pin)
```

## Roadmap (rencana — dikerjakan berurutan per batch)
- **Batch B:** Tags (folder virtual) — buat tag, assign ke berkas, filter Daftar File per tag
- **Batch C:** penghalusan visual lanjutan (kalau ada referensi visual TxtPad+ yang lebih presisi)
- Info Berkas lengkap — tambah jumlah kata & karakter
- Cari berdasarkan isi file (baru ada cari nama file)
- Tema "Ikuti Sistem"
- Hardware keyboard shortcuts (`onKeyEvent`) — belum dijadwalkan ke batch tertentu

