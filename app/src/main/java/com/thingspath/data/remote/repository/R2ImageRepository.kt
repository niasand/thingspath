package com.thingspath.data.remote.repository

import android.util.Log
import com.thingspath.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class R2ImageRepository @Inject constructor() {

    private val client = OkHttpClient()
    private val endpoint = "https://${BuildConfig.R2_ACCOUNT_ID}.r2.cloudflarestorage.com"
    private val publicUrl = BuildConfig.R2_PUBLIC_URL

    suspend fun uploadImage(file: File, key: String): String? = withContext(Dispatchers.IO) {
        try {
            val content = file.readBytes()
            val contentType = when (file.extension.lowercase()) {
                "jpg", "jpeg" -> "image/jpeg"
                "png" -> "image/png"
                "webp" -> "image/webp"
                "gif" -> "image/gif"
                else -> "image/jpeg"
            }

            val url = "$endpoint/${BuildConfig.R2_BUCKET_NAME}/$key"
            val headers = signRequest(
                method = "PUT",
                uri = "/${BuildConfig.R2_BUCKET_NAME}/$key",
                payload = content,
                contentType = contentType
            )

            val request = Request.Builder()
                .url(url)
                .put(content.toRequestBody(contentType.toMediaType()))
                .apply {
                    headers.forEach { (name, value) -> addHeader(name, value) }
                }
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Log.d(TAG, "Uploaded to R2: $key")
                    "$publicUrl/$key"
                } else {
                    Log.e(TAG, "R2 upload failed: ${response.code} - ${response.body?.string()}")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload image to R2", e)
            null
        }
    }

    suspend fun deleteImage(key: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = "$endpoint/${BuildConfig.R2_BUCKET_NAME}/$key"
            val headers = signRequest(
                method = "DELETE",
                uri = "/${BuildConfig.R2_BUCKET_NAME}/$key",
                payload = ByteArray(0),
                contentType = ""
            )

            val request = Request.Builder()
                .url(url)
                .delete()
                .apply {
                    headers.forEach { (name, value) -> addHeader(name, value) }
                }
                .build()

            client.newCall(request).execute().use { response ->
                val success = response.isSuccessful || response.code == 404
                if (success) {
                    Log.d(TAG, "Deleted from R2: $key")
                } else {
                    Log.e(TAG, "R2 delete failed: ${response.code}")
                }
                success
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete image from R2", e)
            false
        }
    }

    /**
     * AWS Signature Version 4
     */
    private fun signRequest(
        method: String,
        uri: String,
        payload: ByteArray,
        contentType: String
    ): Map<String, String> {
        val accessKey = BuildConfig.R2_ACCESS_KEY_ID
        val secretKey = BuildConfig.R2_SECRET_ACCESS_KEY
        val region = "auto"
        val service = "s3"

        val now = Date()
        val dateStamp = DATE_FORMAT.format(now)
        val amzDate = DATETIME_FORMAT.format(now)

        val payloadHash = sha256Hex(payload)

        val headers = mutableMapOf(
            "host" to "${BuildConfig.R2_ACCOUNT_ID}.r2.cloudflarestorage.com",
            "x-amz-content-sha256" to payloadHash,
            "x-amz-date" to amzDate
        )

        if (contentType.isNotEmpty()) {
            headers["content-type"] = contentType
        }

        // Create canonical request
        val signedHeaders = headers.keys.sorted().joinToString(";")
        val canonicalHeaders = headers.toSortedMap()
            .map { "${it.key.lowercase()}:${it.value.trim()}\n" }
            .joinToString("")

        val canonicalRequest = buildString {
            append(method.uppercase())
            append('\n')
            append(uri)
            append('\n')
            append("\n") // query string (empty)
            append(canonicalHeaders)
            append('\n')
            append(signedHeaders)
            append('\n')
            append(payloadHash)
        }

        // Create string to sign
        val credentialScope = "$dateStamp/$region/$service/aws4_request"
        val stringToSign = buildString {
            append("AWS4-HMAC-SHA256")
            append('\n')
            append(amzDate)
            append('\n')
            append(credentialScope)
            append('\n')
            append(sha256Hex(canonicalRequest.toByteArray(StandardCharsets.UTF_8)))
        }

        // Calculate signature
        val signingKey = getSigningKey(secretKey, dateStamp, region, service)
        val signature = hmacHex(signingKey, stringToSign.toByteArray(StandardCharsets.UTF_8))

        // Create authorization header
        val authHeader = "AWS4-HMAC-SHA256 " +
            "Credential=$accessKey/$credentialScope, " +
            "SignedHeaders=$signedHeaders, " +
            "Signature=$signature"

        return headers + ("Authorization" to authHeader)
    }

    private fun getSigningKey(secretKey: String, dateStamp: String, region: String, service: String): ByteArray {
        val kSecret = ("AWS4" + secretKey).toByteArray(StandardCharsets.UTF_8)
        val kDate = hmac(kSecret, dateStamp.toByteArray(StandardCharsets.UTF_8))
        val kRegion = hmac(kDate, region.toByteArray(StandardCharsets.UTF_8))
        val kService = hmac(kRegion, service.toByteArray(StandardCharsets.UTF_8))
        return hmac(kService, "aws4_request".toByteArray(StandardCharsets.UTF_8))
    }

    private fun hmac(key: ByteArray, data: ByteArray): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key, "HmacSHA256"))
        return mac.doFinal(data)
    }

    private fun hmacHex(key: ByteArray, data: ByteArray): String {
        return hmac(key, data).toHex()
    }

    private fun sha256Hex(data: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(data).toHex()
    }

    private fun ByteArray.toHex(): String {
        return joinToString("") { "%02x".format(it) }
    }

    fun extractKeyFromUrl(url: String): String? {
        return if (url.startsWith(publicUrl)) {
            url.removePrefix("$publicUrl/")
        } else {
            null
        }
    }

    fun isR2Url(path: String): Boolean {
        return path.startsWith(publicUrl)
    }

    companion object {
        private const val TAG = "R2ImageRepository"
        private val DATE_FORMAT = SimpleDateFormat("yyyyMMdd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        private val DATETIME_FORMAT = SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    }
}
