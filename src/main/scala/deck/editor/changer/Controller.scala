package deck.editor.changer

import java.util.UUID

import cats.data.Xor
import common._
import deck.editor._
import io.circe.generic.auto._
import io.circe.parser.decode
import io.circe.syntax._
import org.http4s.dsl._
import org.http4s.{EntityDecoder, HttpService}
import slick.driver.H2Driver.api._
import slick.lifted.TableQuery

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}


class Controller(appService: AppService) {
    val httpService = HttpService {
        case request@POST -> Root / id => {
            val body = EntityDecoder.decodeString(request).run
            val title = decode[RequestDto](body)

            title match {
                case Xor.Left(_) => BadRequest(s"Could not parse body into ${RequestDto.getClass.getCanonicalName}.")
                case Xor.Right(a) => {
                    appService.save(id, a) match {
                        case Left(err) => ErrorToHttpStatus(err)
                        case Right(a) => Created(a.asJson.noSpaces)
                    }
                }
            }
        }
    }
}

class AppService(repository: Repository) {
    def save(id: String, request: RequestDto): Either[ErrorMessage, EventResponse] = {
        def exec(event: Event): Either[ErrorMessage, EventResponse] = {
            val future = repository.save(event)
            Await.ready(future, DurationInt(3).seconds).value.get match {
                case Failure(e) => {
                    // TODO: Log error
                    Left(DatabaseError) // or database error - but do not send error message to api?
                }
                case Success(a) => Right(EventResponseMapper(a))
            }
        }

        UuidParser(id) match {
            case Left(e) => Left(e)
            case Right(uuid) => {
                val future = repository.deckExists(uuid.toString)
                Await.ready(future, DurationInt(3).seconds).value.get match {
                    case Failure(e) => {
                        // TODO: Log error
                        Left(DatabaseError)
                    }
                    case Success(deckExists) => {
                        for {
                            event <- Event(uuid, request.title, deckExists).right
                            response <- exec(event).right
                        } yield response
                    }
                }
            }
        }


    }
}

object Event {
    def apply(deckId: UUID, newTitle: String, deckExists: Boolean): Either[ErrorMessage, Event] = {
            if (deckExists)
                deck.editor.Event(deckId, newTitle)
            else
                Left(CouldNotFindEntityWithId("Deck", deckId.toString))
    }
}




