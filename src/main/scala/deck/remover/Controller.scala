package deck.remover

import java.sql.Timestamp
import java.time.ZonedDateTime
import java.util.UUID

import common.{UuidParser, _}
import deck.BaseRepository
import deck.editor.DeckChangedTable
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

        val future = repository.existingDeckIds

        Await.ready(future, DurationInt(3).seconds).value.get match {

            case Failure(e) => {
                // Log error
                Left(DatabaseError)
            }
            case Success(createdDeckIds) => {
                for {
                    uuid <- UuidParser(id).right
                    event <- Event(uuid, createdDeckIds.map(UUID.fromString)).right
                    r <- exec(event).right
                } yield r
            }
        }


    }
}

case class Result(deckId: String)


// DOMAIN

private abstract case class Event(deckId: UUID)

private object Event {
    def apply(deckId: UUID, createdDeckIds: Seq[UUID]): Either[ErrorMessage, Event] = {
        if (createdDeckIds.contains(deckId))
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

class Repository(db: Database) extends BaseRepository(db) {

//    def createdDeckIds: Future[Seq[String]] = {
//        val query = TableQuery[DeckChangedTable]
//            .groupBy(r => r.deckId)
//            .map { case (id, g) => id }
//
//        //        val query = TableQuery[DeckChangedTable].filter(_.deckId === deckId)
//        db.run(query.result)
//    }

    def save(event: Event): Future[Result] = {
        val deckDeletedTable = TableQuery[DeckDeletedTable]
        val insertQuery = deckDeletedTable returning deckDeletedTable.map(_.id) into ((r, _) => Result(r.deckId))
        val row = DeckDeletedRow(0, new Timestamp(ZonedDateTime.now.toInstant.getEpochSecond * 1000L), event.deckId.toString)
        val action = insertQuery += row
        db.run(action)
    }
}