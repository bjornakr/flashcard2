package card.editor

import java.time.ZonedDateTime
import java.util.UUID

private[card] abstract case class Event(t: ZonedDateTime, cardId: UUID, deckId: UUID, front: CardSide, back: CardSide)

private [card] object Event {
    def apply(deckId: UUID, front: CardSide, back: CardSide): Event =
            new Event(ZonedDateTime.now, UUID.randomUUID(), deckId, front, back) {}
}