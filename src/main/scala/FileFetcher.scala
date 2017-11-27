import java.io.File

import HttpDownloadUtility._

import org.jsoup.Jsoup
import org.jsoup.nodes.Element

import scala.collection.JavaConverters._

object FileFetcher extends App {

  def fetchDailyArrivals(dirPath: String): File = {
    val daily_update_key = "Daily Update"
    fetchFile(daily_update_key, dirPath).get // no else; exception would have been thrown
  }
  def fetchDatabase(dirPath: String): File = {
    val database_key = "'Dasa'Base"
    fetchFile(database_key, dirPath).get
  }

  protected def fetchFile(key: String, dirPath: String): Option[File] = {
    try {
      val doc = Jsoup.connect("http://www.dasabookcafe.com").get()
      val links = doc.select("a").asScala
      val filLinks = links.filter(_.text().equalsIgnoreCase(key))
      if (filLinks.isEmpty) { // in case their link names change
        CronException.sendMail("File download error: link of " + key + " is empty")
      }
      Some(downloadXLSFile(filLinks.head, dirPath).get)
    } catch {
      case e: Throwable => e.printStackTrace()
        None
    }
  }

  protected def downloadXLSFile(pageLink: Element, dirPath: String): Option[File] = {
    try {
      val doc = Jsoup.connect(pageLink.absUrl("href")).get()
      val links = doc.select("a").asScala

      val filFileLinks = links.filter(_.text().contains("xls"))
      if (filFileLinks.isEmpty) { // in case their link names change
        CronException.sendMail("File download error: page contains no link with .xls")
      }
      Some(downloadFile(filFileLinks.head.absUrl("href"), dirPath))
    } catch {
        case e: Throwable => e.printStackTrace()
        None
    }
  }

}
