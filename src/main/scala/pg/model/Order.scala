package pg.model

case class Order(firstName: String, lastName: String, email: String, address: Address, items: Seq[Item], id: Option[String] = None)

case class Address(number: Int, streetName: String, postCode: String, city: String)

case class Item(id: String, quantity: Int, pricePaid: Double, name: Option[String])


