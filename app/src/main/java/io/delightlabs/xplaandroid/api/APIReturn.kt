package io.delightlabs.xplaandroid.api

import com.google.gson.annotations.SerializedName

class APIReturn {

    // MARK: - TotalReturn
    data class TotalReturn (
        var supply: List<Balance>,
        var pagination: Pagination
    )
    data class BalanceReturn (
        var balances: List<Balance>,
        var pagination: Pagination
    )
    data class Balance (
        var denom: String,
        var amount: String,
    )

    data class Pagination (
        @SerializedName("next_key")
        var nextKey : String?,
        var total: String,
    )

    data class AccountReturn(
        var account: Account
    )

    data class Account(
        @SerializedName("@type")
        var type: String,
        @SerializedName("base_account")
        var baseAccount: BaseAccount,
        @SerializedName("code_hash")
        var codeHash: String
    )

    data class BaseAccount(
        var address: String,
        @SerializedName("pub_key")
        var pubKey: PubKey,
        @SerializedName("account_number")
        var accountNumber: String,
        var sequence: String
    ) {
        fun getSequenceNumber(): String {
            return sequence
        }

        fun getPublicKey(): String {
            return pubKey.key
        }
    }

    data class PubKey(
        @SerializedName("@type")
        var type: String,
        var key: String
    )


    data class SmartQuery(
        var data: DataClass
    )
    data class DataClass(
        var pairs: List<Pair>
    )

    data class Pair(
        @SerializedName("asset_infos")
        var assetInfos: List<AssetInfo>,
        @SerializedName("contract_addr")
        var contractAddr: String,
        @SerializedName("liquidity_token")
        var liquidityToken: String,
        @SerializedName("asset_decimals")
        var assetDecimasl: List<Int>,
    )

    data class AssetInfo(
        var token: Token?,
        @SerializedName("native_token")
        var navtiveToken: NativeToken?
    )

    data class NativeToken(
        var denom: String
    )
    data class Token(
        @SerializedName("contract_addr")
        var contractAddr: String
    )

    data class BroadcastResponse(
        @SerializedName("tx_response")
        val txResponse: TxResponse
    )

    data class TxResponse(
        val code: Int,
        val codespace: String,
        val data: String,
        val events: List<EventClass>,
        @SerializedName("gas_used")
        val gasUsed: String,
        @SerializedName("gas_wanted")
        val gasWanted: String,
        val height: String,
        val info: String,
        val logs: List<LogClass>,
        @SerializedName("raw_log")
        val rawLog: String,
        val timestamp: String,
        val tx: Transaction?,
        val txhash: String,
    )

    data class EventClass(
        val type: String,
        val attributes: List<AttributeClass>
    )

    data class AttributeClass(
        val key: String,
        val value: String,
        val index: Boolean?
    )

    data class LogClass(
        val events: List<EventClass>,
        val log: String,
        var msgIndex: Int
    )

    data class Transaction(
        val typeURL: String,
        val value: String
    )

    data class SimulateTx(
        val gasInfo: GasInfo,
        val result: ResultClass
    )

    data class GasInfo(
        @SerializedName("gas_wanted")
        val gasWanted: String,
        @SerializedName("gas_used")
        val gasUsed: String
    )

    data class ResultClass(
        val data: String,
        val log: String,
        val events: List<EventClass>
    )

}