# 🚀 Panduan Deploy Backend Veloura di Vercel (100% Gratis Tanpa Kartu Kredit)

Vercel adalah platform cloud kelas dunia yang **100% GRATIS (Hobby Tier)** dan **SAMA SEKALI TIDAK MINTA KARTU KREDIT/DEBIT**.

Konfigurasi Vercel sudah disiapkan di folder `backend/`:
- `vercel.json`
- `api/index.py`
- `requirements.txt`
- `server.py`

---

### Langkah 1: Push / Upload Perubahan ke GitHub
Pastikan file terbaru di folder `backend/` sudah kamu push ke repositori GitHub:
```bash
git add backend/
git commit -m "Add Vercel configuration"
git push
```

---

### Langkah 2: Deploy di Vercel (Gratis Tanpa Kartu)
1. Buka [https://vercel.com/signup](https://vercel.com/signup).
2. Pilih **Continue with GitHub** (login pakai akun GitHub kamu).
3. Setelah masuk ke dashboard Vercel, klik tombol **Add New...** di kanan atas ➔ pilih **Project**.
4. Di daftar repositori GitHub kamu, cari repo **Veloura** lalu klik **Import**.
5. Di formulir konfigurasi proyek:
   - **Root Directory**: Klik tombol *Edit*, lalu pilih folder `backend`.
   - **Framework Preset**: Pilih `Other` (otomatis terdeteksi).
6. Klik tombol **Deploy**!

Tunggu sekitar 30–60 detik sampai muncul animasi kembang api 🎉 (**Congratulations!**).

---

### Langkah 3: Salin URL dan Pasang di HP
1. Salin domain yang diberikan Vercel, contohnya:
   `https://veloura-backend.vercel.app`
2. Buka aplikasi **Veloura** di HP Android kamu:
   - Masuk ke menu **Settings** (ikon gerigi).
   - Di **Audio Backend API URL**, tempelkan URL Vercel kamu.
   - Klik **Test Connection** ➔ Status berubah menjadi *"Connected to Veloura Backend"*.

🎉 **Selesai!** Backend kamu sekarang aktif di cloud Vercel gratis selamanya!
