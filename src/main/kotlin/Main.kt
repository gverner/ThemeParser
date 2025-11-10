import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.csv.CsvMapper
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import jaxb.*
import java.io.*
import java.util.*
import kotlin.collections.HashSet

fun main(args: Array<String>) {
    val inputFolder = "/users/gvern/Downloads"
    val staticData = "src/main/resources/"
    val outputFolder = "src/test/resources/"
    val workFolder = "build/theme-work/"

    println("Convert Brokerage Extracts inputFolder=${inputFolder}  outputFolder=${outputFolder}  workFolder=${workFolder}")
    // Try adding program arguments via Run/Debug configuration.
    // Learn more about running applications: https://www.jetbrains.com/help/idea/running-applications.html.
    println("Program arguments: ${args.joinToString()}")
    File(workFolder).mkdir()

    val ib = InteractiveBrokers()
    val flexQueryResponse = ib.parseFile(
        scanFolder(folder = inputFolder, prefix = "queryPlusCashGlenn"),
        workFolder = workFolder,
        staticData = staticData
    )
    val flexQueryResponseAndrea = ib.parseFile(
        scanFolder(folder = inputFolder, prefix = "queryPlusCashAndrea"),
        workFolder = workFolder,
        staticData = staticData
    )
    //writeFlatPositionsCSV(flexQueryResponse, "src/test/resources/positions.csv")
    val schwab = Schwab()
    // Glenn Andrea-Positions
    // "RETIREMENT-Positions"
    // "All-Accounts-Positions"
    val schwabFlexStatements =
        schwab.parseFile(
            scanFolder(inputFolder, prefix = "RETIREMENT-Positions"),
            workFolder = workFolder,
            staticData = staticData
        )

    val insiderThemes = createThemeSet(loadThemeData("${staticData}themeData.json"))

    flexQueryResponse.flexStatements.flexStatement.addAll(flexQueryResponseAndrea.flexStatements.flexStatement)
    flexQueryResponse.flexStatements.flexStatement.addAll(schwabFlexStatements.flexStatement)
    populateThemeGroup(flexQueryResponse.flexStatements, insiderThemes)
    populateAccountType(flexQueryResponse.flexStatements)
    populateDescription(flexQueryResponse.flexStatements)
    writeFlatPositionsCSV(flexQueryResponse, "${outputFolder}positions2.csv")
}

fun scanFolder(folder: String, prefix: String): String {
    var filename = ""
    var fullFilename = ""
    val regex = """(?i)\b(i|xml|csv)\b""".toRegex()
    File(folder).walk(FileWalkDirection.TOP_DOWN).forEach {
        if (it.name.startsWith(prefix = prefix, ignoreCase = true)) {
            println(it.nameWithoutExtension)
            if (regex.containsMatchIn(it.extension) && it.nameWithoutExtension >= filename) {
                filename = it.nameWithoutExtension
                fullFilename = it.absolutePath
            }
        }
    }
    return fullFilename
}


fun writeFlatPositionsCSV(flexQueryResponse: FlexQueryResponse, filename: String) {
    println("********* WRITE THEMES CSV")

    val CSV_MAPPER = CsvMapper()
    val altSchema = CSV_MAPPER.schemaFor(OpenPosition::class.java).withHeader()
    println(filename)
    StringWriter().use {
        val csvOutputFile = File(filename)
        val seqW = CSV_MAPPER.writer(altSchema)
            .writeValues(csvOutputFile)
        for (statement in flexQueryResponse.flexStatements.flexStatement.listIterator()) {
            for (position in statement.openPositions.openPosition.listIterator()) {
                seqW.write(position)
            }
        }
    }
}


fun buildCashPositions(cashReport: CashReport): List<OpenPosition> {
    val openPositions = ArrayList<OpenPosition>()
    for (cashReportCurrency: CashReportCurrency in cashReport.cashReportCurrency) {
        val openPosition = OpenPosition()
        openPosition.accountId = cashReportCurrency.accountId
        openPosition.acctAlias = cashReportCurrency.acctAlias
        openPosition.markPrice = 1.0
        openPosition.baseMarkPrice = 1.0
        openPosition.listingExchange = "Cash"
        openPosition.symbol = "CASH"
        openPosition.broker = "IB"
        openPosition.usdCashInvestments = cashReportCurrency.endingCash
        openPosition.baseCostBasisMoney = cashReportCurrency.endingCash
        openPosition.baseMoney = cashReportCurrency.endingCash
        if (cashReportCurrency.currency.equals("BASE_SUMMARY", true)) {
            openPositions.add(openPosition)
        } else {
            println("Ignoring account ${openPosition.accountId} ${openPosition.acctAlias} cash currency ${cashReportCurrency.currency }  ${cashReportCurrency.endingCash})")
        }
    }
    return openPositions
}

fun populateThemeGroup(flexStatements: FlexStatements, themeSet: Set<String>) {
    val manualInsiderTheme: HashSet<String> = HashSet( listOf("Dollar"))
    val otherThemeNames: HashSet<String> = HashSet(listOf("Diversification","Value","Bonds"))
    //val allIras: HashSet<String> = HashSet(listOf("Andrea Roth", "Glenn IRA-1", "Glenn Roth", "IRA Andrea"))
    for (statement in flexStatements.flexStatement.listIterator()) {
        for (position in statement.openPositions.openPosition.listIterator()) {
            if ("CASH".equals(position.themeName, ignoreCase = true)) {
                position.themeGroup = "cash"
            } else if ("IRA".equals(position.themeName, ignoreCase = true)) {
                    position.themeGroup = "other"
            } else if (otherThemeNames.contains(position.themeName)) {
                position.themeGroup = "other"
//            } else if (allIras.contains(position.acctAlias)) {
//                position.themeGroup = "allIRA"
            } else if (position.themeName != null && position.themeName!!.startsWith("z", true)) {
                position.themeGroup = "Sanctioned or Delisted"
            } else if (themeSet.contains(position.themeName)) {
                position.themeGroup = "insider"
            } else if (manualInsiderTheme.contains(position.themeName)) {
                position.themeGroup = "insider"
            } else {
                position.themeGroup = "other"
            }
        }
    }
}

fun populateAccountType(flexStatements: FlexStatements) {
//    val iraAccounts: HashSet<String> = HashSet(listOf("IRA Andrea", "Glenn IRA-1", "U7277264 M Asy Verner"));
//    val rothAccounts: HashSet<String> = HashSet(listOf("Andrea Roth", "Glenn Roth"));
//    val taxableAccounts: HashSet<String> = HashSet(listOf("Glenn_Andrea ...649","Offshore Oil & Gas","Main Uranium","Glenn and Andrea Joint","Russian Oil & Gas","Argentina","Base Metals & Copper","Agriculture","Shipping","Coal", "Natural Gas","Eastern Europe and Japan","Gold & Silver"))
    // U19463890	Andrea Roth
    // U19478447	IRA Andrea
    // U19474002	Glenn IRA-1
    // U19455041	Glenn Roth
    // U7277264	    U7277264 M Asy Verner
   val iraAccounts: HashSet<String> = HashSet(listOf("U7277264", "U19474002", "U19478447"))
    val rothAccounts: HashSet<String> = HashSet(listOf("U19455041", "U19463890"))

    for (statement in flexStatements.flexStatement.listIterator()) {
        for (position in statement.openPositions.openPosition.listIterator()) {
            if (iraAccounts.contains(position.accountId)) {
                position.accountType = "ira"
            } else
                if (rothAccounts.contains(position.accountId)) {
                    position.accountType = "roth"
                } else {
                    position.accountType = "taxable"
                }
        }
    }
}

fun populateDescription(flexStatements: FlexStatements) {
    for (statement in flexStatements.flexStatement.listIterator()) {
        for (position in statement.openPositions.openPosition.listIterator()) {
            position.description = position.symbol!!.padEnd(5, ' ') + "- "+ position.description+ " " + position.listingExchange
        }
    }
}
fun populateThemeName(flexStatements: FlexStatements, identityMap: Map<String, Theme>, symbolMap: Map<String, Theme>) {
    //reportDuplicateSymbols(identityMap)
    for (statement in flexStatements.flexStatement.listIterator()) {
        for (position in statement.openPositions.openPosition.listIterator()) {
            position.themeName = identityMap.get(position.symbol?.substringBefore(" ") + position.listingExchange)?.name
            if (position.themeName == "" || null == position.themeName) {
                position.themeName = symbolMap.get(position.symbol)?.name
                println("Theme Not Found for in idMap " + position.symbol+ " Exch "+ position.listingExchange + " using symbolMap " + symbolMap.get(position.symbol)?.name)
            }
        }
    }
}

fun reportDuplicateSymbols(identityMap: Map<String, Theme>) {
    println("report duplicates")
    val symbols = HashSet<String>()
    for (theme in identityMap.values) {
        theme.identities.forEach { identity ->
//            println (" theam $theme symbol ${identity.symbol}")
            val symbol = identity.symbol
            if (!symbols.add(symbol)) {
                println("Duplicate Symbol found in theme $theme symbol $symbol :: ${identity.symbol} ${identity.listingExchange}")
            }
        }
    }
}

fun loadThemeData(filename: String): ThemeData {
    val objectMapper = ObjectMapper()
    val themeData = objectMapper.readValue(File(filename), ThemeData::class.java)
    return themeData
}

fun mapIdentityLookup(themeData: ThemeData): Map<String, Theme> {
    val identityMap = HashMap<String, Theme>()
    for (theme in themeData.themeData!!.listIterator()) {
        for (idenity in theme.identities) {
            identityMap.put(idenity.id(), theme)
        }
    }
    return identityMap
}
fun mapSymbolLookup(themeData: ThemeData): Map<String, Theme> {
    val symbolMap = HashMap<String, Theme>()
    var symbolsInMultipleExchanges = 0
    for (theme in themeData.themeData!!.listIterator()) {
        for (idenity in theme.identities) {
            val dup = symbolMap.put(idenity.symbol, theme)
            if (dup!= null) {
                if (dup.name != theme.name) {
                    println("duplicate symbol theme ${dup} unable to add ${idenity.symbol} : ${idenity.listingExchange} ${theme.name}")
                } else {
                    symbolsInMultipleExchanges++
                }
            }
        }
    }
    println("Symbols in multiple exchanges but in same theme "+symbolsInMultipleExchanges)
    return symbolMap
}
fun createThemeSet(themeData: ThemeData): Set<String> {
    val themeSet = HashSet<String>()
    for (theme in themeData.themeData!!.listIterator()) {
        for (idenity in theme.identities) {
            themeSet.add(theme.name!!)
        }
    }
    // schwab theme
    themeSet.add("Oil & Gas Producers")
    return themeSet
}

fun writeThemesXML(themeData: ThemeData, filename: String) {
    val xm = XmlMapper().writerWithDefaultPrettyPrinter()
    xm.writeValue(File(filename), themeData)
}

fun writeThemesJson(themeData: ThemeData, filename: String) {
    val objectMapper = ObjectMapper().writerWithDefaultPrettyPrinter()
    objectMapper.writeValue(File(filename), themeData)
}


fun writeUniqueThemes(flexStatements: FlexStatements, filename: String) {
    val themes = extractUniqueThemes(flexStatements)

    val themeData = ThemeData()
    themeData.themeData = themes.values.toList()
    if (filename.endsWith("xml", ignoreCase = true)) {
        writeThemesXML(themeData, filename)
    } else {
        writeThemesJson(themeData, filename)
    }
}


private fun extractUniqueThemes(flexStatements: FlexStatements): HashMap<String, Theme> {
    val themes = HashMap<String, Theme>()
    val allIdentities = HashMap<String, Identity>()
    for (statement in flexStatements.flexStatement.listIterator()) {
        val uniqueIdentities = HashMap<String, Identity>()
        for (position in statement.openPositions.openPosition.listIterator()) {
            val identity = Identity()
            identity.symbol = position.symbol!!
            identity.listingExchange = position.listingExchange!!
            if (!"IRA".equals(position.acctAlias)) {
                val theme = themes.get(position.acctAlias) ?: Theme()
                theme.name = position.acctAlias
                themes.put(theme.name!!, theme)
                uniqueIdentities.put(identity.symbol + identity.listingExchange, identity)
                theme.identities = ArrayList<Identity>(uniqueIdentities.values)
            }
        }
        allIdentities.putAll(uniqueIdentities)
    }
// IRA theme only when not part of one of the other themes
    val iraThemes = HashMap<String, Theme>()
    for (statement in flexStatements.flexStatement.listIterator()) {
        val iraIdentities = HashMap<String, Identity>()
        for (position in statement.openPositions.openPosition.listIterator()) {
            if ("IRA".equals(position.acctAlias)) {
                val identity = Identity()
                identity.symbol = position.symbol!!
                identity.listingExchange = position.listingExchange!!
                // if not in the other themes then add to IraThemes
                if (null == allIdentities.get(identity.id())) {
                    val theme = iraThemes.get(position.acctAlias) ?: Theme()
                    theme.name = position.acctAlias
                    iraThemes.put(theme.name!!, theme)
                    iraIdentities.put(identity.symbol + identity.listingExchange, identity)
                    theme.identities = ArrayList<Identity>(iraIdentities.values)
                }
            }
        }
    }
    themes.putAll(iraThemes)

    return themes
}


