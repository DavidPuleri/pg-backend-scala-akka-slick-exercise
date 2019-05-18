package pg.db

import pg.db.OrderComponent.{DbAddress, DbCart}

trait OrderComponent extends UserComponent {
  this: ProfileComponent =>

  import profile.api._


  class Items(tag: Tag) extends Table[OrderComponent.DbItem](tag, "items") {

    val id = column[String]("id", O.PrimaryKey)
    val name = column[String]("name")
    val price = column[Double]("price")

    def * = (id, name, price) <> (OrderComponent.DbItem.tupled, OrderComponent.DbItem.unapply)
  }

  class Orders(tag: Tag) extends Table[OrderComponent.DBOrder](tag, "orders") {

    val id = column[String]("id", O.PrimaryKey)
    val userId = column[String]("userId")
    val status = column[String]("status")

    val houseNumber = column[Int]("houseNumber")
    val streetName = column[String]("streetName")
    val postCode = column[String]("postCode")
    val city = column[String]("city")

    val user = foreignKey("orders_users_user_id_fk", userId, users)(_.id)

    def address = (houseNumber, streetName, postCode, city) <> (DbAddress.tupled, DbAddress.unapply)

    def * = (id, userId, address) <> (OrderComponent.DBOrder.tupled, OrderComponent.DBOrder.unapply)
  }

  class Carts(tag: Tag) extends Table[OrderComponent.DbCart](tag, "carts") {

    val orderId = column[String]("orderId", O.PrimaryKey)
    val itemId = column[String]("itemId", O.PrimaryKey)
    val quantity = column[Int]("quantity")
    val price = column[Double]("price")

    def order = foreignKey("carts_orders_id_fk", orderId, orders)(_.id)

    def item = foreignKey("carts_items_id_fk", itemId, items)(_.id)

    def * = (orderId, itemId, quantity, price) <> (DbCart.tupled, DbCart.unapply)
  }

  val carts = TableQuery[Carts]
  val orders = TableQuery[Orders]
  val items = TableQuery[Items]
}

object OrderComponent {

  case class DBOrder(id: String, userId: String, deliveryAddress: DbAddress)

  case class DbCart(orderId: String, itemId: String, quantity: Int, totalPrice: Double)

  case class DbItem(id: String, name: String, price: Double)

  case class DbAddress(number: Int, streetName: String, postCode: String, city: String)

}