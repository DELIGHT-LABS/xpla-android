package io.delightlabs.xplaandroid

import io.delightlabs.xplaandroid.api.AuthAPI
import io.delightlabs.xplaandroid.api.BankAPI
import io.delightlabs.xplaandroid.api.RetrofitConnection
import io.delightlabs.xplaandroid.api.WasmAPI
import io.delightlabs.xplaandroid.api.XplaNetwork

class LCDClient(
    val network: XplaNetwork,
    val gasPrices: IntArray,
    val gasAdjustment: String,
    val isClass: Boolean = false
) {

    val apiRequester: RetrofitConnection = RetrofitConnection(network)
    val authAPI: AuthAPI = AuthAPI(apiRequester)
    val bankAPI: BankAPI = BankAPI(apiRequester)
    val wasmAPI: WasmAPI = WasmAPI(apiRequester)

    fun wallet(mnemonic: String, passphrase: String = "") : LCDWallet {
        System.loadLibrary("TrustWalletCore")
        return LCDWallet(this, mnemonic, passphrase)
    }

}