package main

import org.http4s.server.Server
import org.http4s.server.blaze._

class Main(
              deckCreator: deck.editor.creator.Controller,
              deckChanger: deck.editor.changer.Controller,
              deckRemover: deck.remover.Controller

          ) {

    def createServer: Server = createServer(8070)

    def createServer(port: Int): Server = {
        val builder = BlazeBuilder.bindHttp(port, "localhost")
            .mountService(deckCreator.httpService, "/api/deck/creator")
            .mountService(deckChanger.httpService, "/api/deck/changer")
            .mountService(deckRemover.httpService, "/api/deck/remover")
        builder.run
    }

}
