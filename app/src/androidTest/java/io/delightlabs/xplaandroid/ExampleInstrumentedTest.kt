package io.delightlabs.xplaandroid

import android.util.Base64
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.gson.Gson
import io.delightlabs.xplaandroid.api.APIReturn
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
        gasPrices = intArrayOf(2, 4)
    )

    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("io.delightlabs.xplaandroid", appContext.packageName)
    }

    @Test
    fun lcdAuth() {
        val response: APIReturn.Account? = lcd.authAPI.accountInfo("xpla1wrkl2pz9v6dgzsqt0kzcrx34rgh0f05548kdy9")
        response?.baseAccount?.let { Log.d("response", it.getPublicKey()) }
    }

    data class Query(val pairs: Pairs)
    data class Pairs(val limit: Int)
    @Test
    fun lcdWasm() {
        val stringMap: MutableMap<String, Any?> = LinkedHashMap()
        val pairs: MutableMap<String, Any?> = LinkedHashMap()
        pairs["limit"] = "20"
        stringMap["pairs"] = pairs
        Log.d("map", Base64.encodeToString(stringMap.toString().toByteArray(), 1))
        val query = Query(Pairs(20))
        val gson = Gson()
        val json = gson.toJson(query)
        Log.d("json", json)
        val response: APIReturn.SmartQuery? = lcd.wasmAPI.contractQuery("xpla1j4kgjl6h4rt96uddtzdxdu39h0mhn4vrtydufdrk4uxxnrpsnw2qug2yx2", json.toString())
        response?.let { Log.d("response",""+it.data.pairs[0]) }
    }
}