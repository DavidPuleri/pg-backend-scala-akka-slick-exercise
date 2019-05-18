package pg.db

import java.util.UUID

import pg.db.OrderComponent.{DBOrder, DbAddress, DbCart, DbItem}
import pg.model.{Address, Order}
import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class Repository(val config: DatabaseConfig[_ <: JdbcProfile])
  extends UserComponent
    with OrderComponent
    with ProfileComponent {


  val db = config.db
  override val profile = config.profile

  import profile.api._

  implicit class addressExtension(address: Address) {
    def toDbObject = {
      DbAddress(
        address.number, address.streetName, address.postCode, address.city
      )
    }
  }

  def getOrder(orderId: String): Future[Map[(DBOrder, DbUser), Seq[(DbCart, DbItem)]]] = {

    val q = for {
      (c, i) <- carts.filter(_.orderId === orderId).join(items).on(_.itemId === _.id)
      o <- c.order
      u <- o.user
    } yield ((c, i), u, o)
    db.run(q.result).map(_.groupBy(s => (s._3, s._2)).map { o =>
      (o._1, o._2.map(_._1))
    })
  }

  def addOrder(order: Order): Future[DBOrder] = {
    for {
      user <- loadOrCreateDbUser(order).map(_.getOrElse(throw new Throwable("Unable to find or create customer")))
      order <- {
        val dbOrder: DBOrder = DBOrder(UUID.randomUUID().toString, user.id, order.address.toDbObject)

        db.run(orders += dbOrder).flatMap { d =>

          val items: Seq[DbCart] = order.items.map { item =>
            DbCart(dbOrder.id, item.id, item.quantity, item.pricePaid)
          }

          db.run(carts ++= items).map { _ => dbOrder }

        }
      }
    } yield order
  }

  private def loadOrCreateDbUser(order: Order): Future[Option[DbUser]] = db.run(users.filter(_.email === order.email).result.headOption).flatMap {
    case Some(foundUser) =>
      Future.successful(Some(foundUser))
    case None =>
      val insertedUser = DbUser(UUID.randomUUID().toString, order.firstName, order.lastName, order.email)
      db.run(users += insertedUser).map {
        case x if x > 0 =>
          Some(insertedUser)
        case _ =>
          None
      }
  }

  def retrieveAllItems(): Future[Seq[DbItem]] = {
    db.run(items.result)
  }

  def bootstrapApp(): Future[(Option[Int], Int, Int)] = {
    val deleteItem = db.run(items.delete)

    val createItem = db.run(items ++= Seq(DbItem(UUID.randomUUID().toString, "Terrasse Silver (sam-dim)", 1699),
      DbItem(UUID.randomUUID().toString, "TERRASSES VIP : Terrasse VIP Platinium (sam-dim)", 2600),
      DbItem(UUID.randomUUID().toString, "TERRASSES VIP : Terrasse VIP Gold (samedi)", 750),
      DbItem(UUID.randomUUID().toString, "TERRASSES VIP : Terrasse VIP Gold (samedi)", 1540)))

    val itemHandling = deleteItem.flatMap(m => createItem)

    val deleteOrder = db.run(orders.delete)
    val deleteUser = db.run(users.delete)

    for {
      u <- itemHandling
      order <- deleteOrder
      user <- deleteUser
    } yield (u, order, user)
  }


  def retrieveAllUsers(): Future[Seq[DbUser]] = {

    db.run(users.result)
  }

}
