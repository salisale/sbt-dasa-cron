

object EntryQuery extends App{
  def filter(entries: List[Book], filter: Filter) = {
    val userAuthorList = filter.authors
    val userGenreList = filter.genres
    val userKeyList = filter.keywords

    // return List in case of collaboration; pitfall of this method being
    // "b, d" will be the filtered result of "a, b/ c, d"
    def authorFiltered(entry: Book): List[(Author, Book)] = {
      val filAuthors = userAuthorList.filter(_.getSegmentedNames().diff(entry.getSplitNameList()).isEmpty)
      if (filAuthors.isEmpty) Nil else filAuthors.map(x => (x, entry))
    }
    // return List because b might be in both "Philosophy" and "Philosophy, 220"
    def genreFiltered(entry: Book): List[(Genre, Book)] = {
      val filGenres = userGenreList.filter(g => g.genre.equalsIgnoreCase(entry.genre)
        && checkPrice(entry.price, g.maxPrice))
      if (filGenres.isEmpty) Nil else filGenres.map(x => (x, entry))
    }
    def checkPrice(entryPrice: Int, opPrice: Option[Int]): Boolean = opPrice match {
      case None => true // no price limit, always true
      case Some(x) => entryPrice <= x
    }
    // to implement: multiple-word key
    def titleFiltered(entry: Book): List[(String, Book)] = {
      val words = entry.titleStr.split("[,;:/()'-/&]\\s*|\\s+")
      val filKeys = userKeyList.filter(words.contains(_))
      if (filKeys.isEmpty) Nil else filKeys.map(x => (x, entry))
    }
    def filterHelper(entries: List[Book], filAuth: List[(Author, Book)], filGenre: List[(Genre, Book)],
                     filKey: List[(String, Book)]): (List[(Author, Book)], List[(Genre, Book)], List[(String, Book)])
    = entries match {
      case Nil => (filAuth, filGenre, filKey)
      case h::t => filterHelper(t, filAuth++authorFiltered(h), filGenre++genreFiltered(h),
        filKey++titleFiltered(h))
    }
    def listToMap[T](lst: List[(T, Book)]) = {
      lst.groupBy(_._1).mapValues(_.map(_._2))
    }

    val (authorList, genreList, keyList) = filterHelper(entries, List(), List(), List())

    Output(listToMap(authorList), listToMap(genreList), listToMap(keyList))
  }
}


/** NAIVE FILTERING
def authorFilter(entries: List[Book], authors: List[Author]): Map[Author, List[Book]] = {
    val filteredBooks =
      for (
        entry <- entries;
        fil <- authors
        if entry.authorStr.contains(fil.fname) && // John Reynold != John Reynolds
          entry.authorStr.contains(fil.lname) &&
          entry.authorStr.contains(fil.mname) // mname? // not okay, bach!=bachnarach
      ) yield (fil, entry)
    filteredBooks.groupBy(_._1).mapValues(_.map(_._2))

    //for ((k,v) <- filteredBooks) println(s"key: $k, value: $v")
  }
  def titleFilter(entries: List[Book], titles: List[String]): Map[String, List[Book]] = { // should be list of list of string
    val filteredBooks =
      for (
        entry <- entries;
        fil <- titles;
        eachKey <- entry.titleStr.split("[,;:/()'-/&]\\s*|\\s+")
          if eachKey.equalsIgnoreCase(fil)
      ) yield (fil , entry)
    filteredBooks.groupBy(_._1).mapValues(_.map(_._2))
  }

  def genreFilter(entries: List[Book], genres: List[Genre]): Map[Genre, List[Book]] = {
    def checkPrice(entryPrice: Int, opPrice: Option[Int]): Boolean = opPrice match {
      case None => true // no price limit, always true
      case Some(x) => entryPrice <= x
    }
    val filteredBooks =
      for (
        entry <- entries;
        fil <- genres
        if entry.genre.equalsIgnoreCase(fil.genre) &&
          checkPrice(entry.price, fil.maxPrice)
      ) yield (fil, entry)
    filteredBooks.groupBy(_._1).mapValues(_.map(_._2))
  }
  **/