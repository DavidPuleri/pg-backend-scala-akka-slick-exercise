package pg

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethods, HttpRequest}
import akka.http.scaladsl.server.RouteConcatenation
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import pg.db.OrderComponent.DbItem
import pg.db.Repository
import pg.http.{ItemService, OrderService}
import pg.model.{Address, Item, Order}
import slick.basic.DatabaseConfig
import slick.jdbc.MySQLProfile

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Random, Success}

object HttpServer extends App with RouteConcatenation {

  import akka.actor.{ActorSystem, Props}
  import com.spingo.op_rabbit.RabbitControl

  implicit val actorSystem = ActorSystem("actorSystem")
  implicit val materializer = ActorMaterializer()
  val db: DatabaseConfig[MySQLProfile] = DatabaseConfig.forConfig("mysql")

  private val repository = new Repository(db)
  val rabbitControl = actorSystem.actorOf(Props[RabbitControl])


  repository.bootstrapApp().onComplete {
    case Success(s) =>

      println("Data reloaded. Please wait until http server starts")
      Http().bindAndHandle(new OrderService(repository, rabbitControl).orderRoute ~ new ItemService(repository).route, "localhost", 13712).onComplete {
        case Success(value) =>
          println(s"HTTP Server started on ${value.localAddress}")

          OrderSimulator.initData()
        case Failure(exception) =>
          println(exception.getLocalizedMessage)
      }
    case Failure(f) =>
      println(f.getMessage)
  }
}


object OrderSimulator {

  import pg.tools.JsonSupport._

  import scala.concurrent.ExecutionContext.Implicits.global
  import scala.concurrent.duration._

  implicit val actorSystem = ActorSystem("actorSystem")
  implicit val materializer = ActorMaterializer()

  private val flow = Http().cachedHostConnectionPool("localhost", 13712)

  val actor: ActorRef = actorSystem.actorOf(Props(new Actor {
    override def receive: Receive = {
      case o: Order =>
        Http().singleRequest(
          HttpRequest(HttpMethods.POST, "http://localhost:13712/orders",
            entity = HttpEntity(ContentTypes.`application/json`, serialization.write(o)))
        ).onComplete {
          case Success(value) =>
            value.discardEntityBytes()
            println(value.status)
          case Failure(exception) =>
            println(exception.getLocalizedMessage)
        }
    }
  }))

  def generateOrder(availableItems: Seq[DbItem]): Order = {

    def pickOne(availableItems: Seq[DbItem]): DbItem = {
      val size = availableItems.size
      val num = Random.nextInt(size)
      availableItems(num)
    }

    val items = Seq(pickOne(availableItems), pickOne(availableItems), pickOne(availableItems)).toSet


    Order(
      "David",
      "Puleri",
      "david@puleri.com",
      Address(290, "avenue de fabron", "06200", "Nice"),
      items.map { i =>
        val qte = Random.nextInt(50)
        Item(i.id, qte, i.price * qte, None)
      }.toSeq

    )

  }

  def initData() = {
    Http().singleRequest(
      HttpRequest(HttpMethods.GET, "http://localhost:13712/items")
    ).flatMap { t =>
      Unmarshal(t.entity).to[Seq[DbItem]]
    }
  }.onComplete {
    case Success(items) =>
      actorSystem.scheduler.schedule(0 millisecond, 500 millisecond, actor, generateOrder(items))

    case Failure(exception) =>
      println(exception.getMessage)
  }

}


