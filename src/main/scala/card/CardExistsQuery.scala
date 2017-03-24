package card

import java.util.UUID

import card.editor.ChangedTable
import slick.driver.H2Driver.api._
import slick.lifted.TableQuery

import scala.concurrent.Future

trait CardExistsQuery {
    def cardExists(db: Database, cardId: UUID): Future[Boolean] = {
        val created: Query[Rep[String], String, Seq] = TableQuery[ChangedTable]
            .map(a => a.cardId)
            .distinct

        val deleted = TableQuery[card.remover.Table]
            .map(_.cardId)
            .distinct

        val query = created.filterNot(_.in(deleted))
            .filter(_ === cardId.toString)
            .exists

        db.run(query.result)
    }
}
