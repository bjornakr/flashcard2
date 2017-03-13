package card.editor

import common.{ErrorMessage, UuidParser}

/**
  * Created by bjornkri on 13.03.2017.
  */
private object RequestToDomainMapper {
    def apply(request: RequestDto, deckId: String, deckExists: Boolean): Either[ErrorMessage, Event] = {
        val front = CardSide.front(request.front.term, request.front.description)
        val back = CardSide.back(request.back.term, request.back.description)

        for {
            dId <- UuidParser(deckId).right
            f <- front.right
            b <- back.right
            e <- Event(dId, f, b, deckExists).right
        } yield e
    }
}
