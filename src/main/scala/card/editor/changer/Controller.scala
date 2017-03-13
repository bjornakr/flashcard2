package card.editor.changer

import card.editor._
import cats.data.Xor
import common.{CouldNotFindEntityWithId, CouldNotParse, ErrorMessage, FutureAwaiter}
import org.http4s.dsl._
import org.http4s.{EntityDecoder, HttpService, Request}
import io.circe.generic.auto._
import io.circe.parser.decode
import io.circe.syntax._

class Controller(appService: AppService) {
    def apply(request: Request, deckId: String, cardId: String) = {
        val requestJson = EntityDecoder.decodeString(request).run
        val requestDto = decode[RequestDto](requestJson)

        requestDto match {
            case Xor.Left(_) => BadRequest(CouldNotParse("body", RequestDto).message)
            case Xor.Right(a) => {
                appService.save(deckId, cardId, a) match {
                    case Left(err) => common.ErrorToHttpStatus(err)
                    case Right(b) => Created(b.asJson.noSpaces)
                }
            }
        }
    }

//    val httpService = HttpService {
//        case requestString@POST -> Root / deckId / "card" / cardId => {
//            val requestJson = EntityDecoder.decodeString(requestString).run
//            val request = decode[RequestDto](requestJson)
//
//            request match {
//                case Xor.Left(_) => BadRequest(CouldNotParse("body", RequestDto).message)
//                case Xor.Right(a) => {
//                    appService.save(deckId, cardId, a) match {
//                        case Left(err) => common.ErrorToHttpStatus(err)
//                        case Right(b) => Created(b.asJson.noSpaces)
//                    }
//                }
//            }
//        }
//    }
}

class AppService(repository: Repository) {
    def save(deckId: String, cardId: String, request: RequestDto): Either[ErrorMessage, ResponseDto] = {
        def save(changedEvent: Event): Either[ErrorMessage, ResponseDto] = {
            val saveAction = repository.save(changedEvent)
            FutureAwaiter(saveAction)(event => Right(EventResponseMapper(event)))
        }

        FutureAwaiter(repository.deckExists(deckId))(deckExists =>
            FutureAwaiter(repository.cardExists(cardId))(cardExists => {
                (deckExists, cardExists) match {
                    case (true, true) =>
                        RequestToDomainMapper(request, deckId) match {
                            case Left(err) => Left(err)
                            case Right(ce) => save(ce)
                        }
                    case (false, _) => Left(CouldNotFindEntityWithId("Deck", deckId))
                    case (_, false) => Left(CouldNotFindEntityWithId("Card", deckId))
                }
            }))
    }
}


