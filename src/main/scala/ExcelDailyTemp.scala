import java.io.{File, FileInputStream}
import java.util.Date

import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.apache.poi.ss.usermodel.{Cell, DataFormatter, Sheet}
import org.joda.time.format.DateTimeFormat
import org.joda.time.{DateTime}

import scala.collection.JavaConverters._
import scala.collection.mutable

object ExcelDailyTemp extends App {
  /**
    * @param file - temporary daily arrivals file
    * @return HTML string for all daily arrivals
    */
  def parseExcel(file: File): String = {
    val workbook = new HSSFWorkbook(new FileInputStream(file))
    val sheet1 = workbook.getSheetAt(0)
    val sheet2 = workbook.getSheetAt(1)
    val sheet3 = workbook.getSheetAt(2)

    val (beginDate, endDate) = getProcessedDateInterval(sheet1)
    val entryMapAuthor = readSheet(sheet1, 2) // start parsing at 3rd non-blank row
    val entryMapGenre = readSheet(sheet2, 1) // start parsing at 2nd non-blank row
    val entryMapKeyword = readSheet(sheet3, 1)

    val t1 = getHTMLDisplayStr(entryMapAuthor)
    val t2 = getHTMLDisplayStr(entryMapGenre)
    val t3 = getHTMLDisplayStr(entryMapKeyword)
    val noMatches = t1.isEmpty && t2.isEmpty && t3.isEmpty
    generateHTML(beginDate, endDate, t1, t2, t3, noMatches)
  }
  protected def getHTMLDisplayStr(entryMap: mutable.MultiMap[String, Book]) = { // what if empty? exception
  val lst = entryMap.toList.sortBy(_._1)
    if (lst.isEmpty) ""
    else lst.map(b => "<pre>" + b._1 + "</pre>" + b._2.map(_.getHTMLStr()).mkString("")).mkString("<br/>") + "<br/>"
  }

  protected def generateHTML(beginDate: Date, endDate: Date, text1: String, text2: String, text3: String, noMatches: Boolean) = {
    def getFormattedDate(d: Date) = new DateTime(d).toString(DateTimeFormat.fullDate())
    val dateStr = beginDate.equals(endDate) match {
      case true => ": " + getFormattedDate(beginDate)
      case _ => "<br/>  From: " + getFormattedDate(beginDate) + "<br/>  To: " + getFormattedDate(endDate)
    }
    val body = noMatches match {
      case true => "No incoming entry matches your preference"
      case _ => text1 + text2 + text3
    }
    "<html><body><pre>Your Book Arrivals " + dateStr + "<br/><br/>" + body+ "</pre></body></html>"
  }

  /**
    * Called in Main to retrieve last date processed
    * file is checked to have existed
    */
  def getLastProcessedDate(filePath: String): DateTime = {
    val file = new File(filePath)
    if (!file.exists()) {DateTime.now().minusDays(2)} // so that it processes one of yesterday
    else {
      val workbook = new HSSFWorkbook(new FileInputStream(file))
      new DateTime(getProcessedDateInterval(workbook.getSheetAt(0))._2)
    }
  }

  protected def getProcessedDateInterval(sheet1: Sheet) = {
    val row = sheet1.getRow(0)
    (row.getCell(0).getDateCellValue, row.getCell(1).getDateCellValue)
  }

  protected def readSheet(sheet: Sheet, rowsToDrop: Int) = {
    val rowIterator = sheet.rowIterator().asScala
    rowIterator.drop(rowsToDrop) // drop 4 for sheet1; drop 2 for sheet2 and sheet3

    // use MultiMap which is: Map[K, Set[V]]
    var entryMap = new mutable.HashMap[String, mutable.Set[Book]] with mutable.MultiMap[String, Book]
    var lastFilStr = ""

    for (row <- rowIterator) {
      val lastCell = row.getLastCellNum // getPhyscialNumberOfCells works too
      if (lastCell==1) { // book-entry; weed out bottom-trailing rows
        val filStr = row.getCell(0).getStringCellValue.trim()
        lastFilStr = filStr
      } else {
        val id = getBookID(row.getCell(0))
        val rawAuthor = row.getCell(1).getStringCellValue
        val rawTitle = getBookTitle(row.getCell(2))
        val genre = row.getCell(3).getStringCellValue
        val price = row.getCell(4).getNumericCellValue.toInt
        val b = Book(id, rawAuthor, rawTitle, genre, price)
        entryMap.addBinding(lastFilStr, b)
      }
    }
    entryMap
  }
  protected def getBookID(cell: Cell) = cell.getCellType match {
    case Cell.CELL_TYPE_STRING => cell.getStringCellValue.toInt
    case Cell.CELL_TYPE_NUMERIC => cell.getNumericCellValue.toInt
    case _ => 0 // this case should not happen
  }
  protected def getBookTitle(cell: Cell) = cell.getCellType match {
    case Cell.CELL_TYPE_STRING => cell.getStringCellValue
    case _ => {
      // Use this method instead of setting cell-type to String Type
      // so 11/22/63 won't be converted to 23337 or 500.00 to 500
      val df = new DataFormatter() // get cell format
      df.formatCellValue(cell)
    }
  }

}
