package controllers

import play.api.mvc.{Action, Controller}
import models._
import play.api.data._ // OR '._'
import play.api.data.Forms._
import play.api.data.validation._

// include facilities for anorm
import anorm._
import anorm.SqlParser._
import play.api.db.DB
import play.api.Play.current

/*
include the facilities for WS i.e. Web Services
and a future response
*/
import play.api.libs.ws._
import scala.concurrent._
import scala.concurrent.duration._

// include the JSON API
import play.api.libs.json._
// read type for convertion from Jsvalue to Scala Object
import play.api.libs.json.Reads._
// functional syntax for reads
import play.api.libs.functional.syntax._

// Java file utility for moving files
import java.io.File

// Java Calender utility for date and time
import java.util.Calendar

object Application extends Controller {

  // request JSON data from youtube API v.3.0
  // NOTE: pass JSON to Ok()
  // NOTE: tell Jen to come up with a naming convention for videos i.e. Capital and lowercase letters, title of band followed by - ect.
  def getInterviews = Action {

    /*
    issue a GET request for all the items from the Interviews playlist
     */
    val responseFuture: Future[Response] = WS.url("https://www.googleapis.com/youtube/v3/playlistItems")
      .withQueryString(
        "key" -> "AIzaSyB0eeTtUnrGityYT4kQmOjZu2PnCLNewyg",
        "part" -> "snippet",
        "fields" -> "items(snippet(publishedAt,title,description,thumbnails,resourceId(videoId)))",
        "playlistId" -> "PLPua5o3DQD_ZCyFGqOYBX4r-8fkpdG0rQ",
        "maxResults" -> "50"
      ).get

    // wait 10 seconds
    val duration: Duration = Duration(10000, "millis")
    val response = Await.result(responseFuture, duration)
    // convert from JSON string to a JsValue type

    // println(response.body)
    val interviewsJs: JsValue = Json.parse(response.body)

    /*
    access a simple path in the JSON through the items property,
    then convert to a Seq of JsValue(s) wrapped in Option
    */
    val interviewsListOpt: Option[Seq[JsValue]] = (interviewsJs \ "items").asOpt[Seq[JsValue]]

    // check to see if asOpt failed to convert to JsValue
    if (interviewsListOpt.get == None) {
      BadRequest("500 Internal Server Error\n\nThe server encountered an unexpected condition which prevented it from fulfilling the request.")
    } else {
      val interviewsList: Seq[JsValue] = interviewsListOpt.get

      val interviewsList2: Seq[JsValue] = interviewsList.map(jsv => jsv \ "snippet")

      val interviewsList3: List[JsObject] = interviewsList2.map(jsv =>
        Json.obj( "publishedAt" -> (jsv \ "publishedAt").toString.init.tail,
                  "title" -> (jsv \ "title").toString.init.tail,
                  "description" -> (jsv \ "description").toString.init.tail,
                  "videoId" -> (jsv \ "resourceId" \ "videoId").toString.init.tail,
                  "thumbnail" -> (jsv \ "thumbnails" \ "high" \ "url").toString.init.tail)
      ).toList

      // sort interviews alphabetically
      val interviewsList4: List[JsObject] = interviewsList3.sortBy(v => (v \ "title").toString)

      // print title property for each jsObject to stdout
      // interviewsList4.foreach(v => println((v \ "title").toString))

      Ok(Json.toJson(interviewsList4))

    }

  }

  def getPerformances = Action {

    /*
    issue a GET request for all the items from the Performances playlist
    NOTE: this code is blocking; you should use an actor to handle asynchronous requests for data in a non-blocking fashion
    */
    val responseFuture: Future[Response] = WS.url("https://www.googleapis.com/youtube/v3/playlistItems")
      .withQueryString(
        "key" -> "AIzaSyB0eeTtUnrGityYT4kQmOjZu2PnCLNewyg",
        "part" -> "snippet",
        "fields" -> "items(snippet(publishedAt,title,description,thumbnails,resourceId(videoId)))",
        "playlistId" -> "PLPua5o3DQD_YedT65WBViOlGb9lovlcfu",
        "maxResults" -> "50"
      ).get

    // wait 10 seconds
    val duration: Duration = Duration(10000, "millis")
    val response = Await.result(responseFuture, duration)
    // convert from JSON string to a JsValue type

    // println(response.body)
    val interviewsJs: JsValue = Json.parse(response.body)

    /*
    access a simple path in the JSON through the items property,
    then convert to a Seq of JsValue(s) wrapped in Option
    */
    val interviewsListOpt: Option[Seq[JsValue]] = (interviewsJs \ "items").asOpt[Seq[JsValue]]

    // check to see if asOpt failed to convert to JsValue
    if (interviewsListOpt.get == None) {
      BadRequest("500 Internal Server Error\n\nThe server encountered an unexpected condition which prevented it from fulfilling the request.")
    } else {
      val performancesList: Seq[JsValue] = interviewsListOpt.get

      val performancesList2: Seq[JsValue] = performancesList.map(jsv => jsv \ "snippet")

      val performancesList3: List[JsObject] = performancesList2.map(jsv =>
        Json.obj("publishedAt" -> (jsv \ "publishedAt").toString.init.tail,
          "title" -> (jsv \ "title").toString.init.tail,
          "description" -> (jsv \ "description").toString.init.tail,
          "videoId" -> (jsv \ "resourceId" \ "videoId").toString.init.tail,
          "thumbnail" -> (jsv \ "thumbnails" \ "high" \ "url").toString.init.tail)
      ).toList

      // sort performances alphabetically
      val performancesList4: List[JsObject] = performancesList3.sortBy(v => (v \ "title").toString)

      Ok(Json.toJson(performancesList4))
    }

  }

  def getFeatureList = Action { implicit request =>
    // read list of Features objects from Feature's data model
    val features: List[Feature] = CRUDinstances.featureInstance.read

    // sort Feature objects from more recent to less recent
    val sortedFeatures = features.sortBy(f => ( f.getCalendar.get(Calendar.YEAR), f.getCalendar.get(Calendar.MONTH), f.getCalendar.get(Calendar.DAY_OF_MONTH), f.getCalendar.get(Calendar.HOUR_OF_DAY), f.getCalendar.get(Calendar.MINUTE), f.getCalendar.get(Calendar.SECOND) ) )
    var sortedFeaturesRev = sortedFeatures.reverse

    // limit maximum number of features displayed to 3
    if (sortedFeaturesRev.length > 3) {
      sortedFeaturesRev = sortedFeaturesRev.take(3)
    }

    // sortedFeaturesRev.foreach(f => f.prettyPrint)

    // List Feature objects to list JsObject

    // Feature (val date: String, val time: String, val intro: Intro, val caption: Caption, val audioPlayer: String, val listOfBandVideos: Seq[BandVideo])
    // Caption (val artistTitle: String, val artistImage: String, val captionText: String, val website: Website)
    // Website (val websiteURLs: Seq[String], val websiteNames: Seq[String])
    // BandVideo (val bandVideoUrl: String, val title: String)
    // Intro (val introTitle: String, val introductoryText: String)

    // transform list of Features to list of Json objects
    val featuresJsObjs = sortedFeaturesRev.map { f => Json.obj(
      "date" -> f.getDate2,
      "time" -> f.getTime2,
      "intro" -> Json.obj("introTitle" -> f.getIntro.introTitle, "introductoryText" -> f.getIntro.getIntroductoryText),
      "caption" -> Json.obj("artistTitle" -> f.getCaption.getArtistTitle,
        "artistImage" -> f.getCaption.getArtistImage,
        "captionText" -> f.getCaption.getCaptionText,
        "website" -> Json.obj("websiteURLs" -> f.getCaption.getWebsite.getWebsiteUrls.toList, "websiteNames" -> f.getCaption.getWebsite.getWebsiteNames.toList)),
      "audioPlayer" -> f.getAudioPlayer,
      "listOfBandVideos" -> f.getListOfBandVideos.toList.map {
        bv => Json.obj("bandVideoUrl" -> bv.getBandVideoUrl, "title" -> bv.getTitle)
      }
    )}

    // pass list of JsObjects to Json.toJson method, result to Ok
    Ok(Json.toJson(featuresJsObjs))
  }

  def getContactForm = Action { implicit request =>
    Ok(views.html.contactForm())
  }

  def getHeader = Action { implicit request =>
    Ok(views.html.header())
  }

  // this action cannot be used because of disablement of the cycle functionality of the carousel when described as directive
  /*
  def getFrontPage = Action { implicit request =>
    Ok(views.html.frontpage())
  }
  */

  def getPerformanceGallery = Action { implicit request =>
    Ok(views.html.performanceGallery())
  }


  def getInterviewGallery = Action { implicit request =>
    Ok(views.html.interviewGallery())
  }

  def getFeatures = Action { implicit request =>
    Ok(views.html.features())
  }

  def getAbout = Action { implicit request =>
    Ok(views.html.about())
  }

  def getFooter = Action { implicit request =>
    Ok(views.html.footer())
  }

  def getAdminPanel = Action {
    Ok(views.html.admin())
  }

  // expose the html for datepicker component
  // NOTE: datepicker is composed of daypicker, monthpicker, and year picker, how to compose these routes?
  def getDatePicker = Action {
    Ok(views.html.datepicker())
  }

  def getDayPicker = Action {
    Ok(views.html.daypicker())
  }

  def getMonthPicker = Action {
    Ok(views.html.monthpicker())
  }

  def getYearPicker = Action {
    Ok(views.html.yearpicker())
  }

  def getUploadImage = Action {
    Ok(views.html.uploadimage())
  }

  // expose the html for the timepicker component
  def getTimePicker = Action {
    Ok(views.html.timepicker())
  }

  /*
  def artistImage = Action { implicit request =>
    println(request.body)
    Ok(views.html.index("Welcome to ROCK THIS TV"))
  }
  */

  // action for handling the uploading of artist images to '/public/img' directory

  // use the multipartFormData body parser to parse our request with a multipart form data encoding
  def uploadArtistImage = Action(parse.multipartFormData) { request =>

    // obtain a singleton sequence of file parts and get the first element, otherwise none
    val fileParts = request.body.files.headOption

    // if non-empty singleton, then get the filename and move the file to '/public/img', otherwise None and we issue an internal server error
    fileParts.map { picture =>
      val filename = picture.filename
      picture.ref.moveTo(new File(s"/home/electro/IdeaProjects/RockThisMusicSite/public/img/$filename"),true)
      Ok("File uploaded")
    }.getOrElse {
      BadRequest("500 Internal Server Error\n\nThe server encountered an unexpected condition which prevented it from fulfilling the request.")
    }
  }

  // how to expose data at an endpoint and load the single page app?
  def index = Action { implicit request =>
    Ok(views.html.index("Welcome to ROCK THIS!"))
  }

  // navigate to the sign-in page
  def signin = Action { implicit request =>
    Ok(views.html.signin("Welcome to ROCK THIS!"))
  }

  // action for authenticating users
  def accessAdminPanel = Action { implicit request =>

    val username: String = request.queryString.get("username").get.apply(0)
    val password: String = request.queryString.get("password").get.apply(0)

    val user: Option[User] =
      if(UserObj.find(username) == None) None else UserObj.find(username).filter(_.checkPassword(password))

    user match {
      // display admin-panel
      case Some(user) => Ok(views.html.adminPanel("Welcome to the Admin-Panel"))
      // display non-authenticated page
      case None => Forbidden("I don't know you")
    }

  }

  // an HTTP post request route
  def postForm = Action { implicit request =>

    // custom validations for fields
    // custom validation for name field
    val nameRegex = """^[a-zA-Z\']+\ [a-zA-Z\']+$""".r
    val nameCheckConstraint: Constraint[String] = Constraint("constraints.checkName")({
      plainText =>
      val errors = plainText match {
        case nameRegex() => Nil
        case _ => Seq(ValidationError("* Letters only"))
      }
      if (errors.isEmpty) {
        Valid
      } else {
        Invalid(errors)
      }
    })

    // custom validation for address field
    val addressRegex = """^[0-9a-zA-Z\ \.\#\,\;\:\']+$""".r
    val addressCheckConstraint: Constraint[String] = Constraint("constraints.checkAddress")({
      plainText =>
      val errors = plainText match {
        case addressRegex() => Nil
        case _ => Seq(ValidationError("* Letters and numbers only"))
      }
      if (errors.isEmpty) {
        Valid
      } else {
        Invalid(errors)
      }
    })

    // custom validation for city field
    val cityRegex = """^[a-zA-Z\ \']+$""".r
    val cityCheckConstraint: Constraint[String] = Constraint("constraints.checkCity")({
      plainText =>
      val errors = plainText match {
        case cityRegex() => Nil
        case _ => Seq(ValidationError("* Letters only"))
      }
      if (errors.isEmpty) {
        Valid
      } else {
        Invalid(errors)
      }
    })

    // custom validation for state field
    val stateRegex = """^[a-zA-Z\ \']+$""".r
    val stateCheckConstraint: Constraint[String] = Constraint("constraints.checkState")({
      plainText =>
      val errors = plainText match {
        case stateRegex() => Nil
        case _ => Seq(ValidationError("* Letters only"))
      }
      if (errors.isEmpty) {
        Valid
      } else {
        Invalid(errors)
      }
    })

    // custom validation for zip field
    val zipRegex = """^[0-9]+$""".r
    val zipCheckConstraint: Constraint[String] = Constraint("constraints.checkZip")({
      plainText =>
      val errors = plainText match {
        case zipRegex() => Nil
        case _ => Seq(ValidationError("* Invalid postal code"))
      }
      if (errors.isEmpty) {
        Valid
      } else {
        Invalid(errors)
      }
    })

    // custom validation for country field
    val countryRegex = """^[A-Z]+$""".r
    val countryCheckConstraint: Constraint[String] = Constraint("constraints.checkCountry")({
      plainText =>
      val errors = plainText match {
        case countryRegex() => Nil
        case _ => Seq(ValidationError("* Capital letters Only"))
      }
      if (errors.isEmpty) {
        Valid
      } else {
      Invalid(errors)
      }
    })

    // custom validation for phone field
    // change phone regex to simple one in coding/regex/.
    val phoneRegex = """^[0-9]+$""".r
    val phoneCheckConstraint: Constraint[String] = Constraint("constraints.checkPhone")({
      plainText =>
      val errors = plainText match {
        case phoneRegex() => Nil
        case _ => Seq(ValidationError("* Invalid phone number. Numbers only"))
      }
      if (errors.isEmpty) {
        Valid
      } else {
        Invalid(errors)
      }
    })

    // custom validation for email field
    // http://www.w3.org/TR/html-markup/datatypes.html#form.data.emailaddress  (RFC 5322 and RFC 1034 standard)
    val emailRegex = """^[a-zA-Z0-9.!#$%&’*+/=?^_`{|}~-]+@[a-zA-Z0-9-]+(?:\.[a-zA-Z0-9-]+)*$""".r
    val emailCheckConstraint: Constraint[String] = Constraint("constraints.checkEmail")({
      plainText =>
      val errors = plainText match {
        case emailRegex() => Nil
        case _ => Seq(ValidationError("* Invalid email"))
      }
      if (errors.isEmpty) {
        Valid
      } else {
        Invalid(errors)
      }
    })

    // custom validation for message field
    // 0 or more characters which are NOT spaces or spaces i.e. any character including '\n' and '\r'
    val messageRegex = """^[\S\s]*$""".r
    val messageCheckConstraint: Constraint[String] = Constraint("constraints.checkMessage")({
      plainText =>
      val errors = plainText match {
        case messageRegex() => Nil
        case _ => Seq(ValidationError("* Unicode characters only"))
      }
      if (errors.isEmpty) {
        Valid
      } else {
        Invalid(errors)
      }
    })

    val contactForm = Form(mapping(
      // ignored utility assigns a fixed value for a field of type anorm.NotAssigned
      "id" -> ignored(NotAssigned: Pk[Int]),
      "name" -> nonEmptyText(minLength = 3, maxLength = 71).verifying(nameCheckConstraint),
      "address" -> nonEmptyText(minLength = 2, maxLength = 95).verifying(addressCheckConstraint),
      "city" -> nonEmptyText(minLength = 2, maxLength = 35).verifying(cityCheckConstraint),
      "state" -> nonEmptyText(minLength = 2, maxLength = 52).verifying(stateCheckConstraint),
      "zip" -> nonEmptyText(minLength = 5, maxLength = 5).verifying(zipCheckConstraint),
      "country" -> nonEmptyText(minLength = 2, maxLength = 2).verifying(countryCheckConstraint),
      "phone" -> nonEmptyText(minLength = 10, maxLength = 10).verifying(phoneCheckConstraint),
      "email" -> nonEmptyText(minLength = 3, maxLength = 254).verifying(emailCheckConstraint),
      "message" -> play.api.data.Forms.optional(text(minLength = 0, maxLength = 1732).verifying(messageCheckConstraint))
    )(Contact.apply)(Contact.unapply))

    contactForm.bindFromRequest()(request).fold(
      // create a nicer error template, STATUS CODE 400 Bad Request
      formWithErrors => BadRequest("Oh no! Invalid Submission!"), 
      contactObject => {
        /* Check if the email exists in our database. if it does exist, we don't insert record and send an email,
        else it doesn't exist and we insert the record and send an email */
        Contact.checkUserEmail(contactObject)
        Ok(views.html.index("Welcome to Electro Statik Code"))
      })
  } // end postFrom action

  def postFeature = Action { implicit request =>
    println(request.body)

    // convert AnyContent type object to JsValue object
    val FeatureOptJs: Option[JsValue] =  (request.body).asJson

    // check to see if asJson fails to convert to JsValue, if it does throw internal server error, otherwise parse JSON

    if (FeatureOptJs == None) {
      BadRequest("500 Internal Server Error\n\nThe server encountered an unexpected condition which prevented it from fulfilling the request.")
    } else {

      // get JsValue
      val featureJsv: JsValue = FeatureOptJs.get

      // convert JsValue to Feature object using Reads type, no constraints

      implicit val introReads: Reads[Intro] = (
        (JsPath \\ "introTitle").read[String] and
          (JsPath \\ "introductoryText").read[String]
        )(Intro.apply _)

      implicit val websiteReads: Reads[Website] = (
        (JsPath \\ "websiteURLs").read[Seq[String]] and
          (JsPath \\ "websiteNames").read[Seq[String]]
        )(Website.apply _)

      implicit val captionReads: Reads[Caption] = (
        (JsPath \\ "artistTitle").read[String] and
          (JsPath \\ "artistImage").read[String] and
          (JsPath \\ "captionText").read[String] and
          (JsPath \\ "website").read[Website]
        )(Caption.apply _)

      implicit val bandVideoReads: Reads[BandVideo] = (
        (JsPath \\ "bandVideoUrl").read[String] and
          (JsPath \\ "title").read[String]
        )(BandVideo.apply _)

      implicit val featureReads: Reads[Feature] = (
        (JsPath \ "date").read[String] and
          (JsPath \ "time").read[String] and
          (JsPath \ "intro").read[Intro] and
          (JsPath \ "caption").read[Caption] and
          (JsPath \ "audioPlayer").read[String] and
          (JsPath \ "listOfBandVideos").read[Seq[BandVideo]]
        )(Feature.apply _)

      // convert from Jsvalue to Feature
      val JsFeatureRes: JsResult[Feature] = featureJsv.validate[Feature](featureReads)

      // check for successful conversion, otherwise internal server error
      JsFeatureRes match {
        case s: JsSuccess[Feature] => {
          val feature: Feature = s.get

          // feature.prettyPrint

          // insert the Feature object
          CRUDinstances.featureInstance.create(feature)

          Ok(views.html.admin())
        }
        case e: JsError => {
          println("Errors: " + JsError.toFlatJson(e).toString())
          BadRequest("500 Internal Server Error\n\nThe server encountered an unexpected condition which prevented it from fulfilling the request.")
        }
      }

    }


  }

}
