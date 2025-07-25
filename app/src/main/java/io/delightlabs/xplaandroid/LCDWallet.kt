package io.delightlabs.xplaandroid

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.protobuf.Descriptors.FileDescriptor
import com.google.protobuf.TypeRegistry
import com.google.protobuf.any
import com.google.protobuf.kotlin.toByteString
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
import io.delightlabs.xplaandroid.core.StdSignDoc
import wallet.core.jni.Curve
import wallet.core.jni.HDWallet
import wallet.core.jni.Hash.keccak256
import wallet.core.jni.PrivateKey
import wallet.core.jni.PublicKey
import xpla.tx.Tx.CreateTxOptions

object GsonSingleton {
    val gson: Gson by lazy {
        GsonBuilder()
            .disableHtmlEscaping()
            .create()
    }
}

object TypeRegistrySingleton {
    private const val EXCLUDE_SUFFIX = "Response"

    val typeRegistry: TypeRegistry by lazy {
        TypeRegistry.newBuilder().apply {
            listOf(
                cosmos.auth.v1beta1.Tx.getDescriptor(),
                cosmos.bank.v1beta1.Tx.getDescriptor(),
                cosmos.crisis.v1beta1.Tx.getDescriptor(),
                cosmos.gov.v1beta1.Tx.getDescriptor(),
                cosmos.gov.v1.Tx.getDescriptor(),
                cosmos.mint.v1beta1.Tx.getDescriptor(),
                cosmos.slashing.v1beta1.Tx.getDescriptor(),
                cosmos.distribution.v1beta1.Tx.getDescriptor(),
                cosmos.staking.v1beta1.Tx.getDescriptor(),
                cosmos.upgrade.v1beta1.Tx.getDescriptor(),
                cosmos.evidence.v1beta1.Tx.getDescriptor(),
                cosmos.feegrant.v1beta1.Tx.getDescriptor(),
                cosmos.authz.v1beta1.Tx.getDescriptor(),
                cosmwasm.wasm.v1.Tx.getDescriptor(),
                ethermint.evm.v1.Tx.getDescriptor(),
                ethermint.feemarket.v1.Tx.getDescriptor(),
                xpla.reward.v1beta1.Tx.getDescriptor(),
                xpla.volunteer.v1beta1.Tx.getDescriptor(),
                xpla.offchain.auth.Msg.getDescriptor(),
                ibc.core.client.v1.Tx.getDescriptor(),
                ibc.core.channel.v1.Tx.getDescriptor(),
                ibc.core.connection.v1.Tx.getDescriptor(),
                ibc.applications.fee.v1.Tx.getDescriptor(),
                ibc.applications.transfer.v1.Tx.getDescriptor(),
                ibc.applications.interchain_accounts.host.v1.Tx.getDescriptor(),
                ibc.applications.interchain_accounts.controller.v1.Tx.getDescriptor(),
                ibc.lightclients.wasm.v1.Tx.getDescriptor(),
            ).forEach { descriptor ->
                registerMsgTypes(descriptor, this)
            }
        }.build()
    }

    private fun registerMsgTypes(descriptor: FileDescriptor, builder: TypeRegistry.Builder) {
        descriptor.messageTypes.filterNot { it.fullName.endsWith(EXCLUDE_SUFFIX) }
            .forEach { builder.add(it) }
    }
}

data class SignOptions(
    val accountNumber: Int?,
    val sequence: Long?,
    val chainId: String,
    val signMode: Signing.SignMode
)

val derivationPath = "m/44\'/60\'/0\'/0/0"
@OptIn(ExperimentalStdlibApi::class)
class LCDWallet(lcdClient: LCDClient, privateKey: PrivateKey, mnemonic: String) {
    val lcdClient: LCDClient
    val privateKey: PrivateKey
    val publicKey: PublicKey
    val address: String
    val mnemonic: String
    constructor(lcdClient: LCDClient, strength: Int, passphrase: String)
            : this(lcdClient,
                    HDWallet(strength, passphrase)) {
    }

    constructor(lcdClient: LCDClient, hdWallet: HDWallet)
            : this(lcdClient,
        hdWallet.getKeyByCurve(Curve.SECP256K1, derivationPath),
        hdWallet.mnemonic()){
    }

    constructor(lcdClient: LCDClient, mnemonic: String, passphrase: String)
            : this(lcdClient,
        HDWallet(mnemonic, passphrase).getKeyByCurve(Curve.SECP256K1, derivationPath),
        mnemonic) {
    }

    constructor(lcdClient: LCDClient, privateKey: PrivateKey)
            : this(lcdClient, privateKey, "") {
    }

    init {
        val publicKeyData = privateKey.getPublicKeySecp256k1(false)
        val hex = publicKeyData.data().toHexString(1)
        val x1 = keccak256(hex.hexToByteArray()).toHexString()
        val x2 = x1.slice(24..<x1.length)
        val xplaAddress = SegwitAddrCoder().encode2("xpla", x2.hexToByteArray())

        this.privateKey = privateKey
        this.publicKey = privateKey.getPublicKeySecp256k1(true)
        this.address = xplaAddress
        this.mnemonic = mnemonic
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
        accountNumber: Int? = null,
        signMode: Signing.SignMode = Signing.SignMode.SIGN_MODE_DIRECT
    ): Tx {
        var accountNumber = accountNumber
        var sequence = options.sequence

        if (accountNumber == null) {
            accountNumAndSequence()?.let {
                accountNumber = it.baseAccount.accountNumber.toInt()
                sequence = it.baseAccount.sequence.toLong()
            }
        }

        val tx = createTx(options)
        accountNumber?.let {
            sequence.let {
                val signOptions = SignOptions(accountNumber, sequence, lcdClient.network.chainId, signMode)
                val authInfo = createAuthInfo(it.toLong(), tx.authInfo.fee, signOptions)
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

    public fun createAuthInfo(sequence: Long, fee: Fee, signOptions: SignOptions): AuthInfo {
        return authInfo {
            signerInfos.add(
                signerInfo {
                    this.sequence = sequence
                    this.publicKey = publicKey.getAsGoogleProto()
                    this.modeInfo = modeInfo {
                        this.single = TxOuterClass.ModeInfo.Single.newBuilder()
                            .setMode(signOptions.signMode)
                            .build()
                    }
                }
            )
            this.fee = fee
        }
    }

    public fun getSignature(
        tx: Tx,
        authInfo: AuthInfo,
        options: SignOptions
    ): ByteArray? {
        val signDoc = signDoc {
            chainId = options.chainId
            accountNumber = options.accountNumber!!.toLong()
            bodyBytes = tx.body.toByteString()
            authInfoBytes = authInfo.toByteString()
        }

        val signDocSerialized: ByteArray
        if(options.signMode == Signing.SignMode.SIGN_MODE_LEGACY_AMINO_JSON) {
            val stdSignDoc = StdSignDoc(signDoc)
            signDocSerialized = GsonSingleton.gson.toJson(stdSignDoc).toByteArray()
        } else {
            signDocSerialized = signDoc.toByteArray()
        }

        privateKey.sign(keccak256(signDocSerialized), Curve.SECP256K1)?.let {
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
