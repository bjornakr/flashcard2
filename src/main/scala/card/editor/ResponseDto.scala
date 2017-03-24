package card.editor

private[editor] case class ResponseDto(cardId: String, deckId: String, front: FrontDto, back: BackDto) extends CardDto
