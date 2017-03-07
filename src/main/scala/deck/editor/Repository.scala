package deck.editor

import common.DeckExistsQuery
import deck.BaseRepository
import slick.driver.H2Driver.api._
import slick.lifted.TableQuery

import scala.concurrent.Future

class Repository(db: Database) extends BaseRepository(db) with DeckExistsQuery {
    private val deckChangedTable = TableQuery[DeckChangedTable]
    private val insertQuery = deckChangedTable returning deckChangedTable.map(_.id) into ((dto, id) => DeckChangedRowMapper.toDomain(dto.copy(id = id)))

    private[deck] def save(event: Event): Future[Event] = {
        val action = insertQuery += DeckChangedRowMapper.fromDomain(event)
        db.run(action)
    }

    private[deck] def deckExists(deckId: String): Future[Boolean] = deckExists(db, deckId)
}
