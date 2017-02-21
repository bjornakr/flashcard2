package deck.editor

import deck.BaseRepository
import slick.driver.H2Driver.api._
import slick.lifted.TableQuery

import scala.concurrent.Future

/**
  * Created by bjornkri on 21.02.2017.
  */
class Repository(db: Database) extends BaseRepository(db) {
    type AffectedRowsCount = Int
    private val deckChangedTable = TableQuery[DeckChangedTable]
    private val insertQuery = deckChangedTable returning deckChangedTable.map(_.id) into ((dto, id) => DeckChangedRowMapper.toDomain(dto.copy(id = id)))

    private[deck] def save(event: Event): Future[Event] = {
        val action = insertQuery += DeckChangedRowMapper.fromDomain(event)
        db.run(action)
    }
}
