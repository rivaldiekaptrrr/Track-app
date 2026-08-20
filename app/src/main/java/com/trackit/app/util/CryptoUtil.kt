package com.trackit.app.util

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object CryptoUtil {
    private const val ALGORITHM = "AES/CBC/PKCS5Padding"
    // 32-byte key for AES-256
    private const val KEY = "Tr@ck1tS3cur3B@ckupK3y2026!@#$%" 
    private const val IV_LENGTH = 16

    private fun getSecretKey(): SecretKeySpec {
        val keyBytes = KEY.toByteArray(Charsets.UTF_8).copyOf(32)
        return SecretKeySpec(keyBytes, "AES")
    }

    fun encryptFile(source: File, dest: File) {
        if (!source.exists() || source.length() == 0L) {
            dest.createNewFile()
            return
        }

        val cipher = Cipher.getInstance(ALGORITHM)
        val iv = ByteArray(IV_LENGTH)
        SecureRandom().nextBytes(iv)
        cipher.init(Cipher.ENCRYPT_MODE, getSecretKey(), IvParameterSpec(iv))

        FileInputStream(source).use { fis ->
            FileOutputStream(dest).use { fos ->
                fos.write(iv)
                CipherOutputStream(fos, cipher).use { cos ->
                    fis.copyTo(cos)
                }
            }
        }
    }

    fun decryptFile(source: File, dest: File) {
        if (!source.exists() || source.length() == 0L) {
            dest.createNewFile()
            return
        }
        FileInputStream(source).use { fis ->
            decryptStream(fis, dest)
        }
    }

    fun decryptStream(inputStream: java.io.InputStream, dest: File) {
        val iv = ByteArray(IV_LENGTH)
        val bytesRead = inputStream.read(iv)
        // If stream is empty (0 bytes)
        if (bytesRead <= 0) {
            dest.createNewFile()
            return
        }
        if (bytesRead != IV_LENGTH) {
            throw IllegalArgumentException("Invalid backup file: Missing IV")
        }
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), IvParameterSpec(iv))

        CipherInputStream(inputStream, cipher).use { cis ->
            FileOutputStream(dest).use { fos ->
                cis.copyTo(fos)
            }
        }
    }
}
