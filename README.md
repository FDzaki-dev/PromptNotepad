# PromptNotepad

Aplikasi catatan native Android (Kotlin + Jetpack Compose), 100% offline, tanpa database eksternal — file mentah `.txt`/`.md` disimpan langsung di penyimpanan internal aplikasi.

**Versi saat ini:** `1.4.2` (`versionCode 8`). Lihat [`CHANGELOG.md`](CHANGELOG.md) untuk riwayat rilis dan [`PROJECT_STATE.md`](PROJECT_STATE.md) untuk konteks arsitektur & riwayat insiden.

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

## Build lokal
```
./gradlew assembleRelease
```
Signing config release membaca dari `keystore.properties` (lokal) atau environment variable `ANDROID_KEYSTORE_*` (CI/GitHub Actions).

## Struktur Proyek
```
app/src/main/java/com/promptnotepad/app/
├── MainActivity.kt        # Entry point, wiring seluruh state & UI, handle Intent VIEW/EDIT
├── model/                 # TabItem (+ isDirty, sourceUri), TodoTask
├── state/                 # TabManager (tab list, active index, eviction)
├── ui/                    # TextEditor, TabBar, ShortcutBar, PremiumLayout,
│                           # BottomFileBar (menu ⋮), MarkdownViewer,
│                           # TodoHighlighter, theme/
└── util/                  # FileUtils (I/O async), RegexUtils (async+timeout),
                            # ExternalFileUtils (impor & sinkron "Buka Dengan"), TodoParser
```

## Roadmap (rencana disepakati — dikerjakan berurutan per batch)
- **Batch 2:** Pengaturan Tampilan — ukuran font bisa diatur, toggle tema terang/gelap (default tetap gelap)
- **Batch 3:** Info Berkas lengkap — tambah jumlah kata & karakter
- Hardware keyboard shortcuts (`onKeyEvent`) — belum dijadwalkan ke batch tertentu

