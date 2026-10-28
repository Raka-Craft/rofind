# RoFind — Cari Map Roblox dengan Bahasa Sendiri

## Versi Web (baru)
Buka `index.html` di browser apa pun (HP maupun Desktop) — tanpa install, tanpa build.

- **Smart Bilingual & Mechanic Search:** ketik deskripsi bahasa Indonesia, misal
  "game balapan", "game yang bisa berenang", atau "game yang bisa mencuri telur".
  Stopword dihapus otomatis, kata kunci diterjemahkan ke tag Inggris lewat
  Synonym Mapper, lalu dicocokkan ke judul, deskripsi Indonesia, mekanik, dan tag
  dengan Multi-Field Fuzzy Scoring.
- **Database lokal 56 map** dari berbagai kategori: Balapan, Air & Eksplorasi,
  Simulasi, Obby/Parkour, Horor, Roleplay, Action/PVP.
- **Fitur:** tema Dark / Light / AMOLED Midnight, accent color picker (termasuk
  custom hex), riwayat pencarian, favorit, filter kategori, urutan
  (Relevan / Populer / Rating), modal detail + Copy Link, dan tombol
  "Pilihkan Map Acak". Semua tersimpan di `localStorage` browser.

Catatan: angka pemain dan rating di kartu adalah data perkiraan dataset untuk
mengurutkan popularitas; angka live bisa berubah, cek di halaman Roblox resmi.

## Versi Android (lama)
Source Android native ada di `src/com/carimaproblox/MainActivity.java`
(package `com.carimaproblox`, ikon RoFind). Versi web di atas adalah
perombakan total yang direkomendasikan.
