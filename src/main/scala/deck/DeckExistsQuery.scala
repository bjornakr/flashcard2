package deck

import deck.editor.DeckChangedTable
import deck.remover.DeckDeletedTable
import slick.driver.H2Driver.api._
import slick.lifted.TableQuery

import scala.concurrent.Future

trait DeckExistsQuery {
    protected def deckExists(db: Database, deckId: String): Future[Boolean] = {
        val created = TableQuery[DeckChangedTable]
            .map(a => a.deckId)
            .distinct

        val deleted = TableQuery[DeckDeletedTable]
            .map(_.deckId)
            .distinct

        val query = created.filterNot(_.in(deleted))
            .filter(_ === deckId)
            .exists

        db.run(query.result)
    }
}
