package controllers

import play.api.mvc.{Action, Controller}
import models.Contact
import play.api.data._ // OR '._'
import play.api.data.Forms._
import play.api.data.validation._

// include facilities for anorm
import anorm._
import anorm.SqlParser._
import play.api.db.DB
import play.api.Play.current


object Application extends Controller {


  def index = Action { implicit request =>
    Ok(views.html.index("Welcome to Electro Statik Code"))
  }

  def postTest = Action { implicit request =>
    println(request)
    Ok(views.html.index("Welcome to Electro Statik Code"))
  }

  /*
  def about = Action { implicit request =>
    Ok(views.html.about("About Shawn"))
  }

  def productsAndServices = Action { implicit request =>
    Ok(views.html.productsAndServices("Our products and Services"))
  }

  def viewForm = Action { implicit request =>
    Ok(views.html.contactForm("Contact Shawn"))
  }
  */

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
      "message" -> optional(text(minLength = 0, maxLength = 1732).verifying(messageCheckConstraint))
    )(Contact.apply)(Contact.unapply))

    contactForm.bindFromRequest()(request).fold(
      // create a nicer error template, STATUS CODE 400 Bad Request OR send an error message in a modal window
      formWithErrors => BadRequest("Oh no! Invalid Submission!"), 
      contactObject => {
        /* Check if the email exists in our database. if it does exists, we don't insert record,
        else it doesn't exist and we insert the record and do nothing.
        ??? how do I get away without sending an HTTP response to the user? i.e. disturb the user's state ???
        */
        Contact.checkUserEmail(contactObject)
        Ok(views.html.index("Welcome to Electro Statik Code"))
      })
  } // end postFrom action

}
