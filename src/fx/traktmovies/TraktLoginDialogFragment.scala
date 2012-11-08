package fx.traktmovies

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

class TraktLoginDialogFragment(onLoggedIn: Boolean => Unit) extends DialogFragment {
  import Configuration._
  import Util._

  override def onCreateDialog(savedInstanceState: Bundle) = {
    val context = getActivity
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
        Trakt.login(usernameField.getText.toString, passwordField.getText.toString)

        // Test login
        val f = future { Trakt.test }
        f onSuccess {
          case r => {
            Log.i ("TraktLogin", "Logged in!")
            ui(context) {
              // Save results
              context.getSharedPreferences("trakt",0)
              .edit
              .putString("auth_hash", Trakt.authHash.get)
              .commit

              // Display message to the user
              Toast.makeText(context, "Succesful!", Toast.LENGTH_SHORT).show

              // Run onLoggedIn
              onLoggedIn(true)
            }
          }
        }

        f onFailure {
          case e => {
            Trakt.logout
            Log.w ("TraktLogin", "Failed with " + e.getMessage)
            ui(context) {
              Toast.makeText(context, "Unable to reach Trakt", Toast.LENGTH_SHORT).show
              onLoggedIn(false)
            }
          }
        }
      }
    })
    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
      def onClick(dialog: DialogInterface, id: Int) { }
    })
    builder.create
  }
}
