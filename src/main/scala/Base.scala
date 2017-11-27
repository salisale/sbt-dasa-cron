import java.time.DayOfWeek

  object UserSpecType extends Enumeration {
    type UserSpecType = Value
    val MAILS, DAYS, FILTER = Value
  }

  object FilterType extends Enumeration {
    type FilterType = Value
    val AUTHOR, GENRE, KEYWORD = Value
  }

  case class User(emails: List[String], days: List[DayOfWeek], stockDays: List[DayOfWeek], filter: Filter)
  case class Author(fname: String, lname: String = "", mname: String = "") {
    def getSegmentedNames() = List(fname, lname, mname).filterNot(_ == "")
    override def toString: String = List(fname, mname, lname).filterNot(_.equals("")).mkString(" ")
  }
  case class Genre(genre: String, maxPrice: Option[Int])
  case class Filter(authors: List[Author], genres: List[Genre], keywords: List[String])
  case class Output(authorMap: Map[Author, List[Book]], genreMap: Map[Genre, List[Book]],
                    keywordMap: Map[String, List[Book]])



