package jaxb

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper

//@JacksonXmlRootElement(localName = "theme")
class Theme() {

    var name: String? = null

    @JacksonXmlElementWrapper(useWrapping=false)
    var identities = ArrayList<Identity>()

    override fun toString(): String {
        return name.toString();
    }
}