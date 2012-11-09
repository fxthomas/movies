package fx.traktmovies

import android.app.Activity
import android.app.AlertDialog
import android.app.DialogFragment
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.EditText
import android.widget.Toast
import android.util.Log

import scala.concurrent._
import ExecutionContext.Implicits.global

object TraktLoginDialogFragment {
  def show(activity: Activity, configuration: Configuration, onLoggedIn: Boolean => Unit) = {
    val f = new TraktLoginDialogFragment(configuration, onLoggedIn)
    f.show(activity.getFragmentManager,"dialog")
  }
}

class TraktLoginDialogFragment(configuration: Configuration, onLoggedIn: Boolean => Unit) extends DialogFragment {
  override def onCreateDialog(savedInstanceState: Bundle) = {
    val context = getActivity.asInstanceOf[DefaultActivity]
    val v = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)
    .asInstanceOf[LayoutInflater]
    .inflate(R.layout.trakt_login, null)

    val builder = new AlertDialog.Builder(getActivity);
    builder.setMessage("Login with Trakt")
    .setView(v)
    .setPositiveButton("Login",  new DialogInterface.OnClickListener() {
      def onClick(dialog: DialogInterface, id: Int) {
        // Retrieve views
        val usernameField = v.findViewById(R.id.trakt_login_username).asInstanceOf[EditText]
        val passwordField = v.findViewById(R.id.trakt_login_password).asInstanceOf[EditText]

        // Setup the Trakt object
        val f = configuration.login(usernameField.getText.toString, passwordField.getText.toString)

        // Test login
        f onSuccess { case r => context.ui { onLoggedIn(true) } }
        f onFailure { case e => context.ui { onLoggedIn(false) } }
      }
    })
    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
      def onClick(dialog: DialogInterface, id: Int) { }
    })
    builder.create
  }
}
