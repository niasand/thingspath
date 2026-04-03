package com.thingspath.data.remote.repository

import android.util.Log
import com.thingspath.BuildConfig
import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.DeleteObjectRequest
import aws.sdk.kotlin.services.s3.model.PutObjectRequest
import aws.smithy.kotlin.runtime.content.asByteStream
import aws.smithy.kotlin.runtime.net.url.Url
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class R2ImageRepository @Inject constructor() {

    suspend fun uploadImage(file: File, key: String): String? {
        return try {
            S3Client {
                region = "auto"
                endpointUrl = Url.parse("https://${BuildConfig.R2_ACCOUNT_ID}.r2.cloudflarestorage.com")
                credentialsProvider = StaticCredentialsProvider {
                    accessKeyId = BuildConfig.R2_ACCESS_KEY_ID
                    secretAccessKey = BuildConfig.R2_SECRET_ACCESS_KEY
                }
            }.use { client ->
                client.putObject(
                    PutObjectRequest {
                        bucket = BuildConfig.R2_BUCKET_NAME
                        this.key = key
                        body = file.asByteStream()
                    }
                )
                "${BuildConfig.R2_PUBLIC_URL}/$key"
            }
        } catch (e: Exception) {
            Log.e("R2ImageRepository", "Failed to upload image to R2", e)
            null
        }
    }

    suspend fun deleteImage(key: String): Boolean {
        return try {
            S3Client {
                region = "auto"
                endpointUrl = Url.parse("https://${BuildConfig.R2_ACCOUNT_ID}.r2.cloudflarestorage.com")
                credentialsProvider = StaticCredentialsProvider {
                    accessKeyId = BuildConfig.R2_ACCESS_KEY_ID
                    secretAccessKey = BuildConfig.R2_SECRET_ACCESS_KEY
                }
            }.use { client ->
                client.deleteObject(
                    DeleteObjectRequest {
                        bucket = BuildConfig.R2_BUCKET_NAME
                        this.key = key
                    }
                )
                Log.d("R2ImageRepository", "Deleted image from R2: $key")
                true
            }
        } catch (e: Exception) {
            Log.e("R2ImageRepository", "Failed to delete image from R2", e)
            false
        }
    }

    /**
     * Extract R2 key from public URL
     * e.g., "https://pub-xxx.r2.dev/items/uuid.jpg" -> "items/uuid.jpg"
     */
    fun extractKeyFromUrl(url: String): String? {
        val publicUrl = BuildConfig.R2_PUBLIC_URL
        return if (url.startsWith(publicUrl)) {
            url.removePrefix("$publicUrl/")
        } else {
            null
        }
    }

    /**
     * Check if the given path is an R2 URL
     */
    fun isR2Url(path: String): Boolean {
        return path.startsWith(BuildConfig.R2_PUBLIC_URL)
    }
}
