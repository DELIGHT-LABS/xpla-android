package io.delightlabs.xplaandroid.api

import com.google.gson.annotations.SerializedName

class APIReturn {

    // MARK: - TotalReturn
    data class TotalReturn (
        val supply: List<Balance>,
        val pagination: Pagination
    )
    data class BalanceReturn (
        val balances: List<Balance>,
        val pagination: Pagination
    )
    data class Balance (
        val denom: String,
        val amount: String,
    )

    data class Pagination (
        @SerializedName("next_key")
        val nextKey : String?,
        val total: String,
    )

    data class AccountReturn(
        val account: Account
    )

    data class Account(
        @SerializedName("@type")
        val type: String,
        @SerializedName("base_account")
        val baseAccount: BaseAccount,
        @SerializedName("code_hash")
        val codeHash: String
    )

    data class BaseAccount(
        val address: String,
        @SerializedName("pub_key")
        val pubKey: PubKey,
        @SerializedName("account_number")
        val accountNumber: String,
        val sequence: String
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
        val type: String,
        val key: String
    )
    data class SmartQuery(
        val data: DataClass
    )
    data class DataClass(
        val pairs: List<Pair>
    )

    data class Pair(
        @SerializedName("asset_infos")
        val assetInfos: List<AssetInfo>,
        @SerializedName("contract_addr")
        val contractAddr: String,
        @SerializedName("liquidity_token")
        val liquidityToken: String,
        @SerializedName("asset_decimals")
        val assetDecimasl: List<Int>,
    )

    data class AssetInfo(
        val token: Token?,
        @SerializedName("native_token")
        val navtiveToken: NativeToken?
    )

    data class NativeToken(
        val denom: String
    )
    data class Token(
        @SerializedName("contract_addr")
        val contractAddr: String
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
        val msgIndex: Int
    )

    data class Transaction(
        val typeURL: String,
        val value: String
    )

    data class SimulateTx(
        @SerializedName("gas_info")
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