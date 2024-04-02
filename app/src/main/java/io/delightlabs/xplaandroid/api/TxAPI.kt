package io.delightlabs.xplaandroid.api

import android.util.Base64
import com.google.protobuf.Any
import com.google.protobuf.ByteString
import com.google.protobuf.kotlin.DslList
import com.google.protobuf.kotlin.toByteString
import cosmos.base.v1beta1.CoinOuterClass.Coin
import cosmos.tx.signing.v1beta1.Signing
import cosmos.tx.v1beta1.FeeKt
import cosmos.tx.v1beta1.TxOuterClass
import cosmos.tx.v1beta1.TxOuterClass.AuthInfo
import cosmos.tx.v1beta1.TxOuterClass.SignerInfo
import cosmos.tx.v1beta1.authInfo
import cosmos.tx.v1beta1.fee
import cosmos.tx.v1beta1.modeInfo
import cosmos.tx.v1beta1.signerInfo
import cosmos.tx.v1beta1.tx
import cosmos.tx.v1beta1.txBody
import io.delightlabs.xplaandroid.CreateTxOptions
import io.delightlabs.xplaandroid.LCDClient
import io.delightlabs.xplaandroid.pubkeyProtoType
import retrofit2.Retrofit

@Suppress("UNUSED_EXPRESSION")
class TxAPI(private val lcdClient: LCDClient) : BaseAPI(lcdClient.apiRequester) {

    private val retrofit = lcdClient.apiRequester
    private val tx = TxOuterClass.Tx.newBuilder()
    private val body = TxOuterClass.TxBody.newBuilder()
    private val authInfo = TxOuterClass.AuthInfo.newBuilder()

    private val emptyFee = fee {
        this.amount.clear()
        this.gasLimit = 0
        this.payer = ""
        this.granter = ""
    }

    enum class BroadcastMode {
        BROADCAST_MODE_SYNC,
        BROADCAST_MODE_BLOCK,
        BROADCAST_MODE_ASYNC
    }

    data class SignerOptions(
        val address: String?,
        val sequenceNumber: Int?,
        val publicKey: String?
    )

    fun TxOuterClass.Tx.appendEmptySignatures(signers: List<SignerInfo>): TxOuterClass.Tx {
        val builder = this.toBuilder()
        val authInfo = this.authInfo.toBuilder()
        signers.forEach {
            val signerInfo: SignerInfo = signerInfo {
                this.publicKey = it.publicKey
                this.sequence = it.sequence
                this.modeInfo = modeInfo {
                    this.single = TxOuterClass.ModeInfo.Single.newBuilder()
                        .setMode(Signing.SignMode.SIGN_MODE_DIRECT).build()
                }
            }
            authInfo.addSignerInfos(signerInfo)
            builder.addSignatures(ByteString.EMPTY)
        }

        return builder
            .setAuthInfo(authInfo.build())
            .build()
    }


    @OptIn(ExperimentalStdlibApi::class)
    fun create(
        signers: List<SignerOptions>,
        options: CreateTxOptions
    ): TxOuterClass.Tx {
        var fee: TxOuterClass.Fee? = null
        val msgs: List<Any> = options.msgs
        val memo = options.memo
        val timeoutHeight = options.timeoutHeight
        val signerDatas: MutableList<SignerInfo> = mutableListOf()

        for (signer in signers) {
            var sequenceNumber = signer.sequenceNumber
            var publicKey = signer.publicKey

            if (sequenceNumber == null || publicKey == null) {
                signer.address?.let { address ->
                    lcdClient.authAPI.accountInfo(address)?.let { accountInfo ->
                        if (sequenceNumber == null) {
                            sequenceNumber = accountInfo.baseAccount.getSequenceNumber().toInt()
                        }

                        if (publicKey == null) {
                            publicKey = accountInfo.baseAccount.getPublicKey()
                        }
                    }
                }
            }

            sequenceNumber?.let { sequenceNumber ->
                publicKey?.let { publicKey ->
                    signerDatas.add(
                        signerInfo {
                            this.sequence = sequenceNumber.toLong()
                            this.publicKey = Any.newBuilder()
                                .setValue("0a21$publicKey".hexToByteArray().toByteString())
                                .setTypeUrl(pubkeyProtoType)
                                .build()
                        }
                    )
                }

            }
        }

        if (fee == null) {
            fee =
                estimateFee(
                    signerDatas,
                    options
                )
        }

        if (msgs.isEmpty()) {
            return tx {
                this.body = txBody {
                    this.messages.clear()
                    this.memo = ""
                    this.timeoutHeight = 0
                }

                this.authInfo = authInfo {
                    this.signerInfos.clear()
                    this.fee = fee
                }
            }
        }

        return tx {
            this.body = txBody {
                msgs.forEach { message ->
                    messages.add(message)
                }
                memo?.let {
                    this.memo = it
                }
                this.timeoutHeight = (timeoutHeight ?: 0).toLong()
            }

            this.authInfo = authInfo {
                this.signerInfos.clear()
                this.fee = fee
            }

            this.signatures.clear()
        }
    }

    fun estimateFee(
        signers: List<SignerInfo>,
        options: CreateTxOptions,
    ): TxOuterClass.Fee {
        val gasPrices = if (options.gasPrices != null) options.gasPrices else lcdClient.gasPrices
        val gasAdjustment =
            if (options.gasPrices != null) options.gasAdjustment else lcdClient.gasAdjustment
        val feeDenoms = options.feeDenoms
        val msgs = options.msgs
        var gas: String? = options.gas
        var gasPricesCoins: List<Coin> = listOf()

        gasPrices?.let { gasPrices ->
            gasPricesCoins = gasPrices
            feeDenoms?.let { feeDenoms ->
                val gasPricesCoinsFiltered = gasPricesCoins.filter { c ->
                    feeDenoms.contains(c.denom)
                }

                if (gasPricesCoinsFiltered.isNotEmpty()) {
                    gasPricesCoins = gasPricesCoinsFiltered
                }
            }
        }

        if (msgs.isEmpty()) {
            return emptyFee
        }

        val txbody = txBody {
            msgs.forEach {
                this.messages.add(it)
            }
            this.memo = options.memo ?: ""
        }


        val authInfo = AuthInfo.newBuilder()
            .clearSignerInfos()
            .setFee(
                TxOuterClass.Fee.newBuilder()
                    .setGasLimit(0)
                    .clearAmount().build()
            ).build()


        var tx = tx {
            this.body = txbody
            this.authInfo = authInfo
            this.signatures.clear()
        }

        println("authInfo fee gasLimit \uD83E\uDD28: ${tx.authInfo.fee.gasLimit}")
        println("authInfo fee Amount \uD83E\uDD28: ${tx.authInfo.fee}")
        println("body messages \uD83E\uDD28: ${body.messagesList}}")


        tx = tx.appendEmptySignatures(signers)

        println("gas \uD83E\uDD28: $gas")
        if (gas == null || gas == "auto" || gas == "0") {
            println("gasAdjustment \uD83E\uDD28: $gasAdjustment")
            gasAdjustment?.toInt().let {
                estimateGas(tx, it!!, signers)?.let {
                    gas = it.toString()
                }
            }
        }

        if (gas == null) {
            return emptyFee
        } else {
            val feeAmount = getCoinAmounts(gasPricesCoins, gas!!)

            val returnFee = fee {
                this.amount.clear()
                feeAmount.let { this.amount.addAll(feeAmount) }
                this.gasLimit = gas?.toLong() ?: 0
                this.payer = ""
                this.granter = ""
            }


            return returnFee
        }

    }


    private fun getCoinAmounts(gasPricesCoins: List<Coin>, gas: String): List<Coin> {
        return gasPricesCoins.map {
            val builder = Coin.newBuilder()
            if (it.amount.toULongOrNull() != null && gas.toULongOrNull() != null) {
                val coinAmount = it.amount.toULong()
                val denom = it.denom
                gas.toULong().let {
                    val multipliedAmount = (coinAmount * it).toString()
                    builder.setAmount(multipliedAmount)
                        .setDenom(denom)

                }
            }
            builder.build()
        }

    }

    fun estimateGas(
        tx: TxOuterClass.Tx,
        gasAdjustment: Int,
        signers: List<SignerInfo>? = null
    ): Int {
        var simTx = tx
        if (tx.signaturesList.isEmpty()) {
            if (!signers.isNullOrEmpty()) {
//                throw TxAPIError.signatureNotValid
            }

            val authInfo = AuthInfo.newBuilder()
                .clearSignerInfos()
                .setFee(
                    TxOuterClass.Fee.newBuilder()
                        .setGasLimit(0)
                        .clearAmount().build()
                ).build()

            simTx = tx {
                this.body = tx.body
                this.authInfo = authInfo
                this.signatures.clear()
            }
            simTx = simTx.appendEmptySignatures(signers ?: emptyList())

        }

        println("txbody:: ${Base64.encodeToString(simTx.body.toByteArray(), 0)}")
//        val simulateRes :  = lcdClient.apiRequester.test2(
//            "cosmos/tx/v1beta1/simulate", hashMapOf<String, kotlin.Any>(
//                "tx_bytes" to Base64.encodeToString(simTx.toByteArray(), 0)
//            )
//        )
        val simul: simulateAPIService = retrofit.getInstance().create(simulateAPIService::class.java)

        val simulateRes = runAPI(simul.simultate( hashMapOf( "tx_bytes" to Base64.encodeToString(simTx.toByteArray(), 0))))


        return gasAdjustment * ((simulateRes?.gasInfo?.gasUsed)?.toInt() ?: 1)
    }

    fun broadcast(tx: TxOuterClass.Tx): Retrofit? {
        val encodedTxBytes = Base64.encodeToString(tx.toByteArray(), 0)

        val params = hashMapOf<String, kotlin.Any>(
            "tx_bytes" to encodedTxBytes,
            "mode" to "BROADCAST_MODE_SYNC"
        )

        return lcdClient.apiRequester.test("/cosmos/tx/v1beta1/txs", params)
    }


}
