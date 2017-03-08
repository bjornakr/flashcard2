package card.editor.creator

import java.sql.Timestamp
import java.time.{ZoneId, ZonedDateTime}
import java.util.UUID

import cats.data.Xor
import common._
import io.circe.generic.auto._
import io.circe.parser.decode
import io.circe.syntax._
import org.http4s.dsl._
import org.http4s.{EntityDecoder, HttpService}
import slick.driver.H2Driver.api._
import slick.lifted.{ProvenShape, TableQuery, Tag}

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}


class Controller(appService: AppService) {
    val httpService = HttpService {
        case requestString@POST -> Root / deckId / "card" => {
            val requestJson = EntityDecoder.decodeString(requestString).run
            val request = decode[CreateCardRequestDto](requestJson)

            request match {
                case Xor.Left(_) => BadRequest(s"Could not parse body into ${CreateCardRequestDto.getClass.getCanonicalName}.")
                case Xor.Right(a) => {
                    appService.save(deckId, a) match {
                        case Left(err) => common.ErrorToHttpStatus(err)
                        case Right(b) => Created(b.asJson.noSpaces)
                    }
                }
            }
        }
    }
}

// APPLICATION

trait CardSideDto {
    def term: String
    def description: Option[String]
}

trait CardDto {
    def front: CardSideDto
    def back: CardSideDto
}

case class FrontDto(term: String, description: Option[String]) extends CardSideDto
case class BackDto(term: String, description: Option[String]) extends CardSideDto
case class CreateCardRequestDto(front: FrontDto, back: BackDto) extends CardDto
case class CreateCardResponseDto(cardId: String, deckId: String, front: FrontDto, back: BackDto) extends CardDto


class AppService(repository: Repository) {
    private[creator] def save(deckId: String, request: CreateCardRequestDto): Either[ErrorMessage, CreateCardResponseDto] = {

        def save(changedEvent: Event): Either[ErrorMessage, CreateCardResponseDto] = {
            val future = repository.save(changedEvent)
            Await.ready(future, DurationInt(3).seconds).value.get match {
                case Failure(e) => {
                    // TODO: Log error
                    Left(DatabaseError) // or database error - but do not send error message to api?
                }
                case Success(event) => Right(EventResponseMapper(event))
            }
        }

        RequestToDomainMapper(request, deckId) match {
            case Left(err) => Left(err)
            case Right(ce) => save(ce)
        }
    }
}

object RequestToDomainMapper {
    def apply(request: CreateCardRequestDto, deckId: String): Either[ErrorMessage, Event] = {
        def emptyToNone(s: String) = if (s.trim == "") None else s

        request.front.description.map(emptyToNone)
        request.back.description.map(emptyToNone)
        val front = CardSide.front(request.front.term, request.front.description)
        val back = CardSide.back(request.back.term, request.back.description)

        for {
            dId <- UuidParser(deckId).right
            f <- front.right
            b <- back.right
        } yield Event(dId, f, b)
    }
}


object EventResponseMapper {
    def apply(event: Event): CreateCardResponseDto = {
        val front = FrontDto(event.front.term, event.front.description)
        val back = BackDto(event.back.term, event.back.description)
        CreateCardResponseDto(event.cardId.toString, event.deckId.toString, front, back)
    }
}


// DOMAIN
abstract case class CardSide(term: String, description: Option[String])

abstract case class Event(t: ZonedDateTime, cardId: UUID, deckId: UUID, front: CardSide, back: CardSide)

object CardSide {
    private def proc(term: String, description: Option[String], errorMessage: String): Either[ErrorMessage, CardSide] = {
        term.trim match {
            case "" => Left(CannotBeEmpty("FrontSide::term"))
            case t => Right(new CardSide(t, description) {})
        }
    }


    def front(term: String, description: Option[String]): Either[ErrorMessage, CardSide] =
        proc(term, description, "FrontSide::term")


    def back(term: String, description: Option[String]): Either[ErrorMessage, CardSide] =
        proc(term, description, "BackSide::term")

}

object Event {
    def apply(deckId: UUID, front: CardSide, back: CardSide): Event =
        new Event(ZonedDateTime.now, UUID.randomUUID(), deckId, front, back) {}
}


// REPOSITORY

class Repository(db: Database) {

    private val changedTable = TableQuery[ChangedTable]
    private val insertQuery = changedTable returning changedTable.map(_.id) into ((dto, id) => ChangedRowToDomain(dto.copy(id = id)))

    def save(event: Event): Future[Event] = {
        val action = insertQuery += DomainToChangedRow(event)
        db.run(action)
    }

    def deckExists(deckId: String) = {

    }
}

object ChangedRowToDomain {
    def apply(row: ChangedRow): Event = {
        val t = ZonedDateTime.ofInstant(row.t.toInstant, ZoneId.of("UTC"))
        val front = new CardSide(row.frontTerm, row.frontDescription) {}
        val back = new CardSide(row.backTerm, row.backDescription) {}
        new Event(t, UUID.fromString(row.cardId), UUID.fromString(row.deckId), front, back) {}
    }
}

object DomainToChangedRow {
    def apply(e: Event): ChangedRow =
        ChangedRow(
            id = 0,
            t = new Timestamp(e.t.toEpochSecond),
            cardId = e.cardId.toString,
            deckId = e.deckId.toString,
            frontTerm = e.front.term,
            frontDescription = e.front.description,
            backTerm = e.back.term,
            backDescription = e.back.description
        )
}

case class ChangedRow(id: Long, t: Timestamp,
                      cardId: String, deckId: String,
                      frontTerm: String, frontDescription: Option[String],
                      backTerm: String, backDescription: Option[String])


class ChangedTable(tag: Tag) extends Table[ChangedRow](tag, "card_changed_events") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)

    def t = column[Timestamp]("t")

    def cardId = column[String]("card_id")

    def deckId = column[String]("deck_id")

    def frontTerm = column[String]("front_term")

    def frontDescription = column[Option[String]]("front_description")

    def backTerm = column[String]("back_term")

    def backDescription = column[Option[String]]("back_description")

    def * : ProvenShape[ChangedRow] = (id, t, cardId, deckId, frontTerm, frontDescription, backTerm, backDescription) <> (ChangedRow.tupled, ChangedRow.unapply)
}
