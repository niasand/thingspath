package com.thingspath.data.remote.repository

import android.util.Log
import io.mockk.CapturingSlot
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File
import java.io.IOException
import java.lang.reflect.Field

/**
 * R2ImageRepository 全面的单元测试。
 *
 * 覆盖：上传成功/失败/异常、删除成功/失败/异常、签名逻辑、URL 构建、key 提取、文件名判断。
 *
 * 注意：R2ImageRepository 内部自己创建 OkHttpClient 并直接引用 BuildConfig，
 * 所以测试通过反射注入 mock client 和设置 BuildConfig 字段。
 */
class R2ImageRepositoryTest {

    private lateinit var repository: R2ImageRepository
    private lateinit var mockClient: OkHttpClient
    private lateinit var mockCall: Call
    private lateinit var requestSlot: CapturingSlot<Request>

    // 测试用值。由于 signRequest() 直接引用 BuildConfig 静态字段（Java 21 无法通过反射修改），
    // endpoint 和 publicUrl 通过反射注入实例字段，但签名和 URL 路径中的 bucket/account
    // 仍使用 BuildConfig 的实际值。
    private val testAccountId = "testaccount123"
    private val testBucketName = "test-bucket"
    private val testPublicUrl = "https://cdn.example.com"
    private val testAccessKeyId = "AKIAIOSFODNN7EXAMPLE"
    private val testSecretAccessKey = "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY"

    @Before
    fun setup() {
        // Mock android.util.Log（单元测试环境没有 Android runtime）
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any<String>()) } returns 0
        every { Log.e(any(), any<String>(), any()) } returns 0

        mockClient = mockk()
        mockCall = mockk()
        requestSlot = CapturingSlot<Request>()

        // 创建 repository
        repository = R2ImageRepository()

        // 通过反射注入 mock OkHttpClient 和测试用的 endpoint / publicUrl
        injectMockClient(repository, mockClient)
        setInstanceField(repository, "endpoint", "https://$testAccountId.r2.cloudflarestorage.com")
        setInstanceField(repository, "publicUrl", testPublicUrl)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    // ==================== 辅助：mock 成功响应 ====================

    private fun mockSuccessfulResponse(): Response {
        val responseBody = mockk<ResponseBody> {
            every { close() } just Runs
        }
        return mockk<Response> {
            every { isSuccessful } returns true
            every { body } returns responseBody
            every { close() } just Runs
        }
    }

    private fun mockFailedResponse(code: Int, bodyMsg: String = "Error"): Response {
        val errorBody = mockk<ResponseBody> {
            every { string() } returns bodyMsg
            every { close() } just Runs
        }
        return mockk<Response> {
            every { isSuccessful } returns false
            every { this@mockk.code } returns code
            every { body } returns errorBody
            every { close() } just Runs
        }
    }

    /** 配置 mock: 捕获 Request，返回给定 Response */
    private fun stubMockCall(response: Response) {
        every { mockCall.execute() } returns response
        every { mockClient.newCall(capture(requestSlot)) } returns mockCall
    }

    /** 配置 mock: 捕获 Request，抛出异常 */
    private fun stubMockCallThrow(exception: Exception) {
        every { mockCall.execute() } throws exception
        every { mockClient.newCall(capture(requestSlot)) } returns mockCall
    }

    /** 配置 mock 不捕获 Request，直接返回 Response */
    private fun stubMockCallNoCapture(response: Response) {
        every { mockCall.execute() } returns response
        every { mockClient.newCall(any()) } returns mockCall
    }

    /** 配置 mock 不捕获 Request，直接抛异常 */
    private fun stubMockCallThrowNoCapture(exception: Exception) {
        every { mockCall.execute() } throws exception
        every { mockClient.newCall(any()) } returns mockCall
    }

    // ==================== 上传测试 ====================

    @Test
    fun `upload success returns correct public URL`() = runTest {
        stubMockCall(mockSuccessfulResponse())

        val tempFile = createTempImageFile("test.jpg", byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte()))
        val result = repository.uploadImage(tempFile, "images/2024/test.jpg")

        assertEquals("$testPublicUrl/images/2024/test.jpg", result)
        tempFile.delete()
    }

    @Test
    fun `upload PNG file uses image-png content type`() = runTest {
        stubMockCall(mockSuccessfulResponse())

        val tempFile = createTempImageFile("photo.png", byteArrayOf(0x89.toByte(), 0x50, 0x4E))
        val result = repository.uploadImage(tempFile, "images/photo.png")

        assertEquals("$testPublicUrl/images/photo.png", result)
        val capturedBody = requestSlot.captured.body
        assertTrue(
            "Content-Type 应为 image/png",
            capturedBody?.contentType()?.toString()?.startsWith("image/png") == true
        )
        tempFile.delete()
    }

    @Test
    fun `upload WEBP file uses image-webp content type`() = runTest {
        stubMockCall(mockSuccessfulResponse())

        val tempFile = createTempImageFile("pic.webp", byteArrayOf(0x52, 0x49))
        val result = repository.uploadImage(tempFile, "images/pic.webp")

        assertEquals("$testPublicUrl/images/pic.webp", result)
        val capturedBody = requestSlot.captured.body
        assertTrue(
            "Content-Type 应为 image/webp",
            capturedBody?.contentType()?.toString()?.startsWith("image/webp") == true
        )
        tempFile.delete()
    }

    @Test
    fun `upload GIF file uses image-gif content type`() = runTest {
        stubMockCall(mockSuccessfulResponse())

        val tempFile = createTempImageFile("anim.gif", byteArrayOf(0x47, 0x49))
        val result = repository.uploadImage(tempFile, "images/anim.gif")

        assertEquals("$testPublicUrl/images/anim.gif", result)
        val capturedBody = requestSlot.captured.body
        assertTrue(
            "Content-Type 应为 image/gif",
            capturedBody?.contentType()?.toString()?.startsWith("image/gif") == true
        )
        tempFile.delete()
    }

    @Test
    fun `upload unknown extension defaults to image-jpeg`() = runTest {
        stubMockCall(mockSuccessfulResponse())

        val tempFile = createTempImageFile("file.xyz", byteArrayOf(0x00))
        val result = repository.uploadImage(tempFile, "images/file.xyz")

        assertEquals("$testPublicUrl/images/file.xyz", result)
        val capturedBody = requestSlot.captured.body
        assertTrue(
            "未知扩展名应默认 image/jpeg",
            capturedBody?.contentType()?.toString()?.startsWith("image/jpeg") == true
        )
        tempFile.delete()
    }

    @Test
    fun `upload failure with 403 returns null`() = runTest {
        stubMockCallNoCapture(mockFailedResponse(403, "Access Denied"))

        val tempFile = createTempImageFile("fail.jpg", byteArrayOf(0xFF.toByte()))
        val result = repository.uploadImage(tempFile, "images/fail.jpg")

        assertNull("非 2xx 状态码应返回 null", result)
        tempFile.delete()
    }

    @Test
    fun `upload failure with 500 returns null`() = runTest {
        stubMockCallNoCapture(mockFailedResponse(500, "Internal Server Error"))

        val tempFile = createTempImageFile("server_error.jpg", byteArrayOf(0xFF.toByte()))
        val result = repository.uploadImage(tempFile, "images/server_error.jpg")

        assertNull(result)
        tempFile.delete()
    }

    @Test
    fun `upload IOException returns null without crash`() = runTest {
        stubMockCallThrowNoCapture(IOException("Network unreachable"))

        val tempFile = createTempImageFile("ioerror.jpg", byteArrayOf(0xFF.toByte()))
        val result = repository.uploadImage(tempFile, "images/ioerror.jpg")

        assertNull("IOException 应被捕获，返回 null", result)
        tempFile.delete()
    }

    @Test
    fun `upload RuntimeException returns null without crash`() = runTest {
        stubMockCallThrowNoCapture(RuntimeException("Unexpected error"))

        val tempFile = createTempImageFile("runtime.jpg", byteArrayOf(0xFF.toByte()))
        val result = repository.uploadImage(tempFile, "images/runtime.jpg")

        assertNull("未预期的异常也应被捕获，返回 null", result)
        tempFile.delete()
    }

    @Test
    fun `upload empty file succeeds without crash`() = runTest {
        stubMockCall(mockSuccessfulResponse())

        val tempFile = createTempImageFile("empty.jpg", byteArrayOf())
        val result = repository.uploadImage(tempFile, "images/empty.jpg")

        assertEquals("$testPublicUrl/images/empty.jpg", result)
        tempFile.delete()
    }

    // ==================== 签名逻辑测试 ====================

    @Test
    fun `request has AWS4-HMAC-SHA256 Authorization header`() = runTest {
        stubMockCall(mockSuccessfulResponse())

        val tempFile = createTempImageFile("sign.jpg", byteArrayOf(0xFF.toByte()))
        repository.uploadImage(tempFile, "images/sign.jpg")

        val authHeader = requestSlot.captured.header("Authorization")
        assertNotNull("Authorization header 不应为 null", authHeader)
        assertTrue(
            "Authorization 应以 AWS4-HMAC-SHA256 开头",
            authHeader!!.startsWith("AWS4-HMAC-SHA256")
        )
        tempFile.delete()
    }

    @Test
    fun `Authorization header contains correct Credential format`() = runTest {
        stubMockCall(mockSuccessfulResponse())

        val tempFile = createTempImageFile("cred.jpg", byteArrayOf(0xFF.toByte()))
        repository.uploadImage(tempFile, "images/cred.jpg")

        val authHeader = requestSlot.captured.header("Authorization")!!
        // AccessKeyId 来自 BuildConfig（无法通过反射修改），验证格式而非具体值
        assertTrue(
            "Credential 应包含 AccessKeyId",
            authHeader.contains("Credential=") && authHeader.contains("/auto/s3/aws4_request")
        )
        assertTrue(
            "Credential scope 应包含 region=auto",
            authHeader.contains("/auto/s3/aws4_request")
        )
        tempFile.delete()
    }

    @Test
    fun `Authorization header contains SignedHeaders field`() = runTest {
        stubMockCall(mockSuccessfulResponse())

        val tempFile = createTempImageFile("signed.jpg", byteArrayOf(0xFF.toByte()))
        repository.uploadImage(tempFile, "images/signed.jpg")

        val authHeader = requestSlot.captured.header("Authorization")!!
        assertTrue("SignedHeaders 应存在", authHeader.contains("SignedHeaders="))
        assertTrue("SignedHeaders 应包含 host", authHeader.contains("host"))
        tempFile.delete()
    }

    @Test
    fun `request has x-amz-date header in ISO8601 basic format`() = runTest {
        stubMockCall(mockSuccessfulResponse())

        val tempFile = createTempImageFile("date.jpg", byteArrayOf(0xFF.toByte()))
        repository.uploadImage(tempFile, "images/date.jpg")

        val amzDate = requestSlot.captured.header("x-amz-date")
        assertNotNull("x-amz-date header 不应为 null", amzDate)
        assertTrue(
            "x-amz-date 格式应为 yyyyMMddTHHmmssZ",
            amzDate!!.matches(Regex("\\d{8}T\\d{6}Z"))
        )
        tempFile.delete()
    }

    @Test
    fun `request has x-amz-content-sha256 header as 64-char hex`() = runTest {
        stubMockCall(mockSuccessfulResponse())

        val tempFile = createTempImageFile("sha.jpg", byteArrayOf(0xFF.toByte()))
        repository.uploadImage(tempFile, "images/sha.jpg")

        val contentSha = requestSlot.captured.header("x-amz-content-sha256")
        assertNotNull("x-amz-content-sha256 header 不应为 null", contentSha)
        assertEquals("SHA-256 hex 应为 64 个字符", 64, contentSha!!.length)
        assertTrue("SHA-256 应全部为 hex 字符", contentSha.matches(Regex("[0-9a-f]+")))
        tempFile.delete()
    }

    // ==================== URL 构建测试 ====================

    @Test
    fun `upload request URL uses correct endpoint and bucket and key`() = runTest {
        stubMockCall(mockSuccessfulResponse())

        val tempFile = createTempImageFile("url.jpg", byteArrayOf(0xFF.toByte()))
        repository.uploadImage(tempFile, "path/to/image.jpg")

        val requestUrl = requestSlot.captured.url.toString()
        // endpoint 通过反射注入使用 testAccountId，但 bucket 名来自 BuildConfig（无法修改）
        val realBucket = com.thingspath.BuildConfig.R2_BUCKET_NAME
        assertEquals(
            "https://${testAccountId}.r2.cloudflarestorage.com/$realBucket/path/to/image.jpg",
            requestUrl
        )
        tempFile.delete()
    }

    @Test
    fun `upload request uses PUT method`() = runTest {
        stubMockCall(mockSuccessfulResponse())

        val tempFile = createTempImageFile("method.jpg", byteArrayOf(0xFF.toByte()))
        repository.uploadImage(tempFile, "images/method.jpg")

        assertEquals("请求方法应为 PUT", "PUT", requestSlot.captured.method)
        tempFile.delete()
    }

    // ==================== deleteImage 测试 ====================

    @Test
    fun `delete success returns true`() = runTest {
        val response = mockk<Response> {
            every { isSuccessful } returns true
            every { close() } just Runs
        }
        stubMockCallNoCapture(response)

        val result = repository.deleteImage("images/delete.jpg")
        assertTrue("删除成功应返回 true", result)
    }

    @Test
    fun `delete non-existent file returns true on 404`() = runTest {
        val response = mockk<Response> {
            every { isSuccessful } returns false
            every { code } returns 404
            every { close() } just Runs
        }
        stubMockCallNoCapture(response)

        val result = repository.deleteImage("images/notexist.jpg")
        assertTrue("404 应视为删除成功", result)
    }

    @Test
    fun `delete failure with 403 returns false`() = runTest {
        val response = mockk<Response> {
            every { isSuccessful } returns false
            every { code } returns 403
            every { close() } just Runs
        }
        stubMockCallNoCapture(response)

        val result = repository.deleteImage("images/forbidden.jpg")
        assertFalse("非 404 的失败应返回 false", result)
    }

    @Test
    fun `delete IOException returns false without crash`() = runTest {
        stubMockCallThrowNoCapture(IOException("Connection refused"))

        val result = repository.deleteImage("images/connrefused.jpg")
        assertFalse("IOException 应被捕获，返回 false", result)
    }

    @Test
    fun `delete request uses DELETE method`() = runTest {
        val response = mockk<Response> {
            every { isSuccessful } returns true
            every { close() } just Runs
        }
        stubMockCall(response)

        repository.deleteImage("images/method_check.jpg")
        assertEquals("删除请求方法应为 DELETE", "DELETE", requestSlot.captured.method)
    }

    @Test
    fun `delete request has AWS4-HMAC-SHA256 Authorization header`() = runTest {
        val response = mockk<Response> {
            every { isSuccessful } returns true
            every { close() } just Runs
        }
        stubMockCall(response)

        repository.deleteImage("images/auth_check.jpg")

        val authHeader = requestSlot.captured.header("Authorization")
        assertNotNull(authHeader)
        assertTrue(
            "删除请求也应包含 AWS4 签名",
            authHeader!!.startsWith("AWS4-HMAC-SHA256")
        )
    }

    // ==================== extractKeyFromUrl 测试 ====================

    @Test
    fun `extractKeyFromUrl extracts key from valid R2 URL`() {
        val url = "$testPublicUrl/images/2024/photo.jpg"
        assertEquals("images/2024/photo.jpg", repository.extractKeyFromUrl(url))
    }

    @Test
    fun `extractKeyFromUrl returns null for non-R2 URL`() {
        assertNull("非 R2 URL 应返回 null", repository.extractKeyFromUrl("https://other-cdn.com/images/photo.jpg"))
    }

    @Test
    fun `extractKeyFromUrl returns null for empty string`() {
        assertNull(repository.extractKeyFromUrl(""))
    }

    @Test
    fun `extractKeyFromUrl for exact publicUrl without trailing slash returns full URL`() {
        // startsWith 匹配但 removePrefix("$publicUrl/") 不会去掉任何字符
        // 因为 publicUrl 不以 / 结尾，所以 removePrefix 找不到匹配
        val result = repository.extractKeyFromUrl(testPublicUrl)
        assertEquals(testPublicUrl, result)
    }

    @Test
    fun `extractKeyFromUrl extracts deeply nested path`() {
        val url = "$testPublicUrl/a/b/c/d/e.png"
        assertEquals("a/b/c/d/e.png", repository.extractKeyFromUrl(url))
    }

    // ==================== isR2Url 测试 ====================

    @Test
    fun `isR2Url returns true for valid R2 URL`() {
        assertTrue(repository.isR2Url("$testPublicUrl/images/photo.jpg"))
    }

    @Test
    fun `isR2Url returns false for non-R2 URL`() {
        assertFalse(repository.isR2Url("https://other-cdn.com/images/photo.jpg"))
    }

    @Test
    fun `isR2Url returns false for empty string`() {
        assertFalse(repository.isR2Url(""))
    }

    @Test
    fun `isR2Url returns true for exact publicUrl`() {
        assertTrue(repository.isR2Url(testPublicUrl))
    }

    @Test
    fun `isR2Url returns false for URL with extra prefix`() {
        assertFalse(repository.isR2Url("x$testPublicUrl/images/photo.jpg"))
    }

    // ==================== 辅助方法 ====================

    /**
     * 创建临时图片文件，自动清理由调用方负责。
     */
    private fun createTempImageFile(fileName: String, content: ByteArray): File {
        val tempFile = File(System.getProperty("java.io.tmpdir"), "r2_test_$fileName")
        tempFile.writeBytes(content)
        tempFile.deleteOnExit()
        return tempFile
    }

    /**
     * 通过反射获取 sun.misc.Unsafe 实例（不直接 import，避免 JDK 9+ 模块限制）。
     */
    @Suppress("PrivateApi")
    private fun getUnsafe(): Any {
        val unsafeClass = Class.forName("sun.misc.Unsafe")
        val field = unsafeClass.getDeclaredField("theUnsafe")
        field.isAccessible = true
        return field.get(null)!!
    }

    /**
     * 使用 Unsafe 设置实例字段（绕过 private final 限制）。
     */
    private fun setInstanceField(obj: Any, field: Field, value: Any?) {
        val unsafe = getUnsafe()
        val unsafeClass = unsafe.javaClass
        val offset = (unsafeClass.getMethod("objectFieldOffset", Field::class.java)
            .invoke(unsafe, field) as Number).toLong()
        unsafeClass.getMethod("putObject", Any::class.java, Long::class.javaPrimitiveType, Any::class.java)
            .invoke(unsafe, obj, offset, value)
    }

    /**
     * 通过 Unsafe 将 mock OkHttpClient 注入到 repository 的 private final client 字段。
     */
    private fun injectMockClient(repo: R2ImageRepository, client: OkHttpClient) {
        val clientField = R2ImageRepository::class.java.getDeclaredField("client")
        setInstanceField(repo, clientField, client)
    }

    /**
     * 通过 Unsafe 设置实例字段的值（绕过 private final 限制）。
     */
    private fun setInstanceField(obj: Any, fieldName: String, value: String) {
        val field = obj.javaClass.getDeclaredField(fieldName)
        setInstanceField(obj, field, value)
    }
}
