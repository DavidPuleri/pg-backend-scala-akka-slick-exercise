package pg.db

trait UserComponent {
  this: ProfileComponent =>

  import profile.api._

  class Users(tag: Tag) extends Table[DbUser](tag, "users") {
    def id = column[String]("id", O.PrimaryKey)

    def firstName = column[String]("firstName")

    def lastName = column[String]("lastName")

    def email = column[String]("email")

    def * = (id, firstName, lastName, email) <> (DbUser.tupled, DbUser.unapply)
  }

  val users = TableQuery[Users]

}
case class DbUser(id: String, first: String, last: String, email: String)
