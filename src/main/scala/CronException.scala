object CronException extends App {
  def sendMail(errorMessage: String) = {
    val mails = List("salisaletheia@gmail.com") // inform me; if this throws an exception, oopsie
    MailSender.sendExceptionReportMail(mails, errorMessage)
    println("Sending error mail")
  }
}