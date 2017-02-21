package deck.editor.creator

import java.util.UUID

import cats.data.Xor
import common._
import deck.editor._
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._
import org.http4s.dsl.{Root, _}
import org.http4s.{EntityDecoder, HttpService}

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}


// TODO: Logging
class Controller(appService: AppService) {
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


class AppService(repository: Repository) {
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
    }
}







