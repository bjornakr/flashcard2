package main

import org.http4s.server.Server
import org.http4s.server.blaze._

class Main(
              authenticator: authentication.Controller,
              deckCreator: deck.editor.creator.Controller,
              deckChanger: deck.editor.changer.Controller,
              deckRemover: deck.remover.Controller,
              deckViewer: deck.viewer.Controller,
              cardController: card.Controller
          ) {

    def createServer: Server = createServer(8070)

    def createServer(port: Int): Server = {
        val builder = BlazeBuilder.bindHttp(port, "localhost")
            .mountService(authenticator.httpService, "/api/auth")
            .mountService(authenticator.loginService, "/api/login")
            .mountService(deckCreator.httpService, "/api/deck/creator")
            .mountService(deckChanger.httpService, "/api/deck/changer")
            .mountService(deckRemover.httpService, "/api/deck/remover")
            .mountService(deckViewer.httpService, "/api/deck/viewer")
            .mountService(cardController.httpService, "/api")
        builder.run
    }

}
