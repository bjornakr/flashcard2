package card.editor

import common.{ErrorMessage, UuidParser}

private object RequestToDomainMapper {
    def apply(request: RequestDto, deckId: String): Either[ErrorMessage, Event] = {
        val front = CardSide.front(request.front.term, request.front.description)
        val back = CardSide.back(request.back.term, request.back.description)

        for {
            dId <- UuidParser(deckId).right
            f <- front.right
            b <- back.right
        } yield Event(dId, f, b)
    }
}
