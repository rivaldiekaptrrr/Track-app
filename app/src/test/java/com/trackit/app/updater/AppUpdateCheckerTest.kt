package com.trackit.app.updater

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AppUpdateCheckerTest {

    private lateinit var mockOkHttpClient: OkHttpClient
    private lateinit var mockCall: Call
    private lateinit var updateChecker: AppUpdateChecker

    @Before
    fun setUp() {
        mockOkHttpClient = mockk()
        mockCall = mockk()
        updateChecker = AppUpdateChecker(mockOkHttpClient)
        every { mockOkHttpClient.newCall(any()) } returns mockCall
    }

    private fun mockResponse(jsonBody: String, code: Int = 200) {
        val response = Response.Builder()
            .request(Request.Builder().url("https://api.github.com/").build())
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message(if (code == 200) "OK" else "Error")
            .body(jsonBody.toResponseBody(null))
            .build()
        every { mockCall.execute() } returns response
    }

    @Test
    fun `checkForUpdate returns update available when remote version is higher`() = runTest {
        val json = """
            {
              "tag_name": "v1.2.0",
              "body": "Bug fixes",
              "assets": [
                {
                  "browser_download_url": "https://example.com/app.apk"
                }
              ]
            }
        """.trimIndent()
        mockResponse(json)

        val result = updateChecker.checkForUpdate("rivaldiekaptrrr", "Track-app", "1.1.0")

        assertTrue(result != null)
        assertTrue(result!!.isUpdateAvailable)
        assertEquals("v1.2.0", result.latestVersion)
        assertEquals("https://example.com/app.apk", result.downloadUrl)
    }

    @Test
    fun `checkForUpdate returns not available when remote version is same`() = runTest {
        val json = """
            {
              "tag_name": "v1.1.0",
              "body": "Bug fixes",
              "assets": [
                {
                  "browser_download_url": "https://example.com/app.apk"
                }
              ]
            }
        """.trimIndent()
        mockResponse(json)

        val result = updateChecker.checkForUpdate("rivaldiekaptrrr", "Track-app", "1.1.0")

        assertTrue(result != null)
        assertFalse(result!!.isUpdateAvailable)
    }

    @Test
    fun `checkForUpdate returns not available when remote version is lower`() = runTest {
        val json = """
            {
              "tag_name": "v1.0.0",
              "body": "Bug fixes",
              "assets": [
                {
                  "browser_download_url": "https://example.com/app.apk"
                }
              ]
            }
        """.trimIndent()
        mockResponse(json)

        val result = updateChecker.checkForUpdate("rivaldiekaptrrr", "Track-app", "1.1.0")

        assertTrue(result != null)
        assertFalse(result!!.isUpdateAvailable)
    }

    @Test
    fun `isVersionNewer correctly compares semantic versions`() {
        assertTrue(updateChecker.isVersionNewer("v1.2.0", "1.1.0"))
        assertTrue(updateChecker.isVersionNewer("2.0.0", "1.9.9"))
        assertTrue(updateChecker.isVersionNewer("v1.1.1", "1.1.0"))
        
        assertFalse(updateChecker.isVersionNewer("v1.1.0", "1.1.0"))
        assertFalse(updateChecker.isVersionNewer("1.0.9", "1.1.0"))
        assertFalse(updateChecker.isVersionNewer("v1", "1.0.0")) // Edge case
    }
}
