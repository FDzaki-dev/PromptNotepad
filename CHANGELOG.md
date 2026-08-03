# Changelog — PromptNotepad

## [1.6.1] — Buka & Baca Isi ZIP (permintaan eksplisit user, di luar pivot Batch B/C)
### Bugfix pra-rilis (ditemukan sebelum sempat di-push ke remote)
- **Tabrakan nama file lokal antar ZIP asal berbeda:** cache ZIP yang dibuka lewat "Buka Dengan" sebelumnya selalu disalin dengan nama tetap `opened.zip`, membuat entri bernama sama dari dua ZIP yang berbeda ter-mapping ke satu file lokal yang sama (isi bisa saling menimpa). Diperbaiki: nama cache sekarang dari hash Uri sumber (`zip-<hash>.zip`), konsisten dengan pola `ExternalFileUtils.localNameFor`. Lihat detail di `PROJECT_STATE.md` insiden #17. `versionCode`/`versionName` tidak dinaikkan karena versi ini belum pernah benar-benar dirilis/di-push.
### Konteks
- Permintaan eksplisit user: bisa membuka berkas `.zip` lewat "Buka Dengan" (sama seperti alur `.txt`/`.md` yang sudah ada), melihat daftar isinya, dan membaca berkas `.txt`/`.md` di dalamnya **tanpa simbol acak (mojibake)**. Bukan bagian dari roadmap Batch B (Tags)/Batch C (visual) yang sedang dijeda — dikerjakan terpisah karena diminta langsung.
### Ditambahkan
- `util/ZipUtils.kt` (baru): daftar entri `.txt`/`.md` di dalam ZIP (`listTextEntries`), baca isi satu entri (`readEntryText`), dan impor satu entri ke penyimpanan lokal agar bisa dibuka sebagai tab biasa (`importEntryToNotes`). Batas ukuran 2MB per entri, konsisten dengan `FileUtils.readFile`.
- `ui/ZipContentsScreen.kt` (baru): layar daftar isi ZIP (nama + ukuran entri), ketuk untuk membuka. Bukan pengelola arsip umum — tidak ada ekstrak-semua/hapus/kompresi.
- `AndroidManifest.xml`: intent-filter VIEW/EDIT untuk `.zip` — pola nama berkas (konsisten dengan pola `.txt`/`.md` yang sudah ada) DAN filter MIME eksplisit (`application/zip`, `application/x-zip-compressed`) untuk cakupan lebih luas dari file manager yang mengenali MIME zip dengan benar.
- `MainActivity.kt`: deteksi ZIP saat Intent VIEW/EDIT masuk (dari MIME atau nama berkas) — jika ZIP, disalin ke cache lokal lalu ditampilkan lewat `ZipContentsScreen`; jika bukan, jalur impor teks lama (`ExternalFileUtils.importFromUri`) berjalan persis seperti sebelumnya, tidak diubah.
### Perbaikan bug/akar masalah "simbol acak"
- **Akar masalah:** `FileUtils.readFile` (dipakai semua berkas lokal biasa) memaksa dekode UTF-8 untuk semua berkas — berkas dari sumber lain yang sebenarnya ANSI/Windows-1252 akan tampil sebagai simbol acak. Untuk entri ZIP (`ZipUtils.readEntryText`), **tidak** memaksa UTF-8: deteksi BOM UTF-8/UTF-16 dulu, lalu coba UTF-8 ketat, baru fallback ke Windows-1252 (jaring pengaman terakhir yang valid untuk byte apa pun).
- **Belum disentuh (di luar scope permintaan ini):** `FileUtils.readFile` untuk berkas `.txt`/`.md` lokal biasa (bukan dari ZIP) TETAP hard-code UTF-8 seperti sebelumnya — mengubahnya berarti menyentuh jalur baca/tulis inti yang dipakai semua fitur (undo/redo, auto-save, dst.), berisiko regresi lebih luas daripada manfaatnya untuk permintaan spesifik ini. Kalau berkas `.txt` biasa (bukan dari ZIP) juga bermasalah, perlu instruksi terpisah.
### Asumsi teknis (dicatat sesuai AI Assumption Log)
- Tidak menambah dependency baru — `java.util.zip.ZipFile` sudah tersedia di JDK/Android standar.
- Konten ZIP disalin ke `context.cacheDir` (bukan `filesDir`), karena sifatnya sementara (sumber baca, bukan data pengguna yang perlu bertahan lama).
### Verifikasi
- Tidak ada toolchain Gradle/Android SDK di lingkungan pembuatan ini untuk build asli — verifikasi dilakukan lewat pengecekan keseimbangan kurung (otomatis, seimbang di 3 file yang disentuh) dan penelusuran manual bahwa referensi import baru (`ZipUtils`, `ZipContentsScreen`) benar dan dipakai.

## [1.6.0] — PIVOT ARAH PROYEK: Batch A, redesain ala TxtPad+ (Daftar File + Pin)
### Konteks
- User menghentikan penambahan fitur baru. Tujuan sekarang: UI/UX/layout & cara tangani berkas dibuat SEDEKAT MUNGKIN meniru app pembanding TxtPad+ (termasuk warna/spacing). Fitur lama yang tidak ada di TxtPad+ (multi-tab, Markdown viewer, highlight Todo.txt, Cari & Ganti Regex) TETAP ADA, hanya bukan prioritas/tidak disorot di alur utama.
- Dipecah jadi beberapa batch kecil berurutan. Ini **Batch A**: layar utama Daftar File + Pin. Batch berikutnya: Tags (folder virtual) dan penghalusan visual lanjutan.
### Diubah (perilaku, bukan sekadar tampilan)
- **Layar utama app sekarang Daftar File** (`FileListScreen`, baru) — bukan langsung membuka QuickNote/editor seperti sebelumnya. Pengguna melihat daftar dulu, baru memilih atau membuat catatan, sama seperti alur TxtPad+.
- Auto-buka QuickNote saat aplikasi dijalankan **dihapus** (bertentangan langsung dengan keputusan "layar utama = daftar file"). Berkas `QuickNote.txt` lama (jika masih ada dari versi sebelumnya) TIDAK dihapus, tetap muncul sebagai berkas biasa di daftar.
- Tombol back sistem Android: dari editor kembali ke Daftar File dulu (ala TxtPad+), bukan langsung keluar app.
- TopAppBar editor sekarang punya tombol panah kembali ke Daftar File (menggantikan peran ikon "Buka Berkas" lama di `BottomFileBar`, yang sekarang juga mengarah ke Daftar File, bukan dialog kecil).
### Ditambahkan
- **Pin berkas**: ikon pin di tiap baris Daftar File, berkas yang di-pin selalu tampil di bagian paling atas. Dipersist lewat `PinStore.kt` (baru, SharedPreferences, pola sama seperti `SettingsStore`).
- Pratinjau baris pertama tiap berkas di Daftar File (`FileUtils.readSnippet`, baca maks. 500 byte pertama saja per berkas, ringan).
- Pencarian nama berkas langsung di Daftar File (ikon kaca pembesar di TopAppBar).
- FAB (+) untuk membuat catatan baru langsung dari Daftar File.
### Dihapus (superseded, bukan regresi)
- `FileListDialog` (dialog kecil "Berkas Tersimpan" di dalam editor) — sepenuhnya digantikan `FileListScreen` yang jauh lebih lengkap (pratinjau, pin, pencarian). Tidak ada fungsi yang hilang, hanya dipindah & diperluas.
### Keterbatasan yang didokumentasikan
- "Sedekat mungkin" secara visual didasarkan pada deskripsi fitur resmi TxtPad+ di Play Store (dicek 2 Agu 2026) — BUKAN pencocokan piksel-demi-piksel terhadap screenshot asli, karena alat pencarian gambar yang tersedia tidak berhasil menampilkan screenshot asli app tersebut secara andal. Warna/spacing memakai palet `AppColors` yang sudah ada (Batch 2), disusun mengikuti pola umum app notepad minimalis (list + FAB + search), bukan menyalin aset visual TxtPad+ secara literal (berpotensi masalah hak cipta aset).
- Tags (folder virtual) BELUM dikerjakan — direncanakan sebagai batch terpisah berikutnya, sesuai kesepakatan user.

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
