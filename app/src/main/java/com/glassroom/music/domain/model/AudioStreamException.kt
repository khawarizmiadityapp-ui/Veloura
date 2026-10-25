package com.glassroom.music.domain.model

sealed class AudioStreamException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class YouTubeBotBlocked(
        message: String = "YouTube memblokir IP server cloud (Sign in to confirm you're not a bot)."
    ) : AudioStreamException(message)

    class TrackNotFound(
        message: String = "Audio stream tidak ditemukan untuk lagu ini."
    ) : AudioStreamException(message)

    class BackendUnreachable(
        message: String = "Tidak dapat terhubung ke server backend.",
        cause: Throwable? = null
    ) : AudioStreamException(message, cause)

    class ServerError(
        val httpCode: Int,
        message: String = "Server backend mengembalikan kesalahan HTTP $httpCode."
    ) : AudioStreamException(message)

    class InvalidStreamUrl(
        message: String = "URL stream audio tidak valid atau kosong."
    ) : AudioStreamException(message)
}
