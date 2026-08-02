# PROJECT_STATE.md — PromptNotepad

> Wajib dibaca AI di awal setiap sesi baru sebelum melanjutkan proyek ini.
> Riwayat insiden bersifat kronologis dan TIDAK BOLEH dihapus — hanya ditambah.

## Status Terakhir
- **Versi:** `versionCode 9` / `versionName "1.4.3"`
- **Batch terakhir selesai:** Hotfix build gagal dari v1.4.2 (lihat insiden #12) — Batch 1 (2 bug audit) kini benar-benar selesai & lolos compile
- **Batch berikutnya (rencana disepakati user — kerjakan berurutan, jangan sekaligus):**
  1. **Batch 2 — Pengaturan Tampilan:** ukuran font bisa diatur + toggle tema terang/gelap (default TETAP gelap, terang jadi opsi)
  2. **Batch 3 — Info Berkas lengkap:** tambah jumlah kata & karakter (saat ini baru ukuran file + tanggal)

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
7. **[Batch 2 — v1.2.0]** Large File Handling Buffer (batas 2MB di `FileUtils.readFile`, pesan error spesifik ditampilkan lewat Snackbar) + Optimasi Recomposition (state `fieldValue` diisolasi ke composable `EditorSection` baru, terpisah dari `TopAppBar`/`Scaffold`, agar kursor/ketikan tidak memicu recompose seluruh tree). ⚠️ **Self-caught bug saat refactor:** sempat salah membuat parameter `regexRequest` bertipe `Any?` di-cast ke interface `RegexRequestPayload` yang tidak pernah diimplementasikan `RegexRequest` — akan membuat fitur regex diam-diam tidak pernah berjalan. Ditemukan & diperbaiki sebelum ZIP dikirim (tidak sempat terkirim ke user). **Item ketiga (auto-save timer-debounce) — KEPUTUSAN FINAL: TIDAK dikerjakan.** User menyerahkan keputusan ke AI; analisis: timer 3 detik membuka jendela risiko data loss hingga 3 detik, sementara auto-save instan yang sudah ada (v1.0.0, sudah async sejak v1.0.1) justru lebih aman dan tidak membekukan UI — mengganti ke timer berarti mundur dari prioritas #1 (cegah data loss) yang sudah ditetapkan user sendiri di evaluasi sebelumnya. Batch 2 dinyatakan **selesai penuh** dengan keputusan ini.
8. **[Batch 3 (SAF) di-skip, diganti "Buka Dengan" — v1.3.0]** User awalnya minta migrasi SAF/Uri (ide dari saran Gemini), tapi setelah dianalisis: premis "scoped storage Android 10+ butuh SAF" TIDAK relevan untuk app ini karena PromptNotepad sejak awal memakai `filesDir` (internal app-private storage) yang **tidak pernah** kena pembatasan scoped storage — migrasi SAF penuh hanya menambah kompleksitas (DocumentFile, persistable URI permission) tanpa manfaat nyata. User setuju skip. Kebutuhan sebenarnya ternyata lebih spesifik: "tool seperti TxtPad+ yang bisa diakses saat tekan file.txt" — yaitu fitur **file association / "Buka Dengan"**, bukan migrasi storage penuh. Diimplementasikan sebagai fitur terisolasi:
   - `AndroidManifest.xml`: intent-filter VIEW/EDIT untuk `text/plain` (.txt, MIME reliable) + fallback pathPattern untuk `.md`/`.txt` (MIME tidak selalu konsisten di file manager), `launchMode="singleTop"` agar tap file berulang tidak bikin instance Activity baru
   - `ExternalFileUtils.kt` (baru): impor berkas eksternal → salin ke `filesDir/notes` dengan nama deterministik dari hash Uri (biar buka file yang sama berulang kali tetap 1 tab, bukan duplikat), lalu sinkron-balik (`writeBackToUri`) ke Uri asal setiap auto-save
   - `TabItem`/`TabManager`: tambah `sourceUri: Uri? = null` (opsional, default null) — **backward compatible**, semua pemanggilan lama (`openFileInTab(file)`) tetap valid tanpa perubahan
   - Arsitektur internal (`FileUtils`, penyimpanan `.txt`/`.md` lokal) **tidak diubah sama sekali** — berkas eksternal tetap lewat jalur lokal yang sudah teruji, sinkronisasi Uri hanya lapisan tambahan di atasnya
   - **Keterbatasan yang didokumentasikan (bukan bug tersembunyi):** setelah process death, link sinkronisasi ke Uri asal terputus (Android tidak selalu beri izin Uri permanen untuk Intent VIEW/EDIT biasa) — salinan lokal & isi teks tetap aman, cuma auto-sync ke file eksternal berhenti sampai file dibuka ulang lewat "Buka Dengan"

9. **[Redesain Layout — v1.4.0]** User memberi masukan: layout dirasa terlalu ramai, tema AMOLED hitam pekat terasa tidak nyaman, dan banyak ikon di TopAppBar tanpa label jelas — dibandingkan dengan aplikasi notepad lain (bottom bar minimal: ikon browse, nama file, ikon pensil, menu ⋮ berisi item terkunci "(Premium feature)"). Perubahan **murni UI/layout, tanpa mengubah arsitektur penyimpanan/I/O/state**:
   - `TopAppBar` disederhanakan jadi hanya judul "PromptNotepad" — 4 ikon aksi (Pratinjau Markdown, Regex, Buka File, File Baru) dipindah, bukan dihapus.
   - `BottomFileBar.kt` (baru): bar minimal di atas `ShortcutBar` — ikon buka berkas + nama tab aktif + ikon file baru tetap tampil langsung (dua aksi paling sering dipakai), sisanya masuk menu ⋮ (`DropdownMenu`): Pratinjau Markdown & Cari/Ganti Regex (fungsional, tetap ke fitur lama yang sama persis), plus Undo/Redo/Cari di Berkas/Cetak/Gulir ke... berlabel "(Segera Hadir)" — abu-abu, meniru pola item terkunci di app pembanding tapi dengan istilah jujur "Segera Hadir" (bukan "Premium") karena PromptNotepad **tidak** punya sistem berlangganan/pembelian.
   - **Info Berkas** (item menu ⋮ terakhir): satu-satunya item baru yang benar-benar fungsional — dialog menampilkan nama, path, ukuran, waktu terakhir diubah dari `TabItem.file` (metadata saja, tanpa I/O baru).
   - `ui/theme/Color.kt`: `PureBlack`/`DeepGray`/`SurfaceGray`/`PremiumBorder` dilunakkan nilainya (bukan lagi 0x000000 murni) untuk mengurangi kontras AMOLED yang dikeluhkan tidak nyaman di mata — nama identifier dipertahankan apa adanya jadi tidak ada file lain yang perlu diubah.
   - `PremiumLayout.kt`: tambah slot `bottomFileBar` (default kosong, backward compatible) di antara area editor dan `ShortcutBar`.
   - **Tidak ada fitur yang dihapus** — semua 4 aksi TopAppBar lama tetap ada dan berfungsi identik, hanya lokasinya berpindah. Verifikasi anti-regresi dilakukan manual (tidak ada toolchain Gradle/Android SDK dengan akses jaringan di lingkungan pembuatan ini untuk build asli) — cek keseimbangan kurung/brace, jejak import tak terpakai dibersihkan, dan penelusuran manual bahwa tiap ikon lama punya callback pengganti persis.

10. **[Koreksi: implementasi NYATA menggantikan placeholder — v1.4.1]** ⚠️ User menegaskan bahwa 5 item menu ⋮ yang di batch sebelumnya diberi label "(Segera Hadir)" (abu-abu, tidak berfungsi) **harus diimplementasikan sungguhan**, bukan diakali dengan tampilan terkunci. Semua 5 item kini fungsional penuh:
    - **Undo/Redo:** stack per-tab (`undoStack`/`redoStack`, direset saat pindah tab), checkpoint diambil setiap jeda ketik 600ms (bukan per-karakter, agar stack tidak meledak) lewat `LaunchedEffect(activeTab?.id, fieldValue.text)` + `delay(600)`. Batas 100 checkpoint per tab. Undo/redo memicu `saveActiveTab` seperti edit biasa.
    - **Cari di Berkas:** `FindInFileDialog` — pencarian case-insensitive, tombol Berikutnya/Sebelumnya berputar (wrap-around) di semua kecocokan.
    - **Gulir ke...:** `ScrollToDialog` — tombol Ke Awal/Ke Akhir + input nomor baris. Kedua fitur ini (Cari & Gulir) **tidak butuh state scroll kustom** — cukup memindah `fieldValue.selection`, karena `BasicTextField` sudah otomatis menggulir agar posisi kursor/seleksi tetap terlihat.
    - **Cetak:** fungsi `printDocument()` — memakai Android Print Framework bawaan (`android.print.PrintManager`) lewat `WebView` sebagai perantara render teks ke `PrintDocumentAdapter`, tanpa dependensi/library baru. **Keterbatasan yang didokumentasikan:** `WebView` dibuat sesaat tanpa dipasang ke hierarki tampilan (view hierarchy) — pola umum untuk print-teks-sederhana, tapi berperilaku tidak 100% konsisten di semua versi/vendor Android dibanding WebView yang ditempel ke layar. Jika ditemukan device yang gagal, solusi lanjutannya adalah menempelkan WebView tersembunyi ke root Activity.
    - **Verifikasi:** tidak ada toolchain Gradle/Android SDK di lingkungan pembuatan ini untuk build asli — verifikasi dilakukan lewat pengecekan keseimbangan kurung, jejak semua import baru dipastikan terpakai, dan penelusuran manual bahwa setiap item menu ⋮ punya `onClick` nyata (bukan `available = false`).

11. **[Audit kecacatan logika + Batch 1 — v1.4.2]** User meminta: "implementasikan semua fitur TxtPad+ hingga matang" + "audit semua kecacatan logika". Seluruh 17 file `.kt` ditelusuri manual dan fitur TxtPad+ asli diriset dari Play Store/ulasan pengguna sebelum mulai kerja (bukan tebakan). Temuan & disepakati: kerjakan berurutan per batch (bukan sekaligus), tema default TETAP gelap. Batch 1 = perbaikan bug saja:
    - **Bug nyata (diperbaiki):** `RegexUtils.regexDispatcher` sebelumnya `Dispatchers.Default.limitedParallelism(1)` — 1 thread dipakai ulang selamanya. Karena mesin regex JVM/Android tidak responsif terhadap cancellation di tengah catastrophic backtracking, thread yang timeout tetap jalan di background (ini SUDAH didokumentasikan sejak awal) — tapi karena cuma 1 thread yang di-reuse, itu berarti SATU pola regex bermasalah memblokir SEMUA permintaan Cari & Ganti Regex berikutnya selamanya (timeout terus tanpa pernah benar-benar jalan) sampai app di-restart — ini bug nyata yang belum tercatat sebelumnya, bukan cuma keterbatasan. Diganti ke `Executors.newCachedThreadPool()` — tetap terisolasi dari `Dispatchers.Default`, tapi thread yang macet ditinggalkan sendirian tanpa memblokir panggilan berikutnya.
    - **Bug nyata (diperbaiki):** `BottomFileBar` — item menu dengan `available=false` ("Segera Hadir") sebelumnya tetap memanggil `onClick()` walau tidak ada guard (untungnya belum bermanifestasi karena semua item saat ini `available=true`). Ditambah `enabled = item.available` pada `DropdownMenuItem` + guard eksplisit di dalam `onClick`.
    - **Ditinjau, SENGAJA TIDAK diubah:** `ExternalFileUtils.localNameFor` pakai hash 32-bit (`String.hashCode()`) dari Uri — risiko tabrakan kecil tapi ada. Sempat diganti ke SHA-256 lalu **dibatalkan**: mengganti algoritma hash membuat berkas yang sudah diimpor sebelumnya (nama lokal format lama) tidak lagi ter-mapping ke salinan lokalnya sendiri saat dibuka ulang lewat "Buka Dengan" — menghasilkan duplikat nyata bagi pengguna lama. Untuk risiko tabrakan 32-bit yang di praktik penggunaan nyata nyaris tidak pernah terjadi, regresi itu tidak sepadan — dibiarkan seperti semula, dicatat sebagai keterbatasan yang diterima.
    - **Belum disentuh (menunggu Batch 2 & 3):** semua fitur tampilan (font, tema) dan Info Berkas (word/char count) — sengaja tidak dicampur dengan perbaikan bug di batch yang sama.

12. **[Build gagal — v1.4.2, hotfix jadi v1.4.3]** ⚠️ CI (`compileReleaseKotlin`) gagal setelah push v1.4.2. Penyebab: perbaikan guard menu terkunci di `BottomFileBar.kt` (insiden #11) memakai `return@onClick` — label ini TIDAK valid karena lambda `onClick = { ... }` di situ adalah argumen bernama (named argument), bukan trailing lambda, sehingga tidak mendapat label implisit dari nama parameter. Kotlin compiler: `Unresolved reference: @onClick`. Diperbaiki dengan mengganti ke struktur `if (item.available) { ... }` tanpa non-local return — perilaku/guard yang dimaksud tetap sama persis, hanya cara penulisannya yang diperbaiki. User memberi tahu lewat log CI (`logs_83322824362.zip`) yang diunggah setelah push v1.4.2 gagal.

## Keputusan Arsitektur Utama
- **Penyimpanan:** `java.io.File` langsung ke `filesDir/notes` (internal storage app-specific, tidak perlu permission). **Belum** migrasi ke Storage Access Framework/`Uri` — itu Batch 3, butuh konfirmasi eksplisit dulu karena mengubah `FileUtils`, `TabItem`, `TabManager` hampir menyeluruh.
- **Concurrency:** `Dispatchers.IO.limitedParallelism(1)` khusus untuk write (urutan auto-save terjamin, no race). `Dispatchers.Default.limitedParallelism(1)` khusus untuk regex (isolasi agar pola "meledak" tidak menyita thread pool lain).
- **State persistence:** `rememberSaveable` dengan `Saver` kustom untuk `TabManager` (bukan `ViewModel`/`SavedStateHandle` formal — itu bagian dari Batch 3).
- **Auto-save:** instan per keystroke (bukan debounce timer) — write dilakukan async tapi tetap dipicu tiap karakter berubah.

## Struktur Modul Singkat
```
app/src/main/java/com/promptnotepad/app/
├── MainActivity.kt        # Entry point, wiring seluruh state & UI, handle Intent VIEW/EDIT
├── model/                 # TabItem (+ isDirty, sourceUri), TodoTask
├── state/                 # TabManager (tab list, active index, eviction)
├── ui/                    # TextEditor, TabBar, ShortcutBar, PremiumLayout,
│                           # BottomFileBar (baru, menu ⋮), MarkdownViewer,
│                           # TodoHighlighter, theme/
└── util/                  # FileUtils (I/O async), RegexUtils (async+timeout),
                            # ExternalFileUtils (impor & sinkron "Buka Dengan"), TodoParser
```

## Belum Dikerjakan (menunggu instruksi)
- Batch 4: Undo/Redo stack, Hardware keyboard shortcuts (`onKeyEvent`)

## Keputusan Ditolak (dengan alasan, jangan diusulkan ulang tanpa alasan baru)
- **Auto-save timer-debounce (3 detik):** ditolak. Auto-save instan async yang sudah ada lebih aman (jendela data-loss lebih kecil) dan tidak membekukan UI — timer hanya akan mengurangi frekuensi tulis dengan trade-off resiko kehilangan data lebih besar.
- **Migrasi SAF/Uri penuh (DocumentFile, folder picker, dst):** ditolak. Premis "scoped storage butuh SAF" tidak berlaku untuk app ini karena penyimpanan sudah `filesDir` (internal, tidak kena scoped storage). Diganti dengan fitur "Buka Dengan" (intent-filter + impor/sinkron Uri) yang benar-benar menjawab kebutuhan user tanpa migrasi arsitektur.
- **Ganti algoritma hash nama file lokal impor eksternal (mis. ke SHA-256):** ditolak (v1.4.2). Risiko tabrakan hash 32-bit saat ini memang ada secara teori, tapi mengganti algoritmanya membuat berkas yang sudah diimpor sebelumnya (nama lokal format lama) tidak lagi ter-mapping ke salinan lokalnya sendiri — duplikat nyata bagi pengguna lama, regresi yang tidak sepadan dengan risiko teoretis yang sangat kecil di praktik.
