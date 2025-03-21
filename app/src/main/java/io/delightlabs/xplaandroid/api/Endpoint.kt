package io.delightlabs.xplaandroid.api

sealed class Endpoint(val path: String) {

    class AccountInfo(address: String)
        : Endpoint("/cosmos/auth/v1beta1/accounts/$address")
    class Balance(address: String)
        : Endpoint("/cosmos/bank/v1beta1/balances/$address")

    class BalanceByDenom(address: String, denom: String)
        : Endpoint("/cosmos/bank/v1beta1/balances/$address/by_denom?denom=$denom")

    class BankTotal
        : Endpoint("/cosmos/bank/v1beta1/supply")

    class SmartQuery(contractAddr: String, queryData: String)
        : Endpoint("/cosmwasm/wasm/v1/contract/$contractAddr/smart/$queryData")

    class Simulate
        : Endpoint("/cosmos/tx/v1beta1/simulate")

    class Broadcast
        : Endpoint("/cosmos/tx/v1beta1/txs")
}