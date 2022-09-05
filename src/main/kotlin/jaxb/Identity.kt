package jaxb

class Identity {

    var symbol: String = ""
    var listingExchange: String = ""

    fun id(): String {
        return symbol+listingExchange
    }
    override fun toString(): String {
        return  symbol+":"+listingExchange
    }
}