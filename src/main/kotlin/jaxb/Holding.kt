package jaxb

import com.fasterxml.jackson.annotation.JsonPropertyOrder

class Holding {
    var accountId: String? = null
    var currency: String? = null
    var symbol: String? = null
    var listingExchange: String? = null
    override fun toString(): String {
        return "accountId:"+accountId+ " currency:"+currency+" symbol:"+symbol+" listingExchange:"+listingExchange
    }
}