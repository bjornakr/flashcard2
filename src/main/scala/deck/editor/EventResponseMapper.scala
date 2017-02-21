package deck.editor

import deck.editor.creator.ChangedEvent

object EventResponseMapper {
    def apply(changedEvent: ChangedEvent) =
        EventResponse(changedEvent.deckId.toString, changedEvent.title)
}
