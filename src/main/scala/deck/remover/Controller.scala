package deck.remover

import java.sql.Timestamp
import java.time.ZonedDateTime
import java.util.UUID

import common.{UuidParser, _}
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s.HttpService
import org.http4s.dsl.{Root, _}
import slick.driver.H2Driver.api._
import slick.lifted.{ProvenShape, TableQuery, Tag}

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}

class Controller(appService: RemoverService) {
    val httpService = HttpService {
        case POST -> Root / id => {
            appService.saveEvent(id) match {
                case Left(e) => ErrorToHttpStatus(e)
                case Right(result) => Created(result.asJson.noSpaces)
            }
        }
    }
}

// Application

class RemoverService(repository: Repository) {
    def saveEvent(id: String): Either[ErrorMessage, Result] = {
        def exec(id: UUID): Either[ErrorMessage, Result] = {
            val future = repository.save(id.toString)
            Await.ready(future, DurationInt(3).seconds).value.get match {
                case Failure(e) => {
                    // Log error
                    Left(DatabaseError) // or database error - but do not send error message to api?
                }
                case Success(deckId) => Right(deckId)
            }
        }

        for {
            uuid <- UuidParser(id).right
            r <- exec(uuid).right
        } yield r
    }
}

case class Result(deckId: String)

// Repository

case class DeckDeletedRow(id: Long, t: Timestamp, deckId: String)

class DeckDeletedTable(tag: Tag) extends Table[DeckDeletedRow](tag, "deck_deleted_events") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)

    def t = column[Timestamp]("t")

    def deckId = column[String]("deck_id")

    def * : ProvenShape[DeckDeletedRow] = (id, t, deckId) <> (DeckDeletedRow.tupled, DeckDeletedRow.unapply)
}

class Repository(db: Database) {

    def save(deckId: String): Future[Result] = {
        val deckDeletedTable = TableQuery[DeckDeletedTable]
        val insertQuery = deckDeletedTable returning deckDeletedTable.map(_.id) into ((r, _) => Result(r.deckId))
        val event = DeckDeletedRow(0, new Timestamp(ZonedDateTime.now.toInstant.getEpochSecond * 1000L), deckId)
        val action = insertQuery += event
        db.run(action)
    }
}