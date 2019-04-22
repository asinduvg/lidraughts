package views.html.simul

import play.api.libs.json.Json

import lidraughts.api.Context
import lidraughts.app.templating.Environment._
import lidraughts.app.ui.ScalatagsTemplate._
import lidraughts.common.String.html.safeJsonValue

import controllers.routes

object show {

  def apply(
    sim: lidraughts.simul.Simul,
    socketVersion: lidraughts.socket.Socket.SocketVersion,
    data: play.api.libs.json.JsObject,
    chatOption: Option[lidraughts.chat.UserChat.Mine],
    stream: Option[lidraughts.streamer.Stream]
  )(implicit ctx: Context) = views.html.base.layout(
    moreCss = cssTag("simul.show"),
    title = sim.fullName,
    moreJs = frag(
      jsAt(s"compiled/lidraughts.simul${isProd ?? (".min")}.js"),
      embedJsUnsafe(s"""lidraughts.simul=${
        safeJsonValue(Json.obj(
          "data" -> data,
          "i18n" -> bits.jsI18n(),
          "socketVersion" -> socketVersion.value,
          "userId" -> ctx.userId,
          "chat" -> chatOption.map { c =>
            views.html.chat.json(c.chat, name = trans.chatRoom.txt(), timeout = c.timeout, public = true)
          }
        ))
      }""")
    )
  ) {
      main(cls := "simul")(
        st.aside(cls := "simul__side")(
          div(cls := "simul__meta")(
            div(cls := "game-infos")(
              div(cls := "header")(
                div(cls := List(
                  "variant-icons" -> true,
                  "rich" -> sim.variantRich
                ))(sim.perfTypes.map { pt => span(dataIcon := pt.iconChar) }),
                div(
                  span(cls := "clock")(sim.clock.config.show),
                  div(cls := "setup")(
                    sim.variants.map(_.name).mkString(", "),
                    " • ",
                    trans.casual()
                  )
                )
              ),
              trans.simulHostExtraTime(),
              ": ",
              pluralize("minute", sim.clock.hostExtraMinutes),
              br,
              trans.hostColorX(sim.color match {
                case Some("white") => trans.white()
                case Some("black") => trans.black()
                case _ => trans.randomColor()
              }),
              sim.spotlight.flatMap(_.drawLimit).map { lim =>
                frag(
                  br,
                  if (lim > 0) trans.drawOffersAfterX(lim)
                  else trans.drawOffersNotAllowed()
                )
              },
              sim.targetPct.map { target =>
                frag(
                  br,
                  trans.targetWinningPercentage(s"$target%")
                )
              },
              (sim.chatmode.isDefined && !sim.chatmode.contains(lidraughts.simul.Simul.ChatMode.Everyone)) option {
                frag(
                  br,
                  trans.chatAvailableForX(sim.chatmode match {
                    case Some(lidraughts.simul.Simul.ChatMode.Spectators) => trans.spectatorsOnly()
                    case _ => trans.participantsOnly()
                  })
                )
              },
              sim.allowed.filter(_.nonEmpty).map { allowed =>
                frag(
                  br,
                  trans.simulParticipationLimited(allowed.size)
                )
              }
            ),
            trans.by(usernameOrId(sim.hostId)),
            " ",
            sim.spotlight.fold(momentFromNow(sim.createdAt)) { s => absClientDateTime(s.startsAt) }
          ),
          stream.map { s =>
            views.html.streamer.bits.contextual(s.streamer.userId)
          },
          chatOption.isDefined option views.html.chat.frag
        ),
        div(cls := "simul__main box")(spinner)
      )
    }
}
