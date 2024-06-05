package io.delightlabs.xplaandroid

import com.google.protobuf.any
import com.google.protobuf.kotlin.toByteString
import cosmos.base.v1beta1.CoinOuterClass.Coin
import cosmos.tx.signing.v1beta1.Signing
import cosmos.tx.v1beta1.TxOuterClass
import cosmos.tx.v1beta1.TxOuterClass.AuthInfo
import cosmos.tx.v1beta1.TxOuterClass.Fee
import cosmos.tx.v1beta1.TxOuterClass.Tx
import cosmos.tx.v1beta1.authInfo
import cosmos.tx.v1beta1.modeInfo
import cosmos.tx.v1beta1.signDoc
import cosmos.tx.v1beta1.signerInfo
import io.delightlabs.xplaandroid.api.APIReturn
import io.delightlabs.xplaandroid.api.SignerOptions
import io.delightlabs.xplaandroid.core.SegwitAddrCoder
import wallet.core.jni.Curve
import wallet.core.jni.HDWallet
import wallet.core.jni.Hash.keccak256
import wallet.core.jni.PrivateKey
import wallet.core.jni.PublicKey


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
    val privateKey: PrivateKey
    val publicKey: PublicKey
    val address: String
    val mnemonic: String

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

        this.privateKey = privateKey
        this.publicKey = privateKey.getPublicKeySecp256k1(true)
        this.address = xplaAddress
        this.mnemonic = hdWallet.mnemonic()
        this.lcdClient = lcdClient
    }

    fun accountNumAndSequence(): APIReturn.Account? {
        return lcdClient.authAPI.accountInfo(this.address)
    }

    fun accountNumber(): Int? {
        val response = lcdClient.authAPI.accountInfo(this.address)
        return response?.baseAccount?.accountNumber?.toIntOrNull()
    }

    fun sequence(): Int? {
        val response = lcdClient.authAPI.accountInfo(this.address)
        return response?.baseAccount?.sequence?.toIntOrNull()
    }

    fun createTx(options: CreateTxOptions): TxOuterClass.Tx {
        val tx = lcdClient.txAPI.create(
            listOf(
                SignerOptions(
                    address = address,
                    sequenceNumber = options.sequence,
                    publicKey = publicKey.data().toHexString()
                )
            ),
            options = options
        )
        return tx
    }

    fun createAndSignTx(
        options: CreateTxOptions,
        accountNumber: Int? = null
    ): Tx {
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
                        signatures.add(it.toByteString())
                        this.authInfo = authInfo
                        body = tx.body
                    }
                }
            }
        }
        return cosmos.tx.v1beta1.tx { }
    }

    private fun createAuthInfo(sequence: Long, fee: Fee): AuthInfo {
        return authInfo {
            signerInfos.add(
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
            this.fee = fee
        }
    }

    private fun getSignature(
        tx: Tx,
        authInfo: AuthInfo,
        options: SignOptions
    ): ByteArray? {
        val signDoc = signDoc {
            chainId = options.chainInt
            accountNumber = options.accountNumber!!.toLong()
            bodyBytes = tx.body.toByteString()
            authInfoBytes = authInfo.toByteString()
        }

        privateKey.sign(keccak256(signDoc.toByteArray()), Curve.SECP256K1)?.let {
            return it
        }
        return null
    }

    fun com.google.protobuf.Any.getAsGoogleProto(): com.google.protobuf.Any {
        return any {
            val hex = "0a21"
            value = "${hex}${publicKey.data().toHexString()}".hexToByteArray().toByteString()
            typeUrl = PubkeyProtoType
        }
    }
}