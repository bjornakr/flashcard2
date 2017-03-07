package deck

import deck.editor.{DeckChangedRow, DeckChangedTable, Event}
import deck.remover.DeckDeletedTable
import slick.lifted.{ProvenShape, QueryBase, TableQuery, Tag}

import scala.concurrent.Future
import slick.driver.H2Driver.api._

trait DeckExistsQuery {
    def deckExists(db: Database): Future[Seq[String]] = {
        val created = TableQuery[DeckChangedTable]
            .map(a => a.deckId)
            .distinct

        val deleted = TableQuery[DeckDeletedTable]
            .map(_.deckId)
            .distinct

//        for {
//            d <- deleted
//            q <- query.filterNot(id => id.inSet(d))
//        } yield q


//        val exists = query.filterNot(c => c.inSet(deleted))

        val ruru = created.filterNot(_.in(deleted))

        //        val z: QueryBase[Seq[String]] = for {
        //             d <- deleted
        //             q <- query if q != d
        //        } yield q
        //

//        db.run(z.result)
    }
}


abstract class BaseRepository(db: Database) {
    def existingDeckIds: Future[Seq[String]] = {
        val query = TableQuery[DeckChangedTable]
            .groupBy(r => r.deckId)
            .map { case (id, g) => id }

        db.run(query.result)
    }
}
