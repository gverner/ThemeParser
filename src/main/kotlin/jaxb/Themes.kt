package jaxb

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement

//@JacksonXmlRootElement
class Themes {
    constructor(theme: List<Theme>?) {
        this.theme = theme
    }

    @JacksonXmlElementWrapper(useWrapping=false)
    var theme: List<Theme>? = null
}