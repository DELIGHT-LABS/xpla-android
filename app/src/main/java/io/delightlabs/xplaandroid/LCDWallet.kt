package io.delightlabs.xplaandroid

import android.util.Log
import io.delightlabs.xplaandroid.api.APIReturn
import io.delightlabs.xplaandroid.core.Bech32
import io.delightlabs.xplaandroid.core.SegwitAddrCoder
import wallet.core.jni.Curve
import wallet.core.jni.HDWallet
import wallet.core.jni.Hash.keccak256
import wallet.core.jni.PrivateKey
import wallet.core.jni.PublicKey

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

        this.privateKey = privateKey
        this.publicKey = privateKey.getPublicKeySecp256k1(true)
        this.address = xplaAddress
        this.mnemonic = hdWallet.mnemonic()
        this.lcdClient = lcdClient
    }

    fun accountNumAndSequence(): APIReturn.Account?{
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

    public fun createTx(){

    }

}