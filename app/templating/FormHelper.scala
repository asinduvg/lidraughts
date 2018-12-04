package lidraughts.app
package templating

import play.api.data._
import play.twirl.api.Html
import scalatags.Text.all._
import scalatags.Text.{ all => st }

import lidraughts.api.Context
import lidraughts.app.ui.Scalatags._
import lidraughts.i18n.I18nDb

trait FormHelper { self: I18nHelper =>

  def errMsg(form: Field)(implicit ctx: Context): Html = errMsg(form.errors)

  def errMsg(form: Form[_])(implicit ctx: Context): Html = errMsg(form.errors)

  def errMsg(error: FormError)(implicit ctx: Context): Html = Html {
    s"""<p class="error">${transKey(error.message, I18nDb.Site, error.args)}</p>"""
  }

  def errMsg(errors: Seq[FormError])(implicit ctx: Context): Html = Html {
    errors map errMsg mkString
  }

  def globalError(form: Form[_])(implicit ctx: Context): Option[Html] =
    form.globalError map errMsg

  val booleanChoices = Seq("true" -> "✓ Yes", "false" -> "✗ No")

  object form3 {

    private val idPrefix = "form3"

    def id(field: Field): String = s"$idPrefix-${field.id}"

    private def groupLabel(field: Field) = div(cls := "form-label", `for` := id(field))
    private val helper = small(cls := "form-help")

    private def errors(errs: Seq[FormError])(implicit ctx: Context): Frag = errs map error
    private def errors(field: Field)(implicit ctx: Context): Frag = errors(field.errors)
    private def error(err: FormError)(implicit ctx: Context): Frag =
      p(cls := "error")(transKey(err.message, I18nDb.Site, err.args))

    /* All public methods must return HTML
     * because twirl just calls toString on scalatags frags
     * and that escapes the content :( */

    def split(html: Html): Html = div(cls := "form-split")(html)

    def group(
      field: Field,
      labelContent: Html,
      klass: String = "",
      half: Boolean = false,
      help: Option[Html] = None
    )(content: Field => Frag)(implicit ctx: Context): Html =
      div(cls := List(
        "form-group" -> true,
        "is-invalid" -> field.hasErrors,
        "form-half" -> half,
        klass -> klass.nonEmpty
      ))(
        groupLabel(field)(labelContent),
        content(field),
        errors(field),
        help map { helper(_) }
      )

    def input(field: Field, typ: String = "", klass: String = ""): BaseTagType =
      st.input(
        st.id := id(field),
        name := field.name,
        value := field.value,
        `type` := typ.nonEmpty.option(typ),
        cls := List("form-control" -> true, klass -> klass.nonEmpty)
      )
    def inputHtml(field: Field, klass: String = "")(modifiers: Modifier*): Html =
      input(field, klass)(modifiers)

    def checkbox(
      field: Field,
      labelContent: Html,
      half: Boolean = false,
      help: Option[Html] = None,
      disabled: Boolean = false
    ): Html =
      div(cls := List(
        "form-check form-group" -> true,
        "form-half" -> half
      ))(
        div(
          span(cls := "form-check-input")(
            input(field, typ = "checkbox", klass = "cmn-toggle")(
              value := "true",
              checked := field.value.has("true"),
              st.disabled := disabled
            ),
            label(`for` := id(field))
          ),
          groupLabel(field)(labelContent)
        ),
        help map { helper(_) }
      )

    def select(
      field: Field,
      options: Iterable[(Any, String)],
      default: Option[String] = None,
      disabled: Boolean = false
    ): Html =
      st.select(
        st.id := id(field),
        name := field.name,
        cls := "form-control",
        st.disabled := disabled
      )(
          default map { option(value := "")(_) },
          options.toSeq map {
            case (value, name) => option(
              st.value := value.toString,
              selected := field.value.has(value.toString)
            )(name)
          }
        )

    def textarea(
      field: Field,
      klass: String = ""
    )(modifiers: Modifier*): Html =
      st.textarea(
        st.id := id(field),
        name := field.name,
        cls := List("form-control" -> true, klass -> klass.nonEmpty)
      )(modifiers)(~field.value)

    def actions(html: Html): Html = div(cls := "form-actions")(html)

    def action(html: Html): Html = div(cls := "form-actions single")(html)

    def submit(
      content: Html,
      icon: Option[String] = Some("E"),
      nameValue: Option[(String, String)] = None,
      klass: String = ""
    ): Html =
      button(
        `type` := "submit",
        dataIcon := icon,
        name := nameValue.map(_._1),
        value := nameValue.map(_._2),
        cls := List(
          "submit button" -> true,
          "text" -> icon.isDefined,
          klass -> klass.nonEmpty
        )
      )(content)

    def hidden(field: Field, value: Option[String] = None): Html =
      input(field, typ = "hidden")(st.value := value.orElse(field.value))

    def password(field: Field, content: Html)(implicit ctx: Context): Html =
      group(field, content)(input(_, typ = "password")(required := true))

    def globalError(form: Form[_])(implicit ctx: Context): Option[Html] =
      form.globalError map { err =>
        div(cls := "form-group is-invalid")(error(err))
      }

    private val dataEnableTime = attr("data-enable-time")
    private val datatime24h = attr("data-time_24h")

    def flatpickr(field: Field, withTime: Boolean = true): Html =
      input(field, klass = "flatpickr")(
        dataEnableTime := withTime,
        datatime24h := withTime
      )

    object file {
      def image(name: String): Html = st.input(`type` := "file", st.name := name, accept := "image/*")
      def pdn(name: String): Html = st.input(`type` := "file", st.name := name, accept := ".pdn")
    }
  }
}
