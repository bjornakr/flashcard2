package deck.editor.creator

import java.sql.Timestamp
import java.time.{LocalDateTime, ZoneId, ZonedDateTime}
import java.util.UUID

import cats.data.Xor
import common._
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


// TODO: Revise naming
// TODO: Logging
class Controller(appService: CreatorService) {
    val httpService = HttpService {
        case request@POST -> Root => {
            val body = EntityDecoder.decodeString(request).run
            val title = decode[CreateRequest](body)

            title match {
                case Xor.Left(_) => BadRequest("Error parsing body.")
                case Xor.Right(a) => {
                    appService.save(a) match {
                        case Left(err) => ErrorToHttpStatus(err)
                        case Right(a) => Created(a.asJson.noSpaces)
                    }
                }
            }

        }
    }
}


// APPLICATION

case class CreateRequest(title: String) // TODO: Option? Would allow more spesific error message.

class CreatorService(repository: Repository) {
    private[creator] def save(request: CreateRequest): Either[ErrorMessage, ChangedEventResult] = {

        def save(changedEvent: ChangedEvent): Either[ErrorMessage, ChangedEventResult] = {
            val future = repository.save(changedEvent)
            Await.ready(future, DurationInt(3).seconds).value.get match {
                case Failure(e) => {
                    // Log error
                    Left(DatabaseError) // or database error - but do not send error message to api?
                }
                case Success(event) => Right(ChangedEventResultMapper(event))
            }
        }

        ChangedEvent(request.title) match {
            case Left(err) => Left(err)
            case Right(ce) => save(ce)
        }
        //        ChangedEvent(title.getOrElse("")).right.map((ce: ChangedEvent) => save(ce))

    }

}


// TODO: Response
case class ChangedEventResult(deckId: String, title: String)

object ChangedEventResultMapper {
    def apply(changedEvent: ChangedEvent) =
        ChangedEventResult(changedEvent.deckId.toString, changedEvent.title)
}


// DOMAIN

abstract case class ChangedEvent(t: ZonedDateTime, deckId: UUID, title: String)

private object ChangedEvent {
    def apply(title: String): Either[ErrorMessage, ChangedEvent] = {
        title.trim match {
            case "" => Left(CannotBeEmpty("title"))
            case a => Right(new ChangedEvent(ZonedDateTime.now, UUID.randomUUID(), a) {})
        }
    }
}


// REPO

class Repository(db: Database) {
    type AffectedRowsCount = Int
    private val deckChangedTable = TableQuery[DeckChangedTable]
    //private val insertQuery = deckChangedTable returning deckChangedTable.map(_.id) into ((dto, id) => dto.copy(id = id))
    private val insertQuery = deckChangedTable returning deckChangedTable.map(_.id) into ((dto, id) => DeckChangedRowMapper.toDomain(dto.copy(id = id)))


    private[creator] def save(event: ChangedEvent): Future[ChangedEvent] = {
        val action = insertQuery += DeckChangedRowMapper.fromDomain(event)
        //val f1 = db.run(deckChangedTable += DeckChangedRowMapper.fromDomain(event))
        db.run(action)
        // f1.onSuccess(_ => )

    }

    //def get(id: Long): Future[Option[ChangedEventResult]] =
        //db.run(deckChangedTable.filter(_.id === id).result).map(_.headOption.map(DeckChangedRowMapper.toResponse))


}

case class DeckChangedRow(id: Long, t: Timestamp, deckId: String, title: String)

object DeckChangedRowMapper {
    def fromDomain(d: ChangedEvent): DeckChangedRow =
        DeckChangedRow(0, new Timestamp(d.t.toEpochSecond), d.deckId.toString, d.title)

    def toDomain(d: DeckChangedRow): ChangedEvent = {
        val t = ZonedDateTime.ofInstant(d.t.toInstant, ZoneId.of("UTC"))
        val deckId = UUID.fromString(d.deckId)
        new ChangedEvent(t, deckId, d.title) {}
    }

//    def toResponse(d: DeckChangedRow): ChangedEventResult =
//        ChangedEventResult(d.deckId, d.title)

//    def toDomain(d: DeckChangedRow): ChangedEvent =
//        new ChangedEvent(d.deckId, d.t, , d.title) {}

}


class DeckChangedTable(tag: Tag) extends Table[DeckChangedRow](tag, "deck_changed_events") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)

    def t = column[Timestamp]("t")

    def deckId = column[String]("deck_id")

    def title = column[String]("title")

    def * : ProvenShape[DeckChangedRow] = (id, t, deckId, title) <> (DeckChangedRow.tupled, DeckChangedRow.unapply)
}
