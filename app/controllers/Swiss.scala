package controllers

import play.api.libs.json._
import play.api.mvc._

import lidraughts.api.Context
import lidraughts.app._
import lidraughts.chat.Chat
import lidraughts.swiss.{ Swiss => SwissModel }
import lidraughts.swiss.Swiss.{ Id => SwissId }
import views._

object Swiss extends LidraughtsController {

  private def env = Env.swiss

  private def swissNotFound(implicit ctx: Context) = NotFound(html.swiss.bits.notFound())

  def show(id: String) = Open { implicit ctx =>
    env.api.byId(SwissId(id)) flatMap {
      _.fold(swissNotFound.fuccess) { swiss =>
        val page = getInt("page") | 1
        for {
          version <- env.version(swiss.id)
          isInTeam <- ctx.userId.??(u => Env.team.cached.teamIds(u).dmap(_ contains swiss.teamId))
          json <- env.json(
            swiss = swiss,
            me = ctx.me,
            page = page,
            socketVersion = version.some,
            isInTeam = isInTeam
          )
          canChat <- canHaveChat(swiss)
          chat <- canChat ?? Env.chat.api.userChat.cached
            .findMine(Chat.Id(swiss.id.value), ctx.me)
            .dmap(some)
          _ <- chat ?? { c => Env.user.lightUserApi.preloadMany(c.chat.userIds) }
        } yield Ok(html.swiss.show(swiss, json, chat))
      }
    }
  }

  def form(teamId: String) = Auth { implicit ctx => me =>
    Ok(html.swiss.form.create(env.forms.create, teamId)).fuccess
  }

  def create(teamId: String) = AuthBody { implicit ctx => me =>
    env.forms.create
      .bindFromRequest()(ctx.body)
      .fold(
        err => BadRequest(html.swiss.form.create(err, teamId)).fuccess,
        data =>
          Tournament.rateLimitCreation(me, false, ctx.req) {
            env.api.create(data, me, teamId) map { swiss =>
              Redirect(routes.Swiss.show(swiss.id.value))
            }
          }
      )
  }

  def websocket(id: String, apiVersion: Int) = SocketOption[JsValue] { implicit ctx =>
    getSocketUid("sri") ?? { uid =>
      env.socketHandler.join(id, uid, ctx.me, getSocketVersion, apiVersion)
    }
  }

  def join(id: String) = AuthBody { implicit ctx => me =>
    NoLameOrBot {
      Env.team.cached.teamIds(me.id) flatMap { teamIds =>
        env.api.joinWithResult(SwissId(id), me, teamIds.contains) flatMap { result =>
          negotiate(
            html = Redirect(routes.Swiss.show(id)).fuccess,
            api = _ =>
              fuccess {
                if (result) jsonOkResult
                else BadRequest(Json.obj("joined" -> false))
              }
          )
        }
      }
    }
  }

  private def canHaveChat(swiss: SwissModel)(implicit ctx: Context): Fu[Boolean] =
    (swiss.hasChat && ctx.noKid) ?? ctx.userId.?? {
      Env.team.api.belongsTo(swiss.teamId, _)
    }
}
