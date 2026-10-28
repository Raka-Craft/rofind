# RoFind

Aplikasi pencari map/game Roblox dengan bahasa sendiri.

## Cara kerja (v6.0)
- Ketik deskripsi dengan bahasa sendiri, misal "game balapan" atau "game yang bisa mencuri telur".
- Aplikasi TIDAK mencari mentah-mentah. Satu kategori dipecah jadi banyak kata kunci Inggris:
  - balapan -> race, racing car, racing motorcycle, car race, bike race
  - mencuri telur -> steal egg, steal an egg, egg steal
  - brainrot -> steal brainrot, brainrot
- Tiap kata kunci dicari lewat DuckDuckGo/Bing, hasilnya digabung dan dideduplikasi.
- Kalau pencarian live kosong, dipakai daftar kategori terverifikasi (ID game asli Roblox):
  - Balapan: Driving Empire, Race Clicker
  - Blox Fruits: Blox Fruits
- Angka pemain dan foto map diambil terbaru dari API Roblox (games + thumbnails).
- Hasil bisa diurutkan: Terbanyak / Terpopuler / Tersedikit.

## Build
Android native (Java, tanpa library tambahan). Lihat `AndroidManifest.xml` dan `src/com/carimaproblox/MainActivity.java`.
