package card

import org.http4s.HttpService
import org.http4s.dsl._

class Controller(
                    cardCreatorController: card.editor.creator.Controller,
                    cardChangerController: card.editor.changer.Controller,
                    cardRemoverController: card.remover.Controller,
                    cardScorer: card.scorer.Controller
                ) {
    val httpService = HttpService {
        case request@POST -> Root / "deck" / deckId / "card" / "creator" =>
            cardCreatorController(request, deckId)
        case request@POST -> Root / "deck " / deckId / "card" / cardId / "changer" =>
            cardChangerController(request, deckId, cardId)
        case POST -> Root / "card" / cardId / "remover" =>
            cardRemoverController(cardId)
        case POST -> Root / "card" / cardId / "scorer" / "win" =>
            cardScorer.win(cardId)
        case POST -> Root / "card" / cardId / "scorer" / "lose" =>
            cardScorer.lose(cardId)

    }
}
