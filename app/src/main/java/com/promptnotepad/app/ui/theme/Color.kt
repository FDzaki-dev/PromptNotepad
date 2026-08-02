package com.promptnotepad.app.ui.theme

import androidx.compose.ui.graphics.Color

val PureBlack = Color(0xFF141414)       // Latar belakang utama — dilunakkan dari AMOLED murni (0x000000)
                                         // agar tidak terlalu keras di mata; nama identifier dipertahankan
                                         // apa adanya supaya seluruh file yang sudah memakainya tidak perlu diubah.
val DeepGray = Color(0xFF1C1C1C)        // Latar belakang komponen / Tab tidak aktif (sedikit lebih terang dari background utama)
val SurfaceGray = Color(0xFF242424)     // Latar belakang dialog atau input teks
val PremiumBorder = Color(0xFF333333)   // Garis batas tipis antar komponen
val TextPrimary = Color(0xFFE5E5E5)     // Warna teks utama (off-white agar tidak silau)
val TextSecondary = Color(0xFF888888)   // Warna teks keterangan / tidak aktif
val PremiumAccent = Color(0xFFD4AF37)   // Aksen emas premium
val CodeGreen = Color(0xFF4AF626)       // Aksen terminal untuk elemen perintah
val LockedText = Color(0xFF5C5C5C)      // Warna teks/ikon untuk menu item yang belum tersedia ("Segera Hadir")

// --- Palet Tema Terang (Batch 2 — toggle tema) ---
// Nilai gelap di atas TIDAK diubah/dihapus (tetap jadi default), palet ini
// hanya alternatif yang dipilih lewat AppColors di Theme.kt saat pengguna
// mengaktifkan tema terang.
val LightBackground = Color(0xFFFAFAFA)  // Latar belakang utama
val LightSurface = Color(0xFFECECEC)     // Latar komponen / tab tidak aktif
val LightBorder = Color(0xFFD0D0D0)      // Garis batas tipis antar komponen
val LightTextPrimary = Color(0xFF1A1A1A) // Warna teks utama
val LightTextSecondary = Color(0xFF666666) // Warna teks keterangan / tidak aktif
val LightAccent = Color(0xFF9C7A1D)      // Emas didalamkan agar kontras cukup di atas latar terang
val LightCodeGreen = Color(0xFF1E8A0F)   // Hijau terminal didalamkan agar kontras cukup di atas latar terang
val LightLocked = Color(0xFFAFAFAF)      // Warna teks/ikon menu "(Segera Hadir)" di tema terang
