package io.delightlabs.xplaandroid.api

import android.util.Base64
import android.util.Log
import com.google.protobuf.Any
import com.google.protobuf.ByteString
import com.google.protobuf.kotlin.toByteString
import cosmos.base.v1beta1.CoinOuterClass.Coin
import cosmos.tx.signing.v1beta1.Signing
import cosmos.tx.v1beta1.TxOuterClass
import cosmos.tx.v1beta1.TxOuterClass.AuthInfo
import cosmos.tx.v1beta1.TxOuterClass.SignerInfo
import cosmos.tx.v1beta1.TxOuterClass.Tx
import cosmos.tx.v1beta1.authInfo
import cosmos.tx.v1beta1.fee
import cosmos.tx.v1beta1.modeInfo
import cosmos.tx.v1beta1.signerInfo
import cosmos.tx.v1beta1.tx
import cosmos.tx.v1beta1.txBody
import io.delightlabs.xplaandroid.LCDClient
import io.delightlabs.xplaandroid.PubkeyProtoType
import xpla.tx.Tx.CreateTxOptions
import java.math.BigDecimal
import java.math.RoundingMode

enum class BroadcastMode {
    BROADCAST_MODE_SYNC,
    BROADCAST_MODE_BLOCK,
    BROADCAST_MODE_ASYNC
}

data class SignerOptions(
    val address: String?,
    val sequenceNumber: Long?,
    val publicKey: String?
)

class TxAPI(private val lcdClient: LCDClient) {

    private val apiRequester = lcdClient.apiRequester

    private val emptyFee = fee {
        this.amount.clear()
        this.gasLimit = 0
        this.payer = ""
        this.granter = ""
    }

    @OptIn(ExperimentalStdlibApi::class)
    fun create(
        signers: List<SignerOptions>,
        options: CreateTxOptions
    ): Tx {
        var fee: TxOuterClass.Fee? = options.fee
        val msgs: List<Any> = options.msgsList
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
                            sequenceNumber = accountInfo.baseAccount.getSequenceNumber().toLong()
                        }

                        if (publicKey == null) {
                            publicKey = accountInfo.baseAccount.getPublicKey()
                        }
                    }
                }
            }

            sequenceNumber?.let { sequencenum ->
                publicKey?.let { publicKey ->
                    signerDatas.add(
                        signerInfo {
                            this.sequence = sequencenum.toLong()
                            this.publicKey = Any.newBuilder()
                                .setValue("0a21$publicKey".hexToByteArray().toByteString())
                                .setTypeUrl(PubkeyProtoType)
                                .build()
                        }
                    )
                }
            }
        }
        if (fee == null) {
            fee = estimateFee(signerDatas, options)
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
        val gasPrices = if (options.gasPricesList != null) options.gasPricesList else lcdClient.gasPrices
        val gasAdjustment =
            if (options.gasPricesList != null) options.gasAdjustment else lcdClient.gasAdjustment
        val feeDenoms = options.feeDenomsList
        val msgs = options.msgsList
        var gas: String? = options.gas
        var gasPricesCoins: List<Coin> = listOf()

        gasPrices?.let { gasprices ->
            gasPricesCoins = gasprices
            feeDenoms?.let { feeDenoms ->
                val gasPricesCoinsFiltered = gasPricesCoins.filter { coin ->
                    feeDenoms.contains(coin.denom)
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

        tx = tx.appendEmptySignatures(signers)

        if (gas == null || gas == "auto" || gas == "0") {
            gasAdjustment?.toDouble().let {
                estimateGas(tx, it!!, signers).let {
                    gas = it.toString()
                }
            }
        }
        val feeAmount = getCoinAmounts(gasPricesCoins, gas!!)

        return fee {
            this.amount.clear()
            feeAmount.let { this.amount.addAll(feeAmount) }
            this.gasLimit = gas?.toDouble()?.toLong() ?: 0
            this.payer = ""
            this.granter = ""
        }
    }

    private fun getCoinAmounts(gasPricesCoins: List<Coin>, gas: String): List<Coin> {
        return gasPricesCoins.map { coin ->
            val builder = Coin.newBuilder()
            coin.amount.toBigDecimalOrNull()?.let { coinAmount ->
                val gasAmount = BigDecimal.valueOf(gas.toDouble())
                val multipliedAmount = coinAmount.multiply(gasAmount).setScale(0, RoundingMode.HALF_UP)
                builder.setAmount(multipliedAmount.toString())
                    .setDenom(coin.denom)
            }
            builder.build()
        }
    }

    private fun estimateGas(
        tx: Tx,
        gasAdjustment: Double,
        signers: List<SignerInfo>? = null
    ): Double {
        var simTx = tx

        if (tx.signaturesList.isEmpty()) {
            if (!signers.isNullOrEmpty()) {
                throw Exception("SignatureIsNotValid")
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

        val simulateTx = simulate(simTx)

        val gasUsed = simulateTx?.gasInfo?.gasUsed?.toInt() ?: 1
        return gasAdjustment * gasUsed
    }

    fun simulate(tx: Tx): APIReturn.SimulateTx? {
        val params = hashMapOf("tx_bytes" to Base64.encodeToString(tx.toByteArray(), 0))
        Log.d("simulate", params.toString())
        return apiRequester.request<APIReturn.SimulateTx>(
            HttpMethod.POST,
            Endpoint.Simulate().path,
            params
        )
    }

    fun broadcast(tx: Tx): APIReturn.BroadcastResponse? {
        val encodedTxBytes = Base64.encodeToString(tx.toByteArray(), 0)
        val params = hashMapOf(
            "tx_bytes" to encodedTxBytes,
            "mode" to BroadcastMode.BROADCAST_MODE_SYNC.name
        )

        return apiRequester.request<APIReturn.BroadcastResponse>(
            HttpMethod.POST,
            Endpoint.Broadcast().path,
            params
        )
    }

    private fun Tx.appendEmptySignatures(signers: List<SignerInfo>): Tx {
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

}
