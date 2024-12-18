package lidraughts.tournament

import akka.actor._
import akka.pattern.pipe
import play.api.libs.iteratee._
import play.api.libs.json._
import scala.collection.breakOut
import scala.concurrent.duration._

import actorApi._
import lidraughts.chat.Chat
import lidraughts.hub.Trouper
import lidraughts.socket.actorApi.{ Connected => _, _ }
import lidraughts.socket.{ SocketTrouper, History, Historical, Socket }

private[tournament] final class TournamentSocket(
    system: ActorSystem,
    tournamentId: String,
    protected val history: History[Messadata],
    jsonView: JsonView,
    lightUser: lidraughts.common.LightUser.Getter,
    lightWfdUser: lidraughts.common.LightWfdUser.Getter,
    toWfdName: String => Option[String],
    isWfdTournament: String => Boolean,
    uidTtl: Duration,
    keepMeAlive: () => Unit
) extends SocketTrouper[Member](system, uidTtl) with Historical[Member, Messadata] {

  private var delayedCrowdNotification = false
  private var delayedReloadNotification = false

  private var clock = none[draughts.Clock.Config]

  private var waitingUsers = WaitingUsers.empty

  private def chatClassifier = Chat classify Chat.Id(tournamentId)

  lidraughtsBus.subscribe(this, chatClassifier)

  TournamentRepo clockById tournamentId foreach {
    _ foreach { c => this ! SetTournamentClock(c) }
  }

  override def stop(): Unit = {
    super.stop()
    lidraughtsBus.unsubscribe(this, chatClassifier)
  }

  protected def receiveSpecific = ({

    case SetTournamentClock(c) => clock = c.some

    case StartGame(game) =>
      game.players foreach { player =>
        player.userId foreach { userId =>
          firstMemberByUserId(userId) foreach { member =>
            notifyMember("redirect", game fullIdOf player.color)(member)
          }
        }
      }
      notifyReload

    case Reload => notifyReload

    case GetWaitingUsersP(promise) =>
      waitingUsers = waitingUsers.update(members.values.flatMap(_.userId)(breakOut), clock)
      promise success waitingUsers

    case lidraughts.socket.Socket.GetVersion(promise) => promise success history.version

    case Join(uid, user, version, promise) =>
      val (enumerator, channel) = Concurrent.broadcast[JsValue]
      val member = Member(channel, user)
      addMember(uid, member)
      notifyCrowd
      promise success Connected(
        prependEventsSince(version, enumerator, member),
        member
      )

    case NotifyCrowd =>
      delayedCrowdNotification = false
      showSpectators(lightUser, isWfdTournament(tournamentId) option lightWfdUser)(members.values) foreach {
        notifyAll("crowd", _)
      }

    case NotifyReload =>
      delayedReloadNotification = false
      notifyAll("reload")

  }: Trouper.Receive) orElse lidraughts.chat.Socket.out(
    send = (t, d, trollish) => notifyVersion(t, d, Messadata(trollish)),
    pimpUser = Some(() => isWfdTournament(tournamentId) option toWfdName)
  )

  override protected def broom: Unit = {
    super.broom
    if (members.nonEmpty) keepMeAlive()
  }

  override protected def afterQuit(uid: Socket.Uid, member: Member) = notifyCrowd

  private def notifyCrowd: Unit =
    if (!delayedCrowdNotification) {
      delayedCrowdNotification = true
      system.scheduler.scheduleOnce(1 second)(this ! NotifyCrowd)
    }

  private def notifyReload: Unit =
    if (!delayedReloadNotification) {
      delayedReloadNotification = true
      // keep the delay low for immediate response to join/withdraw,
      // but still debounce to avoid tourney start message rush
      system.scheduler.scheduleOnce(1 second)(this ! NotifyReload)
    }

  protected def shouldSkipMessageFor(message: Message, member: Member) =
    message.metadata.trollish && !member.troll
}
