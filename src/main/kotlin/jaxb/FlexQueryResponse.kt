package jaxb

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement

@JacksonXmlRootElement
class FlexQueryResponse {
    @JsonProperty("FlexStatements")
    var flexStatements: FlexStatements = FlexStatements()
    var queryName: String? = null
    var type: String? = null
}