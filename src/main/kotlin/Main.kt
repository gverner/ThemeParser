import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.csv.CsvMapper
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import jaxb.*
import java.io.*
import java.util.*


fun main(args: Array<String>) {
    val inputFolder = "/users/glennverner/Downloads"
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
        scanFolder(folder = inputFolder, prefix = "queryPlusCash"),
        workFolder = workFolder,
        staticData = staticData
    )
    //writeFlatPositionsCSV(flexQueryResponse, "src/test/resources/positions.csv")
    val schwab = Schwab()
    val schwabFlexStatements =
        schwab.parseFile(
            scanFolder(inputFolder, prefix = "All-Accounts-Positions"),
            workFolder = workFolder,
            staticData = staticData
        )

    val insiderThemes = createThemeSet(loadThemeData("${staticData}themeData.json"))
    flexQueryResponse.flexStatements.flexStatement.addAll(schwabFlexStatements.flexStatement)
    populateThemeGroup(flexQueryResponse.flexStatements, insiderThemes)
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
        openPosition.baseMoney = cashReportCurrency.endingCash
        openPositions.add(openPosition)
    }
    return openPositions
}

fun populateThemeGroup(flexStatements: FlexStatements, themeSet: Set<String>) {
    for (statement in flexStatements.flexStatement.listIterator()) {
        for (position in statement.openPositions.openPosition.listIterator()) {
            if ("CASH".equals(position.themeName, ignoreCase = true)) {
                position.themeGroup = "other"
            } else if ("IRA".equals(position.themeName, ignoreCase = true)) {
                    position.themeGroup = "other"
            } else if (themeSet.contains(position.themeName)) {
                position.themeGroup = "insider"
            } else {
                position.themeGroup = "other"
            }
        }
    }
}
fun populateDescription(flexStatements: FlexStatements) {
    for (statement in flexStatements.flexStatement.listIterator()) {
        for (position in statement.openPositions.openPosition.listIterator()) {
            position.description = position.symbol!!.padEnd(5) + ": " + position.description
        }
    }
}
fun populateThemeName(flexStatements: FlexStatements, identityMap: Map<String, Theme>) {
    for (statement in flexStatements.flexStatement.listIterator()) {
        for (position in statement.openPositions.openPosition.listIterator()) {
            position.themeName = identityMap.get(position.symbol?.substringBefore(" ") + position.listingExchange)?.name
            if (position.themeName == "") {
                println("Theme Not Found for " + position.symbol)
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

fun createThemeSet(themeData: ThemeData): Set<String> {
    val themeSet = HashSet<String>()
    for (theme in themeData.themeData!!.listIterator()) {
        for (idenity in theme.identities) {
            themeSet.add(theme.name!!)
        }
    }
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


