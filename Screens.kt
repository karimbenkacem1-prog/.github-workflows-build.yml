package com.karimbenkacem.yourtime.ui

import android.Manifest
import android.app.TimePickerDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.karimbenkacem.yourtime.MainVM
import com.karimbenkacem.yourtime.UiState
import com.karimbenkacem.yourtime.data.*
import kotlinx.coroutines.delay
import java.time.LocalDate

private fun Context.openIntent(i: Intent) { try { startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } catch (e: ActivityNotFoundException) { } }
private fun Context.usageSettings() = openIntent(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
private fun Context.url(u: String) = openIntent(Intent(Intent.ACTION_VIEW, Uri.parse(u)))

@Composable private fun Body(text: String, center: Boolean = true, alpha: Float = .85f) =
    Text(text, textAlign = if (center) TextAlign.Center else null, color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
        style = MaterialTheme.typography.bodyMedium)

@Composable private fun Title(text: String, color: Color = Gold) =
    Text(text, color = color, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 20.sp, textAlign = TextAlign.Center)

// ===================== Onboarding =====================
@Composable
fun Onboarding(vm: MainVM, s: Prefs) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    val ctx = LocalContext.current
    val notif = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { vm.put(K.onboarded, true) }
    Column(Modifier.fillMaxSize().systemBarsPadding().verticalScroll(rememberScrollState()).padding(24.dp),
        Arrangement.SpaceBetween, Alignment.CenterHorizontally) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
            QuranIllustration(Modifier.fillMaxWidth().height(130.dp))
            Text("وقتك ثمين", fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 38.sp, color = Gold)
            Text("Your Time Is Precious", color = MaterialTheme.colorScheme.onBackground.copy(alpha = .8f))
            Spacer(Modifier.height(8.dp))
            GlassCard {
                when (step) {
                    0 -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(s.lang == "ar", { vm.put(K.lang, "ar") }, { Text("العربية") })
                            FilterChip(s.lang == "en", { vm.put(K.lang, "en") }, { Text("English") })
                        }
                        Title(t("وقتك أمانة", "Your time is a trust"))
                        Body(t("يذكّرك هذا التطبيق بلطف أن وقتك ثمين، ويشجعك على تقليل الاستخدام غير الضروري للهاتف وقضاء وقت أكثر في القرآن والعلم والعبادة.",
                            "This app gently reminds you that your time is precious and encourages you to spend less time on unnecessary phone use and more on the Quran, learning and worship."))
                    }
                    1 -> {
                        Title(t("صلاحية الوصول إلى الاستخدام", "Usage access"))
                        Body(t("لحساب مدة استخدامك للهاتف نحتاج إلى صلاحية «الوصول إلى بيانات الاستخدام». تبقى البيانات على جهازك فقط ولا تُرسل إلى أي جهة. فعّل «وقتك ثمين» من القائمة التي ستظهر.",
                            "To measure how long you use your phone we need the “Usage access” permission. Data stays on your device and is never sent anywhere. Enable “Your Time Is Precious” in the list that opens."))
                        Button({ ctx.usageSettings() }) { Text(t("فتح الإعدادات", "Open settings")) }
                    }
                    else -> {
                        Title(t("الإشعارات", "Notifications"))
                        Body(t("نحتاج إذن الإشعارات لنرسل لك تذكيرات هادئة. يمكنك تعديل كل شيء لاحقاً من الإعدادات.",
                            "We need notification permission to send you gentle reminders. You can change everything later in Settings."))
                    }
                }
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Button({ if (step < 2) step++ else if (Build.VERSION.SDK_INT >= 33) notif.launch(Manifest.permission.POST_NOTIFICATIONS) else vm.put(K.onboarded, true) },
                Modifier.fillMaxWidth()) { Text(if (step < 2) t("التالي", "Next") else t("ابدأ", "Start")) }
            if (step == 1) TextButton({ step++ }) { Text(t("لاحقاً", "Later")) }
            if (step == 2) TextButton({ vm.put(K.onboarded, true) }) { Text(t("تخطَّ", "Skip")) }
        }
    }
}

// ===================== Scaffold =====================
@Composable
fun MainScaffold(vm: MainVM, s: Prefs, ui: UiState) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var about by rememberSaveable { mutableStateOf(false) }
    BackHandler(about || tab != 0) { if (about) about = false else tab = 0 }
    Scaffold(containerColor = Color.Transparent, contentWindowInsets = WindowInsets.systemBars,
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = .92f)) {
                listOf("🏠" to t("الرئيسية", "Home"), "📊" to t("إحصاءات", "Stats"), "🌿" to t("استثمر", "Invest"), "⚙️" to t("الإعدادات", "Settings"))
                    .forEachIndexed { i, (e, l) ->
                        NavigationBarItem(tab == i && !about, { tab = i; about = false }, { Text(e) }, label = { Text(l, fontSize = 11.sp) })
                    }
            }
        }) { pad ->
        Box(Modifier.padding(pad)) {
            when {
                about -> AboutScreen()
                tab == 0 -> HomeScreen(s, ui) { tab = it }
                tab == 1 -> StatsScreen(s, ui)
                tab == 2 -> InvestScreen(vm, s)
                else -> SettingsScreen(vm, s) { about = true }
            }
        }
    }
}

@Composable private fun Page(content: @Composable ColumnScope.() -> Unit) =
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), Arrangement.spacedBy(16.dp), Alignment.CenterHorizontally, content = content)

@Composable
private fun PermissionCard() {
    val ctx = LocalContext.current
    GlassCard {
        Title(t("فعّل صلاحية الاستخدام", "Enable usage access"))
        Body(t("لنقيس وقت استخدامك نحتاج هذه الصلاحية. الإعدادات ← الوصول إلى بيانات الاستخدام ← وقتك ثمين ← السماح.",
            "We need this to measure your usage. Settings → Usage access → Your Time Is Precious → Allow."))
        Button({ ctx.usageSettings() }) { Text(t("فتح الإعدادات", "Open settings")) }
    }
}

// ===================== Home =====================
@Composable
fun HomeScreen(s: Prefs, ui: UiState, go: (Int) -> Unit) {
    val ctx = LocalContext.current; val ar = LocalAr.current
    val d = DAILY[LocalDate.now().dayOfYear % DAILY.size]
    Page {
        Text("وقتك ثمين", fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 36.sp, color = Gold)
        Text("Your Time Is Precious", color = MaterialTheme.colorScheme.onBackground.copy(alpha = .8f))
        QuranIllustration(Modifier.fillMaxWidth().height(110.dp))
        GlassCard {
            Text("وَالْعَصْرِ ۝ إِنَّ الْإِنْسَانَ لَفِي خُسْرٍ", fontFamily = FontFamily.Serif, fontSize = 24.sp, lineHeight = 40.sp,
                textAlign = TextAlign.Center, color = Gold)
            Body("“By time, indeed, mankind is in loss.”")
            Body("سورة العصر ١٠٣:١-٢ | Surah Al-Asr 103:1-2", alpha = .6f)
        }
        GlassCard {
            Text(t("رسالة اليوم", "Today’s reminder"), color = Gold, fontWeight = FontWeight.Bold)
            Body(if (ar) d.ar else d.en); Body(if (ar) d.srcAr else d.srcEn, alpha = .6f)
        }
        if (!ui.hasAccess) PermissionCard() else GlassCard {
            UsageRing(ui.todayMs.toFloat() / (s.goalMin * 60_000f), fmtMs(ui.todayMs, ar), t("من ${fmtMin(s.goalMin, true)}", "of ${fmtMin(s.goalMin, false)}"))
            Body(if (ui.todayMs <= s.goalMin * 60_000L) t("أنت ضمن هدفك اليوم، بارك الله في وقتك 🌿", "You’re within your goal today. May Allah bless your time 🌿")
            else t("لحظة جديدة لبداية طيبة، فاجعل لك نصيباً من الذكر والقرآن.", "A new moment is a good start. Give yourself a share of dhikr and Quran."))
            ui.nextMin?.let { Text(t("التذكير القادم بعد ", "Next reminder in ") + fmtMin(it, ar), color = Gold, fontWeight = FontWeight.Bold) }
        }
        Button({ go(1) }, Modifier.fillMaxWidth()) { Text(t("📊 الإحصاءات التفصيلية", "📊 Detailed statistics")) }
        OutlinedButton({ go(3) }, Modifier.fillMaxWidth()) { Text(t("⏰ تخصيص التذكيرات", "⏰ Customize reminders")) }
        FilledTonalButton({ ctx.url("https://quran.com") }, Modifier.fillMaxWidth()) { Text(t("📖 افتح المصحف", "📖 Open the Quran")) }
        TextButton({ go(2) }) { Text(t("🌿 استثمر وقتك في عمل نافع", "🌿 Invest your time in something beneficial")) }
    }
}

// ===================== Stats =====================
@Composable
fun StatsScreen(s: Prefs, ui: UiState) {
    val ar = LocalAr.current
    Page {
        Title(t("إحصاءات استخدامك", "Your usage"))
        if (!ui.hasAccess) { PermissionCard(); return@Page }
        GlassCard {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceEvenly) {
                Stat(fmtMs(ui.todayMs, ar), t("اليوم", "Today"))
                Stat("${ui.sessions}", t("جلسات متواصلة", "Sessions"))
                Stat(fmtMs(ui.longestMs, ar), t("أطول جلسة", "Longest"))
                Stat("${ui.reminders}", t("تذكيرات", "Reminders"))
            }
        }
        GlassCard {
            Text(t("آخر ٧ أيام", "Last 7 days"), color = Gold, fontWeight = FontWeight.Bold)
            BarChart(ui.week, s.goalMin * 60_000L, ar)
            val prev = ui.week.dropLast(1).map { it.second }.filter { it > 0 }
            if (prev.isNotEmpty()) {
                val avg = prev.average().toLong()
                Body(if (ui.todayMs <= avg) t("اليوم أقل من معدّل الأيام الماضية بـ ${fmtMs(avg - ui.todayMs, true)} — أحسنت 🌿", "Today is ${fmtMs(avg - ui.todayMs, false)} below your recent average — well done 🌿")
                else t("يمكنك اليوم أن تخصص وقتاً نافعاً بعيداً عن الشاشة 🌿", "You can set aside some beneficial screen-free time today 🌿"))
            }
        }
        GlassCard {
            Text(t("التطبيقات الأكثر استخداماً اليوم", "Most used apps today"), color = Gold, fontWeight = FontWeight.Bold)
            val max = (ui.apps.maxOfOrNull { it.second } ?: 1L).toFloat()
            if (ui.apps.isEmpty()) Body(t("لا توجد بيانات بعد.", "No data yet."))
            ui.apps.forEach { (name, ms) ->
                Column(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) { Text(name, Modifier.weight(1f), maxLines = 1); Text(fmtMs(ms, ar)) }
                    LinearProgressIndicator({ ms / max }, Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)), color = Emerald, trackColor = Gold.copy(alpha = .2f))
                }
            }
        }
    }
}
@Composable private fun Stat(v: String, l: String) = Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Text(v, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Gold); Text(l, fontSize = 11.sp, textAlign = TextAlign.Center)
}

// ===================== Invest =====================
@Composable
fun InvestScreen(vm: MainVM, s: Prefs) {
    val ctx = LocalContext.current; val ar = LocalAr.current
    val end by vm.sessionEnd.collectAsState()
    var left by remember { mutableLongStateOf(0L) }
    LaunchedEffect(end) {
        while (end != null) { left = (end!! - System.currentTimeMillis()) / 1000; if (left <= 0) { vm.sessionEnd.value = null; break }; delay(1000) }
    }
    Page {
        Title(t("استثمر وقتك | Invest Your Time", "Invest Your Time | استثمر وقتك"))
        GlassCard {
            if (end == null) {
                Text(t("ابدأ جلسة نافعة", "Start a beneficial session"), color = Gold, fontWeight = FontWeight.Bold)
                Body(t("اختر مدة، وضع الهاتف جانباً.", "Pick a duration and put your phone aside."))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(10, 15, 30).forEach { m -> Button({ vm.startSession(m) }) { Text(t("$m د", "$m min")) } }
                }
            } else {
                Text("%02d:%02d".format(left / 60, left % 60), fontSize = 44.sp, fontFamily = FontFamily.Serif, color = Gold)
                Body(t("بارك الله في وقتك 🌿 ستصلك رسالة عند الانتهاء.", "May Allah bless your time 🌿 You’ll get a notification when done."))
                OutlinedButton({ vm.cancelSession() }) { Text(t("إنهاء", "End")) }
            }
        }
        Text(t("اختر أنشطتك المفضلة", "Choose your preferred activities"), color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold)
        ACTIVITIES.forEach { a ->
            val on = a.id in s.acts
            Surface(Modifier.fillMaxWidth().clickable { vm.toggleActivity(a.id) }, RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = .85f), border = BorderStroke(1.dp, if (on) Gold else Gold.copy(alpha = .25f))) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(a.emoji, fontSize = 24.sp); Text(if (ar) a.ar else a.en, Modifier.weight(1f)); Checkbox(on, { vm.toggleActivity(a.id) })
                }
            }
        }
        FilledTonalButton({ ctx.url("https://quran.com") }) { Text(t("📖 افتح المصحف", "📖 Open the Quran")) }
    }
}

// ===================== Settings =====================
@Composable private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) = GlassCard {
    Text(title, color = Gold, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif)
    Column(Modifier.fillMaxWidth(), Arrangement.spacedBy(10.dp), content = content)
}
@Composable private fun SwitchRow(label: String, v: Boolean, onChange: (Boolean) -> Unit) =
    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) { Text(label, Modifier.weight(1f)); Switch(v, onChange) }
@Composable private fun Stepper(label: String, value: Int, min: Int, max: Int, step: Int, fmt: (Int) -> String, onChange: (Int) -> Unit) =
    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f))
        FilledTonalIconButton({ onChange((value - step).coerceAtLeast(min)) }) { Text("−", fontSize = 20.sp) }
        Text(fmt(value), Modifier.widthIn(min = 80.dp), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
        FilledTonalIconButton({ onChange((value + step).coerceAtMost(max)) }) { Text("+", fontSize = 20.sp) }
    }
@Composable private fun Chips(options: List<String>, selected: Int, onPick: (Int) -> Unit) =
    Row(Modifier.horizontalScroll(rememberScrollState()), Arrangement.spacedBy(8.dp)) {
        options.forEachIndexed { i, o -> FilterChip(selected == i, { onPick(i) }, { Text(o) }) }
    }

@Composable
fun SettingsScreen(vm: MainVM, s: Prefs, openAbout: () -> Unit) {
    val ctx = LocalContext.current; val ar = LocalAr.current
    fun pickTime(cur: Int, onSet: (Int) -> Unit) = TimePickerDialog(ctx, { _, h, m -> onSet(h * 60 + m) }, cur / 60, cur % 60, true).show()
    fun hm(m: Int) = "%02d:%02d".format(m / 60, m % 60)
    Page {
        Title(t("الإعدادات", "Settings"))
        Section(t("اللغة والمظهر", "Language & appearance")) {
            Chips(listOf("العربية", "English"), if (s.lang == "ar") 0 else 1) { vm.put(K.lang, if (it == 0) "ar" else "en") }
            Chips(listOf(t("النظام", "System"), t("فاتح", "Light"), t("داكن", "Dark")), s.theme) { vm.put(K.theme, it) }
            Text(t("الخلفية الإسلامية", "Islamic background"))
            Chips(listOf(t("نجوم هندسية", "Geometric stars"), t("محراب وأقواس", "Mihrab arches"), t("زخارف", "Arabesque")), s.bg) { vm.put(K.bg, it) }
        }
        Section(t("التذكيرات", "Reminders")) {
            SwitchRow(t("تذكير الاستخدام الكلي", "Total usage reminder"), s.totalOn) { vm.put(K.totalOn, it) }
            if (s.totalOn) Stepper(t("كل", "Every"), s.totalMin, 30, 720, 30, { fmtMin(it, ar) }) { vm.put(K.totalMin, it) }
            SwitchRow(t("تذكير الاستخدام المتواصل", "Continuous usage reminder"), s.contOn) { vm.put(K.contOn, it) }
            if (s.contOn) {
                Stepper(t("كل", "Every"), s.contMin, 15, 480, 15, { fmtMin(it, ar) }) { vm.put(K.contMin, it) }
                Stepper(t("استراحة تُعدّ فاصلاً", "Break counts after"), s.breakMin, 1, 30, 1, { fmtMin(it, ar) }) { vm.put(K.breakMin, it) }
            }
            Stepper(t("الهدف اليومي", "Daily goal"), s.goalMin, 30, 720, 30, { fmtMin(it, ar) }) { vm.put(K.goalMin, it) }
        }
        Section(t("الصوت والاهتزاز", "Sound & vibration")) {
            SwitchRow(t("الصوت", "Sound"), s.sound) { vm.put(K.sound, it) }
            SwitchRow(t("الاهتزاز", "Vibration"), s.vibrate) { vm.put(K.vibrate, it) }
        }
        Section(t("ساعات الهدوء", "Quiet hours")) {
            SwitchRow(t("عدم الإزعاج أثناء النوم", "Don’t disturb while sleeping"), s.quietOn) { vm.put(K.quietOn, it) }
            if (s.quietOn) Row(Modifier.fillMaxWidth(), Arrangement.SpaceEvenly) {
                OutlinedButton({ pickTime(s.quietStart) { vm.put(K.quietStart, it) } }) { Text(t("من ", "From ") + hm(s.quietStart)) }
                OutlinedButton({ pickTime(s.quietEnd) { vm.put(K.quietEnd, it) } }) { Text(t("إلى ", "To ") + hm(s.quietEnd)) }
            }
        }
        Section(t("الخصوصية والصلاحيات", "Privacy & permissions")) {
            Body(t("كل البيانات تُحفظ على جهازك فقط. لا حسابات ولا إنترنت ولا إعلانات.", "All data stays on your device. No accounts, no internet, no ads."), center = false)
            Body(t("ملاحظة: يحسب التطبيق الاستخدام المتواصل من أحداث تشغيل/إطفاء الشاشة. إن أوقف نظامك الخدمة الخلفية قد يتأخر التذكير حتى ١٥ دقيقة تقريباً.",
                "Note: continuous use is derived from screen on/off events. If your phone kills the background service, a reminder may be delayed by up to ~15 minutes."), center = false, alpha = .65f)
            OutlinedButton({ ctx.usageSettings() }, Modifier.fillMaxWidth()) { Text(t("صلاحية الوصول إلى الاستخدام", "Usage access")) }
            OutlinedButton({ ctx.openIntent(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName)) }, Modifier.fillMaxWidth()) { Text(t("إعدادات الإشعارات", "Notification settings")) }
            OutlinedButton({ ctx.openIntent(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) }, Modifier.fillMaxWidth()) { Text(t("استثناء من توفير البطارية (يحسّن الدقة)", "Battery optimization (improves accuracy)")) }
        }
        Button(openAbout, Modifier.fillMaxWidth()) { Text(t("ℹ️ عن التطبيق", "ℹ️ About the app")) }
    }
}

// ===================== About =====================
@Composable
fun AboutScreen() {
    val ctx = LocalContext.current
    Page {
        QuranIllustration(Modifier.fillMaxWidth().height(120.dp))
        Text("وقتك ثمين", fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 34.sp, color = Gold)
        Text("Your Time Is Precious")
        GlassCard {
            Text(t("عن التطبيق", "About the app"), color = Gold, fontWeight = FontWeight.Bold)
            Body("وقتك ثمين | Your Time Is Precious")
            Text(t("تطوير", "Created by"), color = Gold, fontWeight = FontWeight.Bold)
            Body("كريم بن قاسم | Karim Benkacem")
            Text("WhatsApp", color = Gold, fontWeight = FontWeight.Bold)
            TextButton({ ctx.url("https://wa.me/213778388878") }) { Text("+213 778 38 88 78", fontSize = 18.sp) }
            Body("الإصدار 1.0.0 | Version 1.0.0", alpha = .6f)
        }
    }
}
