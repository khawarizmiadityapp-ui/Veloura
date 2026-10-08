# 🚀 Panduan Deploy Backend Veloura di Hugging Face Spaces (100% Gratis Tanpa Kartu Kredit)

Hugging Face Spaces memberikan server gratis **2 vCPU & 16 GB RAM** tanpa meminta kartu kredit/debit sama sekali.

---

### Langkah 1: Buat Akun Hugging Face
1. Buka [https://huggingface.co/join](https://huggingface.co/join).
2. Daftar dengan email kamu atau klik **Sign in with GitHub**.
3. Buka email kamu dan klik tautan konfirmasi pendaftaran.

---

### Langkah 2: Buat Space Baru
1. Buka [https://huggingface.co/new-space](https://huggingface.co/new-space).
2. Isi formulir:
   - **Space name**: `veloura-backend`
   - **License**: `mit`
   - **Select the Space SDK**: Pilih **Docker** ➔ pilih opsi **Blank**
   - **Space hardware**: Pilih **CPU basic · 2 vCPU · 16GB RAM · Free**
   - **Visibility**: Pilih **Public**
3. Klik tombol **Create Space**.

---

### Langkah 3: Upload File Backend
1. Di halaman Space yang baru dibuat, klik tab **Files** (di samping kanan tab *App*).
2. Klik tombol **Add file** ➔ pilih **Upload files**.
3. Drag & drop 3 file berikut dari folder proyek `backend/`:
   - `Dockerfile`
   - `requirements.txt`
   - `server.py`
4. Di bagian bawah, klik tombol **Commit changes to main**.

---

### Langkah 4: Ambil URL dan Sambungkan ke HP
1. Tunggu sekitar 1 menit sampai status di bagian atas berubah dari *Building* menjadi **Running** (hijau).
2. Di pojok kanan atas (di samping tombol restart/pause), klik ikon titik tiga (⋮) atau tombol share, lalu klik **Embed this Space**.
3. Salin tautan **Direct URL**, formatnya seperti:
   `https://username-veloura-backend.hf.space`
4. Buka aplikasi **Veloura** di HP kamu:
   - Masuk ke tab **Settings** (ikon gerigi).
   - Di **Audio Backend API URL**, tempelkan URL tersebut.
   - Klik **Test Connection**. Status akan berubah menjadi *"Connected to Veloura Backend"*.

🎉 **Selesai!** Musik kamu sekarang online 24/7 gratis tanpa perlu kartu kredit!
