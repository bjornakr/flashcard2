package deck.editor

object EventResponseMapper {
    def apply(changedEvent: Event) =
        EventResponse(changedEvent.deckId.toString, changedEvent.title)
}
