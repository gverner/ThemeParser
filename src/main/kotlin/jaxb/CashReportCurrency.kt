//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2022.07.10 at 07:48:11 PM EDT 
//
package jaxb

/**
 *
 * Java class for CashReportCurrencyType complex type.
 *
 *
 * The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="CashReportCurrencyType">
 * &lt;simpleContent>
 * &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema>string">
 * &lt;attribute name="accountId" type="{http://www.w3.org/2001/XMLSchema}string" />
 * &lt;attribute name="acctAlias" type="{http://www.w3.org/2001/XMLSchema}string" />
 * &lt;attribute name="currency" type="{http://www.w3.org/2001/XMLSchema}string" />
 * &lt;attribute name="endingCash" type="{http://www.w3.org/2001/XMLSchema}string" />
 * &lt;attribute name="endingCashSec" type="{http://www.w3.org/2001/XMLSchema}string" />
 * &lt;attribute name="endingCashCom" type="{http://www.w3.org/2001/XMLSchema}string" />
 * &lt;/extension>
 * &lt;/simpleContent>
 * &lt;/complexType>
</pre> *
 *
 *
 */
class CashReportCurrency {
    var value: String? = null
    var accountId: String? = null
    var acctAlias: String? = null
    var currency: String? = null
    var endingCash = 0.0
    var endingCashSec: String? = null
    var endingCashCom: String? = null
}