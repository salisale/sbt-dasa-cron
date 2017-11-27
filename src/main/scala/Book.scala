
case class Book(id: Int, authorStr: String, titleStr: String,
            genre: String, price: Int) {
  /**
    * Split individual name(s) of author into List e.g. List("fname", "middlename", "lname")
    * and while there is a possiblity that one of the strings will be "Editor",
    * it will not change the correctness our filtering method
    */
  def getSplitNameList(): List[String] = {
    authorStr.split("[/&,() ]").map(_.trim).toList.filterNot(_ == "")
  }
  def getHTMLStr() = {
    val names = authorStr.split(", ").map(_.trim()).reverse
    val titles = titleStr.split(", ").reverse
    "<p><pre>      "+names.mkString(" ")+" &loz; <b><i>"+titles.mkString(" ")+"</i></b> ["+price+"B] </pre></p>"
  }
  override def toString() = {
    val names = authorStr.split(", ").reverse
    val titles = titleStr.split(", ").reverse
    names.mkString(" ")+": "+titles.mkString(" ")+" ["+price+"] " + " in " + genre
  }
}

