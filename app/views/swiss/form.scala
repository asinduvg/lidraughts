package views.html.swiss

import play.api.data.Form

import controllers.routes
import lidraughts.api.Context
import lidraughts.app.templating.Environment._
import lidraughts.tournament.{ DataForm => TourForm }
import lidraughts.swiss.{ Swiss, SwissForm }
import lidraughts.app.ui.ScalatagsTemplate._
import lidraughts.hub.lightTeam.TeamId

object form {

  def create(form: Form[_], teamId: TeamId)(implicit ctx: Context) =
    views.html.base.layout(
      title = "New Swiss tournament",
      moreCss = cssTag("swiss.form"),
      moreJs = frag(
        flatpickrTag,
        jsTag("tournamentForm.js")
      )
    ) {
        val fields = new SwissFields(form)
        main(cls := "page-small")(
          div(cls := "swiss__form tour__form box box-pad")(
            h1("New Swiss tournament"),
            postForm(cls := "form3", action := routes.Swiss.create(teamId))(
              form3.split(fields.name, fields.nbRounds),
              form3.split(fields.rated, fields.variant),
              fields.clock,
              fields.description,
              fields.startsAt,
              form3.globalError(form),
              form3.actions(
                a(href := routes.Team.show(teamId))(trans.cancel()),
                form3.submit(trans.createANewTournament(), icon = "g".some)
              )
            )
          )
        )
      }

  def edit(swiss: Swiss, form: Form[_])(implicit ctx: Context) =
    views.html.base.layout(
      title = swiss.name,
      moreCss = cssTag("swiss.form"),
      moreJs = frag(
        flatpickrTag,
        jsTag("tournamentForm.js")
      )
    ) {
        val fields = new SwissFields(form)
        main(cls := "page-small")(
          div(cls := "swiss__form box box-pad")(
            h1("Edit ", swiss.name),
            postForm(cls := "form3", action := routes.Swiss.update(swiss.id.value))(
              form3.split(fields.name, fields.nbRounds),
              form3.split(fields.rated, fields.variant),
              fields.clock,
              fields.description,
              swiss.isCreated option fields.startsAt,
              form3.globalError(form),
              form3.actions(
                a(href := routes.Swiss.show(swiss.id.value))(trans.cancel()),
                form3.submit(trans.save(), icon = "g".some)
              )
            ),
            postForm(cls := "terminate", action := routes.Swiss.terminate(swiss.id.value))(
              submitButton(dataIcon := "j", cls := "text button button-red confirm")(
                "Cancel the tournament"
              )
            )
          )
        )
      }
}

final private class SwissFields(form: Form[_])(implicit ctx: Context) {

  def name =
    form3.group(form("name"), trans.name()) { f =>
      div(
        form3.input(f),
        br,
        small(cls := "form-help")(
          trans.safeTournamentName(),
          br,
          trans.inappropriateNameWarning(),
          br,
          trans.emptyTournamentName()
        )
      )
    }
  def nbRounds =
    form3.group(form("nbRounds"), "Number of rounds", half = true)(
      form3.input(_, typ = "number")
    )

  def rated = form3.checkbox(
    form("rated"),
    trans.rated(),
    help = raw("Games are rated<br>and impact players ratings").some
  )
  def variant =
    form3.group(form("variant"), trans.variant(), half = true)(
      form3.select(_, translatedVariantChoicesWithVariants(_.key).map(x => x._1 -> x._2))
    )
  def clock =
    form3.split(
      form3.group(form("clock.limit"), trans.clockInitialTime(), half = true)(
        form3.select(_, SwissForm.clockLimitChoices)
      ),
      form3.group(form("clock.increment"), trans.increment(), half = true)(
        form3.select(_, TourForm.clockIncrementChoices)
      )
    )
  def description =
    form3.group(
      form("description"),
      frag("Tournament description"),
      help = frag("Anything special you want to tell the participants? Try to keep it short.").some
    )(form3.textarea(_)(rows := 2))
  def startsAt =
    form3.group(
      form("startsAt"),
      frag("Tournament start date")
    )(form3.flatpickr(_))
}
