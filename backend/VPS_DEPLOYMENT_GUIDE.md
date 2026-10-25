# Panduan Deployment Backend Veloura ke VPS (24/7 Persistent Server)

Dokumen ini menjelaskan cara menjalankan backend Python Veloura secara mandiri dan terus-menerus pada Virtual Private Server (VPS) agar aplikasi Veloura Android tidak bergantung pada laptop atau start_backend.bat.

---

## 1. Mengapa Vercel Memiliki Batasan untuk Ekstraksi Audio?
Vercel adalah platform *serverless* berbasis cloud (AWS Lambda/GCP datacenter):
1. **IP Datacenter Diblokir YouTube**: YouTube secara otomatis menandai subnet IP serverless Vercel (seperti rentang AWS `iad1`, `sin1`) dan menampilkan respon:
   `"Sign in to confirm you're not a bot"`.
2. **Batas Eksekusi Singkat (Execution Timeout)**: Serverless function di Vercel memiliki batas waktu 10–15 detik. Proxy streaming audio (`/api/stream/audio/<id>`) terputus di tengah pemutaran lagu.
3. **Ketiadaan FFmpeg**: Transcoding audio tidak tersedia secara bawaan di runtime Vercel Python.
4. **IP Binding pada URL Audio**: URL `googlevideo.com` yang diekstrak terikat pada IP pembuat request (`&ip=...`). URL yang diekstrak oleh Vercel akan ditolak oleh YouTube saat diputar langsung oleh ponsel di jaringan seluler/Wi-Fi yang berbeda.

---

## 2. Pilihan Hosting VPS Murah / Gratis
- **Oracle Cloud Free Tier**: Gratis 2 instance VM selamanya (Always Free).
- **DigitalOcean / Linode / Hetzner**: $4 - $5 / bulan.
- **Domainesia / Niagahoster / IdCloudHost**: VPS Indonesia (sangat baik untuk latensi rendah dan IP lokal).

---

## 3. Cara Setup Backend Menggunakan Docker (Rekomendasi Tercepat)

### Langkah 1: Hubungkan ke VPS melalui SSH
```bash
ssh root@IP_VPS_ANDA
```

### Langkah 2: Install Docker & Docker Compose
```bash
sudo apt update && sudo apt install -y docker.io docker-compose git
```

### Langkah 3: Clone Repository Veloura
```bash
git clone https://github.com/khawarizmiadityapp-ui/Veloura.git
cd Veloura/backend
```

### Langkah 4: Jalankan Container di Background
```bash
docker-compose up -d --build
```

Backend Anda sekarang berjalan 24 jam di `http://IP_VPS_ANDA:5000/`.

---

## 4. Cara Setup HTTPS Otomatis dengan Caddy (Opsional namun Sangat Dianjurkan)
Android versi modern memprioritaskan koneksi HTTPS. Menggunakan Caddy membuat VPS Anda mendapatkan sertifikat SSL gratis secara otomatis:

1. Install Caddy:
   ```bash
   sudo apt install -y debian-keyring debian-archive-keyring apt-transport-https
   curl -1sLf 'https://dl.cloudsmith.io/public/caddy/stable/gpg.key' | sudo gpg --dearmor -o /usr/share/keyrings/caddy-stable-archive-keyring.gpg
   curl -1sLf 'https://dl.cloudsmith.io/public/caddy/stable/debian.deb.txt' | sudo tee /etc/apt/sources.list.d/caddy-stable.list
   sudo apt update && sudo apt install caddy
   ```

2. Edit `/etc/caddy/Caddyfile`:
   ```caddy
   music.domainanda.com {
       reverse_proxy 127.0.0.1:5000
   }
   ```

3. Restart Caddy:
   ```bash
   sudo systemctl restart caddy
   ```

---

## 5. Cara Mengatur URL di Aplikasi Veloura
1. Buka aplikasi Veloura di Android.
2. Masuk ke menu **Settings** > bagian **Backend Service**.
3. Masukkan URL VPS Anda (misalnya `https://music.domainanda.com/` atau `http://IP_VPS:5000/`).
4. Tekan tombol **Test Koneksi**. Jika muncul `✓ Online: Veloura Audio Backend v1.0.0 (yt-dlp ...)`, tekan tombol **Terapkan**.
5. Selesai! Aplikasi kini memutar lagu secara mandiri tanpa memerlukan laptop menyala.

---

## 6. Catatan Teknis Mengenai Penolakan YouTube (Anti-Bot)
> [!IMPORTANT]
> Berpindah ke VPS **tidak secara otomatis menjamin 100% bebas dari penolakan YouTube**.
> Jika penyedia VPS memiliki blok IP yang sering digunakan bot scraping, YouTube dapat meminta verifikasi captcha/bot sewaktu-waktu.
> 
> **Strategi Mitigasi jika VPS terkena batasan:**
> - Gunakan penyedia VPS dengan IP regional yang bersih (misalnya ISP lokal Indonesia atau penyedia niche).
> - Backend Veloura telah dilengkapi rotasi player client (`android_vr`, `android`, `tv_embedded`) yang meminimalisir deteksi bot.
> - Hindari melakukan ratusan request stream per menit dari satu IP.
