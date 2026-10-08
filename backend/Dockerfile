FROM python:3.12-slim

# Install ffmpeg & curl (needed for audio stream extraction)
RUN apt-get update && apt-get install -y --no-install-recommends \
    ffmpeg \
    curl \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY . .

ENV PORT=7860
EXPOSE 7860

CMD gunicorn server:app --bind 0.0.0.0:$PORT --workers 2 --timeout 120
