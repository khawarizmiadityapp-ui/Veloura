# 🚀 Panduan Deploy Backend Veloura ke Cloud Gratis (Render.com)

Ikuti langkah cepat ini agar aplikasi **Veloura** di HP kamu bisa digunakan **tanpa perlu menyalakan laptop sama sekali**:

---

### Langkah 1: Upload / Push Kode ke GitHub
Pastikan folder `backend/` sudah ter-upload ke repositori GitHub kamu (bisa public atau private).

---

### Langkah 2: Deploy di Render.com (Gratis)
1. Buka [render.com](https://render.com/) dan klik **Get Started for Free** (bisa login langsung pakai akun **GitHub** kamu).
2. Di dashboard Render, klik tombol **New +** di pojok kanan atas, lalu pilih **Web Service**.
3. Pilih opsi **Build and deploy from a Git repository**.
4. Cari dan pilih repositori GitHub proyek ini.
5. Isi formulir singkat:
   - **Name**: `veloura-backend` (atau nama pilihanmu)
   - **Root Directory**: `backend`
   - **Environment / Runtime**: `Python 3`
   - **Build Command**: `pip install -r requirements.txt`
   - **Start Command**: `gunicorn server:app --bind 0.0.0.0:$PORT --workers 2 --timeout 120`
   - **Instance Type**: Pilih **Free** ($0/month)
6. Klik tombol **Deploy Web Service** di bagian bawah.

Tunggu sekitar 1–2 menit sampai statusnya berubah menjadi **Live** (berwarna hijau).

---

### Langkah 3: Salin URL dan Tempel ke Aplikasi di HP
1. Di halaman Render, salin URL websitemu, contoh:
   `https://veloura-backend.onrender.com`
2. Buka aplikasi **Veloura** di HP Android kamu.
3. Masuk ke menu **Settings** (ikon gerigi di kanan bawah).
4. Di bagian **Audio Backend API URL**, ganti alamatnya dengan URL Render kamu.
5. Klik **Test Connection**.
   *(Status akan berubah menjadi: "Connected to Veloura Backend")*

---

🎉 **Selesai!**
Sekarang laptop kamu bisa **dimatikan total**. Kamu bisa mencari lagu YouTube apa saja, memutar musik dengan kualitas penuh, dan mengimpor playlist Spotify kapan pun dan di mana pun langsung dari HP!
