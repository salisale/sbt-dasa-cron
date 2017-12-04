import java.time.DayOfWeek

import org.joda.time.{DateTimeConstants, LocalDate}

import scala.io.Source


object SpecFileParser extends App{

  def load(filename: String): User = {

    // Handle exception--this is dangerous
    def loadHelper[T](lines: List[String], mails: List[String], days: List[DayOfWeek], stockDays: List[DayOfWeek],
                      filter: Filter): User = lines match {
      case Nil => User(mails, days, stockDays, filter)
      case h::t =>
        if (h.startsWith("1.")) {
          val inMails = parseMails(t.take(1).head) // 1 line only
          loadHelper(t.drop(1), inMails, days, stockDays, filter)
        } else if (h.startsWith("2.")) {
          val inDays = parseDays(t.take(1).head) // 1 line only
          loadHelper(t.drop(1), mails, inDays, stockDays, filter)
        } else if (h.startsWith("3.")) {
          val inStockDays = parseDays(t.take(1).head)
          loadHelper(t.drop(1), mails, days, inStockDays, filter)
        } else {
          val inFilter = parseFilter(t)
          loadHelper(Nil, mails, days, stockDays, inFilter)
        }
    }
    val lines = Source.fromFile(filename).getLines().toList
              .filterNot(_.isEmpty).map(_.trim) // remove blank lines and trim all
    loadHelper(lines, Nil, Nil, Nil, Filter(Nil, Nil, Nil))
  }
  def parseMails(str: String): List[String] = {
    str.split(",").map(_.trim).toList
  }
  def parseDays(str: String): List[DayOfWeek] = {
    str.split(",").map(_.trim().toLowerCase match {
      case "monday" => Some(DayOfWeek.MONDAY)
      case "tuesday" => Some(DayOfWeek.TUESDAY)
      case "wednesday" => Some(DayOfWeek.WEDNESDAY)
      case "thursday" => Some(DayOfWeek.THURSDAY)
      case "friday" => Some(DayOfWeek.FRIDAY)
      case "saturday" => Some(DayOfWeek.SATURDAY)
      case "sunday" => Some(DayOfWeek.SUNDAY)
      case _ => None
    }).toList.flatten
  }
  def parseFilter(lines: List[String]): Filter = {
    val cleanLines = lines.filterNot(_.isEmpty).map(_.trim()) // remove blank lines and trim all
    var count = 0
    val blocks = cleanLines.foldLeft[Map[Int, List[String]]](Map())((x, l) =>
                      if (l.head.isLetter) {
                        count+=1
                        x + (count -> List())
                      } else {
                        x.updated(count, l::x(count))
                      })
    val authors = getAuthors(blocks(1).reverse.map(_.substring(1).trim()))
    val genres = getGenres(blocks(2).reverse.map(_.substring(1).trim()))
    val keywords = blocks(3).reverse.map(_.substring(1).trim())
    Filter(authors, genres, keywords)
  }

  def getAuthors(lines: List[String]): List[Author] = {
    lines.map(x => toAuthor(x.split("\\s+")))
  }

  def getGenres(lines: List[String]): List[Genre] = {
    lines.map(x => toGenre(x.split(",\\s*")))
  }

  def toGenre(arr: Array[String]): Genre = arr.length match {
    case 1 => Genre(arr(0), None)
    case 2 => Genre(arr(0), Some(arr(1).toInt)) // god forbids user fucks up
  }

  def toAuthor(arr: Array[String]): Author = arr.length match {
    case 1 => Author(arr(0))
    case 2 => Author(arr(0), arr(1))
    case 3 => Author(arr(0), arr(2), arr(1))
    case _ => Author("") // No, tell user that?
  }

}
