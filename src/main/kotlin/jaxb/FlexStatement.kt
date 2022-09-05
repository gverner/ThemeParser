package jaxb

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement

@JacksonXmlRootElement
class FlexStatement {
    @JacksonXmlProperty(localName = "OpenPositions")
    var openPositions: OpenPositions = OpenPositions()

    @JsonProperty("CashReport")
    var cashReport: CashReport = CashReport()

    var accountId: String? = null
    var fromDate = 0
    var toDate = 0
    var period: String? = null
    var whenGenerated: String? = null
}