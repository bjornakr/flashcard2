package card.editor

private[card] trait CardDto {
    def front: CardSideDto
    def back: CardSideDto
}
