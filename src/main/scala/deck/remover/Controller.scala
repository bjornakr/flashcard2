package deck.remover

import java.sql.Timestamp
import java.time.{LocalDateTime, ZoneId, ZonedDateTime}
import java.util.UUID

import cats.data.Xor
import common.{UuidParser, _}
import org.http4s.{EntityDecoder, HttpService}
import slick.driver.H2Driver.api._
import slick.lifted.{ProvenShape, TableQuery, Tag}

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}
import org.http4s.dsl.{Root, _}
import org.http4s._
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._

import scalaz.concurrent.Task

class Controller(appService: RemoverService) {
    val httpService = HttpService {
        case DELETE -> Root / id => {
            Ok()
        }
    }
}

class RemoverService(repository: Repository) {
    def delete(id: String): Either[ErrorMessage, Unit] = {
        def exec(id: UUID): Either[ErrorMessage, Unit] = {
            val future = repository.saveEvent(id)
            Await.ready(future, DurationInt(3).seconds).value.get match {
                case Failure(e) => {
                    // Log error
                    Left(DatabaseError) // or database error - but do not send error message to api?
                }
                case Success(_) => Right(())
            }
        }

        for {
            uuid <- UuidParser(id).right
            r <- exec(uuid).right
        } yield r
    }
}


case class DeckDeletedRow(id: Long, t: Timestamp, deckId: String)

class DeckDeletedTable(tag: Tag) extends Table[DeckDeletedRow](tag, "deck_changed_events") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)

    def t = column[Timestamp]("t")

    def deckId = column[String]("deck_id")

    def * : ProvenShape[DeckDeletedRow] = (id, t, deckId) <> (DeckDeletedRow.tupled, DeckDeletedRow.unapply)
}

class Repository(db: Database) {
    def saveEvent(deckId: UUID): Future[Unit] = {
        val event = DeckDeletedRow(0, new Timestamp(ZonedDateTime.now.toInstant.getEpochSecond * 1000L), deckId.toString)
        val action = slick.dbio.DBIO.seq(TableQuery[DeckDeletedTable] += event)
        db.run(action)
    }

}