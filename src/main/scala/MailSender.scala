import java.nio.file.Paths
import java.util.Properties
import javax.activation.{DataHandler, FileDataSource}
import javax.mail._
import javax.mail.internet.{MimeBodyPart, MimeMessage, MimeMultipart}

object MailSender extends App {

  def getSession(): Session = {
    val user = "salisadasa@gmail.com"
    val pass = "Gese11schaft"

    val props = new Properties
    props.put("mail.smtp.auth", "true")
    props.put("mail.smtp.starttls.enable", "true")
    props.put("mail.smtp.host", "smtp.gmail.com")
    props.put("mail.smtp.port", "587")

    Session.getInstance(props,
      new javax.mail.Authenticator() {
        protected override def getPasswordAuthentication = {
          new PasswordAuthentication(user, pass)
        }
      })
  }
  def sendDailyArrivalsMail(mails: List[String], htmlStr: String) = {
    val session = getSession()
    try {

      val message = new MimeMessage(session)
      message.addRecipients(Message.RecipientType.CC, mails.mkString(","))
      message.setSubject("Daily Arrivals Notification")
      message.setText(htmlStr, "utf-8", "html")

      Transport.send(message)

      println("Mail Sent.\nDone.")

    } catch {
      case e: MessagingException=> throw new RuntimeException(e)
    }

  }

  def sendExceptionReportMail(mails: List[String], errMessage: String) = {
    val session = getSession()
    try {

      val message = new MimeMessage(session)
      message.addRecipients(Message.RecipientType.CC, mails.mkString(","))
      message.setSubject("Dasa Cron Job Exception")
      message.setText(errMessage)

      Transport.send(message)
      println("Mail Sent.\nDone.")
    } catch {
      case e: MessagingException=> throw new RuntimeException(e)
    }

  }

  def sendStockMailWithExcel(mails: List[String], attachedFilePath: String) = {
    println(s"Sending mail with stock: attachedFilePath is $attachedFilePath")
    val session = getSession()
    try {

      val message = new MimeMessage(session)
      message.addRecipients(Message.RecipientType.CC, mails.mkString(","))
      message.setSubject("Weekly Stock Notification")

      val multipart = new MimeMultipart()

      val textPart = new MimeBodyPart() // set message body
      textPart.setText("Filtered entries: " + Paths.get(attachedFilePath).getFileName)
      multipart.addBodyPart(textPart)

      val filePart = new MimeBodyPart() // set attachement
      val source = new FileDataSource(attachedFilePath)
      filePart.setDataHandler(new DataHandler(source))
      filePart.setFileName(attachedFilePath)
      multipart.addBodyPart(filePart)

      message.setContent(multipart)

      Transport.send(message)

      System.out.println("Mail Sent.\nDone.")

    } catch {
      case e: MessagingException=> throw new RuntimeException(e)
    }

  }

}
