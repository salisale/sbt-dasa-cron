import java.io.File
import java.nio.file.{Files, Paths}
import java.time.{DayOfWeek, LocalDate}


object Main extends App {

  def runDasaBase(user: User) = {
    def processDasaBase(dbFile: String, excelOutputFile: String, filter: Filter): File = {
      val entries = ExcelReader.parseDasaBase(dbFile) // retrieve all entries
      val output = EntryQuery.filter(entries, filter) // filtered entries
      ExcelOutput.writeAllToExcel(excelOutputFile, output)
    }
    def getExcelOutputFilePath(dbFileName: String) = getDirPath() + File.separator + "filtered_" + dbFileName
    val dbFile = FileFetcher.fetchDatabase(getDirPath())
    val excelOutputFilePath = getExcelOutputFilePath(dbFile.getName)

    val excel = processDasaBase(dbFile.getAbsolutePath, excelOutputFilePath, user.filter)

    MailSender.sendStockMailWithExcel(user.emails, excel.getAbsolutePath)

    dbFile.delete() // delete downloaded excel
    excel.delete() // delete filtered excel
  }

  /**
    * Run every time the script is executed i.e. run every day
    */
  def runDailyArrivals(user: User) = {
    def processDailyArrival(dailyFile: String, excelOutputFile: String, filter: Filter, newExcel: Boolean) = {
      // getLastProcessedDate() will return yesterday if temp excel does not exist
      // assuming last noti day was yesterday
      val lastDateProcessed = ExcelDailyTemp.getLastProcessedDate(getDailyTempFilePath())
      val (entries, newLastDateProcessed) = ExcelReader.parseDailyArrivals(dailyFile, lastDateProcessed)
      val output = EntryQuery.filter(entries, filter)
      ExcelOutput.writeDailyToExcel(excelOutputFile, output, newLastDateProcessed, newExcel)
    }

    val dailyFile = FileFetcher.fetchDailyArrivals(getDirPath())
    val excelOutputFile = getDailyTempFilePath()

    if (emailNotiToday(user.days)) {
      println("This is Email Noti day...")
      // get temp daily arrivals file
      val tempExcelFile = if (!Files.exists(Paths.get(excelOutputFile)))
        processDailyArrival(dailyFile.getAbsolutePath, excelOutputFile, user.filter, true) // new daily file
      else
        processDailyArrival(dailyFile.getAbsolutePath, excelOutputFile, user.filter, false) // append to old daily temp file

      val HTMLStr = ExcelDailyTemp.parseExcel(tempExcelFile) // get HTML ready to send

      MailSender.sendDailyArrivalsMail(user.emails, HTMLStr) // send file

      tempExcelFile.delete() // delete temp file

    } else { // Not noti-day, append to existing temp file
      println("This is not Email Noti day...")
      if (!Files.exists(Paths.get(excelOutputFile)))
        processDailyArrival(dailyFile.getAbsolutePath, excelOutputFile, user.filter, true) // new daily file
      else
        processDailyArrival(dailyFile.getAbsolutePath, excelOutputFile, user.filter, false) // append to old daily temp file
    }

    dailyFile.delete() // delete downloaded file
  }
  protected def emailNotiToday(daysOfWeek: List[DayOfWeek]): Boolean = {
    val today = LocalDate.now().getDayOfWeek
    daysOfWeek.contains(today)
  }

  println("Main class running")
   run()

  def run() = {
    val specFile = getDirPath() + "/DasaCronUserFile.txt"
    val user = SpecFileParser.load(specFile)
    val (emails, daysOfWeek, stockDays, filter) = (user.emails, user.days, user.stockDays, user.filter)

    runDailyArrivals(user) // runs everyday
    if (emailNotiToday(stockDays)) { // if stock noti day
      println("This is Stock day...processing dasabase")
      runDasaBase(user)
    } else {
      println("This is not Stock day...end")
    }

  }
  def getDirPath() = {
    val dirPath = System.getProperty("user.home") + File.separator + "dasacron" + File.separator
    val dir = new File(dirPath)
    if (!dir.exists()) dir.mkdir()
    dir.getAbsolutePath
  }
  def getDailyTempFilePath() = {
    getDirPath() + File.separator + "dailytemp.xls"
  }

}
