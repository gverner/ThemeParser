package jaxb

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper

class OpenPositions {
    @JsonProperty("OpenPosition")
    @JacksonXmlElementWrapper(localName = "OpenPosition", useWrapping = false)
    var openPosition: ArrayList<OpenPosition> = ArrayList<OpenPosition>()
}