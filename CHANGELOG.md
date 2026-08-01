# Changelog — PromptNotepad

## [1.3.0] — Fitur "Buka Dengan" (pengganti Batch 3 SAF)
### Ditambahkan
- PromptNotepad kini muncul di menu "Buka Dengan" untuk file `.txt`/`.md` dari file manager/app lain
- Berkas eksternal diimpor otomatis ke penyimpanan lokal & disinkron-balik ke berkas asal setiap auto-save
- `launchMode="singleTop"`: tap file lain saat app sudah berjalan tetap tertangkap tanpa membuka instance baru
### Diputuskan untuk TIDAK dikerjakan
- Migrasi SAF/`Uri` penuh (DocumentFile, folder picker): tidak relevan karena app pakai internal storage yang tidak kena scoped storage — diganti fitur "Buka Dengan" yang lebih terarah

## [1.2.0] — Batch 2 (selesai)
### Ditambahkan
- Large File Handling Buffer: batas 2MB saat membuka file (`FileUtils.readFile`), pesan error spesifik ditampilkan lewat Snackbar
- Optimasi Recomposition: state `fieldValue` diisolasi ke composable `EditorSection`, terpisah dari `TopAppBar`/`Scaffold` — kursor/ketikan tidak lagi memicu recompose seluruh tree
### Diputuskan untuk TIDAK dikerjakan
- Auto-save timer-debounce (3 detik): auto-save instan yang sudah ada lebih aman (jendela data-loss lebih kecil) dan sudah async — mengganti ke timer adalah kemunduran, bukan peningkatan

## [1.1.0] — Batch 1 (evaluasi lanjutan 14 item)
### Ditambahkan
- Unsaved Changes Indicator (`isDirty` + titik penanda tab + dialog konfirmasi tutup tab)
- Normalisasi encoding UTF-8 eksplisit + newline (`\r\n`/`\r` → `\n`)
- Proteksi timeout regex (2 detik) di dispatcher terisolasi
- Cek `canWrite()` sebelum menulis file (proteksi read-only)
- Batas 12 tab terbuka + eviction otomatis (FIFO, notifikasi Snackbar)
- `PROJECT_STATE.md`, `FILE_MANIFEST.txt`, `CHANGELOG.md` (dokumentasi wajib, sebelumnya luput dibuat)
### Diperbaiki
- `README.md` disinkronkan (sebelumnya masih versi v1.0.0 meski sudah 2 rilis berjalan)

## [1.0.1] — Perbaikan stabilitas
### Diperbaiki
- Bug build gagal: import `Modifier`/`TransformedText` salah package, konflik resolusi `weight()`, `TopAppBar` butuh opt-in eksperimental
### Ditambahkan
- I/O file & regex asinkron (`Dispatchers.IO`/`Default`), urutan write terjamin lewat dispatcher paralelisme-1
- Pemulihan tab otomatis via `rememberSaveable` setelah process death
- Debounce highlighting todo.txt (250ms)
- Safety-net `runCatching` + notifikasi Snackbar untuk kegagalan I/O

## [1.0.0] — Rilis awal
### Ditambahkan
- Multi-tab, QuickNote otomatis, auto-save instan per karakter
- Cari & ganti Regex, Shortcut bar, Markdown viewer offline
- Deteksi & highlight sintaksis Todo.txt
- Tema Dark AMOLED Ultra Premium
