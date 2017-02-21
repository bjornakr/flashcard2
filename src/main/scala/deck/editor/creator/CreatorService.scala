package deck.editor.creator

import java.sql.Timestamp
import java.time.{LocalDateTime, ZoneId, ZonedDateTime}
import java.util.UUID

import cats.data.Xor
import common._
import deck.editor._
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
            val title = decode[RequestDto](body)

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

 // TODO: Option? Would allow more spesific error message.

class CreatorService(repository: Repository) {
    private[creator] def save(request: RequestDto): Either[ErrorMessage, EventResponse] = {

        def save(changedEvent: Event): Either[ErrorMessage, EventResponse] = {
            val future = repository.save(changedEvent)
            Await.ready(future, DurationInt(3).seconds).value.get match {
                case Failure(e) => {
                    // TODO: Log error
                    Left(DatabaseError) // or database error - but do not send error message to api?
                }
                case Success(event) => Right(EventResponseMapper(event))
            }
        }

        Event(UUID.randomUUID(), request.title) match {
            case Left(err) => Left(err)
            case Right(ce) => save(ce)
        }
        //        ChangedEvent(title.getOrElse("")).right.map((ce: ChangedEvent) => save(ce))

    }

}







// DOMAIN






// REPO









