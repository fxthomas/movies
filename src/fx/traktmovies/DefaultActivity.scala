package fx.traktmovies

import android.app.Activity
import android.app.AlertDialog
import android.view.Menu
import android.view.MenuItem
import android.content.Intent
import android.content.Context
import android.content.DialogInterface

trait DefaultActivity extends Activity {

  implicit val apiKey = "93f34432220b4d372501cff4f0caeb9c"
  var configuration: Configuration = null

  def ui(fun: => Unit) =
    runOnUiThread(new Runnable() { def run() = fun })

  def error(message: String) = {
    val alertbox = new AlertDialog.Builder(this)
    alertbox.setMessage(message)
    alertbox.setNeutralButton("OK", new DialogInterface.OnClickListener() {
      def onClick(arg0: DialogInterface, arg1: Int) = {}
    })
    alertbox
  }

  def setupActivity() = {
    configuration = new Configuration(this)
    configuration.restore_login
    handleIntent(getIntent)
  }

  override def onPrepareOptionsMenu(menu: Menu): Boolean = {
    val loginMenuItem = menu.findItem(R.id.menu_login).asInstanceOf[MenuItem]
    if (Trakt.isLoggedIn) loginMenuItem.setTitle ("Logout")
    else loginMenuItem.setTitle ("Login")

    return true
  }

  def setupOptionsMenu(menu: Menu) = {
    // Configure login button
    val loginMenuItem = menu.findItem(R.id.menu_login).asInstanceOf[MenuItem]
    loginMenuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
      override def onMenuItemClick(item: MenuItem): Boolean = {
        // If we're logged in, we log out
        if (Trakt.isLoggedIn) {
          configuration.logout
          loginMenuItem.setTitle("Login")

        // If we're not, try to login
        } else {
          TraktLoginDialogFragment.show(DefaultActivity.this, configuration, b => {
            loginMenuItem.setTitle(if(b) "Logout" else "Login")
          })
        }

        return true
      }
    })
  }

  def handleIntent(intent: Intent)
  override def onNewIntent(intent: Intent) = { setIntent(intent); handleIntent(intent) }
}
