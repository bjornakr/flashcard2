package deck.editor

import java.time.ZonedDateTime
import java.util.UUID

import common.{CannotBeEmpty, ErrorMessage}

private[deck] abstract case class Event(t: ZonedDateTime, deckId: UUID, title: String)

private[deck] object Event {
    def apply(id: UUID, title: String): Either[ErrorMessage, Event] = {
        title.trim match {
            case "" => Left(CannotBeEmpty("title"))
            case a => Right(new Event(ZonedDateTime.now, id, a) {})
        }
    }
}