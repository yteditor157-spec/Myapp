package com.ishan.sms

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.json.JSONObject
import java.io.IOException
import java.util.Calendar
import java.util.concurrent.atomic.AtomicBoolean

class MainActivity : AppCompatActivity() {

    private val handler = Handler(Looper.getMainLooper())
    private val syncing = AtomicBoolean(false)
    private var tick = 0
    private var chainBattery = false
    private var lastListSig = ""
    private var strikes = 0
    private var tampered = false
    private lateinit var adapter: SmsAdapter

    // ---- views ----
    private val root by lazy { findViewById<View>(R.id.root) }
    private val brandLogin by lazy { findViewById<BrandFooterView>(R.id.brandLogin) }
    private val brandMain by lazy { findViewById<BrandFooterView>(R.id.brandMain) }
    private val loginRoot by lazy { findViewById<View>(R.id.loginRoot) }
    private val mainRoot by lazy { findViewById<View>(R.id.mainRoot) }
    private val pageHome by lazy { findViewById<View>(R.id.pageHome) }
    private val pageSms by lazy { findViewById<View>(R.id.pageSms) }
    private val pageSettings by lazy { findViewById<View>(R.id.pageSettings) }
    private val bottomNav by lazy { findViewById<BottomNavigationView>(R.id.bottomNav) }

    private val etKey by lazy { findViewById<EditText>(R.id.etKey) }
    private val btnLogin by lazy { findViewById<MaterialButton>(R.id.btnLogin) }
    private val tvLoginMsg by lazy { findViewById<TextView>(R.id.tvLoginMsg) }

    private val tvGreeting by lazy { findViewById<TextView>(R.id.tvGreeting) }
    private val dotStatus by lazy { findViewById<View>(R.id.dotStatus) }
    private val tvStatus by lazy { findViewById<TextView>(R.id.tvStatus) }
    private val btnSync by lazy { findViewById<View>(R.id.btnSync) }
    private val tvError by lazy { findViewById<TextView>(R.id.tvError) }
    private val tvToday by lazy { findViewById<TextView>(R.id.tvToday) }
    private val ring by lazy { findViewById<RingView>(R.id.ring) }
    private val tvUnused by lazy { findViewById<TextView>(R.id.tvUnused) }
    private val tvUsed by lazy { findViewById<TextView>(R.id.tvUsed) }
    private val chart by lazy { findViewById<BarChartView>(R.id.chart) }
    private val tvTaka by lazy { findViewById<TextView>(R.id.tvTaka) }
    private val tvPending by lazy { findViewById<TextView>(R.id.tvPending) }
    private val tvLastSync by lazy { findViewById<TextView>(R.id.tvLastSync) }

    private val etSearch by lazy { findViewById<EditText>(R.id.etSearch) }
    private val chipGroup by lazy { findViewById<ChipGroup>(R.id.chipGroup) }
    private val tvCount by lazy { findViewById<TextView>(R.id.tvCount) }
    private val rvSms by lazy { findViewById<RecyclerView>(R.id.rvSms) }
    private val tvEmpty by lazy { findViewById<TextView>(R.id.tvEmpty) }

    private val tvKeyMasked by lazy { findViewById<TextView>(R.id.tvKeyMasked) }
    private val tvKeyStatus by lazy { findViewById<TextView>(R.id.tvKeyStatus) }
    private val stSms by lazy { findViewById<TextView>(R.id.stSms) }
    private val stBattery by lazy { findViewById<TextView>(R.id.stBattery) }
    private val stNotif by lazy { findViewById<TextView>(R.id.stNotif) }
    private val swKeepAlive by lazy { findViewById<SwitchCompat>(R.id.swKeepAlive) }
    private val tvAbout by lazy { findViewById<TextView>(R.id.tvAbout) }

    private val permLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        if (!hasSms() && !shouldShowRequestPermissionRationale(Manifest.permission.RECEIVE_SMS)) {
            Toast.makeText(this, "সেটিংস থেকে SMS পারমিশন দিন", Toast.LENGTH_LONG).show()
            openAppSettings()
        } else if (chainBattery && !batteryOk()) {
            requestBattery()
        }
        chainBattery = false
        renderAll()
    }

    private val loop = object : Runnable {
        override fun run() {
            guard()
            if (tampered) return
            if (Prefs.key(this@MainActivity) != null) {
                renderAll()
                tick++
                if (tick % 10 == 0) syncAsync(false)
            }
            handler.postDelayed(this, 3000)
        }
    }

    // =====================================================================
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (!Brand.valid(this)) { tamper(); return }

        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val b = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(b.left, b.top, b.right, b.bottom)
            insets
        }

        adapter = SmsAdapter { showDetails(it) }
        rvSms.layoutManager = LinearLayoutManager(this)
        rvSms.adapter = adapter

        bottomNav.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.nav_home -> showPage(0)
                R.id.nav_sms -> showPage(1)
                R.id.nav_settings -> showPage(2)
            }
            true
        }

        btnLogin.setOnClickListener { doLogin() }
        btnSync.setOnClickListener { syncAsync(true) }
        findViewById<View>(R.id.btnLogout).setOnClickListener { confirmLogout() }
        findViewById<View>(R.id.btnSyncNow).setOnClickListener { syncAsync(true) }
        findViewById<View>(R.id.btnClear).setOnClickListener { confirmClear() }
        findViewById<View>(R.id.btnAllPerm).setOnClickListener { requestAllPermissions() }
        findViewById<View>(R.id.rowSms).setOnClickListener { requestSms() }
        findViewById<View>(R.id.rowBattery).setOnClickListener { requestBattery() }
        findViewById<View>(R.id.rowNotif).setOnClickListener { requestNotif() }
        findViewById<View>(R.id.rowAuto).setOnClickListener { showAutostartHelp() }
        findViewById<View>(R.id.rowKeep).setOnClickListener { swKeepAlive.toggle() }

        swKeepAlive.setOnCheckedChangeListener { _, on ->
            Prefs.setKeepAlive(this, on)
            if (on && Prefs.key(this) != null) KeepAliveService.start(this) else KeepAliveService.stop(this)
        }

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) { lastListSig = ""; renderList() }
        })
        chipGroup.setOnCheckedStateChangeListener { _, _ -> lastListSig = ""; renderList() }

        refreshScreen()
    }

    override fun onResume() {
        super.onResume()
        if (tampered || !Brand.valid(this)) { tamper(); return }
        refreshScreen()
        handler.removeCallbacks(loop)
        handler.post(loop)
        if (Prefs.key(this) != null) {
            syncAsync(false)
            if (Prefs.keepAlive(this)) KeepAliveService.start(this)
        }
    }

    override fun onPause() {
        handler.removeCallbacks(loop)
        super.onPause()
    }

    // =====================================================================
    // Integrity
    private fun guard() {
        if (tampered) return
        val footer = if (Prefs.key(this) == null) brandLogin else brandMain
        val ok = Brand.valid(this) && footer.isIntact()
        strikes = if (ok) 0 else strikes + 1
        if (strikes >= 2) tamper()
    }

    private fun tamper() {
        if (tampered) return
        tampered = true
        KeepAliveService.stop(this)
        MaterialAlertDialogBuilder(this)
            .setTitle("অ্যাপ পরিবর্তিত")
            .setMessage("এই অ্যাপের অফিসিয়াল ক্রেডিট বা সিকিউরিটি পরিবর্তন করা হয়েছে। অনুগ্রহ করে অফিসিয়াল সংস্করণ ইনস্টল করুন।")
            .setCancelable(false)
            .setPositiveButton("ঠিক আছে") { _, _ -> finishAffinity() }
            .show()
    }

    // =====================================================================
    // Screens
    private fun refreshScreen() {
        val loggedIn = Prefs.key(this) != null
        loginRoot.visibility = if (loggedIn) View.GONE else View.VISIBLE
        mainRoot.visibility = if (loggedIn) View.VISIBLE else View.GONE
        if (loggedIn) renderAll()
    }

    private fun showPage(i: Int) {
        pageHome.visibility = if (i == 0) View.VISIBLE else View.GONE
        pageSms.visibility = if (i == 1) View.VISIBLE else View.GONE
        pageSettings.visibility = if (i == 2) View.VISIBLE else View.GONE
        renderAll()
    }

    // =====================================================================
    // Login / logout
    private fun doLogin() {
        val key = etKey.text.toString().trim().uppercase()
        if (key.isEmpty()) { tvLoginMsg.text = "Key লিখুন"; return }
        btnLogin.isEnabled = false
        tvLoginMsg.setTextColor(Color.parseColor("#6B6785"))
        tvLoginMsg.text = "যাচাই করা হচ্ছে..."
        Thread {
            var ok = false
            var msg = ""
            try {
                val raw = Api.get("readers/$key")
                if (raw == "null") msg = "ভুল Key"
                else if (!JSONObject(raw).optBoolean("active", false)) msg = "এই Key বন্ধ করা আছে"
                else ok = true
            } catch (e: IOException) {
                msg = Sync.errorText(e)
            } catch (e: Exception) {
                msg = "ত্রুটি: ${e.message}"
            }
            runOnUiThread {
                btnLogin.isEnabled = true
                if (ok) {
                    Prefs.setKey(this, key)
                    etKey.setText("")
                    tvLoginMsg.text = ""
                    bottomNav.selectedItemId = R.id.nav_home
                    showPage(0)
                    refreshScreen()
                    if (Prefs.keepAlive(this)) KeepAliveService.start(this)
                    requestAllPermissions()
                    syncAsync(false)
                } else {
                    tvLoginMsg.setTextColor(Color.parseColor("#C2255C"))
                    tvLoginMsg.text = msg
                }
            }
        }.start()
    }

    private fun confirmLogout() {
        MaterialAlertDialogBuilder(this)
            .setTitle("লগআউট করবেন?")
            .setMessage("লগআউট করলে নতুন SMS আর আপলোড হবে না।")
            .setPositiveButton("লগআউট") { _, _ ->
                Prefs.setKey(this, null)
                KeepAliveService.stop(this)
                refreshScreen()
            }
            .setNegativeButton("বাতিল", null)
            .show()
    }

    private fun confirmClear() {
        MaterialAlertDialogBuilder(this)
            .setTitle("লোকাল হিস্টোরি মুছবেন?")
            .setMessage("শুধু এই ফোনের তালিকা মুছবে। সার্ভারের ডাটা ও ইউজারদের ব্যালেন্স অপরিবর্তিত থাকবে।\n\nআপলোড বাকি SMS থাকলে সেগুলোও হারিয়ে যাবে।")
            .setPositiveButton("মুছুন") { _, _ -> Store.clear(this); lastListSig = ""; renderAll() }
            .setNegativeButton("বাতিল", null)
            .show()
    }

    // =====================================================================
    // Sync
    private fun syncAsync(toast: Boolean) {
        val key = Prefs.key(this) ?: return
        if (!syncing.compareAndSet(false, true)) return
        Thread {
            val r = try { Sync.flush(applicationContext, key) }
                    catch (e: Exception) { SyncResult(false, "ত্রুটি: ${e.message}") }
            syncing.set(false)
            runOnUiThread {
                renderAll()
                if (toast) Toast.makeText(this, r.message, Toast.LENGTH_SHORT).show()
            }
        }.start()
    }

    // =====================================================================
    // Rendering
    private fun renderAll() {
        if (Prefs.key(this) == null) return
        renderHome()
        renderList()
        renderSettings()
    }

    private fun greeting(): String = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
        in 5..11 -> "শুভ সকাল"
        in 12..14 -> "শুভ দুপুর"
        in 15..17 -> "শুভ বিকাল"
        in 18..19 -> "শুভ সন্ধ্যা"
        else -> "শুভ রাত্রি"
    }

    private fun startOfToday(): Long {
        val c = Calendar.getInstance()
        c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }

    private fun renderHome() {
        val all = Store.all(this)
        val pays = all.filter { it.isPayment }
        val day = 86_400_000L
        val t0 = startOfToday()
        val today = pays.filter { it.time >= t0 }

        tvGreeting.text = greeting()
        tvToday.text = today.size.toString()
        ring.percent = if (today.isEmpty()) 0 else today.count { it.used } * 100 / today.size
        tvUnused.text = pays.count { !it.used }.toString()
        tvUsed.text = pays.count { it.used }.toString()
        tvTaka.text = "৳ ${Ui.money(today.sumOf { it.amount })}"
        tvPending.text = pays.count { !it.uploaded }.toString()

        // weekly chart (last 7 days, today on the right)
        val names = arrayOf("র", "সো", "ম", "বু", "বৃ", "শু", "শ")   // Sun..Sat
        val vals = IntArray(7)
        val labs = Array(7) { "" }
        for (i in 0 until 7) {
            val start = t0 - (6 - i) * day
            vals[i] = pays.count { it.time >= start && it.time < start + day }
            val c = Calendar.getInstance().apply { timeInMillis = start + day / 2 }
            labs[i] = names[c.get(Calendar.DAY_OF_WEEK) - 1]
        }
        chart.labels = labs
        chart.values = vals

        // status
        val err = Prefs.lastError(this)
        val online = err.isEmpty() && System.currentTimeMillis() - Prefs.lastOk(this) < 180_000
        dotStatus.backgroundTintList =
            android.content.res.ColorStateList.valueOf(Color.parseColor(if (online) "#22A55B" else "#E53E3E"))
        tvStatus.text = "SMS Reader • " + if (online) "Online" else "Offline"

        val banner = when {
            !hasSms() -> "SMS পারমিশন দেওয়া হয়নি। সেটিংসে গিয়ে পারমিশন দিন।"
            err.isNotEmpty() -> err
            else -> ""
        }
        tvError.visibility = if (banner.isEmpty()) View.GONE else View.VISIBLE
        tvError.text = banner

        val ok = Prefs.lastOk(this)
        tvLastSync.text = if (ok == 0L) "এখনও সিঙ্ক হয়নি" else "সর্বশেষ সিঙ্ক: ${Ui.time(ok)}"
    }

    private fun filteredList(): List<Sms> {
        val q = etSearch.text.toString().trim().lowercase()
        val all = Store.all(this)
        val byChip = when (chipGroup.checkedChipId) {
            R.id.chipUnused -> all.filter { it.isPayment && !it.used }
            R.id.chipUsed -> all.filter { it.used }
            R.id.chipPending -> all.filter { it.isPayment && !it.uploaded }
            R.id.chipIgnored -> all.filter { !it.isPayment }
            else -> all
        }
        if (q.isEmpty()) return byChip
        return byChip.filter {
            it.trx.lowercase().contains(q) || it.from.contains(q) ||
                it.address.lowercase().contains(q) || Ui.money(it.amount).contains(q)
        }
    }

    private fun renderList() {
        val list = filteredList()
        val sig = list.joinToString("|") { it.id + it.used + it.uploaded }
        if (sig == lastListSig) return
        lastListSig = sig
        adapter.submit(list)
        tvCount.text = "মোট ${list.size} টি"
        tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun renderSettings() {
        val key = Prefs.key(this) ?: return
        tvKeyMasked.text = if (key.length > 10) key.take(8) + "••••••" + key.takeLast(4) else key
        val err = Prefs.lastError(this)
        if (err.isEmpty() && Prefs.lastOk(this) > 0) {
            tvKeyStatus.text = "সক্রিয় • সংযুক্ত"; Ui.pill(tvKeyStatus, "#1B7F55", "#CDF0E1")
        } else {
            tvKeyStatus.text = if (err.isEmpty()) "যাচাই হচ্ছে..." else err
            Ui.pill(tvKeyStatus, "#C2255C", "#FFD9E3")
        }

        setPerm(stSms, hasSms(), "চালু", "দিন")
        setPerm(stBattery, batteryOk(), "Unrestricted", "Restricted")
        setPerm(stNotif, notifOk(), "চালু", "দিন")

        if (swKeepAlive.isChecked != Prefs.keepAlive(this)) {
            swKeepAlive.setOnCheckedChangeListener(null)
            swKeepAlive.isChecked = Prefs.keepAlive(this)
            swKeepAlive.setOnCheckedChangeListener { _, on ->
                Prefs.setKeepAlive(this, on)
                if (on && Prefs.key(this) != null) KeepAliveService.start(this) else KeepAliveService.stop(this)
            }
        }
        tvAbout.text = "SMS READER • v1.0\nসার্ভার: ${Api.DB_URL}\nশুধু bKash / Nagad / Rocket-এর SMS সংরক্ষণ করা হয়, অন্য কোনো SMS পড়া বা সংরক্ষণ করা হয় না।"
    }

    private fun setPerm(tv: TextView, ok: Boolean, yes: String, no: String) {
        tv.text = if (ok) yes else no
        if (ok) Ui.pill(tv, "#1B7F55", "#CDF0E1") else Ui.pill(tv, "#C2255C", "#FFD9E3")
    }

    private fun showDetails(s: Sms) {
        val status = when {
            !s.isPayment -> "অন্যান্য SMS (পেমেন্ট নয়)"
            s.used -> "ব্যবহৃত"
            !s.uploaded -> "আপলোড বাকি"
            else -> "অব্যবহৃত"
        }
        val sb = StringBuilder()
        if (s.isPayment) {
            sb.append("TrxID: ${s.trx}\n")
            sb.append("পরিমাণ: ৳ ${Ui.money(s.amount)}\n")
            sb.append("প্রেরক: ${if (s.from.isEmpty()) "অজানা" else s.from}\n")
        }
        sb.append("সময়: ${Ui.time(s.time)}\n")
        sb.append("অবস্থা: $status\n\n")
        sb.append("SMS:\n${s.body}")
        val b = MaterialAlertDialogBuilder(this)
            .setTitle(Ui.methodName(s.method))
            .setMessage(sb.toString())
            .setNegativeButton("বন্ধ", null)
        if (s.isPayment) {
            b.setPositiveButton("TrxID কপি") { _, _ ->
                val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("trx", s.trx))
                Toast.makeText(this, "কপি হয়েছে", Toast.LENGTH_SHORT).show()
            }
        }
        b.show()
    }

    // =====================================================================
    // Permissions
    private fun hasSms() =
        ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED

    private fun batteryOk(): Boolean =
        (getSystemService(Context.POWER_SERVICE) as PowerManager).isIgnoringBatteryOptimizations(packageName)

    private fun notifOk(): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun requestSms() {
        if (hasSms()) toast("SMS পারমিশন দেওয়া আছে")
        else permLauncher.launch(arrayOf(Manifest.permission.RECEIVE_SMS))
    }

    private fun requestNotif() {
        if (notifOk()) toast("নোটিফিকেশন পারমিশন দেওয়া আছে")
        else permLauncher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
    }

    private fun requestBattery() {
        if (batteryOk()) { toast("ব্যাটারি অপটিমাইজেশন আগেই বন্ধ আছে"); return }
        try {
            startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:$packageName")))
        } catch (e: Exception) {
            try { startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) }
            catch (_: Exception) { openAppSettings() }
        }
    }

    private fun requestAllPermissions() {
        val need = mutableListOf<String>()
        if (!hasSms()) need.add(Manifest.permission.RECEIVE_SMS)
        if (!notifOk()) need.add(Manifest.permission.POST_NOTIFICATIONS)
        if (need.isNotEmpty()) {
            chainBattery = true
            permLauncher.launch(need.toTypedArray())
        } else if (!batteryOk()) {
            requestBattery()
        } else {
            toast("সব পারমিশন দেওয়া আছে")
        }
    }

    private fun showAutostartHelp() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Autostart চালু করুন")
            .setMessage(
                "Xiaomi / Redmi / Poco: Security → Permissions → Autostart → SMS READER চালু\n\n" +
                "Oppo / Realme / Vivo: Settings → Battery → App battery management → SMS READER → Allow background activity / Autostart\n\n" +
                "Samsung: Settings → Battery → Background usage limits → \"Never sleeping apps\"-এ SMS READER যোগ করুন\n\n" +
                "এছাড়া Recent Apps-এ অ্যাপটি লক (🔒) করে রাখুন।"
            )
            .setPositiveButton("অ্যাপ সেটিংস খুলুন") { _, _ -> openAppSettings() }
            .setNegativeButton("বন্ধ", null)
            .show()
    }

    private fun openAppSettings() {
        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
    }

    private fun toast(m: String) = Toast.makeText(this, m, Toast.LENGTH_SHORT).show()
}
