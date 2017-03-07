package deck

import deck.editor.DeckChangedTable
import slick.driver.H2Driver.api._
import slick.lifted.TableQuery

import scala.concurrent.Future


abstract class BaseRepository(db: Database) {
    def existingDeckIds: Future[Seq[String]] = {
        val query = TableQuery[DeckChangedTable]
            .groupBy(r => r.deckId)
            .map { case (id, g) => id }

        db.run(query.result)
    }
}
