package card.editor

private[card] case class ResponseDto(cardId: String, deckId: String, front: FrontDto, back: BackDto) extends CardDto
