import java.io.{File, FileInputStream, FileOutputStream}
import java.time.LocalDate

import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.apache.poi.ss.usermodel.Sheet
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

object ExcelOutput extends App {
  def writeDailyToExcel(filePath: String, output: Output, lastDateProcessed: DateTime, newFile: Boolean): File = {
    if (newFile) {
      val workbook = new HSSFWorkbook()
      writeDailyNewFile(workbook, output, filePath, lastDateProcessed)
    } else {
      val file = new File(filePath)
      try {
        val fis = new FileInputStream(file) // ensure that file exists
        val workbook = new HSSFWorkbook(fis); // what if not exists? handle it
        fis.close()
        writeDailyExistingFile(workbook, output, filePath, lastDateProcessed)
      } catch {
        case e: Throwable => CronException.sendMail(e.getMessage)
      }
      file
    }
  }

  protected def writeDailyNewFile(workbook: HSSFWorkbook, output: Output, filePath: String, lastDateProcessed: DateTime): File = {
    def getDateCellStyle(workbook: HSSFWorkbook) = {
      val dateStyle = workbook.createCellStyle()
      val createHelper = workbook.getCreationHelper()
      val dateFormat = createHelper.createDataFormat().getFormat("dd/MM/yyyy")
      dateStyle.setDataFormat(dateFormat)
      dateStyle
    }
    println("New File: dailytemp.xls")

    val sheet1 = workbook.createSheet("Author")

    // first row records the interval of date processed
    val dateStyle = getDateCellStyle(workbook)
    val firstCell = sheet1.createRow(0).createCell(0) // first cell records the beginning
    firstCell.setCellStyle(dateStyle)
    firstCell.setCellValue(lastDateProcessed.toDate)
    val secondCell = sheet1.getRow(0).createCell(1) // second cell records the most recent date
    secondCell.setCellStyle(dateStyle)
    secondCell.setCellValue(lastDateProcessed.toDate)

    // Fill Author Sheet
    val authorList = mapToList(output.authorMap)
    val authorHeader = sheet1.createRow(2).createCell(0).setCellValue("Author Filters: ")
    writeEntryHelper(authorList, 3, sheet1)
    autoSizeCol(sheet1) // adjust col to wrap text

    // Fill Genre Sheet
    val sheet2 = workbook.createSheet("Genre")
    sheet2.createRow(0).createCell(0).setCellValue("Genre Filters: ")
    val genreList = mapToList(output.genreMap)
    writeEntryHelper(genreList, 1, sheet2)
    autoSizeCol(sheet2) // adjust col to wrap text

    // Fill Keyword Sheet
    val sheet3 = workbook.createSheet("Keyword")
    sheet3.createRow(0).createCell(0).setCellValue("Keyword Filters: ")
    val keywordList = mapToList(output.keywordMap)
    writeEntryHelper(keywordList, 1, sheet3)
    autoSizeCol(sheet3) // / adjust col to wrap text

    val file = new File(filePath)
    try {
      val fos = new FileOutputStream(file)
      workbook.write(fos)
      fos.flush()
      fos.close()
    } catch {
      case e: Throwable => CronException.sendMail(e.getMessage)
    }
    file
  }
  def writeDailyExistingFile(workbook: HSSFWorkbook, output: Output, filePath: String, lastDateProcessed: DateTime): File = {
    println("Existing File: dailytemp.xls")
    val sheet1 = workbook.getSheetAt(0)
    val sheet2 = workbook.getSheetAt(1)
    val sheet3 = workbook.getSheetAt(2)

    val authorList = mapToList(output.authorMap)
    writeEntryHelper(authorList, sheet1.getLastRowNum+1, sheet1) // append to last row
    autoSizeCol(sheet1) // adjust col to wrap text

    val genreList = mapToList(output.genreMap)
    writeEntryHelper(genreList, sheet2.getLastRowNum+1, sheet2)
    autoSizeCol(sheet2) // adjust col to wrap text

    val keywordList = mapToList(output.keywordMap)
    writeEntryHelper(keywordList, sheet3.getLastRowNum+1, sheet3)
    autoSizeCol(sheet3) // adjust col to wrap text

    // update recent date
    val secondCell = sheet1.getRow(0).getCell(1) // second cell records the most recent date
    secondCell.setCellValue(lastDateProcessed.toDate)

    val file = new File(filePath)
    val fos =new FileOutputStream(file);  //Open FileOutputStream to write updates
    workbook.write(fos); //write changes
    fos.flush()
    fos.close();  //close the stream
    file
  }

  def writeAllToExcel(filePath: String, output: Output) = {
    // create new Excel
    val file = new File(filePath)
    val workbook = new HSSFWorkbook()

    // Fill Author Sheet
    val sheet1 = workbook.createSheet("Author")
    val firstCell = sheet1.createRow(0).createCell(0)
    val today = DateTime.now.toString(DateTimeFormat.fullDate())
    firstCell.setCellValue("Generated: " + today) // first cell records today's date

    val authorList = mapToList(output.authorMap)

    val authorHeader = sheet1.createRow(2).createCell(0).setCellValue("Author Filters: ")
    writeEntryHelper(authorList, 3, sheet1)
    autoSizeCol(sheet1) // adjust col to wrap text

    // Fill Genre Sheet
    val sheet2 = workbook.createSheet("Genre")
    sheet2.createRow(0).createCell(0).setCellValue("Genre Filters: ")
    val genreList = mapToList(output.genreMap)
    writeEntryHelper(genreList, 1, sheet2)
    autoSizeCol(sheet2) // adjust col to wrap text

    // Fill Keyword Sheet
    val sheet3 = workbook.createSheet("Keyword")
    sheet3.createRow(0).createCell(0).setCellValue("Keyword Filters: ")
    val keywordList = mapToList(output.keywordMap)
    writeEntryHelper(keywordList, 1, sheet3)
    autoSizeCol(sheet3) // / adjust col to wrap text

    try {
      // Write to file
      val fos = new FileOutputStream(file)
      workbook.write(fos)
      fos.flush()
      fos.close()
    } catch {
      case e: Throwable => CronException.sendMail(e.getMessage)
    }
    file
  }

  protected def mapToList[T](map: Map[T, List[Book]]) = {
    map.view.map { case (k, v) => (k, v) }.toList
  }

  protected def autoSizeCol(sheet: Sheet) = (0 to 5).toList.map(x => sheet.autoSizeColumn(x))

  protected def writeEntryHelper[T](lst: List[(T, List[Book])], currRow: Int, sheet: Sheet): Sheet = lst match {
    case Nil => sheet
    case (x, books) :: t => {
      val filNameCell = sheet.createRow(currRow + 1).createCell(0)
      filNameCell.setCellValue(x.toString)
      val newSheet = writeIndividualEntry(books, currRow + 2, sheet)
      writeEntryHelper(t, newSheet._1, newSheet._2)
    }
  }

  protected def writeIndividualEntry[T](books: List[Book], currRow: Int, sheet: Sheet): (Int, Sheet) = books match {
    case Nil => (currRow, sheet)
    case h :: t => {
      val row = sheet.createRow(currRow)
      row.createCell(0).setCellValue(h.id)
      row.createCell(1).setCellValue(h.authorStr)
      row.createCell(2).setCellValue(h.titleStr)
      row.createCell(3).setCellValue(h.genre)
      row.createCell(4).setCellValue(h.price)
      row.createCell(5).setCellValue("baht")
      writeIndividualEntry(t, currRow + 1, sheet)
    }

  }
}
