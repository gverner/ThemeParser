package jaxb

class OpenPosition {
    var broker = ""
    var accountId: String? = null
    var acctAlias: String? = null
    var currency: String? = null
    var symbol: String? = null
    var position = 0.0
    var markPrice = 0.0
    var costBasisPrice = 0.0
    var costBasisMoney = 0.0
    var fxRateToBase = 0.0
    var description: String? = null
    var securityID: String? = null
    var securityIDType: String? = null
    var listingExchange: String? = null
    var themeName: String? = null
    var baseMarkPrice = 0.0
    var baseCostBasisPrice = 0.0
    var baseCostBasisMoney = 0.0
    var baseMoney = 0.0
    var baseGainLoss = 0.0
    var positionValue = 0.0
    var fifoPnlUnrealized = 0.0

    override fun toString(): String {
        return "Account ('$accountId') Alias ('$acctAlias') Symbol ('$symbol') "
    }


}