package jaxb

import com.fasterxml.jackson.annotation.JsonPropertyOrder

@JsonPropertyOrder(
    "account",
    "symbol",
    "description",
    "quantity",
    "price",
    "priceChangeDollar",
    "priceChangePercent",
    "marketValue",
    "dayChangeDol",
    "dayChangePercent",
    "costBasis",
    "gainLossDollar",
    "gainLossPercent ",
    "reinvestDividends",
    "capitalGains",
    "percentOfAccount",
    "dividendYield",
    "lastDividend",
    "fxDividendDate",
    "PERatio",
    "weekLow",
    "weekHigh",
    "volume",
    "intrinsicValue",
    "inTheMoney",
    "securityType"

)
class SchwabData {
    var account: String = ""
    var symbol: String = ""
    var description = ""
    var quantity = 0.0
    var price = 0.0
    var priceChangeDollar = 0.0
    var priceChangePercent = 0.0
    var marketValue = 0.0
    var dayChangeDol = 0.0
    var dayChangePercent = 0.0
    var costBasis = 0.0
    var gainLossDollar = 0.0
    var gainLossPercent = 0.0
    var reinvestDividends = ""
    var capitalGains = 0.0
    var percentOfAccount = 0.0
    var dividendYield = 0.0
    var lastDividend = 0.0
    var fxDividendDate = ""
    var PERatio = 0.0
    var weekLow = 0.0
    var weekHigh = 0.0
    var volume = 0.0
    var intrinsicValue = 0
    var inTheMoney = ""
    var securityType = ""
    public override fun toString() : String {
        return "account=$account symbol=$symbol qty=$quantity price=$price value=$marketValue costBasis=$costBasis gain=$gainLossDollar percent=$gainLossPercent"
    }
}