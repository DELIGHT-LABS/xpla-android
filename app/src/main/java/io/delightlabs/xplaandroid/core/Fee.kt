package io.delightlabs.xplaandroid.core


import cosmos.tx.v1beta1.TxOuterClass.Fee
import java.io.Serializable


data class StdFee(
    var amount: List<Coin> = listOf(),
    var gas: String = "",
    var payer: String? = null,
    var granter: String? = null
) : Serializable {
    constructor(fee: Fee) : this() {
        this.amount = fee.amountList.map {
            Coin(amount = it.amount, denom = it.denom)
        }
        this.gas = fee.gasLimit.toString()
        if(!fee.payer.isNullOrBlank()) {
            this.payer = fee.payer
        }
        if(!granter.isNullOrBlank()) {
            this.granter = fee.granter
        }
    }
}