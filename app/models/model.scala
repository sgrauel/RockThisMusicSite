package models

// include facilities for anorm
import anorm._
import anorm.SqlParser._
import play.api.db.DB
import play.api.Play.current


// NOTE: refactor this model class to be more functional
// model class
case class Contact(id: Pk[Int], name: String, address: String, city: String, 
  state: String, zip: String, country: String, 
  phone: String, email: String, message: Option[String]) {
    
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

// Data access object (DAO) : allows access to test data
// a companion object to our model class
object Contact {
  
  def checkUserEmail(contactObject: Contact): Unit = {
    
    def read: List[(Int, String, String, String, String, Int, String, String, String, Option[String])] = {
      DB.withConnection { implicit c =>
        val selectFromContact = SQL("SELECT * FROM Contact;")
        return (selectFromContact().map(row =>
          (row[Int]("id"), row[String]("name"), row[String]("address"),
            row[String]("city"), row[String]("state"), row[Int]("zip"),
            row[String]("country"), row[String]("phone"), row[String]("email"),
            row[Option[String]]("message"))
        ).toList)
      }
    }

    def save(person: Contact) {
      DB.withConnection { implicit connection =>
        SQL("""
            INSERT INTO Contact(name,address,city,state,zip,country,phone,email,message) 
            VALUES({name},{address},{city},{state},{zip},{country},{phone},{email},{message})
      """).on(
          'name -> person.getName,
          'address -> person.getAddress,
          'city -> person.getCity,
          'state -> person.getState,
          'zip -> person.getZip,
          'country -> person.getCountry,
          'phone -> person.getPhone,
          'email -> person.getEmail,
          'message -> person.getMessage
      ).executeUpdate 
      // java.sql.connection object passed implicitly to executeUpdate from call to withConnection
      }
    }

    val databaseRecords: List[(Int, String, String, String, String, Int, String, String, String, Option[String])] = read
    val listOfEmails: List[String] = databaseRecords map { record => record._9 }
    val isEmptySet = for (email <- listOfEmails if email == contactObject.getEmail) yield email

    // check if the list is empty, if so insert the new record else send the user to the homepage
    if (isEmptySet.isEmpty) { save(contactObject) }
  }

}
