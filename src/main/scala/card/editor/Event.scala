package card.editor

import java.time.ZonedDateTime
import java.util.UUID

import common.{CouldNotFindEntityWithId, ErrorMessage}

private[card] abstract case class Event(t: ZonedDateTime, cardId: UUID, deckId: UUID, front: CardSide, back: CardSide)

private [card] object Event {
    def apply(deckId: UUID, front: CardSide, back: CardSide, deckExists: Boolean): Either[ErrorMessage, Event] =
        if (deckExists)
            Right(new Event(ZonedDateTime.now, UUID.randomUUID(), deckId, front, back) {})
        else
            Left(CouldNotFindEntityWithId("Deck", deckId.toString))
}