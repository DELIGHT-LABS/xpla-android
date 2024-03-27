package io.delightlabs.xplaandroid

import cosmos.base.v1beta1.CoinOuterClass.Coin
import cosmos.base.v1beta1.coin
import io.delightlabs.xplaandroid.api.AuthAPI
import io.delightlabs.xplaandroid.api.BankAPI
import io.delightlabs.xplaandroid.api.APIRequester
import io.delightlabs.xplaandroid.api.TxAPI
import io.delightlabs.xplaandroid.api.WasmAPI
import io.delightlabs.xplaandroid.api.XplaNetwork

const val PubkeyProtoType: String = "/ethermint.crypto.v1.ethsecp256k1.PubKey"
class LCDClient(
    val network: XplaNetwork,
    val gasPrices: List<Coin>,
    val gasAdjustment: String,
    val isClass: Boolean = false
) {

    val apiRequester: APIRequester = APIRequester(network)
    val authAPI: AuthAPI = AuthAPI(apiRequester)
    val bankAPI: BankAPI = BankAPI(apiRequester)
    val wasmAPI: WasmAPI = WasmAPI(apiRequester)
    private var _txAPI: TxAPI? = null

    val txAPI: TxAPI
        get() {
            if (_txAPI == null) {
                val lcdClient = LCDClient(
                    network = XplaNetwork.TestNet,
                    gasPrices = arrayListOf(
                        coin {
                            this.amount = "850000000000"
                            this.denom = "axpla"
                        }
                    ),
                    gasAdjustment = "3"
                )
                _txAPI = TxAPI(lcdClient)
            }
            return _txAPI!!
        }

    fun wallet(strength: Int, passphrase: String): LCDWallet {
        System.loadLibrary("TrustWalletCore")
        return LCDWallet(this, strength, passphrase)
    }

    fun wallet(mnemonic: String, passphrase: String = ""): LCDWallet {
        System.loadLibrary("TrustWalletCore")
        return LCDWallet(this, mnemonic, passphrase)
    }
}