package com.fsck.k9.activity


import android.annotation.SuppressLint
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.Observer
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.RecyclerView
import android.text.TextUtils.TruncateAt
import android.view.*
import android.widget.AdapterView.OnItemClickListener
import android.widget.BaseAdapter
import android.widget.TextView
import android.widget.Toast
import com.fsck.k9.Account
import com.fsck.k9.Account.FolderMode
import com.fsck.k9.K9
import com.fsck.k9.Preferences
import com.fsck.k9.activity.setup.FolderSettings
import com.fsck.k9.controller.MessagingController
import com.fsck.k9.controller.SimpleMessagingListener
import com.fsck.k9.mail.Folder
import com.fsck.k9.mail.MessagingException
import com.fsck.k9.mailstore.LocalFolder
import com.fsck.k9.mailstore.LocalStore
import com.fsck.k9.mailstore.LocalStoreProvider
import com.fsck.k9.service.MailService
import com.fsck.k9.ui.R
import org.koin.android.ext.android.inject
import timber.log.Timber
import java.util.*

@SuppressLint("StringFormatInvalid")
class ManageFoldersActivity : K9RecyclerActivity<ThisViewHolder>() {
    private val localStoreProvider: LocalStoreProvider by inject()
    private val preferences: Preferences by inject()
    private val messagingController: MessagingController by inject()
    private val fontSizes = K9.getFontSizes()

    private lateinit var account: Account
    private lateinit var folderListAdapter: FolderListAdapter
    private lateinit var refreshMenuItem: MenuItem
    private lateinit var actionBarProgressView: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (UpgradeDatabases.actionUpgradeDatabases(this, intent)) {
            finish()
            return
        }

        actionBarProgressView = getActionBarProgressView()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setContentView(R.layout.folder_list)
        listView.apply {
            scrollBarStyle = View.SCROLLBARS_INSIDE_OVERLAY
            isLongClickable = true
            onItemClickListener = OnItemClickListener { _, _, position, _ ->
                onClickFolder((folderListAdapter.getItem(position) as FolderInfoHolder).serverId)
            }

            isSaveEnabled = true
        }

        onNewIntent(intent)
    }

    public override fun onNewIntent(intent: Intent) {
        setIntent(intent) // onNewIntent doesn't set our "internal" intent on its own

        val accountUuid = intent.getStringExtra(EXTRA_ACCOUNT)
        val account = preferences.getAccount(accountUuid)
        if (account == null) {
            /*
             * This can happen when a launcher shortcut is created for an
             * account, and then the account is deleted or data is wiped, and
             * then the shortcut is used.
             */
            finish()
            return
        }
        this.account = account

        if (intent.getBooleanExtra(EXTRA_FROM_SHORTCUT, false) && account.autoExpandFolder != null) {
            onClickFolder(account.autoExpandFolder)
            finish()
        } else {
            initializeActivityView()
        }
    }

    private fun initializeActivityView() {
        if (adapter != null) {
            return
        }

        folderListAdapter = FolderListAdapter()
        setListAdapter(folderListAdapter)

        try {
            val accountLocalStore = localStoreProvider.getInstance(account)
            FoldersLiveData(accountLocalStore, messagingController).observe(this, Observer {
                localFolders -> folderListAdapter.setData(localFolders)
            })
        } catch (e: MessagingException) {
            Timber.e(e)
            finish()
        }

    }

    /**
     * On resume we refresh the folder list (in the background) and we refresh the
     * messages for any folder that is currently open. This guarantees that things
     * like unread message count and read status are updated.
     */
    override fun onResume() {
        super.onResume()

        if (!account.isAvailable(this)) {
            Timber.i("account unavailable, not showing folder-list but account-list")
            Accounts.listAccounts(this)
            finish()
            return
        }

        initializeActivityView()
    }

    @SuppressLint("InflateParams")
    private fun getActionBarProgressView(): View {
        return layoutInflater.inflate(R.layout.actionbar_indeterminate_progress_actionview, null)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_Q -> {
                finish()
                return true
            }

            KeyEvent.KEYCODE_H -> {
                val toast = Toast.makeText(this, R.string.folder_list_help_key, Toast.LENGTH_LONG)
                toast.show()
                return true
            }

            KeyEvent.KEYCODE_1 -> {
                setDisplayMode(FolderMode.FIRST_CLASS)
                return true
            }
            KeyEvent.KEYCODE_2 -> {
                setDisplayMode(FolderMode.FIRST_AND_SECOND_CLASS)
                return true
            }
            KeyEvent.KEYCODE_3 -> {
                setDisplayMode(FolderMode.NOT_SECOND_CLASS)
                return true
            }
            KeyEvent.KEYCODE_4 -> {
                setDisplayMode(FolderMode.ALL)
                return true
            }
        }

        return super.onKeyDown(keyCode, event)
    }

    private fun setDisplayMode(newMode: FolderMode) {
        account.folderDisplayMode = newMode
        Preferences.getPreferences(applicationContext).saveAccount(account)
        if (account.folderPushMode != FolderMode.NONE) {
            MailService.actionRestartPushers(this, null)
        }
    }

    internal class FoldersLiveData(private val localStore: LocalStore, private val messagingController: MessagingController) : LiveData<List<LocalFolder>>() {
        private val messagingListener: SimpleMessagingListener

        init {
            this.messagingListener = object : SimpleMessagingListener() {
                override fun listFoldersFinished(account: Account) {
                    if (account === this@FoldersLiveData.localStore.account) {
                        asyncLoadFolders()
                    }
                }
            }
        }

        fun asyncLoadFolders() {
            value = try {
                val personalNamespaces = localStore.getPersonalNamespaces(false)
                personalNamespaces
            } catch (e: MessagingException) {
                null
            }

        }

        override fun onActive() {
            asyncLoadFolders()
            messagingController.addListener(messagingListener)
        }

        override fun onInactive() {
            messagingController.removeListener(messagingListener)
        }
    }

    private fun refreshRemoteFolders() {
        MessagingController.getInstance(application).listFolders(account, true, object : SimpleMessagingListener() {
            override fun listFoldersStarted(account: Account) {
                showProgress(true)
            }

            override fun listFoldersFinished(account: Account) {
                showProgress(false)
            }

            override fun listFoldersFailed(account: Account, message: String) {
                showProgress(false)
            }

            private fun showProgress(progress: Boolean) {
                runOnUiThread {
                    if (progress) {
                        refreshMenuItem.actionView = actionBarProgressView
                    } else {
                        refreshMenuItem.actionView = null
                    }
                }
            }
        })
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            R.id.list_folders -> {
                refreshRemoteFolders()
                true
            }
            R.id.compact -> {
                onCompact(account)
                true
            }
            R.id.display_1st_class -> {
                setDisplayMode(FolderMode.FIRST_CLASS)
                true
            }
            R.id.display_1st_and_2nd_class -> {
                setDisplayMode(FolderMode.FIRST_AND_SECOND_CLASS)
                true
            }
            R.id.display_not_second_class -> {
                setDisplayMode(FolderMode.NOT_SECOND_CLASS)
                true
            }
            R.id.display_all -> {
                setDisplayMode(FolderMode.ALL)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun onClickFolder(folderServerId: String) {
        FolderSettings.actionSettings(this, account, folderServerId)
    }

    private fun onCompact(account: Account?) {
        MessagingController.getInstance(application).compact(account, null)

        val toastText = getString(R.string.compacting_account, account!!.description)
        val toast = Toast.makeText(application, toastText, Toast.LENGTH_SHORT)
        toast.show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.folder_list_option, menu)
        refreshMenuItem = menu.findItem(R.id.list_folders)
        return true
    }

    internal inner class FolderListAdapter : RecyclerView.Adapter<ThisViewHolder>() {
        private val folders = ArrayList<FolderInfoHolder>()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ThisViewHolder {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun getItemCount(): Int {
            return folders.size
        }

        override fun onBindViewHolder(holder: ThisViewHolder, position: Int) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        fun getItem(position: Long): Any {
            return getItem(position.toInt())
        }

        override fun getItem(position: Int): Any {
            return folders[position]
        }

        override fun getItemId(position: Int): Long {
            return folders[position].folder.serverId.hashCode().toLong()
        }

        override fun isEnabled(item: Int): Boolean {
            return true
        }

        override fun areAllItemsEnabled(): Boolean {
            return true
        }

        fun setData(folders: List<LocalFolder>?) {
            folders ?: return

            val newFolders = LinkedList<FolderInfoHolder>()
            val topFolders = LinkedList<FolderInfoHolder>()

            val aMode = account.folderDisplayMode
            for (folder in folders) {
                val fMode = folder.displayClass

                if (aMode == FolderMode.FIRST_CLASS && fMode != Folder.FolderClass.FIRST_CLASS
                        || aMode == FolderMode.FIRST_AND_SECOND_CLASS &&
                        fMode != Folder.FolderClass.FIRST_CLASS &&
                        fMode != Folder.FolderClass.SECOND_CLASS
                        || aMode == FolderMode.NOT_SECOND_CLASS && fMode == Folder.FolderClass.SECOND_CLASS) {
                    continue
                }

                var holder: FolderInfoHolder? = null

                val folderIndex = getFolderIndex(folder.serverId)
                if (folderIndex >= 0) {
                    holder = getItem(folderIndex) as FolderInfoHolder
                }

                if (holder == null) {
                    holder = FolderInfoHolder(applicationContext, folder, this@ManageFoldersActivity.account)
                } else {
                    holder.populate(applicationContext, folder, this@ManageFoldersActivity.account)

                }
                if (folder.isInTopGroup) {
                    topFolders.add(holder)
                } else {
                    newFolders.add(holder)
                }
            }
            newFolders.sort()
            topFolders.sort()
            topFolders.addAll(newFolders)

            this.folders.clear()
            this.folders.addAll(topFolders)
            notifyDataSetChanged()
        }

        private fun getFolderIndex(folder: String): Int {
            val searchHolder = FolderInfoHolder()
            searchHolder.serverId = folder
            return folders.indexOf(searchHolder)
        }

        override fun getView(position: Int, convertView: View, parent: ViewGroup): View? {
            return if (position <= count) {
                getItemView(position, convertView, parent)
            } else {
                Timber.e("getView with illegal position=%d called! count is only %d", position, count)
                null
            }
        }

        private fun getItemView(itemPosition: Int, convertView: View?, parent: ViewGroup): View {
            val folder = getItem(itemPosition) as FolderInfoHolder
            val view = convertView ?: layoutInflater.inflate(R.layout.folder_list_item, parent, false)

            var holder: FolderViewHolder? = view.tag as FolderViewHolder

            if (holder == null) {
                holder = FolderViewHolder(view)
                holder.folderServerId = folder.serverId

                view.tag = holder
            }

            holder.folderName.text = folder.displayName
            if (folder.status != null) {
                holder.folderStatus.text = folder.status
                holder.folderStatus.visibility = View.VISIBLE
            } else {
                holder.folderStatus.visibility = View.GONE
            }

            fontSizes.setViewTextSize(holder.folderName, fontSizes.folderName)

            if (K9.wrapFolderNames()) {
                holder.folderName.ellipsize = null
                holder.folderName.setSingleLine(false)
            } else {
                holder.folderName.ellipsize = TruncateAt.START
                holder.folderName.setSingleLine(true)
            }
            fontSizes.setViewTextSize(holder.folderStatus, fontSizes.folderStatus)

            return view
        }
    }

    internal class FolderViewHolder(view: View) {
        val folderName: TextView = view.findViewById(R.id.folder_name)
        val folderStatus: TextView = view.findViewById(R.id.folder_status)

        var folderServerId: String? = null
    }

    companion object {
        private const val EXTRA_ACCOUNT = "account"
        private const val EXTRA_FROM_SHORTCUT = "fromShortcut"

        @JvmStatic
        fun actionHandleAccountIntent(context: Context, account: Account, fromShortcut: Boolean): Intent {
            val intent = Intent(context, ManageFoldersActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            intent.putExtra(EXTRA_ACCOUNT, account.uuid)

            if (fromShortcut) {
                intent.putExtra(EXTRA_FROM_SHORTCUT, true)
            }

            return intent
        }

        @JvmStatic
        fun actionHandleAccount(context: Context, account: Account) {
            val intent = actionHandleAccountIntent(context, account, false)
            context.startActivity(intent)
        }
    }
}

class ThisViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)