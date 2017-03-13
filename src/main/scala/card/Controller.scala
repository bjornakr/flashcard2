package card

import org.http4s.HttpService
import org.http4s.dsl._

class Controller(
                    cardCreatorController: card.editor.creator.Controller,
                    cardChangerController: card.editor.changer.Controller
                ) {
    val httpService = HttpService {
        case request@POST -> Root / deckId / "card" / "creator" =>
            cardCreatorController(request, deckId)
        case request@POST -> Root / deckId / "card" / cardId / "changer" =>
            cardChangerController(request, deckId, cardId)
    }
}
