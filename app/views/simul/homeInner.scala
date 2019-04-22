package views.html.simul

import play.api.libs.json.Json

import lidraughts.api.Context
import lidraughts.app.templating.Environment._
import lidraughts.app.ui.ScalatagsTemplate._

import controllers.routes

object homeInner {

  def apply(
    createds: List[lidraughts.simul.Simul],
    starteds: List[lidraughts.simul.Simul],
    finisheds: List[lidraughts.simul.Simul]
  )(implicit ctx: Context) =
    div(cls := "box")(
      h1(trans.simultaneousExhibitions()),
      table(cls := "slist slist-pad")(
        thead(
          tr(
            th(trans.createdSimuls()),
            th(trans.host()),
            th(trans.players())
          )
        ),
        tbody(
          createds.map { sim =>
            tr(cls := "scheduled")(
              simTd(sim),
              simHost(sim),
              td(cls := "text", dataIcon := "r")(sim.applicants.size)
            )
          },
          ctx.isAuth option tr(cls := "create")(
            td(colspan := "4")(
              a(href := routes.Simul.form(), cls := "action button text")(trans.hostANewSimul())
            )
          )
        ),
        starteds.nonEmpty option (frag(
          thead(
            tr(
              th(trans.eventInProgress()),
              th(trans.host()),
              th(trans.players())
            )
          ),
          starteds.map { sim =>
            tr(
              simTd(sim),
              simHost(sim),
              td(cls := "text", dataIcon := "r")(sim.pairings.size)
            )
          }
        )),
        thead(
          tr(
            th(trans.finished()),
            th(trans.host()),
            th(trans.players())
          )
        ),
        tbody(
          finisheds.map { sim =>
            tr(
              simTd(sim),
              simHost(sim),
              td(cls := "text", dataIcon := "r")(sim.pairings.size)
            )
          }
        )
      )
    )

  private def simTd(sim: lidraughts.simul.Simul)(implicit ctx: Context) =
    td(cls := "header")(
      a(href := routes.Simul.show(sim.id))(
        span(cls := "name")(sim.fullName),
        bits.setup(sim)
      )
    )

  private def simHost(sim: lidraughts.simul.Simul) =
    td(cls := "host")(
      userIdLink(sim.hostId.some, withOnline = false),
      br,
      strong(sim.hostRating)
    )
}
