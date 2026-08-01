# Changelog — PromptNotepad

## [1.1.0] — Batch 1 (evaluasi lanjutan 14 item)
### Ditambahkan
- Unsaved Changes Indicator (`isDirty` + titik penanda tab + dialog konfirmasi tutup tab)
- Normalisasi encoding UTF-8 eksplisit + newline (`\r\n`/`\r` → `\n`)
- Proteksi timeout regex (2 detik) di dispatcher terisolasi
- Cek `canWrite()` sebelum menulis file (proteksi read-only)
- Batas 12 tab terbuka + eviction otomatis (FIFO, notifikasi Snackbar)
- `PROJECT_STATE.md`, `FILE_MANIFEST.txt`, `CHANGELOG.md` (dokumentasi wajib, sebelumnya luput dibuat)

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
