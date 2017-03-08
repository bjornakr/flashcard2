package deck.remover

import java.sql.Timestamp
import java.time.ZonedDateTime
import java.util.UUID

import common.{UuidParser, _}
import deck.DeckExistsQuery
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s.HttpService
import org.http4s.dsl.{Root, _}
import slick.driver.H2Driver.api._
import slick.lifted.{ProvenShape, TableQuery, Tag}

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}

// TODO: Prevent removing twice

class Controller(appService: AppService) {
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

class AppService(repository: Repository) {
    def saveEvent(id: String): Either[ErrorMessage, Result] = {

        def exec(event: Event): Either[ErrorMessage, Result] = {
            val future = repository.save(event)
            Await.ready(future, DurationInt(3).seconds).value.get match {
                case Failure(e) => {
                    // Log error
                    Left(DatabaseError) // or database error - but do not send error message to api?
                }
                case Success(deckId) => Right(deckId)
            }
        }

        val future = repository.deckExists(id)
        Await.ready(future, DurationInt(3).seconds).value.get match {
            case Failure(e) => {
                // Log error
                Left(DatabaseError)
            }
            case Success(deckExists) => {
                for {
                    uuid <- UuidParser(id).right
                    event <- Event(uuid, deckExists).right
                    r <- exec(event).right
                } yield r
            }
        }
    }
}

case class Result(deckId: String)


// DOMAIN

abstract case class Event(deckId: UUID)

private object Event {
    def apply(deckId: UUID, deckExists: Boolean): Either[ErrorMessage, Event] = {
        if (deckExists)
            Right(new Event(deckId) {})
        else {
            Left(CouldNotFindEntityWithId("Deck", deckId.toString))
        }
    }
}


// Repository

case class DeckDeletedRow(id: Long, t: Timestamp, deckId: String)

class DeckDeletedTable(tag: Tag) extends Table[DeckDeletedRow](tag, "deck_deleted_events") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)

    def t = column[Timestamp]("t")

    def deckId = column[String]("deck_id")

    def * : ProvenShape[DeckDeletedRow] = (id, t, deckId) <> (DeckDeletedRow.tupled, DeckDeletedRow.unapply)
}

class Repository(db: Database) extends DeckExistsQuery {
    def deckExists(deckId: String): Future[Boolean] =
        deckExists(db, deckId)

    def save(event: Event): Future[Result] = {
        val deckDeletedTable = TableQuery[DeckDeletedTable]
        val insertQuery = deckDeletedTable returning deckDeletedTable.map(_.id) into ((r, _) => Result(r.deckId))
        val row = DeckDeletedRow(0, new Timestamp(ZonedDateTime.now.toInstant.getEpochSecond * 1000L), event.deckId.toString)
        val action = insertQuery += row
        db.run(action)
    }
}