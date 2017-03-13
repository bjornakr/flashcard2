package card.editor

/**
  * Created by bjornkri on 13.03.2017.
  */
private object EventResponseMapper {
    def apply(event: Event): ResponseDto = {
        val front = FrontDto(event.front.term, event.front.description)
        val back = BackDto(event.back.term, event.back.description)
        ResponseDto(event.cardId.toString, event.deckId.toString, front, back)
    }
}
