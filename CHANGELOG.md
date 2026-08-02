# Changelog — PromptNotepad

## [1.5.0] — Batch 2: Pengaturan Tampilan (font size + tema terang/gelap)
### Ditambahkan
- **Ukuran font editor bisa diatur** (stepper +/− di dialog "Pengaturan Tampilan", rentang 11sp–26sp, langkah 1sp) — dipersist lewat `SettingsStore` (SharedPreferences), diterapkan langsung (live) tanpa restart app
- **Toggle tema terang/gelap** — default TETAP gelap sesuai kesepakatan; tema terang jadi opsi eksplisit, juga dipersist
- `SettingsStore.kt` (baru): wrapper SharedPreferences kecil khusus 2 nilai pengaturan tampilan, tidak menambah dependensi/DB baru
- Item menu ⋮ baru: "Pengaturan Tampilan"
### Diubah (arsitektur internal, bukan perilaku fitur lama)
- Seluruh warna UI yang sebelumnya konstanta statis (`PureBlack`, `DeepGray`, dst.) kini dibaca dinamis lewat `AppColors`/`LocalAppColors` (CompositionLocal) agar bisa berubah saat tema di-toggle — nilai default (tema gelap) identik persis dengan konstanta lama, jadi tampilan default TIDAK berubah
- Ukuran & warna font editor (`TextEditor`) serta warna highlight todo.txt (prioritas/konteks) kini mengikuti tema & pengaturan aktif lewat `editorTextStyle()`/`LocalEditorFontSize`
- Penamaan file APK build (`app/build.gradle`) dan nama artifact GitHub Actions (`build.yml`) dibuat **dinamis** mengikuti versionName + commit SHA saat itu (bukan nama statis yang sama di setiap build) — memudahkan menelusuri balik APK/artifact ke commit persisnya
### Diperbaiki
- ⚠️ **Temuan audit (bukan bagian Batch 2, ditemukan saat verifikasi):** `.gitignore` ternyata TIDAK PERNAH memuat `release.keystore` di riwayat repo (meski Kotak Perintah B setup awal seharusnya menambahkannya) — celah ini berarti keystore asli berisiko ikut ter-commit jika ada workflow lokal yang menyentuhnya di luar CI. Ditambahkan sekarang.
### Catatan
- Batch 3 (Info Berkas: tambah jumlah kata & karakter) menyusul di rilis berikutnya

## [1.4.3] — Hotfix build gagal (v1.4.2)
### Diperbaiki
- ⚠️ **Build CI gagal:** `BottomFileBar.kt` memakai `return@onClick` yang tidak valid (lambda `onClick = { ... }` adalah argumen bernama, bukan trailing lambda, jadi tidak dapat label implisit). Diganti ke struktur `if (item.available) { ... }` — guard yang dimaksud (mencegah item menu terkunci menjalankan aksi) tetap sama persis.

## [1.4.2] — Audit kecacatan logika, Batch 1 (perbaikan bug)
### Diperbaiki
- **Regex dispatcher macet permanen:** `RegexUtils` sebelumnya memakai 1 thread yang dipakai ulang selamanya (`Dispatchers.Default.limitedParallelism(1)`) — satu pola regex bermasalah (catastrophic backtracking) yang timeout akan memblokir SEMUA permintaan Cari & Ganti Regex berikutnya sampai app di-restart. Diganti ke cached thread pool terisolasi.
- **Menu terkunci belum ada guard:** item menu ⋮ berstatus "Segera Hadir" (`available=false`) sebelumnya tetap bisa memicu `onClick` (belum bermanifestasi karena semua item saat ini fungsional). Ditambah `enabled=` + guard eksplisit.
### Ditinjau, sengaja tidak diubah
- Hash 32-bit nama file lokal untuk berkas impor eksternal — mengganti algoritmanya berisiko menduplikasi berkas yang sudah pernah diimpor pengguna lama; risiko tabrakan di praktik nyaris tidak pernah terjadi
### Catatan
- Ini Batch 1 dari rencana besar "implementasikan semua fitur TxtPad+ hingga matang + audit kecacatan logika" — Batch 2 (font & tema) dan Batch 3 (Info Berkas: jumlah kata/karakter) menyusul di rilis berikutnya

## [1.4.1] — Implementasi nyata (koreksi dari placeholder v1.4.0)
### Ditambahkan
- Undo/Redo sungguhan (stack per-tab, checkpoint tiap jeda ketik 600ms)
- Cari di Berkas sungguhan (dialog cari, Berikutnya/Sebelumnya, wrap-around)
- Gulir ke... sungguhan (Ke Awal/Ke Akhir/nomor baris)
- Cetak sungguhan (Android Print Framework via WebView, tanpa dependensi baru)
### Diperbaiki
- Kelima item di atas sebelumnya (v1.4.0) hanya placeholder berlabel "(Segera Hadir)" — sekarang seluruhnya fungsional

## [1.4.0] — Redesain Layout (minimal, terinspirasi notepad pembanding)
### Diubah
- TopAppBar disederhanakan jadi hanya judul — 4 ikon aksi lama (Pratinjau Markdown, Regex, Buka File, File Baru) dipindah ke `BottomFileBar` baru, tidak ada yang dihapus
- Warna latar AMOLED hitam pekat (`0x000000`) dilunakkan ke abu-abu gelap agar tidak terlalu keras di mata
### Ditambahkan
- `BottomFileBar`: bar minimal (ikon buka berkas, nama tab aktif, ikon file baru, menu ⋮)
- Menu ⋮ berisi: Pratinjau Markdown & Cari/Ganti Regex (fungsional, fitur lama yang sama), Undo/Redo/Cari di Berkas/Cetak/Gulir ke... (ditandai "Segera Hadir", belum diimplementasikan), dan **Info Berkas** (baru, fungsional — nama/path/ukuran/waktu ubah)

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
