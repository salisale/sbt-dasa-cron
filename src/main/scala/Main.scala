import java.io.File
import java.nio.file.{Files, Paths}
import java.time.{DayOfWeek, LocalDate}


object Main extends App {

  def runDasaBase(user: User, userName: String) = {
    def processDasaBase(dbFile: String, excelOutputFile: String, filter: Filter): File = {
      val entries = ExcelReader.parseDasaBase(dbFile) // retrieve all entries
      val output = EntryQuery.filter(entries, filter) // filtered entries
      ExcelOutput.writeAllToExcel(excelOutputFile, output)
    }
    def getExcelOutputFilePath(dbFileName: String) = "filtered_" + userName + "_" + dbFileName
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
  def runDailyArrivals(user: User, userName: String) = {
    def processDailyArrival(dailyFile: String, excelOutputFile: String, filter: Filter, newExcel: Boolean) = {
      // getLastProcessedDate() will return the day before yesterday if temp excel does not exist
      // assuming last noti day was yesterday
      val lastDateProcessed = ExcelDailyTemp.getLastProcessedDate(getDailyTempFilePath(userName))
      val (entries, newLastDateProcessed) = ExcelReader.parseDailyArrivals(dailyFile, lastDateProcessed)
      val output = EntryQuery.filter(entries, filter)
      ExcelOutput.writeDailyToExcel(excelOutputFile, output, newLastDateProcessed, newExcel)
    }

    val dailyFile = FileFetcher.fetchDailyArrivals(getDirPath())
    val excelOutputFile = getDailyTempFilePath(userName)

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
  mainRun()


  def mainRun() = {
    val allUserFiles = getAllUserFiles()
    for (file <- allUserFiles) {
      val thread = new Thread { // use concurrent run
        override def run {
          println(s"sRunning user: ${file.getAbsolutePath}")
          val user = SpecFileParser.load(file.getAbsolutePath)
          val (emails, daysOfWeek, stockDays, filter) = (user.emails, user.days, user.stockDays, user.filter)
          val userName = getUserName(file.getName) // to separate user temp and filtered files
          runDailyArrivals(user, userName) // runs everyday
          if (emailNotiToday(stockDays)) { // if stock noti day
            println("This is Stock day...processing dasabase")
            runDasaBase(user, userName)
          } else {
            println("This is not Stock day...end")
          }
        }
      }
      thread.start()
    }
  }


  def getDirPath() = {
    val dirPath = System.getProperty("user.home") + File.separator + "dasacron" + File.separator
    val dir = new File(dirPath)
    if (!dir.exists()) dir.mkdir()
    dir.getAbsolutePath
  }
  def getDailyTempFilePath(userName: String) = {
    getDirPath() + File.separator + "dailytemp-" + userName + ".xls"
  }
  def getAllUserFiles(): List[File] = {
    val d = new File(getDirPath)
    if (d.exists && d.isDirectory) {
      d.listFiles.filter(_.isFile).filter(_.getName.endsWith(".txt")).toList
    } else {
      List[File]()
    }
  }
  // if userSpecFile does not specify user's name, it takes the whole string
  // and filename of userSpecFile should be changed only on Email Noti day
  def getUserName(fileName: String) = {
    fileName.substring(fileName.lastIndexOf("-") + 1, fileName.lastIndexOf("."))
  }

}
