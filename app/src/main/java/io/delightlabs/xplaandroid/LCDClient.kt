package io.delightlabs.xplaandroid

import WasmGRPC
import cosmos.base.v1beta1.CoinOuterClass.Coin
import cosmos.base.v1beta1.coin
import io.delightlabs.xplaandroid.api.APIRequester
import io.delightlabs.xplaandroid.api.AuthAPI
import io.delightlabs.xplaandroid.api.BankAPI
import io.delightlabs.xplaandroid.api.TxAPI
import io.delightlabs.xplaandroid.api.WasmAPI
import io.delightlabs.xplaandroid.api.XplaNetwork
import wallet.core.jni.PrivateKey

const val PubkeyProtoType: String = "/ethermint.crypto.v1.ethsecp256k1.PubKey"

class LCDClient(
    var network: XplaNetwork,
    var gasPrices: List<Coin>,
    var gasAdjustment: String,
    var isClass: Boolean = false
) {
    var apiRequester: APIRequester = APIRequester(network)
    var grpcClient:GRPCClient = GRPCClient(network)
    var authAPI: AuthAPI = AuthAPI(apiRequester)
    var bankAPI: BankAPI = BankAPI(apiRequester)
    var wasmAPI: WasmAPI = WasmAPI(apiRequester)
    var wasmGRPC: WasmGRPC = WasmGRPC(grpcClient.channel)
    var txAPI: TxAPI = TxAPI(this)
    fun wallet(strength: Int, passphrase: String): LCDWallet {
        System.loadLibrary("TrustWalletCore")
        return LCDWallet(this, strength, passphrase)
    }

    fun wallet(mnemonic: String, passphrase: String = ""): LCDWallet {
        System.loadLibrary("TrustWalletCore")
        return LCDWallet(this, mnemonic, passphrase)
    }

    fun wallet(privateKey: PrivateKey): LCDWallet {
        System.loadLibrary("TrustWalletCore")
        return LCDWallet(this, privateKey)
    }
}
