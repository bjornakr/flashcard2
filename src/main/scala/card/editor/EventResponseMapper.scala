package card.editor

private object EventResponseMapper {
    def apply(event: Event): ResponseDto = {
        val front = FrontDto(event.front.term, event.front.description)
        val back = BackDto(event.back.term, event.back.description)
        ResponseDto(event.cardId.toString, event.deckId.toString, front, back)
    }
}
