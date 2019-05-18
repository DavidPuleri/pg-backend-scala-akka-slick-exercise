package pg.http

import akka.actor.ActorRef
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives.{complete, path, _}
import com.spingo.op_rabbit.{Message, RabbitMarshaller}
import pg.db.OrderComponent.{DBOrder, DbCart, DbItem}
import pg.db.{DbUser, Repository}
import pg.model.{Address, Item, Order}
import pg.tools.JsonSupport._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

class OrderService(repository: Repository, rabbitControl: ActorRef) {

  implicit val orderMarshaller = new RabbitMarshaller[Order] {
    protected val contentType = "application/json"
    private val encoding = "UTF-8"
    protected val contentEncoding = Some(encoding)

    def marshall(value: Order) = serialization.write(value).getBytes("utf-8")
  }

  val orderRoute = path("orders") {
    pathEndOrSingleSlash {
      post {
        entity(as[Order]) { order =>
          complete(repository.addOrder(order).map { d =>

            repository.getOrder(d.id).onComplete {
              case Success(value) =>
                value.map(_.asOrder).headOption.map { s =>
                  rabbitControl ! Message.queue(s, "order")
                }

              case Failure(exception) =>
            }

            HttpResponse(StatusCodes.Created)
              .addHeader(RawHeader("Location", s"/orders/${d.id}"))
          })
        }
      }
    }
  } ~ getOrder

  def getOrder = path("orders" / Segment) { orderId =>
    pathEndOrSingleSlash {
      get {
        rejectEmptyResponse {
          val eventualOrders = repository.getOrder(orderId).map { o =>
            o.map(_.asOrder).headOption
          }
          complete(eventualOrders)
        }
      }
    }
  }

  implicit class orderExtension(order: ((DBOrder, DbUser), Seq[(DbCart, DbItem)])) {

    def asOrder: Order = {

      Order(order._1._2.first, order._1._2.last, order._1._2.email,
        Address(
          order._1._1.deliveryAddress.number,
          order._1._1.deliveryAddress.streetName,
          order._1._1.deliveryAddress.postCode,
          order._1._1.deliveryAddress.city,
        ),
        order._2.map { c =>
          Item(c._2.id, c._1.quantity, c._1.totalPrice, Some(c._2.name))
        },
        Some(order._1._1.id)
      )

    }
  }

}

