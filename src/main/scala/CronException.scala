object CronException extends App {
  def sendMail(errorMessage: String) = {
    val specFile = Main.getDirPath() + "/DasaCronFile.txt" // don't hard-code textfile
    val mails = SpecFileParser.load(specFile).emails // if this throws exception, oopsie
    MailSender.sendExceptionReportMail(mails, errorMessage)
    println("Sending error mail")
  }
}