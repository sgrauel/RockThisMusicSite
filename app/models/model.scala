package models

// include facilities for anorm
import anorm._
import anorm.SqlParser._
import play.api.db.DB
import play.api.Play.current

// import java Calendar utility and Time Zone object
import java.util.Calendar
import java.util.TimeZone

// import java mail v. 1.5.2 facilities for sending emails through Amazon's SMS SMTP servers
// NOTE: this package is not apart of java SE 7 stl, therefore it is included as an unmanaged dependecy in the '/lib' dir.
import java.util.Properties
import javax.mail._
import javax.mail.internet._

// typeclass that defines and describes the behavior of CRUD
trait CRUD[A] {
  def create(obj: A): Unit
  def read: List[A]
}

// companion object to CRUD typeclass to hold type instances
object CRUDinstances {

  // Contact type instance of CRUD typeclass
  val contactInstance = new CRUD[Contact] {

    // java.sql.connection object passed implicitly to executeUpdate from call to withConnection
    def create(obj: Contact): Unit = {
      DB.withConnection { implicit connection =>
        SQL("""
                INSERT INTO Contact(name,address,city,state,zip,country,phone,email,message)
                VALUES({name},{address},{city},{state},{zip},{country},{phone},{email},{message})
            """).on(
            'name -> obj.getName,
            'address -> obj.getAddress,
            'city -> obj.getCity,
            'state -> obj.getState,
            'zip -> obj.getZip,
            'country -> obj.getCountry,
            'phone -> obj.getPhone,
            'email -> obj.getEmail,
            'message -> obj.getMessage
          ).executeUpdate
        ()
      }
    }

    def read: List[Contact] =  {
      DB.withConnection { implicit c =>
        val selectFromContact = SQL("SELECT * FROM Contact;")
        val listOfRecords = (selectFromContact().map(row =>
          (row[Pk[Int]]("id"), row[String]("name"), row[String]("address"),
            row[String]("city"), row[String]("state"), row[Int]("zip"),
            row[String]("country"), row[String]("phone"), row[String]("email"),
            row[Option[String]]("message"))
        ).toList)
        val listOfContacts = listOfRecords.map(r => Contact(r._1,r._2,r._3,r._4,r._5,(r._6).toString,r._7,r._8,r._9,r._10))
        listOfContacts
      }
    }

  }

  // Feature type instance

  val featureInstance = new CRUD[Feature] {

    // bind Feature data to Feature model EER diagram
    def create(obj: Feature): Unit = {

      // java.sql.connection object passed implicitly to executeUpdate from call to withConnection
      DB.withConnection { implicit connection =>

        // Create an one-to-one associated Caption instance

        SQL(
          """
                INSERT INTO Caption(artistTitle,artistImage,captionText)
                VALUES({artistTitle},{artistImage},{captionText});
          """).on(
            'artistTitle -> obj.getCaption.getArtistTitle,
            'artistImage -> obj.getCaption.getArtistImage,
            'captionText -> obj.getCaption.getCaptionText
          ).executeUpdate


        // Create a one-to-one associated Intro instance

        SQL(
          """
                INSERT INTO Intro(introTitle,introductoryText)
                VALUES({introTitle},{introductoryText});
          """).on(
            'introTitle -> obj.getIntro.getIntroTitle,
            'introductoryText -> obj.getIntro.getIntroductoryText
          ).executeUpdate

        // get the 'caption_id' from the last record in the Caption table and 'intro_id' from the last record in the Intro table

        val getLastRecordFromCaptionByCaptionId = SQL("SELECT caption_id FROM Caption\nORDER BY caption_id DESC\nLIMIT 1;")
        val getLastRecordFromIntroByIntroId = SQL("SELECT intro_id FROM Intro\nORDER BY intro_id DESC\nLIMIT 1;")

        val caption_id: Int = getLastRecordFromCaptionByCaptionId().map(row =>
          (row[Pk[Int]]("caption_id"))).toList.apply(0).get


        val intro_id : Int = getLastRecordFromIntroByIntroId().map(row =>
          (row[Pk[Int]]("intro_id"))).toList.apply(0).get


        // Create a Feature instance
        SQL(
          """
                INSERT INTO Feature (caption_id,intro_id,date,time,audioPlayer)
                VALUES ({caption_id},{intro_id},{date},{time},{audioPlayer});
            """).on(
            'caption_id -> caption_id,
            'intro_id -> intro_id,
            'date -> obj.getDate,
            'time -> obj.getTime,
            'audioPlayer -> obj.getAudioPlayer
          ).executeUpdate


        // Create one-to-many associated BandVideo instances

        // get the last record to be inserted intro Feature by feature_id

        val getLastRecordFromFeatureByFeatureId = SQL("SELECT feature_id FROM Feature\nORDER BY feature_id DESC\nLIMIT 1;")

        val feature_id = getLastRecordFromFeatureByFeatureId().map(row =>
          (row[Pk[Int]]("feature_id"))).toList.apply(0).get


        for (bandVideo <- obj.getListOfBandVideos) {

          SQL(
            """
                INSERT INTO BandVideo(feature_id,bandVideoUrl,title)
                VALUES({feature_id},{bandVideoUrl},{title});
          """).on(
              'feature_id -> feature_id,
              'bandVideoUrl -> bandVideo.getBandVideoUrl,
              'title -> bandVideo.getTitle
          ).executeUpdate

        }

        // Create one-to-many Caption to Website instances

        for (i <- 0 to (obj.getCaption.getWebsite.getWebsiteNames.length - 1)) {

          var websiteURL: String = obj.getCaption.getWebsite.getWebsiteUrls(i)
          var websiteName: String = obj.getCaption.getWebsite.getWebsiteNames(i)

          SQL(
            """
                INSERT INTO Website(caption_id,websiteURL,websiteName)
                VALUES({caption_id},{websiteURL},{websiteName});
            """).on(
              'caption_id -> caption_id,
              'websiteURL -> websiteURL,
              'websiteName -> websiteName
            ).executeUpdate

        }

        ()
      }
    }

    def read: List[Feature] = {

      DB.withConnection { implicit c =>

        // read records for natural join between Feature, Caption and Intro tables
        val naturalJoinFeatureCaptionIntro = SQL("SELECT feature_id, DATE_FORMAT(date, '%Y-%m-%d') AS date, TIME_FORMAT(time,'%H:%i:%s') as time, audioPlayer, artistTitle, artistImage, captionText, introTitle, introductoryText FROM Feature NATURAL JOIN Caption NATURAL JOIN Intro;")
        val listOfFeatureCaptionIntroRecords = naturalJoinFeatureCaptionIntro().map(row =>
          (row[Pk[Int]]("feature_id"), row[String]("date"), row[String]("time"),
            row[String]("audioPlayer"), row[String]("artistTitle"), row[String]("artistImage"),
            row[String]("captionText"), row[String]("introTitle"), row[String]("introductoryText")
        )).toList

        // read records for natural join between Feature and BandVideo tables
        val naturalJoinFeatureBandVideo = SQL("SELECT feature_id, bandVideoUrl, title FROM Feature NATURAL JOIN BandVideo;")
        val listOfFeatureBandVideoRecords = naturalJoinFeatureBandVideo().map(row =>
          (row[Pk[Int]]("feature_id"), row[String]("bandVideoUrl"), row[String]("title")
            )).toList

        // read records for natural join between Feature and BandVideo tables
        val naturalJoinCaptionWebsite = SQL("SELECT caption_id, websiteURL, websiteName FROM Caption NATURAL JOIN Website; ")
        val listOfCaptionWebsiteRecords = naturalJoinCaptionWebsite().map(row =>
          (row[Pk[Int]]("caption_id"), row[String]("websiteURL"), row[String]("websiteName")
            )).toList


        // Feature (val date: String, val time: String, val intro: Intro, val caption: Caption, val audioPlayer: String, val listOfBandVideos: Seq[BandVideo])
        // Caption (val artistTitle: String, val artistImage: String, val captionText: String, val website: Website)
        // Website (val websiteURLs: Seq[String], val websiteNames: Seq[String])
        // BandVideo (val bandVideoUrl: String, val title: String)

        val listOfFeatures = listOfFeatureCaptionIntroRecords.map { r =>

          // process for obtaining Seq of BandVideos
          val bandVideoRecords = listOfFeatureBandVideoRecords.filter(b => b._1 == r._1)
          val listOfBandVideos = bandVideoRecords.map(v => BandVideo(v._2,v._3))
          val seqOfBandVideos = listOfBandVideos.toSeq

          // process for obtaining a Website Object
          /*
          if the value in the caption_id column for the natural join of the Caption and Website tables is equivalent to the value in the feature_id column
          for the natural join between the Feature, Caption and Intro tables, then include the record else exclude
          */
          val websiteRecords = listOfCaptionWebsiteRecords.filter(w => w._1 == r._1)

          // create website object

          var websiteURLs: List[String] = List()
          var websiteNames: List[String] = List()

          for (w <- websiteRecords) {
            websiteURLs = w._2 :: websiteURLs
            websiteNames = w._3 :: websiteNames
          }

          // reverse the websiteURLs and Names and convert from List to Seq
          val websiteURLs2: Seq[String] = websiteURLs.reverse.toSeq
          val websiteNames2: Seq[String] = websiteNames.reverse.toSeq

          Feature(r._2.toString,r._3,Intro(r._8,r._9),Caption(r._5,r._6,r._7,Website(websiteURLs2,websiteNames2)),r._4,seqOfBandVideos)
        }

        listOfFeatures
      }
    }

  }


}


// Contact model class
/*
  NOTE: in Scala the type constructor / data constructor / w/ accompanying methods pattern is most elegantly
  expressed as a case class with companion object containing methods
*/

case class Contact (val id: Pk[Int], val name: String, val address: String, val city: String,
  val state: String, val zip: String, val country: String,
  val phone: String, val email: String, val message: Option[String]) {
    
    // accessor methods
    def getName = name
    def getAddress = address
    def getCity = city
    def getState = state
    def getZip = zip
    def getCountry = country
    def getPhone = phone
    def getEmail = email
    def getMessage = message

}

// companion object to Contact model class
object Contact {

  // checks to see if the user email is already in our database if so just send an email, otherwise insert a new record and send an email
  def checkUserEmail(contactObject: Contact): Unit = {

    /*
    task: send outbound email using Amazon's SMS SMTP servers to an email address
    - from/to email addresses
    - body / subject of the message
    - SMTP credentials
    - Amazon SES SMTP hostname
    - port we are connecting to on the SMTP endpoint, port 25 for encrypted connections, using STARTTLS
    */

    def sendEmail(from: String, to: String)(body: String, subject: String)(smtp_uname: String, smtp_pwd: String)(hostname: String)(port: String): Unit = {

      // Create a Properties object to contain connection configuration information.
      val props: Properties = System.getProperties()
      props.put("mail.transport.protocol", "smtp")
      props.put("mail.smtp.port", port)

      // Set properties indicating that we want to use STARTTLS to encrypt the connection.
      // The SMTP session will begin on an unencrypted connection, and then the client
      // will issue a STARTTLS command to upgrade to an encrypted connection.
      props.put("mail.smtp.auth", "true")
      props.put("mail.smtp.starttls.enable", "true")
      props.put("mail.smtp.starttls.required", "true")

      // Create a Session object to represent a mail session with the specified properties.
      val session: Session = Session.getDefaultInstance(props)

      // Create a message with the specified information.
      val msg: MimeMessage = new MimeMessage(session)
      msg.setFrom(new InternetAddress(from))
      msg.setRecipient(Message.RecipientType.TO, new InternetAddress(to))
      msg.setSubject(subject)
      msg.setContent(body,"text/plain")

      // Create a transport.
      val transport: Transport = session.getTransport()

      // Send the message.
      try {
        println("Attempting to send an email through the Amazon SES SMTP interface...")

        // Connect to Amazon SES using the SMTP username and password you specified above.
        transport.connect(hostname,smtp_uname,smtp_pwd)

        // Send the email.
        transport.sendMessage(msg, msg.getAllRecipients())
        println("Email sent!")
      } catch {
        case ex: Exception => {
          println("The email was not sent.")
          println("Error message: " + ex.getMessage())
        }
      } finally {
        // Close and terminate the connection.
        transport.close()
      }

      ()
    }

   // read all records from our database
   val databaseRecords: List[Contact] = CRUDinstances.contactInstance.read

   // gather all the emails from our database
   val listOfEmails: List[String] = databaseRecords map { c => c.getEmail }

   // if user email already exists, then we have a non-empty list, otherwise an empty list
   val isEmptySet = for (email <- listOfEmails if email == contactObject.getEmail) yield email

    // if the list is empty, insert the new record and send an email else just end an email
    if (isEmptySet.isEmpty) {
      CRUDinstances.contactInstance.create(contactObject)
      sendEmail("shawn.m.grauel@gmail.com", "jenrockthis@gmail.com")(contactObject.getMessage.get +
        "\n\n" + contactObject.getName + "\n" + contactObject.getAddress + "\n" + contactObject.getZip +
        ", " + contactObject.getCity + ", " + contactObject.getState + ", " + contactObject.getCountry +
        "\n" + contactObject.getPhone + "\n" + contactObject.getEmail, contactObject.getName)("AKIAJNXGVHPQMAGDGK3A","Ah+yUhAFR00Ux1Hxda1kj3+oTFQ3hsnL0+dYKQvYIHFd")("email-smtp.us-west-2.amazonaws.com")("25")
    } else {
      sendEmail("shawn.m.grauel@gmail.com", "jenrockthis@gmail.com")(contactObject.getMessage.get +
        "\n\n" + contactObject.getName + "\n" + contactObject.getAddress + "\n" + contactObject.getZip +
        ", " + contactObject.getCity + ", " + contactObject.getState + ", " + contactObject.getCountry +
        "\n" + contactObject.getPhone + "\n" + contactObject.getEmail, contactObject.getName)("AKIAJNXGVHPQMAGDGK3A","Ah+yUhAFR00Ux1Hxda1kj3+oTFQ3hsnL0+dYKQvYIHFd")("email-smtp.us-west-2.amazonaws.com")("25")
    }
    ()

  }

}

// User class for modeling authenticated users
case class User (val uname: String, val pwd: String) {
  def getUserName = uname
  def getPwd = pwd
  def checkPassword(pwd: String): Boolean = this.pwd == pwd
}

// companion object to User class
object UserObj {
  val users = List(new User("admin","shoegazemusic"),new User("admin2","lavieenrose"))
  def find(username: String): Option[User] = users.filter(_.getUserName == username).headOption
}

// Feature classes for modeling Artist Features

case class Intro (val introTitle: String, val introductoryText: String) {
  def getIntroTitle = introTitle
  def getIntroductoryText = introductoryText
}

case class Website (val websiteURLs: Seq[String], val websiteNames: Seq[String]) {
  def getWebsiteUrls = websiteURLs
  def getWebsiteNames = websiteNames
}

case class Caption (val artistTitle: String, val artistImage: String, val captionText: String, val website: Website) {
  def getArtistTitle = artistTitle
  def getArtistImage = artistImage
  def getCaptionText = captionText
  def getWebsite = website
}

case class BandVideo (val bandVideoUrl: String, val title: String) {
  def getBandVideoUrl = bandVideoUrl
  def getTitle = title
}

case class Feature (val date: String, val time: String, val intro: Intro, val caption: Caption, val audioPlayer: String, val listOfBandVideos: Seq[BandVideo]) {

  // getters
  // convert to mySQL date format YYYY-MM-DD and time format HH:MM:SS
  def getDate: String = date.takeWhile(c => c != 'T')
  def getTime: String = time.dropWhile(c => c != 'T').drop(1).takeWhile(c => c != '.')

  def getDate2 = date
  def getTime2 = time

  def getCalendar: Calendar = {

    // instantiate calender object and configure for UTC time zone
    val cal1: Calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))

    // set year, month, day of the month
    val year = getDate.takeWhile(c => c != '-').toInt
    val month = getDate.dropWhile(c => c != '-').drop(1).takeWhile(c => c != '-').toInt
    val dayOfMonth = getDate.dropWhile(c => c != '-').drop(1).dropWhile(c => c != '-').drop(1).toInt

    cal1.set(Calendar.YEAR, year)
    cal1.set(Calendar.MONTH, month)
    cal1.set(Calendar.DAY_OF_MONTH, dayOfMonth)

    // set the hour of day, minute, and second

    val hourOfDay = time.takeWhile(c => c != ':').toInt
    val minute = time.dropWhile(c => c != ':').drop(1).takeWhile(c => c != ':').toInt
    val second = time.dropWhile(c => c != ':').drop(1).dropWhile(c => c != ':').drop(1).toInt

    cal1.set(Calendar.HOUR_OF_DAY, hourOfDay)
    cal1.set(Calendar.MINUTE, minute)
    cal1.set(Calendar.SECOND, second)

    cal1
  }

  def getIntro: Intro = intro
  def getCaption: Caption = caption
  def getAudioPlayer: String = audioPlayer
  def getListOfBandVideos: Seq[BandVideo] = listOfBandVideos



  // printing
  def prettyPrint: Unit = {
    println("date: " + date)
    println()
    println("time: " + time)
    println()
    println("intro: " + intro)
    println()
    println("caption: " + caption)
    println()
    println("audioPlayer: " + audioPlayer)
    println()
    println("listOfBandVideos: " + listOfBandVideos)
  }

}

// companion object to Feature type
object FeatureObj {


}



