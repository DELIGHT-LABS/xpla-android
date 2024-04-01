package io.delightlabs.xplaandroid

import android.util.Log
import com.google.protobuf.ByteString
import com.google.protobuf.UInt64Value
import com.google.protobuf.any
import com.google.protobuf.kotlin.toByteString
import com.google.protobuf.kotlin.toByteStringUtf8
import cosmos.base.v1beta1.CoinOuterClass.Coin
import cosmos.tx.signing.v1beta1.SignatureDescriptorKt.data
import cosmos.tx.signing.v1beta1.Signing
import cosmos.tx.v1beta1.TxOuterClass
import cosmos.tx.v1beta1.TxOuterClass.AuthInfo
import cosmos.tx.v1beta1.TxOuterClass.Fee
import cosmos.tx.v1beta1.authInfo
import cosmos.tx.v1beta1.authInfoOrNull
import cosmos.tx.v1beta1.modeInfo
import cosmos.tx.v1beta1.signDoc
import cosmos.tx.v1beta1.signDocDirectAux
import cosmos.tx.v1beta1.signerInfo
import io.delightlabs.xplaandroid.api.APIReturn
import io.delightlabs.xplaandroid.api.TxAPI
import io.delightlabs.xplaandroid.core.Bech32
import io.delightlabs.xplaandroid.core.SegwitAddrCoder
import org.checkerframework.checker.units.qual.g
import wallet.core.jni.Curve
import wallet.core.jni.HDWallet
import wallet.core.jni.Hash.keccak256
import wallet.core.jni.PrivateKey
import wallet.core.jni.PublicKey
import wallet.core.jni.proto.NEAR


data class SignOptions(
    val accountNumber: Int?,
    val sequence: Int?,
    val chainInt: String
)

data class CreateTxOptions(
    val msgs: List<com.google.protobuf.Any>,
    var fee: Fee? = null,
    var memo: String? = null,
    var gas: String? = null,
    var gasPrices: List<Coin>? = null,
    var gasAdjustment: String? = null,
    var feeDenoms: List<String>? = null,
    var timeoutHeight: Int? = null,
    var sequence: Int? = null
)

@OptIn(ExperimentalStdlibApi::class)
class LCDWallet(lcdClient: LCDClient, hdWallet: HDWallet) {
    private val lcdClient: LCDClient
    public val privateKey: PrivateKey
    public val publicKey: PublicKey
    public val address: String
    public val mnemonic: String

    constructor(lcdClient: LCDClient, strength: Int, passphrase: String)
            : this(lcdClient, HDWallet(strength, passphrase)) {
    }

    constructor(lcdClient: LCDClient, mnemonic: String, passphrase: String)
            : this(lcdClient, HDWallet(mnemonic, passphrase)) {
    }

    init {
        val privateKey = hdWallet.getKeyByCurve(Curve.SECP256K1, "m/44\'/60\'/0\'/0/0")
        val publicKeyData = privateKey.getPublicKeySecp256k1(false)
        val hex = publicKeyData.data().toHexString(1)
        val x1 = keccak256(hex.hexToByteArray()).toHexString()
        val x2 = x1.slice(24..<x1.length)
        val xplaAddress = SegwitAddrCoder().encode2("xpla", x2.hexToByteArray())
        println("xplaAddress \uD83E\uDD28: $xplaAddress")

        this.privateKey = privateKey
        this.publicKey = privateKey.getPublicKeySecp256k1(true)
        this.address = xplaAddress
        this.mnemonic = hdWallet.mnemonic()
        this.lcdClient = lcdClient
    }

    fun accountNumAndSequence(): APIReturn.Account? {
        println("account \uD83E\uDD28: ${lcdClient.authAPI.accountInfo(this.address)}")
        return lcdClient.authAPI.accountInfo(this.address)
    }

    fun accountNumber(): Int? {
        val response = lcdClient.authAPI.accountInfo(this.address)
        println("accountNumber \uD83E\uDD28: $response")
        return response?.baseAccount?.accountNumber?.toIntOrNull()
    }

    fun sequence(): Int? {
        val response = lcdClient.authAPI.accountInfo(this.address)
        println("sequence \uD83E\uDD28: $response")
        return response?.baseAccount?.sequence?.toIntOrNull()
    }

    public fun createTx(options: CreateTxOptions): TxOuterClass.Tx {
        val tx = lcdClient.txAPI.create(
            listOf(
                TxAPI.SignerOptions(
                    address = address,
                    sequenceNumber = options.sequence,
                    publicKey = publicKey.data().toHexString()
                )
            ),
            options = options
        )
        println("address \uD83E\uDD28: $address")
        println("sequenceNumber \uD83E\uDD28: ${options.sequence}")
        println("publicKey \uD83E\uDD28: ${publicKey.data().toHexString()}")

        println("optionMsgs \uD83E\uDD28: ${options.msgs[0].value}")
        println("txResult \uD83E\uDD28: $tx")
        return tx
    }
    public fun createAndSignTx(
        options: CreateTxOptions,
        accountNumber: Int? = null
    ): cosmos.tx.v1beta1.TxOuterClass.Tx {
        var accountNumber = accountNumber
        var sequence = options.sequence

        if (accountNumber == null || sequence == null) {
            accountNumAndSequence()?.let {
                accountNumber = it.baseAccount.accountNumber.toInt()
                sequence = it.baseAccount.sequence.toInt()
            }
        }

        val tx = createTx(options)
        accountNumber?.let {
            sequence?.let {
                val signOptions = SignOptions(accountNumber, sequence, lcdClient.network.chainId)
                val authInfo = createAuthInfo(it.toLong(), tx.authInfo.fee)
                getSignature(tx, authInfo, signOptions)?.let {
                   return cosmos.tx.v1beta1.tx {
                       this.signatures.add(it.toByteString())
                       this.authInfo = authInfo
                       this.body = tx.body
                   }
                }
            }
        }
        return cosmos.tx.v1beta1.tx {  }
    }

    private fun createAuthInfo(sequence: Long, fee: Fee): TxOuterClass.AuthInfo {
        return authInfo {
            this.signerInfos.add(
                signerInfo {
                    this.sequence = sequence
                    this.publicKey = publicKey.getAsGoogleProto()
                    this.modeInfo = modeInfo {
                        this.single = TxOuterClass.ModeInfo.Single.newBuilder()
                            .setMode(Signing.SignMode.SIGN_MODE_DIRECT)
                            .build()
                    }
                }
            )
        }
    }

    private fun getSignature(tx: TxOuterClass.Tx, authInfo: AuthInfo, options: SignOptions): ByteArray? {
        val signDoc = signDoc {
            this.chainId = options.chainInt
            this.accountNumber = options.accountNumber!!.toLong()
            this.bodyBytes = tx.body.toByteString()
            this.authInfoBytes = tx.authInfo.toByteString()
        }

        privateKey.sign(keccak256(signDoc.toByteArray()), Curve.SECP256K1)?.let {
            val sig = it.dropLast(1).map { it.toUInt() }.toTypedArray()
            val byteArray = ByteArray(sig.size * 4) // Each UInt occupies 4 bytes

            sig.forEachIndexed { index, uintValue ->
                val byteIndex = index * 4
                byteArray[byteIndex] = (uintValue and 0xFF000000U).toInt().ushr(24).toByte()
                byteArray[byteIndex + 1] = (uintValue and 0x00FF0000U).toInt().ushr(16).toByte()
                byteArray[byteIndex + 2] = (uintValue and 0x0000FF00U).toInt().ushr(8).toByte()
                byteArray[byteIndex + 3] = (uintValue and 0x000000FFU).toInt().toByte()
            }
            return byteArray

        }
        return null
    }

    fun com.google.protobuf.Any.getAsGoogleProto(): com.google.protobuf.Any {
        return any {
            val hex = "0a21"
            this.value = "0a21${publicKey.data().toHexString()}".toByteStringUtf8()
            this.typeUrl = pubkeyProtoType
        }
    }

}