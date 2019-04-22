package lidraughts.security

import scalatags.Text.all._

import lidraughts.common.{ Lang, EmailAddress }
import lidraughts.user.User
import lidraughts.i18n.I18nKeys.{ emails => trans }

final class WelcomeEmail(
    mailgun: Mailgun,
    baseUrl: String
) {

  def apply(user: User, email: EmailAddress)(implicit lang: Lang): Funit = {
    val profileUrl = s"$baseUrl/@/${user.username}"
    val editUrl = s"$baseUrl/account/profile"
    mailgun send Mailgun.Message(
      to = email,
      subject = trans.welcome_subject.literalTxtTo(lang, List(user.username)),
      text = s"""
${trans.welcome_text.literalTxtTo(lang, List(profileUrl, editUrl))}

${Mailgun.txt.serviceNote}
""",
      htmlBody = raw(s"""
<div itemscope itemtype="http://schema.org/EmailMessage">
  <p itemprop="description">${trans.welcome_text.literalTo(lang, List(profileUrl, editUrl)).render}</p>
  ${Mailgun.html.serviceNote}
</div>""").some
    )
  }
}
