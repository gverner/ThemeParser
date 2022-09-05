package jaxb

import com.fasterxml.jackson.core.JacksonException
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import java.io.IOException
import java.math.BigDecimal

class SchwabDataDeserializer @JvmOverloads constructor(t: Class<SchwabData?>? = null) : StdDeserializer<SchwabData>(t) {
    @Throws(IOException::class, JacksonException::class)
    override fun deserialize(jsonParser: JsonParser, ctxt: DeserializationContext): SchwabData {
        var schwabData = SchwabData()
        val jsonNode = jsonParser.codec.readTree<JsonNode>(jsonParser)
        schwabData.account = jsonNode.get("account").asText()
        schwabData.symbol = jsonNode.get("symbol").asText()
        schwabData.costBasis = cleanDouble(jsonNode.get("costBasis"))
        schwabData.description = jsonNode.get("description").asText()
        schwabData.quantity = cleanDouble(jsonNode.get("quantity"))
        schwabData.price = cleanDouble(jsonNode.get("price"))
        schwabData.priceChangeDollar = cleanDouble(jsonNode.get("priceChangeDollar"))
        schwabData.priceChangePercent = cleanDouble(jsonNode.get("priceChangePercent"))
        schwabData.marketValue = cleanDouble(jsonNode.get("marketValue"))
        schwabData.dayChangeDol = cleanDouble(jsonNode.get("dayChangeDol"))
        schwabData.dayChangePercent = cleanDouble(jsonNode.get("dayChangePercent"))
        schwabData.costBasis = cleanDouble(jsonNode.get("costBasis"))
        schwabData.gainLossDollar = cleanDouble(jsonNode.get("gainLossDollar"))
        schwabData.gainLossPercent  = cleanDouble(jsonNode.get("gainLossPercent"))
        schwabData.reinvestDividends = jsonNode.get("reinvestDividends").asText()
        schwabData.capitalGains = cleanDouble(jsonNode.get("capitalGains"))
        schwabData.percentOfAccount = cleanDouble(jsonNode.get("percentOfAccount"))
        schwabData.dividendYield = cleanDouble(jsonNode.get("dividendYield"))
        schwabData.lastDividend = cleanDouble(jsonNode.get("lastDividend"))
        schwabData.fxDividendDate = (jsonNode.get("fxDividendDate").asText())
        schwabData.PERatio = cleanDouble(jsonNode.get("peratio"))
        schwabData.weekLow = cleanDouble(jsonNode.get("weekLow"))
        schwabData.weekHigh = cleanDouble(jsonNode.get("weekHigh"))
        schwabData.volume = cleanDouble(jsonNode.get("volume"))
        schwabData.intrinsicValue = cleanInt(jsonNode.get("intrinsicValue"))
        schwabData.inTheMoney = (jsonNode.get("inTheMoney")).asText()
        schwabData.securityType = (jsonNode.get("securityType")).asText()
        return schwabData
    }

    fun cleanDouble(node: JsonNode): Double {
        var result =node?.asText()?.replace("%", "")?.replace("$", "")?.replace(",", "")?.toDoubleOrNull()
        return result ?: 0.0
    }
    fun cleanDecimal(node: JsonNode): BigDecimal {
        return  (node?.asText()?.replace("%", "")?.replace("$", "")?.replace(",", ""))?.toBigDecimalOrNull()?: BigDecimal.ZERO
    }
    fun cleanInt(node: JsonNode): Int {
        return  (node?.asText()?.replace("%", "")?.replace("$", "")?.replace(",", ""))?.toIntOrNull()?:0
    }

}