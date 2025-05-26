package io.delightlabs.xplaandroid.core.wasm.v1

import cosmwasm.wasm.v1.Tx
import io.delightlabs.xplaandroid.GsonSingleton
import io.delightlabs.xplaandroid.amino.AminoType
import io.delightlabs.xplaandroid.amino.ProtocolAminoMsg
import io.delightlabs.xplaandroid.core.Coin
import java.io.Serializable

data class MsgExecuteContract(
    var sender: String = "",
    var contract: String = "",
    var msg: Any,
    var funds: List<Coin> = listOf()
) : ProtocolAminoMsg, Serializable {

    constructor(tx: Tx.MsgExecuteContract): this(
        sender = tx.sender,
        contract = tx.contract,
        msg = parseExecuteMsg(tx.msg.toStringUtf8()),
        funds = tx.fundsList.map { Coin(it.amount, it.denom) }
    )

    override fun toAmino(): AminoType {
        return AminoType(
            type = "wasm/MsgExecuteContract",
            value = this)
    }

    companion object {
        private fun parseExecuteMsg(msgJson: String): Any {
            val supportedTypes = listOf(
                Cw721ExecuteMsg::class.java,
                Cw20ExecuteMsg::class.java
            )

            return supportedTypes
                .mapNotNull { type ->
                    runCatching {
                        GsonSingleton.gson.fromJson(msgJson, type)
                    }.getOrNull()
                }
                .firstOrNull { result ->
                    when (result) {
                        is Cw721ExecuteMsg -> result.transfer_nft != null
                        is Cw20ExecuteMsg -> result.transfer != null
                        else -> false
                    }
                } ?: GsonSingleton.gson.fromJson(msgJson, Map::class.java)
        }
    }
}

data class Cw20ExecuteMsg(
    var transfer: TransferMsg?=null
) : Serializable

data class Cw721ExecuteMsg(
    var transfer_nft: TransferNFTMsg?=null
) : Serializable

data class TransferMsg(
    var recipient: String = "",
    var amount: String = ""
) : Serializable

data class TransferNFTMsg(
    var recipient: String = "",
    var token_id: String = ""
) : Serializable