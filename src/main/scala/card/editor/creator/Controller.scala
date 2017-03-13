package card.editor.creator

import java.io.Serializable
import java.sql.Timestamp
import java.time.{ZoneId, ZonedDateTime}
import java.util.UUID

import card.editor._
import cats.data.Xor
import common._
import deck.DeckExistsQuery
import io.circe.generic.auto._
import io.circe.parser.decode
import io.circe.syntax._
import org.http4s.dsl._
import org.http4s.{EntityDecoder, HttpService}
import slick.driver.H2Driver.api._
import slick.lifted.{ProvenShape, TableQuery, Tag}

import scala.concurrent.Future


class Controller(appService: AppService) {
    val httpService = HttpService {
        case requestString@POST -> Root / deckId / "card" => {
            val requestJson = EntityDecoder.decodeString(requestString).run
            val request = decode[RequestDto](requestJson)

            request match {
                case Xor.Left(_) => BadRequest(CouldNotParse("body", RequestDto).message)
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

class AppService(repository: Repository) {
    private[creator] def save(deckId: String, request: RequestDto): Either[ErrorMessage, ResponseDto] = {

        def save(changedEvent: Event): Either[ErrorMessage, ResponseDto] = {
            val saveAction = repository.save(changedEvent)
            FutureAwaiter(saveAction)(event => Right(EventResponseMapper(event)))
        }

        FutureAwaiter(repository.deckExists(deckId))(deckExists =>
            RequestToDomainMapper(request, deckId, deckExists) match {
                case Left(err) => Left(err)
                case Right(ce) => save(ce)
            })
    }
}
















// REPOSITORY











