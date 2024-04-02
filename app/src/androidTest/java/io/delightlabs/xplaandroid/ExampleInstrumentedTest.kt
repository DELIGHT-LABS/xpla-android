package io.delightlabs.xplaandroid

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.protobuf.Any
import cosmos.bank.v1beta1.Tx.MsgSend
import cosmos.bank.v1beta1.msgSend
import cosmos.base.v1beta1.CoinOuterClass.Coin
import io.delightlabs.xplaandroid.api.XplaNetwork
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith


/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {


    private val lcd = LCDClient(
        network = XplaNetwork.TestNet,
        gasAdjustment = "3",
        gasPrices = listOf()
    )

//    @Test
//    fun useAppContext() {
//        // Context of the app under test.
//        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
//        assertEquals("io.delightlabs.xplaandroid", appContext.packageName)
//    }
//
//    @Test
//    fun lcdWallet(){
//        val seedPhrase = "table dinner sibling crisp hen genuine wing volume sport north omit cushion struggle script dinosaur merge medal visa also mixture faint surge boy wild"
//        val wallet = lcd.wallet(seedPhrase)
//        assertEquals("xpla1wrkl2pz9v6dgzsqt0kzcrx34rgh0f05548kdy9", wallet.address)
//    }
//
//    @Test
//    fun lcdAuth() {
//        val response: APIReturn.Account? = lcd.authAPI.accountInfo("xpla1wrkl2pz9v6dgzsqt0kzcrx34rgh0f05548kdy9")
//        response?.baseAccount?.let { Log.d("response", it.getPublicKey()) }
//    }
//
//    data class Query(val pairs: Pairs)
//    data class Pairs(val limit: Int)
//    @Test
//    fun lcdWasm() {
//        val stringMap: MutableMap<String, Any?> = LinkedHashMap()
//        val pairs: MutableMap<String, Any?> = LinkedHashMap()
//        pairs["limit"] = "20"
//        stringMap["pairs"] = pairs
//        Log.d("map", Base64.encodeToString(stringMap.toString().toByteArray(), 1))
//        val query = Query(Pairs(20))
//        val gson = Gson()
//        val json = gson.toJson(query)
//        Log.d("json", json)
//        val response: APIReturn.SmartQuery? = lcd.wasmAPI.contractQuery("xpla1j4kgjl6h4rt96uddtzdxdu39h0mhn4vrtydufdrk4uxxnrpsnw2qug2yx2", json.toString())
//        response?.let { Log.d("response",""+it.data.pairs[0]) }
//    }

    @Test
    fun testCreateTx() {
        val seedPhrase = "table dinner sibling crisp hen genuine wing volume sport north omit cushion struggle script dinosaur merge medal visa also mixture faint surge boy wild"
        lcd.wallet(mnemonic = seedPhrase)?.let {
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
                CreateTxOptions(msgs = listOf(any))
            )

            lcd.txAPI.broadcast(createTx)?.let {
                println("broadcastzz: " + "${it}")
            }
        }
    }



}

