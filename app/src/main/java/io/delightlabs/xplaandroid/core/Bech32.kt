package io.delightlabs.xplaandroid.core

import java.nio.charset.StandardCharsets
import java.lang.Exception
import java.util.Locale

class Bech32 {
    private val gen: IntArray = intArrayOf(0x3b6a57b2, 0x26508e6d, 0x1ea119fa, 0x3d4233dd, 0x2a1462b3)
    private val checksumMarker: String = "1"
    private val encCharset: ByteArray = "qpzry9x8gf2tvdw0s3jn54khce6mua7l".toByteArray(StandardCharsets.UTF_8)
    private val decCharset: ByteArray = byteArrayOf(
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
        15, -1, 10, 17, 21, 20, 26, 30, 7, 5, -1, -1, -1, -1, -1, -1,
        -1, 29, -1, 24, 13, 25, 9, 8, 23, -1, 18, 22, 31, 27, 19, -1,
        1, 0, 3, 16, 11, 28, 12, 14, 6, 4, 2, -1, -1, -1, -1, -1,
        -1, 29, -1, 24, 13, 25, 9, 8, 23, -1, 18, 22, 31, 27, 19, -1,
        1, 0, 3, 16, 11, 28, 12, 14, 6, 4, 2, -1, -1, -1, -1, -1
    )

    private fun polymod(values: ByteArray): Int {
        var chk: Int = 1
        for (v in values) {
            val top = (chk shr 25)
            chk = (chk and 0x1ffffff) shl 5 xor v.toInt()
            for (i: Int in 0 until 5) {
                chk = chk xor if (((top shr i) and 1) == 0) 0 else gen[i]
            }
        }
        return chk
    }

    private fun expandHrp(hrp: String): ByteArray {
        val hrpBytes = hrp.toByteArray(StandardCharsets.UTF_8)
        val result = ByteArray(hrpBytes.size * 2 + 1)
        for ((i, c) in hrpBytes.withIndex()) {
            result[i] = (c.toInt() shr 5).toByte()
            result[i + hrpBytes.size + 1] = (c.toInt() and 0x1f).toByte()
        }
        result[hrp.length] = 0
        return result
    }

    private fun verifyChecksum(hrp: String, checksum: ByteArray): Boolean {
        val data = expandHrp(hrp) + checksum
        return polymod(data) == 1
    }

    private fun createChecksum(hrp: String, values: ByteArray): ByteArray {
        val enc = expandHrp(hrp) + values + ByteArray(6)
        val mod: Int = polymod(enc) xor 1
        val ret: ByteArray = ByteArray(6)
        for (i in 0 until 6) {
            ret[i] = ((mod shr (5 * (5 - i))) and 31).toByte()
        }
        return ret
    }

    fun encode(hrp: String, values: ByteArray): String {
        val checksum = createChecksum(hrp, values)
        val combined = values + checksum
        val hrpBytes = hrp.toByteArray(StandardCharsets.UTF_8)
        var ret = hrpBytes + "1".toByteArray(StandardCharsets.UTF_8)
        for (i in combined) {
            ret += encCharset[i.toInt()]
        }
        return String(ret, StandardCharsets.UTF_8)
    }

    @Throws(DecodingError::class)
    fun decode(str: String): Pair<String, ByteArray> {
        val strBytes = str.toByteArray(StandardCharsets.UTF_8)
        require(strBytes.size <= 90) { throw DecodingError.StringLengthExceeded() }
        var lower = false
        var upper = false
        for (c in strBytes) {
            if (c < 33 || c > 126) {
                throw DecodingError.NonPrintableCharacter()
            }
            if (c in 97..122) {
                lower = true
            }
            if (c in 65..90) {
                upper = true
            }
        }
        require(!(lower && upper)) { throw DecodingError.InvalidCase() }
        val pos = str.indexOf(checksumMarker, 0, true)
        require(pos != -1) { throw DecodingError.NoChecksumMarker() }
        require(pos >= 1) { throw DecodingError.IncorrectHrpSize() }
        require(pos + 7 <= str.length) { throw DecodingError.IncorrectChecksumSize() }
        val vSize = str.length - 1 - pos
        val values = ByteArray(vSize)
        for (i in 0 until vSize) {
            val c = strBytes[i + pos + 1]
            val decInt = decCharset[c.toInt()]
            require(decInt.toInt() != -1) { throw DecodingError.InvalidCharacter() }
            values[i] = decInt.toByte()
        }
        val hrp = str.substring(0, pos).lowercase(Locale.getDefault())
        require(verifyChecksum(hrp, values)) { throw DecodingError.ChecksumMismatch() }
        return Pair(hrp, values.copyOfRange(0, vSize - 6))
    }

    sealed class DecodingError(message: String? = null) : Exception(message) {
        class NonUTF8String : DecodingError("Non-UTF8 string")
        class NonPrintableCharacter : DecodingError("Non-printable character")
        class InvalidCase : DecodingError("Invalid case")
        class NoChecksumMarker : DecodingError("No checksum marker")
        class IncorrectHrpSize : DecodingError("Incorrect HRP size")
        class IncorrectChecksumSize : DecodingError("Incorrect checksum size")
        class StringLengthExceeded : DecodingError("String length exceeded")
        class InvalidCharacter : DecodingError("Invalid character")
        class ChecksumMismatch : DecodingError("Checksum mismatch")
    }


}


