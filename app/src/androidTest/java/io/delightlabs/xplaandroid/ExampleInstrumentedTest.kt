package io.delightlabs.xplaandroid

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.protobuf.Any
import com.google.protobuf.ByteString
import com.google.protobuf.TypeRegistry
import com.google.protobuf.util.JsonFormat
import cosmos.bank.v1beta1.msgSend
import cosmos.base.v1beta1.CoinOuterClass
import cosmos.base.v1beta1.CoinOuterClass.Coin
import cosmos.tx.signing.v1beta1.Signing.SignMode
import cosmos.tx.v1beta1.TxOuterClass.Fee
import cosmwasm.wasm.v1.msgExecuteContract
import io.delightlabs.xplaandroid.api.APIRequester
import io.delightlabs.xplaandroid.api.APIReturn
import io.delightlabs.xplaandroid.api.HttpMethod
import io.delightlabs.xplaandroid.api.XplaNetwork
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import wallet.core.jni.PrivateKey
import xpla.tx.Tx
import xpla.tx.createTxOptions


/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    private val lcd = LCDClient(
        network = XplaNetwork.Testnet,
        gasAdjustment = "3",
        gasPrices = listOf(CoinOuterClass.Coin.newBuilder().apply {
            this.amount = "850000000000"
            this.denom = "axpla"
        }.build())
    )

//    @Test
//    fun useAppContext() {
//        // Context of the app under test.
//        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
//        assertEquals("io.delightlabs.xplaandroid", appContext.packageName)
//    }

    @Test
    fun lcdWallet() {
        val seedPhrase =
            "table dinner sibling crisp hen genuine wing volume sport north omit cushion struggle script dinosaur merge medal visa also mixture faint surge boy wild"
        val wallet = lcd.wallet(seedPhrase)
        print(wallet.privateKey)
        val wallet2 = lcd.wallet(PrivateKey(wallet.privateKey.data()))
        print(wallet2.address)
        assertEquals("xpla1wrkl2pz9v6dgzsqt0kzcrx34rgh0f05548kdy9", wallet.address)
    }

    @Test
    fun lcdAuth() {
        val response: APIReturn.Account? =
            lcd.authAPI.accountInfo("xpla1wrkl2pz9v6dgzsqt0kzcrx34rgh0f05548kdy9")
        response?.baseAccount?.let { println("public address: ${it.address}") }
    }

    data class Query(val pairs: Pairs)
    data class Pairs(val limit: Int)

    @Test
    fun lcdWasm() {
        val query = Query(Pairs(20))
        val json = GsonSingleton.gson.toJson(query)
        val response: APIReturn.TokenInfoResponse? = lcd.wasmAPI.contractQueryTokenInfo(
            "xpla1w6hv0suf8dmpq8kxd8a6yy9fnmntlh7hh9kl37qmax7kyzfd047qnnp0mm",
        )
        println("wasm smartQuery:: $response")
        response?.let { println("wasm smartQuery:: ${it.data}") }
    }

    @Test
    fun balanceAPI() {
        val response: APIReturn.BalanceReturn? =
            lcd.bankAPI.balance("xpla1wrkl2pz9v6dgzsqt0kzcrx34rgh0f05548kdy9")
        response?.let { println("balance response: $response") }
    }

    @Test
    fun networkTest() {
        val apiRequester = APIRequester(XplaNetwork.Testnet)
        val result = apiRequester.request<APIReturn.AccountReturn>(
            HttpMethod.GET,
            "/cosmos/auth/v1beta1/accounts/xpla1wrkl2pz9v6dgzsqt0kzcrx34rgh0f05548kdy9"
        )
        if (result != null) {
            println("result: $result")
        }
    }

    @Test
    fun testCreateTx() {
        val seedPhrase =
            "table dinner sibling crisp hen genuine wing volume sport north omit cushion struggle script dinosaur merge medal visa also mixture faint surge boy wild"

        val lcdClient = LCDClient(
            network = XplaNetwork.Mainnet,
            gasAdjustment = "1.3",
            gasPrices = listOf(
                CoinOuterClass.Coin.newBuilder().apply {
                    this.amount = "850000000000"
                    this.denom = "axpla"
                }.build(),
            ),
        )
        lcdClient.wallet(mnemonic = seedPhrase).let {
            val offerAmount = 1000000000000000000


            val sendCoin = Coin.newBuilder()
                .setAmount("$offerAmount")
                .setDenom("axpla")
                .build()

            val txSend = msgSend {
                this.toAddress = it.address
                this.fromAddress = it.address
                this.amount.add(sendCoin)
            }

            val any = Any.newBuilder()
                .setTypeUrl("/cosmos.bank.v1beta1.MsgSend")
                .setValue(txSend.toByteString())
                .build()

            val createTx = it.createAndSignTx(
                createTxOptions {
                    this.msgs.add(any)
                }
            )

            val broadcastRes = lcdClient.txAPI.broadcast(createTx)
            println("broadcastRes: $broadcastRes")
        }
    }

    @Test
    fun testSignCosmosMsg() {
        val seedPhrase = "segment symbol pigeon tourist shop brush enter combine tornado pole snow federal lobster reopen drama wagon company salmon comfort rural palm fiscal crack roof"
        val lcd = LCDClient(
            XplaNetwork.Localnet,
            gasAdjustment = "1.3",
            gasPrices = listOf()
        )
        lcd.wallet(mnemonic = seedPhrase).let {
            val sendCoin = Coin.newBuilder()
                .setAmount("1")
                .setDenom("axpla")
                .build()

            val txSend = msgSend {
                this.toAddress = "xpla1wrkl2pz9v6dgzsqt0kzcrx34rgh0f05548kdy9"
                this.fromAddress = "xpla1nns26tapuzt36vdz0aadk7svm8p6xndtmwlyg8"
                this.amount.add(sendCoin)
            }

            val msg = Any.newBuilder()
                .setTypeUrl("/cosmos.bank.v1beta1.MsgSend")
                .setValue(txSend.toByteString())
                .build()

            val createTx = it.createAndSignTx(
                createTxOptions {
                    this.msgs.add(msg)
                    this.fee = Fee.newBuilder().setGasLimit(200000).build()
                    this.sequence = 1
                },
                accountNumber = 0
            )

            assertEquals("RVlBVVTX6Sp2+2B1DBjsNBbHGxBJZUxAy1nPbiEQ4GkyRDe+lytfFg0Q105cIPQnt59Z8fpRhhb853fgSl5PWwA=", java.util.Base64.getEncoder().encodeToString(createTx.getSignatures(0).toByteArray()) )
           createTx.getSignatures(0)
        }
    }

    @Test
    fun testSignAmino() {
        val seedPhrase = "segment symbol pigeon tourist shop brush enter combine tornado pole snow federal lobster reopen drama wagon company salmon comfort rural palm fiscal crack roof"
        val lcd = LCDClient(
            XplaNetwork.Localnet,
            gasAdjustment = "1.3",
            gasPrices = listOf()
        )
        lcd.wallet(mnemonic = seedPhrase).let {
            val sendCoin = Coin.newBuilder()
                .setAmount("1")
                .setDenom("axpla")
                .build()

            val txSend = msgSend {
                this.toAddress = "xpla1wrkl2pz9v6dgzsqt0kzcrx34rgh0f05548kdy9"
                this.fromAddress = "xpla1nns26tapuzt36vdz0aadk7svm8p6xndtmwlyg8"
                this.amount.add(sendCoin)
            }

            val msg = Any.newBuilder()
                .setTypeUrl("/cosmos.bank.v1beta1.MsgSend")
                .setValue(txSend.toByteString())
                .build()

            val createTx = it.createAndSignTx(
                createTxOptions {
                    this.msgs.add(msg)
                    this.fee = Fee.newBuilder().addAmount(0, sendCoin).setGasLimit(200000).build()
                    sequence = 1
                },
                accountNumber = 0,
                signMode = SignMode.SIGN_MODE_LEGACY_AMINO_JSON
            )

            assertEquals("MEgb9Kz2awtey5G9rC2ptBTYlRqUN+qfS1Qz27r3tdsmNr6qU+L2fMupYfUvTUNlT4GPN+7SL5nfWkGixPa39QE=", java.util.Base64.getEncoder().encodeToString(createTx.getSignatures(0).toByteArray()) )
            createTx.getSignatures(0)
        }
    }

    @Test
    fun testSignAminoWithPayerWhenCosmos47() {
        val seedPhrase = "segment symbol pigeon tourist shop brush enter combine tornado pole snow federal lobster reopen drama wagon company salmon comfort rural palm fiscal crack roof"
        val lcd = LCDClient(
            XplaNetwork.Testnet,
            gasAdjustment = "1.3",
            gasPrices = listOf()
        )
        lcd.wallet(mnemonic = seedPhrase).let {
            val sendCoin = Coin.newBuilder()
                .setAmount("1")
                .setDenom("axpla")
                .build()

            val txSend = msgSend {
                this.toAddress = "xpla1wrkl2pz9v6dgzsqt0kzcrx34rgh0f05548kdy9"
                this.fromAddress = "xpla1nns26tapuzt36vdz0aadk7svm8p6xndtmwlyg8"
                this.amount.add(sendCoin)
            }

            val msg = Any.newBuilder()
                .setTypeUrl("/cosmos.bank.v1beta1.MsgSend")
                .setValue(txSend.toByteString())
                .build()

            val createTx = it.createAndSignTx(
                createTxOptions{
                    this.msgs.add(msg)
                    this.fee = Fee.newBuilder().addAmount(0, sendCoin).setGasLimit(200000).setPayer("xpla1nns26tapuzt36vdz0aadk7svm8p6xndtmwlyg8").build()
                    this.sequence = 2
                },
                accountNumber = 0,
                signMode = SignMode.SIGN_MODE_LEGACY_AMINO_JSON
            )

            assertEquals("1WW5WdDSn/7DAImtpZxgggTMcWoQrxqGHUGyQUpuCFcsMEgAl3Vxgteye4DSyxVzlM5imp3eRp/UJsPYgnn/5AE=", java.util.Base64.getEncoder().encodeToString(createTx.getSignatures(0).toByteArray()) )
            createTx.getSignatures(0)
        }
    }

    @Test
    fun testSignAminoContractMsg() {
        val seedPhrase = "segment symbol pigeon tourist shop brush enter combine tornado pole snow federal lobster reopen drama wagon company salmon comfort rural palm fiscal crack roof"
        val lcd = LCDClient(
            XplaNetwork.Localnet,
            gasAdjustment = "1.3",
            gasPrices = listOf()
        )

        lcd.wallet(mnemonic = seedPhrase).let {
            val feeCoin = Coin.newBuilder()
                .setAmount("1")
                .setDenom("axpla")
                .build()

            val transferMsg = """{
                "transfer": {
                    "amount": "1",
                "recipient": "xpla1wrkl2pz9v6dgzsqt0kzcrx34rgh0f05548kdy9"
            }
            }""".toByteArray()

            val msgExecute = msgExecuteContract {
                this.sender = "xpla1nns26tapuzt36vdz0aadk7svm8p6xndtmwlyg8"
                this.contract = "xpla14hj2tavq8fpesdwxxcu44rty3hh90vhujrvcmstl4zr3txmfvw9s525s0h"
                this.msg = ByteString.copyFrom(transferMsg)
            }

            val msg = Any.newBuilder()
                .setTypeUrl("/cosmwasm.wasm.v1.MsgExecuteContract")
                .setValue(msgExecute.toByteString())
                .build()

            val createTx = it.createAndSignTx(
                createTxOptions {
                    this.msgs.add(msg)
                    this.fee = Fee.newBuilder().addAmount(0, feeCoin).setGasLimit(200000).build()
                    this.sequence = 8
                },
                accountNumber = 0,
                signMode = SignMode.SIGN_MODE_LEGACY_AMINO_JSON
            )

            assertEquals("uuuGzlHcidASzxYRUtfldCjkPSq/YHUdPmUDl1Tl591a52zGqgniKLUSgzUEBUF+SetkcRtXOGJrB2bbXBUQ4QE=", java.util.Base64.getEncoder().encodeToString(createTx.getSignatures(0).toByteArray()) )
            createTx.getSignatures(0)
        }
    }

    @Test
    fun testSignAminoWithMemo() {
        val seedPhrase = "segment symbol pigeon tourist shop brush enter combine tornado pole snow federal lobster reopen drama wagon company salmon comfort rural palm fiscal crack roof"
        val lcd = LCDClient(
            XplaNetwork.Testnet,
            gasAdjustment = "1.5",
            gasPrices = listOf()
        )

        lcd.wallet(mnemonic = seedPhrase).let {

            val sendCoin = Coin.newBuilder()
                .setAmount("1")
                .setDenom("axpla")
                .build()

            val txSend = msgSend {
                this.toAddress = "xpla1wrkl2pz9v6dgzsqt0kzcrx34rgh0f05548kdy9"
                this.fromAddress = "xpla1nns26tapuzt36vdz0aadk7svm8p6xndtmwlyg8"
                this.amount.add(sendCoin)
            }

            val msg = Any.newBuilder()
                .setTypeUrl("/cosmos.bank.v1beta1.MsgSend")
                .setValue(txSend.toByteString())
                .build()

            val fee =
                Fee.newBuilder()
                    .setGasLimit(200000)
                    .addAmount(sendCoin)
                    .build()

            val createTx = it.createAndSignTx(
                createTxOptions {
                    this.memo = "U2FsdGVkX18bzNU4JzqtCxkderonQ0mZnkEFoCH/s9Thngscv/0s7hdQsdIzFDVFKK6PPJL1PYcAEfu46EVEjjLTaghGGQj9kncghM5YoV8="
                    this.msgs.add(msg)
                    this.fee = fee
                    this.sequence = 1
                },
                accountNumber = 8,
                signMode = SignMode.SIGN_MODE_LEGACY_AMINO_JSON
            )

            // z/8awwXiFSObxVIHMeIDDy7NRiHPzLYIwAx4AMcehfFZ/VyxlYrILxeXSuSmiO+WacT/C6ApBsoxtzvRzV7O6gA=
//oVN7JefL5gxLzqO8UOY9R7HjqtQfJxvYLBH955NGG6Ar3JntX8IxbuPnFa6fmEqAJ/NBCyXkBnyd3g31t2J6VgA=
                assertEquals("z/8awwXiFSObxVIHMeIDDy7NRiHPzLYIwAx4AMcehfFZ/VyxlYrILxeXSuSmiO+WacT/C6ApBsoxtzvRzV7O6gA=", java.util.Base64.getEncoder().encodeToString(createTx.getSignatures(0).toByteArray()) )
            createTx.getSignatures(0)
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun testJsonToAny() {
        val strJson = """
            {
            "fee": {
                "amount": [
                    {
                        "amount": "32254320000000000",
                        "denom": "axpla"
                    }
                ],
                "gas_limit": "115194"
            },
            "memo": "",
            "msgs": [
                {
                    "@type": "/cosmos.bank.v1beta1.MsgSend",
                    "amount": [
                        {
                            "amount": "10000000000000000",
                            "denom": "axpla"
                        }
                    ],
                    "from_address": "xpla1lg22287cj523vgdah8z4287nuzct43tmdtj69w",
                    "to_address": "xpla1m9ttxu9dewu9s7jyuzaz9przerq4x8ev3crsem"
                }
            ]
        }
        """.trimIndent()

        val builder = Tx.CreateTxOptions.newBuilder()
        JsonFormat.parser().usingTypeRegistry(TypeRegistrySingleton.typeRegistry).merge(strJson, builder)
        val createTx = builder.build()

        assertEquals(
            "0a1c2f636f736d6f732e62616e6b2e763162657461312e4d736753656e6412760a2b78706c61316c673232323837636a3532337667646168387a343238376e757a63743433746d64746a363977122b78706c61316d397474787539646577753973376a79757a617a3970727a657271347838657633637273656d1a1a0a056178706c6112113130303030303030303030303030303030",
            createTx.getMsgs(0).toByteArray().toHexString()
        )
    }
}

