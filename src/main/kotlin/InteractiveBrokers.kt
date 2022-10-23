import com.fasterxml.jackson.dataformat.xml.XmlMapper
import jaxb.FlexQueryResponse
import jaxb.OpenPositions
import java.io.File

class InteractiveBrokers {

    fun parseFile(filename: String, workFolder: String, staticData: String): FlexQueryResponse {
        if (!File(filename).exists()) {
            println("Interactive Brokers File NOT Found")
            return FlexQueryResponse()
        } else {
            println("Interactive Brokers File $filename")
        }
        val flexQueryResponse: FlexQueryResponse = import(filename)
        writeUniqueThemes(flexQueryResponse.flexStatements, "${workFolder}extractedIBThemes.json")
        calcUSDFields(flexQueryResponse)
        this.addCashPositions(flexQueryResponse)
        populateThemeName(
            flexQueryResponse.flexStatements,
            mapIdentityLookup(loadThemeData("${staticData}themeData.json"))
        )
        return flexQueryResponse
    }


    fun calcUSDFields(flexQueryResponse: FlexQueryResponse) {
        for (statement in flexQueryResponse.flexStatements.flexStatement.listIterator()) {
            calcUSDFields(statement.openPositions)
        }
    }

    fun calcUSDFields(openPositions: OpenPositions) {
        for (openPosition in openPositions.openPosition.listIterator()) {
            with(openPosition) {
                broker = "IB"
                baseCostBasisMoney = costBasisMoney * fxRateToBase
                baseCostBasisPrice = costBasisPrice * fxRateToBase
                baseMarkPrice = markPrice * fxRateToBase
                if (!symbol.equals("CASH", ignoreCase = true)) {
                    baseMoney = positionValue * fxRateToBase
                }
                baseGainLoss = fifoPnlUnrealized * fxRateToBase
            }
        }
    }

   fun addCashPositions(flexQueryResponse: FlexQueryResponse) {
        for (statement in flexQueryResponse.flexStatements.flexStatement.listIterator()) {
            statement.openPositions.openPosition.addAll(buildCashPositions(statement.cashReport))
        }
    }


    fun import(filename: String): FlexQueryResponse {
        val xm = XmlMapper()
        return xm.readValue(File(filename), FlexQueryResponse::class.java)
    }



}
