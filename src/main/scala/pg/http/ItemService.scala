package pg.http

import akka.http.scaladsl.server.Directives.{complete, path, _}
import pg.db.Repository
import pg.tools.JsonSupport._

class ItemService(repository: Repository) {

  val route = path("items") {
    get {
      complete(repository.retrieveAllItems())
    }
  }
}


