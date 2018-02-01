import java.io.{File, FileInputStream}
import java.util.Date

import org.apache.poi.hssf.usermodel.{HSSFSheet, HSSFWorkbook}
import org.apache.poi.ss.usermodel.{Cell, DataFormatter, Row}
import org.joda.time.{DateTime, LocalDate}

import scala.collection.mutable.ListBuffer
import scala.collection.JavaConverters._

object ExcelReader extends App {

  /**
    * Process all entries after the date that's been processed last time
    * @param filePath
    * @param lastDateProcessed
    * @return (entries of or after the date specified, last date that entries has been processed)
    */
  def parseDailyArrivals(filePath: String, lastDateProcessed: DateTime): (List[Book], DateTime) = {
    println(s"parseDailyArrivals in ExcelReader: filepath is $filePath")
    var entryList = new ListBuffer[Book]
    var newLastDateProcessed = lastDateProcessed // last batch of entry
    try {
      val file = new File(filePath)
      val workbook = new HSSFWorkbook(new FileInputStream(file))
      val sheet = workbook.getSheetAt(0)
      val rowIterator = sheet.rowIterator().asScala
      var processEntry = false // flagged as true when input date > lastDateProcessed

      for (row <- rowIterator) {
        val cellCount = row.getPhysicalNumberOfCells
        if (cellCount == 1 && row.getCell(0) != null) { // date
          val thisInstant = row.getCell(0).getDateCellValue.toInstant
          val dateEntry = new DateTime(Date.from(thisInstant))
          if (dateEntry.compareTo(lastDateProcessed) > 0) {
            processEntry = true
            newLastDateProcessed = dateEntry
          }
        } else { // entry
          if (processEntry) {
            val id = getBookID(row.getCell(0))
            val rawAuthor = row.getCell(1).getStringCellValue
            val rawTitle = getBookTitle(row.getCell(2))
            val genre = row.getCell(3).getStringCellValue
            val price = row.getCell(4).getNumericCellValue.toInt
            val b = Book(id, rawAuthor, rawTitle, genre, price)
            entryList += b
          }
        }
      }
    } catch {
      case e: Throwable => CronException.sendMail(e.getMessage)
    }
    println(s"\t[Incoming Books: ${entryList.size}]")
    (entryList.toList, newLastDateProcessed)
  }

  /**
    * @param filePath
    * @return all entries
    */
  def parseDasaBase(filePath: String): List[Book] = {
    println(s"parseDasaBase in ExcelReader: filepath is $filePath")
    val file = new File(filePath)
    val workbook = new HSSFWorkbook(new FileInputStream(file))
    val sheet = workbook.getSheetAt(0) // get first sheet from the workbook
    val rowIterator = sheet.rowIterator().asScala
    rowIterator.next() // skip header for 'Dasa'Base file

    var entryList = new ListBuffer[Book]

    for (row <- rowIterator) {
      val cellCount = row.getPhysicalNumberOfCells
      val cellLast = row.getLastCellNum

      if (cellCount==cellLast) { // book-entry; weed out bottom-trailing rows
                                // with weird behavior e.g. cellCount=1,cellLast=5
        val id = getBookID(row.getCell(0))
        val rawAuthor = getAuthorStr(row.getCell(1))
        val rawTitle = getBookTitle(row.getCell(2))
        val genre = row.getCell(3).getStringCellValue
        val price = row.getCell(4).getNumericCellValue.toInt
        val b = Book(id, rawAuthor, rawTitle, genre, price)
        entryList += b
      }
    }
    println(s"\t[Incoming Books: ${entryList.size}]")
    entryList.toList
  }

  /**
    *  Good God, author can be a number
    */
  protected def getAuthorStr(cell: Cell) = cell.getCellType match {
    case Cell.CELL_TYPE_STRING => cell.getStringCellValue
    case _ => {
      val df = new DataFormatter()
      df.formatCellValue(cell)
    }
  }

  protected def getBookID(cell: Cell) = cell.getCellType match {
    case Cell.CELL_TYPE_STRING => cell.getStringCellValue.toInt
    case Cell.CELL_TYPE_NUMERIC => cell.getNumericCellValue.toInt
    case _ => 0 // this case should not happen
  }

  /**
    * Get correct string-format of book title
    * e.g. "11/22/63" is set as Date Type
    */
  protected def getBookTitle(cell: Cell) = cell.getCellType match {
    case Cell.CELL_TYPE_STRING => cell.getStringCellValue
    case _ => {
        // Use this method instead of setting cell-type to String Type
        // so 11/22/63 won't be converted to 23337 or 500.00 to 500
        val df = new DataFormatter() // get cell format
        df.formatCellValue(cell)
      }
  }

  /**
    * Not used
    * @return (tuples mapping each date to its first entry [i.e. ID],
    *         all book entries)
    */
  def parseAllDailyArrivals(filePath: String): (List[(Int, LocalDate)], List[Book]) = {
    val file = new File(filePath)
    val workbook = new HSSFWorkbook(new FileInputStream(file))
    val sheet = workbook.getSheetAt(0) // get first sheet from the workbook
    val rowIterator = sheet.rowIterator().asScala

    var beginIDDateList = new ListBuffer[(Int, LocalDate)] // (BeginningID, Date)
    var entryList = new ListBuffer[Book]

    for (row <- rowIterator) {
      val cellCount = row.getPhysicalNumberOfCells
      // val cellLast = row.getLastCellNum
      if (cellCount==1 && row.getCell(0)!=null) { // date
        val thisInstant = row.getCell(0).getDateCellValue.toInstant
        val date = new DateTime(Date.from(thisInstant)).toLocalDate
        val tup = (getCellBelow(sheet, row), date)
        beginIDDateList += tup // append to date record
      } else { // if cellCount=cellLast for safe parsing?
        val id = getBookID(row.getCell(0))
        val rawAuthor = row.getCell(1).getStringCellValue
        val rawTitle = getBookTitle(row.getCell(2))
        val genre = row.getCell(3).getStringCellValue
        val price = row.getCell(4).getNumericCellValue.toInt
        val b = Book(id, rawAuthor, rawTitle, genre, price)
        entryList += b
      }

    }
    beginIDDateList.map(tup => printTup(tup))
    (beginIDDateList.toList, entryList.toList)
  }
  protected def printTup(tups: (Int, LocalDate)) = {
    println(tups._1, " => ", tups._2)
  }
  /**
    * Called after date-row is processed to get the beginning ID of that date
    */
  protected def getCellBelow(sheet: HSSFSheet, row: Row): Int =
    sheet.getRow(row.getRowNum+1).getCell(0).getNumericCellValue.toInt

}


