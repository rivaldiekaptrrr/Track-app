package com.trackit.app.util

import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataWithBytes
import platform.Foundation.stringByAppendingPathComponent
import platform.Foundation.writeToFile
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.posix.memcpy
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned

actual class FileExporter {
    @OptIn(ExperimentalForeignApi::class)
    actual fun exportCsv(fileName: String, content: String, onComplete: (Boolean) -> Unit) {
        try {
            val tempDir = NSTemporaryDirectory()
            val filePath = (tempDir as NSString).stringByAppendingPathComponent(fileName)
            val nsString = NSString.create(string = content)
            val success = nsString.writeToFile(filePath, atomically = true, encoding = NSUTF8StringEncoding, error = null)
            if (success) {
                val fileUrl = NSURL.fileURLWithPath(filePath)
                presentShareSheet(fileUrl)
                onComplete(true)
            } else {
                onComplete(false)
            }
        } catch (e: Exception) {
            onComplete(false)
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    actual fun exportPdf(fileName: String, bytes: ByteArray, onComplete: (Boolean) -> Unit) {
        try {
            val tempDir = NSTemporaryDirectory()
            val filePath = (tempDir as NSString).stringByAppendingPathComponent(fileName)
            val nsData = bytes.usePinned { pinned ->
                NSData.dataWithBytes(pinned.addressOf(0), bytes.size.toULong())
            }
            val success = nsData.writeToFile(filePath, atomically = true)
            if (success) {
                val fileUrl = NSURL.fileURLWithPath(filePath)
                presentShareSheet(fileUrl)
                onComplete(true)
            } else {
                onComplete(false)
            }
        } catch (e: Exception) {
            onComplete(false)
        }
    }

    private fun presentShareSheet(fileUrl: NSURL) {
        val activityViewController = UIActivityViewController(
            activityItems = listOf(fileUrl),
            applicationActivities = null
        )
        val rootViewController = UIApplication.sharedApplication.keyWindow?.rootViewController
        rootViewController?.presentViewController(activityViewController, animated = true, completion = null)
    }
}
