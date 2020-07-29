package pl.zyper.autosms

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.MenuItem
import android.view.Window
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.children
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*


class MainActivity : AppCompatActivity() {
    lateinit var presenter: MainPresenter

    val triggerQueryListener: SearchView.OnQueryTextListener =
        object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                presenter.triggerListAdapter.filter.filter(query)
                return true
            }

            override fun onQueryTextChange(newText: String): Boolean {
                presenter.triggerListAdapter.filter.filter(newText)
                return true
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.i("FUNCTION", "onCreate")

        super.onCreate(savedInstanceState)

        presenter = MainPresenter(this)

        requestWindowFeature(Window.FEATURE_NO_TITLE)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        actionBar?.setDisplayShowTitleEnabled(false)

        setContentView(R.layout.activity_main)
        //setSupportActionBar(toolbar)

        presenter.addTriggerFab.run {
            setFab(add_fab)
            setButton(getString(R.string.fab_timeanddate), time_fab, (fun(_: Context) {
                presenter.startTriggerCreationActivity(R.layout.trigger_creation_time)
            }))
            setButton(getString(R.string.fab_location), gps_fab, (fun(_: Context) {
                presenter.startTriggerCreationActivity(R.layout.trigger_creation_location)
            }))
            setButton(getString(R.string.fab_autoreply), reply_fab, (fun(context: Context) {
                Toast.makeText(
                    context,
                    getString(R.string.not_implemented_autoreply),
                    Toast.LENGTH_LONG
                ).show()
            }))
        }

        val triggerList = findViewById<RecyclerView>(R.id.trigger_list)
        triggerList.layoutManager = LinearLayoutManager(this)
        triggerList.adapter = presenter.triggerListAdapter

        val tabs = findViewById<LinearLayout>(R.id.list_tabs)
        if (tabs.childCount >= 3) {
            val it = tabs.children.iterator()
            val tabPending = it.next() as ToggleButton?
            val tabAll = it.next() as ToggleButton?
            val tabComplete = it.next() as ToggleButton?

            tabPending?.setOnClickListener {
                tabAll?.isChecked = false
                tabComplete?.isChecked = false
                tabPending.isChecked = true

                presenter.triggerListAdapter.category = TriggerListAdapter.PENDING
                presenter.triggerListAdapter.filter.filter(trigger_search.query)
                presenter.triggerListAdapter.notifyDataSetChanged()
            }
            tabAll?.setOnClickListener {
                tabPending?.isChecked = false
                tabComplete?.isChecked = false
                tabAll.isChecked = true

                presenter.triggerListAdapter.category = TriggerListAdapter.ALL
                presenter.triggerListAdapter.filter.filter(trigger_search.query)
                presenter.triggerListAdapter.notifyDataSetChanged()
            }
            tabComplete?.setOnClickListener {
                tabAll?.isChecked = false
                tabPending?.isChecked = false
                tabComplete.isChecked = true


                presenter.triggerListAdapter.category = TriggerListAdapter.COMPLETED
                presenter.triggerListAdapter.filter.filter(trigger_search.query)
                presenter.triggerListAdapter.notifyDataSetChanged()
            }
        }

        trigger_search.queryHint = resources.getString(R.string.search)
        trigger_search.setOnQueryTextListener(triggerQueryListener)
        trigger_search.setOnClickListener { trigger_search.isIconified = false }
        trigger_search.inputType = InputType.TYPE_TEXT_VARIATION_PERSON_NAME

        val menuButton = findViewById<ImageButton>(R.id.menu_button)
        menuButton.setOnClickListener {
            val popup = PopupMenu(this, menuButton)
            popup.menuInflater.inflate(R.menu.menu_main, popup.menu)
            popup.setOnMenuItemClickListener { item: MenuItem ->
                onOptionsItemSelected(item)
            }
            popup.show()
        }

        swipe_refresh_layout.setOnRefreshListener {
            presenter.loadTriggers()
            swipe_refresh_layout.isRefreshing = false
        }
    }

    override fun onResume() {
        super.onResume()

        Log.i("MAIN ACTIVITY", "on Resume")

        swipe_refresh_layout.isRefreshing = true
        presenter.addTriggerFab.switchFab(add_fab, false)
        presenter.loadTriggers()
        swipe_refresh_layout.isRefreshing = false
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        return when (item.itemId) {
            R.id.action_settings -> true
            R.id.action_flush_database -> {
                presenter.onFlushDatabase()
            }
            R.id.action_refresh -> {
                swipe_refresh_layout.isRefreshing = true
                presenter.loadTriggers()
                swipe_refresh_layout.isRefreshing = false
                true
            }
            R.id.action_about -> {
                aboutDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun aboutDialog() {
        val builder: AlertDialog.Builder = let { AlertDialog.Builder(it) }

        builder.setTitle(getString(R.string.dialog_about_title))
        builder.setMessage(getString(R.string.dialog_about_content))

        val dialog: AlertDialog? = builder.create()
        dialog?.show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == TriggerCreationActivity.CREATE_TRIGGER_REQUEST) {
            if (resultCode == Activity.RESULT_OK) {
                presenter.loadTriggers()
            }
        }
    }
}