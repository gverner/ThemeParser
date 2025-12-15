import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.dataformat.csv.CsvMapper
import jaxb.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileReader
import java.net.URL
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.io.path.Path
import kotlin.io.path.bufferedWriter

class Schwab {
    val exchangeCache = HashMap<String, String>()

    fun parseFile(filename: String, workFolder: String, staticData: String): FlexStatements {
        if (File(filename).exists()) {
            println("Schwab File found ${filename}")
        } else {
            println("Schwab File NOT found")
            return FlexStatements()
        }
        val workFilename = prepareFile(filename)
        val schwabData = readSchwab(workFilename)
        val flexStatements = schwabToFlexStatement(schwabData)
        enhanceCashPositions(flexStatements)
        countAccountsPositions(flexStatements)
        lookupExchanges(flexStatements)
        val themeData = loadThemeData("${staticData}themeDataSchwab.json")
        lookupExchangesFromThemeData(flexStatements, themeData)
        writeUniqueThemes(flexStatements, "${workFolder}extractedSchwabThemes.json")
        populateThemeName(flexStatements, mapIdentityLookup(themeData), mapSymbolLookup(themeData))
        return flexStatements
    }

    fun countAccountsPositions(flexStatements: FlexStatements) {
        var accountCount = 0
        var positionCount = 0
        for (flexStatment in flexStatements.flexStatement) {
            accountCount++
            for (openPosition in flexStatment.openPositions.openPosition) {
                positionCount++
            }
        }
        println("Schwab Imported ${accountCount} Accounts with ${positionCount} Positions")
    }

    fun prepareFile(filename: String): String {
        val workFilename = filename.replaceAfterLast(".","wrk", "${filename}wrk")
        val dateFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy")
        var reportDate = LocalDate.now().format(dateFormatter)
        var currentAccount = ""
        var headerWritten = true // don't write header
        val input = File(filename)
        val output = Path(workFilename)
        output.bufferedWriter().use { out ->
            input.forEachLine { line ->
                line.count { it == 'e' }
                val c = "\",\"".toRegex().findAll(line).count()
                if (line.length == 0) {
                } else if (c == 0) {
                    if (line.indexOf("Positions") == 1) {
                        reportDate = line.substringAfter(',', reportDate.trim())
                        reportDate = reportDate.substringBefore(',', reportDate.trim())
                        reportDate = "\"" + reportDate.trim()
                    } else {
                        currentAccount = "\"" + line.substringBefore("\",\"") + "\""
                    }
                } else if (c == 17)
                 {
                        if (line.indexOf("Symbol") == 1) {
                            if (!headerWritten) {
                                out.append("\"Account\"")
                                out.append(',')
                                out.append(line.replace("$", "").replace("%", "").replace(",", ""))
                                out.newLine()
                                headerWritten = true
                            }
                        } else if (line.indexOf("Account Total") == 1) {
                            // skip Account Total
                        } else {
                            out.append(currentAccount)
                            out.append(',')
                            out.append(line.replace("$", "").replace("%", ""))
                            out.newLine()
                        }

                }
            }
        }
        return workFilename
    }

    fun readSchwab(filename: String): List<SchwabData> {
        println("********* READING SCHWAB ${filename}")
        //val csvMapper = CsvMapper.builder().disable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
        val csvMapper = CsvMapper()
        val simpleModule = SimpleModule()
        simpleModule.addDeserializer(SchwabData::class.java, SchwabDataDeserializer())
        csvMapper.registerModule(simpleModule)
        val reader = csvMapper.readerWithSchemaFor(SchwabData::class.java)
        FileReader(filename).use {
            val list = reader.readValues<SchwabData>(it).readAll()
            println ("Schwab parse logging disabled?")
            if (1==2) {
                for (item in list) {
                    println(item)
                }
            }
            return list
        }

    }
    fun enhanceCashPositions(flexStatements: FlexStatements) {
        for (flexStatment in flexStatements.flexStatement) {
            for (openPosition in flexStatment.openPositions.openPosition) {
                if (openPosition.symbol!!.startsWith("CASH", ignoreCase = true)|| openPosition.symbol!!.startsWith("SWVXX", ignoreCase = true)) {
                    openPosition.symbol = "CASH"
                    openPosition.listingExchange = "Cash"
                    openPosition.costBasisPrice = 0.0
                    openPosition.baseGainLoss = 0.0
                    openPosition.baseCostBasisPrice = 0.0
                    openPosition.fxRateToBase = 1.0
                    openPosition.baseMarkPrice = 1.0
                    openPosition.markPrice = 1.0
                    openPosition.baseCostBasisMoney = openPosition.baseMoney
                    openPosition.usdCashInvestments = openPosition.baseMoney
                    //openPosition.baseCostBasisMoney = 0.0
                    //openPosition.baseMoney = 0.0
                }

            }
        }
    }

    fun lookupExchanges(flexStatements: FlexStatements) {
        println("fetching exchanges 1")
        val start = System.currentTimeMillis()
        for (flexStatment in flexStatements.flexStatement) {
            for (openPosition in flexStatment.openPositions.openPosition) {
                    openPosition.listingExchange = "NONE"
            }
        }
        println("completed Fetching Exchanges seconds=${(System.currentTimeMillis() - start) / 1000}")
    }
    fun lookupExchangesFromThemeData(flexStatements: FlexStatements, themeData: ThemeData ) {
        println("fetching exchanges")
        val symbolMap = createSymbol2ExchangeMap(themeData)
        val start = System.currentTimeMillis()
        for (flexStatment in flexStatements.flexStatement) {
            for (openPosition in flexStatment.openPositions.openPosition) {
                if (!openPosition.symbol!!.startsWith("CASH", ignoreCase = true)) {
                    if (openPosition.symbol!!.length > 10) {
                        openPosition.listingExchange = "NYSE"
                    } else {
                        openPosition.listingExchange = symbolMap.get(openPosition.symbol)
                    }
                    if (openPosition.listingExchange == null || openPosition.listingExchange.isNullOrBlank()){
                        println("Exchange not found #2 "+openPosition.symbol + " default = NYSE")
                        openPosition.listingExchange = "NYSE"
                    } else if (openPosition.listingExchange!!.startsWith("Nasdaq", true)) {
                        openPosition.listingExchange = "Nasdaq"
                    } else if (openPosition.listingExchange!!.startsWith("NYSE", true)) {
                        openPosition.listingExchange = "NYSE"
                    }
                }
            }
        }
        println("completed Fetching Exchanges seconds=${(System.currentTimeMillis() - start) / 1000}")
    }

    fun createSymbol2ExchangeMap(themeData: ThemeData): Map<String, String> {
        val exchangeMap = HashMap<String, String>()
        for (theme in themeData.themeData!!) {
            for (identity in theme.identities) {
                exchangeMap.put(identity.symbol, identity.listingExchange)
            }
        }
        return exchangeMap
    }

    fun lookupExchangesOld(flexStatements: FlexStatements) {
        println("fetching exchanges")
        val start = System.currentTimeMillis()
        for (flexStatment in flexStatements.flexStatement) {
            for (openPosition in flexStatment.openPositions.openPosition) {
                if (!openPosition.symbol!!.startsWith("CASH", ignoreCase = true)) {
                    openPosition.listingExchange = fetchExchange(openPosition.symbol)
                    if (openPosition.listingExchange!!.startsWith("Nasdaq", true)) {
                        openPosition.listingExchange = "Nasdaq"
                    } else if (openPosition.listingExchange!!.startsWith("NYSE", true)) {
                        openPosition.listingExchange = "NYSE"
                    } else if (openPosition.listingExchange == null || openPosition.listingExchange.isNullOrBlank()){
                        println("Exchange not found #1 "+openPosition.symbol + " default = NYSE")
                        openPosition.listingExchange = "NYSE"
                    }
                }
            }
        }
        println("completed Fetching Exchanges seconds=${(System.currentTimeMillis() - start) / 1000}")
    }

    fun fetchExchange(symbol: String?): String {
         if (symbol == null)  {
            return ""
        }
        if (exchangeCache.get(symbol) != null) {
            return exchangeCache.get(symbol).toString()
        }
        println(symbol)
        if (symbol.length > 10) {
            return "NYSE"
        }
        val client = OkHttpClient()
        val url =
            URL("https://query1.finance.yahoo.com/v10/finance/quoteSummary/" + symbol + "?formatted=true&crumb=sRaAb86KidE&lang=en-US&region=US&modules=price&corsDomain=finance.yahoo.com")
        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        val response = client.newCall(request).execute()

        val responseBody = response.body!!.string()

        //Response
        //println("Response Body: " + responseBody)

        //we could use jackson if we got a JSON
        val mapperAll = ObjectMapper()
        val objData = mapperAll.readTree(responseBody)
        objData.get("quoteSummary")!!.get("result").get(0).get("price").fields().forEach {
            if ("exchangeName".equals(it.key)) {
                exchangeCache.put(symbol, it.value.textValue())
                return it.value.textValue()
            }
        }
        return "unknownExchange"
    }

    fun schwabToFlexStatement(schwabList: List<SchwabData>): FlexStatements {
        val flexStatements = FlexStatements()
        for (schwabData in schwabList) {
            val flexStatement = findOrCreateOpenPosition(schwabData.account, flexStatements)
            val openPosition = OpenPosition()
            flexStatement.openPositions.openPosition.add(openPosition)
            openPosition.accountId = schwabData.account
            openPosition.acctAlias = schwabData.account
            openPosition.position = schwabData.quantity
            openPosition.fxRateToBase = 1.0
            openPosition.acctAlias = schwabData.account
            openPosition.markPrice = schwabData.price
            openPosition.costBasisMoney = schwabData.costBasis
            openPosition.costBasisPrice = schwabData.costBasis / schwabData.quantity
            openPosition.currency = "USD"
            openPosition.symbol = schwabData.symbol
            openPosition.baseMarkPrice = schwabData.price
            openPosition.baseCostBasisPrice = openPosition.costBasisPrice
            openPosition.baseCostBasisMoney = schwabData.costBasis
            openPosition.baseMoney = schwabData.marketValue
            openPosition.positionValue = schwabData.marketValue
            openPosition.fifoPnlUnrealized = schwabData.marketValue - schwabData.costBasis
            openPosition.baseGainLoss = schwabData.marketValue - schwabData.costBasis
            openPosition.description = schwabData.description
            openPosition.broker = "Schwab"
        }
        return flexStatements
    }

    fun findOrCreateOpenPosition(account: String, flexStatements: FlexStatements): FlexStatement {
        for (flexStatement in flexStatements.flexStatement) {
            if (flexStatement.accountId == account) {
                return flexStatement
            }
        }
        val flexStatement = FlexStatement()
        flexStatements.flexStatement.add(flexStatement)
        flexStatement.accountId = account
        return flexStatement
    }
}