package deck.editor

private[deck] object EventResponseMapper {
    def apply(changedEvent: Event) =
        EventResponse(changedEvent.deckId.toString, changedEvent.title)
}
