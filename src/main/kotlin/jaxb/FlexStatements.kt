package jaxb

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper

class FlexStatements {
    @JsonProperty("FlexStatement")
    @JacksonXmlElementWrapper(localName = "FlexStatement", useWrapping=false)
    var flexStatement = ArrayList<FlexStatement>();
    var count = 0

}