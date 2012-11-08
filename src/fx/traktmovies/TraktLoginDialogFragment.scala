package fx.traktmovies

import android.app.AlertDialog
import android.app.DialogFragment
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.EditText

class TraktLoginDialogFragment extends DialogFragment {

  override def onCreateDialog(savedInstanceState: Bundle) = {
    val v = getActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE)
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

        // Save login info to the preferences
        getActivity.getSharedPreferences("trakt",0)
        .edit
        .putString("auth_hash", Trakt.authHash.get)
        .commit
      }
    })
    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
      def onClick(dialog: DialogInterface, id: Int) { }
    })
    builder.create
  }
}
