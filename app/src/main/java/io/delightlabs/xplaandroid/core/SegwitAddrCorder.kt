package io.delightlabs.xplaandroid.core

import java.lang.Exception

/// Segregated Witness Address encoder/decoder
class SegwitAddrCoder {
    private val bech32 = Bech32()
    /// Convert from one power-of-2 number base to another
    @Throws(Exception::class)
    private fun convertBits(from: Int, to: Int, pad: Boolean, idata: ByteArray): ByteArray {
        var acc = 0
        var bits = 0
        val maxv = (1 shl to) - 1
        val maxAcc = (1 shl (from + to - 1)) - 1
        val odata = mutableListOf<Byte>()
        for (ibyte in idata) {
            acc = ((acc shl from) or (ibyte.toInt() and 0xFF)) and maxAcc
            bits += from
            while (bits >= to) {
                bits -= to
                odata.add(((acc shr bits) and maxv).toByte())
            }
        }
        if (pad) {
            if (bits != 0) {
                odata.add(((acc shl (to - bits)) and maxv).toByte())
            }
        } else if (bits >= from || ((acc shl (to - bits)) and maxv) != 0) {
            throw CoderError.BitsConversionFailed()
        }
        return odata.toByteArray()
    }
    /// Decode segwit address
    @Throws(Exception::class)
    fun decode(hrp: String, addr: String): Pair<Int, ByteArray> {
        val dec = bech32.decode(addr)
        if (dec.first != hrp) {
            throw CoderError.HrpMismatch(dec.first)
        }
        if (dec.second.isEmpty()) {
            throw CoderError.ChecksumSizeTooLow()
        }
        val conv = convertBits(5, 8, false, dec.second.copyOfRange(1, dec.second.size))
        if (conv.size < 2 || conv.size > 40) {
            throw CoderError.DataSizeMismatch(conv.size)
        }
        if (dec.second[0] > 16) {
            throw CoderError.SegwitVersionNotSupported(dec.second[0])
        }
        if (dec.second[0] == 0.toByte() && (conv.size != 20 && conv.size != 32)) {
            throw CoderError.SegwitV0ProgramSizeMismatch(conv.size)
        }
        return Pair(dec.second[0].toInt(), conv)
    }
    /// Encode segwit address
    @Throws(Exception::class)
    fun encode(hrp: String, version: Int, program: ByteArray): String {
        val enc = ByteArray(1) { version.toByte() } + convertBits(8, 5, true, program)
        val result = bech32.encode(hrp, enc)
        try {
            decode(hrp, result)
        } catch (e: Exception) {
            throw CoderError.BitsConversionFailed()
        }
        return result
    }
    fun encode2(hrp: String, program: ByteArray): String {
        val enc = convertBits(8, 5, true, program)
        return bech32.encode(hrp, enc)
    }

    sealed class CoderError(message: String? = null) : Exception(message) {
        class BitsConversionFailed
            : CoderError("Failed to perform bits conversion")
        class HrpMismatch(hrp: String)
            : CoderError( "Human-readable-part does not match requested: '$hrp'")
        class ChecksumSizeTooLow
            : CoderError("Checksum size is too low")
        class DataSizeMismatch(size: Int)
            : CoderError("Program size does not meet required range 2...40: '$size'")
        class SegwitVersionNotSupported(version: Byte)
            : CoderError("Segwit version is not supported by this decoder: '$version'")
        class SegwitV0ProgramSizeMismatch(size: Int)
            : CoderError("Segwit program size does not meet version 0 requirements: '$size'")
        class EncodingCheckFailed
            : CoderError("Failed to check result after encoding")
    }


}