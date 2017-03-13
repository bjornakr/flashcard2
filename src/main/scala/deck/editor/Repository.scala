package deck.editor

import deck.DeckExistsQuery
import slick.driver.H2Driver.api._
import slick.lifted.TableQuery

import scala.concurrent.Future

class Repository(db: Database) extends DeckExistsQuery {
    private val deckChangedTable = TableQuery[DeckChangedTable]
    private val insertQuery = deckChangedTable returning deckChangedTable.map(_.id) into ((dto, id) => DeckChangedRowMapper.toDomain(dto.copy(id = id)))

    def save(event: Event): Future[Event] = {
        val action = insertQuery += DeckChangedRowMapper.fromDomain(event)
        db.run(action)
    }

    def deckExists(deckId: String): Future[Boolean] = deckExists(db, deckId)
}
