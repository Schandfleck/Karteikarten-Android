@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package de.moritz.karteikarten

import android.content.SharedPreferences
import android.database.sqlite.SQLiteConstraintException
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private data class Confirmation(val title: String, val body: String, val action: () -> Unit)

class MainActivity : ComponentActivity() {
    private lateinit var store: CardStore
    private lateinit var prefs: SharedPreferences
    private var revision by mutableIntStateOf(0)
    private var deck by mutableLongStateOf(-1)
    private var currentId by mutableLongStateOf(-1)
    private var tab by mutableIntStateOf(0)
    private var settingsOrigin by mutableIntStateOf(0)
    private var mode by mutableIntStateOf(0)
    private var filter by mutableIntStateOf(0)
    private var sort by mutableIntStateOf(0)
    private var revealed by mutableStateOf(false)
    private var sessionTotal by mutableIntStateOf(0)
    private val queue = mutableListOf<Long>()
    private var english by mutableStateOf(false)
    private var appearance by mutableIntStateOf(0)
    private var feedback by mutableStateOf<String?>(null)
    private var newDeck by mutableStateOf(false)
    private var editorId by mutableLongStateOf(-2)
    private var confirmation by mutableStateOf<Confirmation?>(null)

    private val exportFile = registerForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        if(uri != null) runCatching {
            contentResolver.openOutputStream(uri,"wt")?.use { store.exportTo(it) } ?: error("Cannot open file")
        }.onSuccess { feedback=t("Datenbank exportiert","Database exported") }
         .onFailure { feedback=t("Export fehlgeschlagen. Bitte eine andere Datei wählen.","Export failed. Please choose another file.") }
    }
    private val importFile = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if(uri != null) runCatching {
            contentResolver.openInputStream(uri)?.use { store.importFrom(it) } ?: error("Cannot open file")
        }.onSuccess {
            deck=store.decks().firstOrNull()?.id ?: -1; startSession(); revision++
            feedback=t("Datenbank importiert","Database imported")
        }.onFailure {
            feedback=t("Import fehlgeschlagen. Vorhandene Karten bleiben erhalten.","Import failed. Existing cards were preserved.")
        }
    }
    override fun onCreate(saved: Bundle?) {
        super.onCreate(saved)
        enableEdgeToEdge()
        store=CardStore(this); prefs=getSharedPreferences("settings",MODE_PRIVATE)
        english=prefs.getBoolean("english",false); appearance=prefs.getInt("appearance",0)
        deck=saved?.getLong("deck",-1) ?: -1
        if(store.decks().none { it.id==deck }) deck=store.decks().firstOrNull()?.id ?: -1
        if(saved != null) {
            settingsOrigin=saved.getInt("settingsOrigin",0).coerceIn(0,1)
            tab=saved.getInt("tab"); mode=saved.getInt("mode").coerceIn(0,2); filter=saved.getInt("filter"); sort=saved.getInt("sort")
            currentId=saved.getLong("current",-1); revealed=saved.getBoolean("revealed"); sessionTotal=saved.getInt("sessionTotal")
            queue.addAll(saved.getLongArray("queue")?.toList() ?: emptyList())
            newDeck=saved.getBoolean("newDeck"); editorId=saved.getLong("editorId",-2)
            if(currentId>=0 && store.card(currentId)==null) startSession()
        } else startSession()
        setContent { AppTheme { Application() } }
    }
    override fun onSaveInstanceState(out: Bundle) {
        super.onSaveInstanceState(out)
        out.putLong("deck",deck); out.putLong("current",currentId); out.putInt("tab",tab); out.putInt("mode",mode)
        out.putInt("filter",filter); out.putInt("sort",sort); out.putBoolean("revealed",revealed)
        out.putInt("sessionTotal",sessionTotal); out.putLongArray("queue",queue.toLongArray())
        out.putInt("settingsOrigin",settingsOrigin)
        out.putBoolean("newDeck",newDeck); out.putLong("editorId",editorId)
    }
    override fun onDestroy() { store.close(); super.onDestroy() }
    private fun t(de: String, en: String) = if(english) en else de
    private fun navigateTo(destination: Int) {
        if(destination==2 && tab!=2) settingsOrigin=tab.coerceIn(0,1)
        tab=destination
    }
    private fun rating(r: Int) = listOf(t("Nochmal","Again"),t("Schwer","Hard"),t("Gut","Good"),t("Einfach","Easy"))[r]
    private fun modes() = listOf(t("Alle Karten","All cards"),t("Nur schwere Karten","Hard cards only"),t("Nur einfache Karten","Easy cards only"))
    private fun filters() = listOf(t("Alle Bewertungen","All ratings"),rating(1),rating(2),rating(3))
    private fun sorts() = listOf(t("Reihenfolge","Original order"),t("Schwere zuerst","Hardest first"),t("Einfache zuerst","Easiest first"))
    private fun startSession() {
        queue.clear(); queue.addAll(if(deck>=0) store.session(deck,mode) else emptyList())
        sessionTotal=queue.size; nextCard()
    }
    private fun nextCard() { revealed=false; currentId=if(queue.isEmpty()) -1 else queue.removeAt(0); revision++ }
    private fun rate(q: Int) {
        if(!revealed) return
        val card=store.card(currentId) ?: return
        store.rate(card,q)
        nextCard()
    }
    private fun confirm(title: String, body: String, action: () -> Unit) { confirmation=Confirmation(title,body,action) }
    private fun deleteDeck() {
        if(deck<0) return
        val id=deck
        confirm(t("Stapel löschen","Delete deck"),t("Diesen Stapel und alle zugehörigen Karten löschen?","Delete this deck and all its cards?")) {
            store.deleteDeck(id); deck=store.decks().firstOrNull()?.id ?: -1; startSession(); revision++
        }
    }
    private fun askImport() = confirm(t("Datenbank importieren","Import database"),t("Der Import ersetzt alle Stapel und Karten. Exportiere bei Bedarf vorher eine Sicherung.","Import replaces all decks and cards. Export a backup first if needed.")) { importFile.launch(arrayOf("*/*")) }
    @Composable private fun AppTheme(content: @Composable () -> Unit) {
        val dark=when(appearance) { 1 -> false; 2 -> true; else -> isSystemInDarkTheme() }
        val dynamic=prefs.getBoolean("dynamic",true) && Build.VERSION.SDK_INT>=31
        val baseline=when {
            dynamic && dark -> dynamicDarkColorScheme(this)
            dynamic -> dynamicLightColorScheme(this)
            dark -> darkColorScheme()
            else -> lightColorScheme()
        }
        // Existing personal colors remain an optional override of Material color roles.
        val keys=listOf("window","base","text","button","accent")
        val custom=remember(revision) { keys.any { prefs.contains(it) } }
        val scheme=if(custom) {
            fun col(key: String, fallback: Color): Color = runCatching { prefs.getString(key,null)?.let { Color(android.graphics.Color.parseColor(it)) } ?: fallback }.getOrDefault(fallback)
            val surface=col("window",baseline.surface)
            val container=col("base",baseline.surfaceContainerLow)
            val ink=col("text",baseline.onSurface)
            val primary=col("accent",baseline.primary)
            val tonal=col("button",baseline.secondaryContainer)
            baseline.copy(primary=primary,onPrimary=contrast(primary),surface=surface,background=surface,onSurface=ink,onBackground=ink,
                onSurfaceVariant=ink.copy(alpha=.8f),surfaceContainerLow=container,surfaceContainer=container,surfaceContainerHigh=container,
                secondaryContainer=tonal,onSecondaryContainer=contrast(tonal))
        } else baseline
        SideEffect {
            val controls=WindowCompat.getInsetsController(window,window.decorView)
            controls.isAppearanceLightStatusBars=!dark; controls.isAppearanceLightNavigationBars=!dark
        }
        MaterialTheme(colorScheme=scheme,typography=Typography(),shapes=Shapes(),content=content)
    }
    private fun contrast(color: Color) = if(color.luminance()>.179f) Color.Black else Color.White

    @Composable private fun Symbol(resource: Int, description: String? = null) {
        Icon(painterResource(resource),description)
    }
    @Composable private fun Application() {
        val decks=remember(revision) { store.decks() }
        val selected=decks.firstOrNull { it.id==deck }
        val wide=with(LocalDensity.current) { LocalWindowInfo.current.containerSize.width.toDp()>=600.dp }
        var menu by remember { mutableStateOf(false) }
        val snackbar=remember { SnackbarHostState() }
        LaunchedEffect(feedback) { feedback?.let { snackbar.showSnackbar(it); feedback=null } }
        BackHandler(enabled=tab==2 && !menu && !newDeck && editorId < -1 && confirmation==null) { tab=settingsOrigin }
        Row(Modifier.fillMaxSize()) {
            Scaffold(
                modifier=Modifier.weight(1f),
                topBar={
                    TopAppBar(title={
                        Column {
                            Text(when(tab) { 1 -> t("Karten","Cards"); 2 -> t("Einstellungen","Settings"); else -> t("Lernen","Learn") })
                            if(wide && tab==0 && currentId>=0) Text(t("Noch ${queue.size+1} Karte(n)","${queue.size+1} card(s) left"),style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    },navigationIcon={
                        if(tab==2) IconButton(onClick={tab=settingsOrigin}) { Symbol(R.drawable.ic_back,t("Zurück","Back")) }
                    },actions={
                        IconButton(onClick={menu=true}) { Symbol(R.drawable.ic_more,t("Weitere Optionen","More options")) }
                        DropdownMenu(expanded=menu,onDismissRequest={menu=false}) {
                            DropdownMenuItem(text={Text(t("Lernen","Learn"))},leadingIcon={Symbol(R.drawable.ic_learn)},onClick={menu=false;navigateTo(0)})
                            DropdownMenuItem(text={Text(t("Karten","Cards"))},leadingIcon={Symbol(R.drawable.ic_cards)},onClick={menu=false;navigateTo(1)})
                            DropdownMenuItem(text={Text(t("Einstellungen","Settings"))},leadingIcon={Symbol(R.drawable.ic_settings)},onClick={menu=false;navigateTo(2)})
                            HorizontalDivider()
                            DropdownMenuItem(text={Text(t("Neuer Stapel","New deck"))},leadingIcon={Symbol(R.drawable.ic_add)},onClick={menu=false;newDeck=true})
                            DropdownMenuItem(text={Text(t("Stapel löschen","Delete deck"))},leadingIcon={Symbol(R.drawable.ic_delete)},enabled=deck>=0,onClick={menu=false;deleteDeck()})
                            HorizontalDivider()
                            DropdownMenuItem(text={Text(t("Datenbank importieren","Import database"))},onClick={menu=false;askImport()})
                            DropdownMenuItem(text={Text(t("Datenbank exportieren","Export database"))},onClick={menu=false;exportFile.launch("karten.db")})
                        }
                    })
                },
                snackbarHost={SnackbarHost(snackbar)},
                floatingActionButton={if(tab==1 && deck>=0) ExtendedFloatingActionButton(text={Text(t("Karte hinzufügen","Add card"))},icon={Symbol(R.drawable.ic_add)},onClick={editorId=-1})}
            ) { padding ->
                Column(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).imePadding()) {
                    if(tab==2) SettingsScreen()
                    else if(selected==null) EmptyScreen()
                    else {
                        Row(Modifier.fillMaxWidth().padding(horizontal=16.dp,vertical=8.dp),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                            ChoiceField(t("Stapel","Deck"),decks.map { it.name },decks.indexOf(selected),{index->deck=decks[index].id;startSession()},Modifier.weight(1f),compact=wide)
                            if(wide && tab==0) ChoiceField(t("Lernmodus","Study mode"),modes(),mode,{mode=it;startSession()},Modifier.weight(1f),compact=true)
                        }
                        if(tab==0) LearnScreen(wide) else CardsScreen()
                    }
                }
            }
        }
        if(newDeck) NewDeckDialog()
        if(editorId>=-1) EditCardDialog(editorId)
        confirmation?.let { info -> AlertDialog(
            onDismissRequest={confirmation=null},
            title={Text(info.title)},text={Text(info.body)},
            confirmButton={TextButton(onClick={confirmation=null;info.action()}) { Text(t("Bestätigen","Confirm")) }},
            dismissButton={TextButton(onClick={confirmation=null}) { Text(t("Abbrechen","Cancel")) }}
        ) }
    }
    @Composable private fun ChoiceField(label: String, options: List<String>, selected: Int, choose: (Int)->Unit, modifier: Modifier=Modifier, compact: Boolean=false) {
        var expanded by remember { mutableStateOf(false) }
        if(compact) {
            Box(modifier) {
                AssistChip(onClick={expanded=true},label={Text(options.getOrElse(selected){""},maxLines=1,overflow=TextOverflow.Ellipsis)},trailingIcon={Symbol(R.drawable.ic_expand)},modifier=Modifier.fillMaxWidth())
                DropdownMenu(expanded=expanded,onDismissRequest={expanded=false}) {
                    options.forEachIndexed { i,option -> DropdownMenuItem(text={Text(option)},onClick={expanded=false;if(i!=selected)choose(i)}) }
                }
            }
            return
        }
        ExposedDropdownMenuBox(expanded=expanded,onExpandedChange={expanded=it},modifier=modifier) {
            OutlinedTextField(value=options.getOrElse(selected){""},onValueChange={},readOnly=true,
                label={Text(label)},trailingIcon={ExposedDropdownMenuDefaults.TrailingIcon(expanded)},singleLine=true,
                modifier=Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable))
            ExposedDropdownMenu(expanded=expanded,onDismissRequest={expanded=false}) {
                options.forEachIndexed { i,option -> DropdownMenuItem(text={Text(option)},onClick={expanded=false;if(i!=selected) choose(i)}) }
            }
        }
    }
    @Composable private fun EmptyScreen() {
        Column(Modifier.fillMaxSize().padding(32.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.Center,horizontalAlignment=Alignment.CenterHorizontally) {
            Surface(shape=MaterialTheme.shapes.extraLarge,color=MaterialTheme.colorScheme.primaryContainer,modifier=Modifier.size(80.dp)) {
                Box(contentAlignment=Alignment.Center) { Icon(painterResource(R.drawable.ic_cards),null,Modifier.size(40.dp),tint=MaterialTheme.colorScheme.onPrimaryContainer) }
            }
            Spacer(Modifier.height(24.dp))
            Text(t("Dein Wissen. Deine Karten.","Your knowledge. Your cards."),style=MaterialTheme.typography.headlineMedium,textAlign=TextAlign.Center)
            Spacer(Modifier.height(12.dp))
            Text(t("Erstelle deinen ersten Stapel und lerne in deinem Tempo. Alles bleibt auf deinem Gerät.","Create your first deck and study at your own pace. Everything stays on your device."),style=MaterialTheme.typography.bodyLarge,textAlign=TextAlign.Center,color=MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(24.dp))
            Button(onClick={newDeck=true}) { Symbol(R.drawable.ic_add); Spacer(Modifier.width(8.dp));Text(t("Ersten Stapel anlegen","Create first deck")) }
        }
    }
    @Composable private fun ColumnScope.LearnScreen(wide: Boolean) {
        val card=remember(revision,currentId) { if(currentId>=0) store.card(currentId) else null }
        val totalCards=remember(revision,deck) { store.cards(deck).size }
        val remaining=if(card==null) 0 else queue.size+1
        Column(Modifier.weight(1f).fillMaxWidth().padding(horizontal=16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            if(!wide) ChoiceField(t("Lernmodus","Study mode"),modes(),mode,{mode=it;startSession()},Modifier.fillMaxWidth())

            if(card!=null) {
                if(!wide) Text(t("Noch $remaining Karte(n)","$remaining card(s) left"),style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.onSurfaceVariant)
                if(!wide) LinearProgressIndicator(progress={if(sessionTotal<=0) 0f else (1f-remaining.toFloat()/sessionTotal).coerceIn(0f,1f)},modifier=Modifier.fillMaxWidth())
                Card(modifier=Modifier.weight(1f).fillMaxWidth(),shape=MaterialTheme.shapes.extraLarge,colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surfaceContainerLow)) {
                    Box(Modifier.fillMaxSize().padding(if(wide) 16.dp else 24.dp),contentAlignment=Alignment.Center) {
                        SelectionContainer {
                            if(wide && revealed) {
                                Row(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(24.dp)) {
                                    CardFace(card.front,t("Vorderseite","Front"),Modifier.weight(1f),compact=true)
                                    CardFace(card.back,t("Rückseite","Back"),Modifier.weight(1f),compact=true)
                                }
                            } else {
                                Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).animateContentSize(),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(24.dp)) {
                                    CardFace(card.front,t("Vorderseite","Front"))
                                    if(revealed) { HorizontalDivider();CardFace(card.back,t("Rückseite","Back")) }
                                }
                            }
                        }
                    }
                }
                if(!revealed) Button(onClick={revealed=true},modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)) { Text(t("Antwort zeigen","Show answer")) }
                else {
                    val columns=if(wide) 4 else 2
                    Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
                        repeat(4/columns) { line ->
                            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                                repeat(columns) { cell ->
                                    val q=line*columns+cell
                                    FilledTonalButton(onClick={rate(q)},modifier=Modifier.weight(1f).heightIn(min=56.dp),colors=ButtonDefaults.filledTonalButtonColors()) {
                                        Column(horizontalAlignment=Alignment.CenterHorizontally) {
                                            Text(rating(q))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center) {
                    Icon(painterResource(if(totalCards==0)R.drawable.ic_cards else R.drawable.ic_learn),null,Modifier.size(48.dp),tint=MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(20.dp))
                    Text(when { totalCards==0 -> t("Dieser Stapel hat noch keine Karten.","This deck has no cards yet.");mode==0 -> t("Lernrunde abgeschlossen.","Session complete.");else -> t("Keine Karten in dieser Auswahl.","No cards in this selection.") },style=MaterialTheme.typography.headlineSmall,textAlign=TextAlign.Center)
                    Spacer(Modifier.height(20.dp))
                    if(totalCards==0) Button(onClick={editorId=-1}) { Text(t("Karte hinzufügen","Add card")) }
                    else {
                        FilledTonalButton(onClick={startSession()}) {Text(t("Neue Lernrunde","New session"))}
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
        }
    }
    @Composable private fun CardFace(value: String, label: String, modifier: Modifier=Modifier, compact: Boolean=false) {
        Column(modifier,horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(if(compact)8.dp else 12.dp)) {
            Text(label,style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value,style=if(compact)MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineMedium,textAlign=TextAlign.Center)
        }
    }
    @Composable private fun ColumnScope.CardsScreen() {
        val cards=remember(revision,deck,filter,sort) { store.cards(deck,filter,sort) }
        Column(Modifier.weight(1f).fillMaxWidth()) {
            Row(Modifier.padding(horizontal=16.dp,vertical=8.dp),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                ChoiceField(t("Anzeigen","Show"),filters(),filter,{filter=it},Modifier.weight(1f))
                ChoiceField(t("Sortieren","Sort"),sorts(),sort,{sort=it},Modifier.weight(1f))
            }
            Text(t("${cards.size} Karte(n)","${cards.size} card(s)"),Modifier.padding(horizontal=20.dp,vertical=8.dp),style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.onSurfaceVariant)
            if(cards.isEmpty()) Box(Modifier.weight(1f).fillMaxWidth().padding(24.dp),contentAlignment=Alignment.Center) {
                Text(t("Keine Karten in dieser Auswahl.","No cards in this selection."),style=MaterialTheme.typography.bodyLarge,textAlign=TextAlign.Center)
            }
            else LazyColumn(Modifier.weight(1f),contentPadding=PaddingValues(start=16.dp,end=16.dp,top=8.dp,bottom=100.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                items(cards,key={it.id}) { card ->
                    Card(onClick={editorId=card.id},modifier=Modifier.fillMaxWidth(),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surfaceContainerLow)) {
                        Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                            Text(card.front,style=MaterialTheme.typography.titleMedium)
                            Text(card.back,style=MaterialTheme.typography.bodyMedium,maxLines=3,overflow=TextOverflow.Ellipsis,color=MaterialTheme.colorScheme.onSurfaceVariant)
                            Text((card.rating?.let { rating(it) } ?: t("Unbewertet","Unrated")),style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
    @Composable private fun NewDeckDialog() {
        var name by rememberSaveable { mutableStateOf("") }
        var error by rememberSaveable { mutableStateOf<String?>(null) }
        AlertDialog(onDismissRequest={newDeck=false},title={Text(t("Neuer Stapel","New deck"))},text={
            OutlinedTextField(name,{name=it;error=null},label={Text(t("Name des Stapels","Deck name"))},singleLine=true,isError=error!=null,supportingText={error?.let {Text(it)}},modifier=Modifier.fillMaxWidth())
        },confirmButton={TextButton(onClick={
            if(name.isBlank()) error=t("Bitte einen Namen eingeben","Please enter a name")
            else try { deck=store.addDeck(name.trim());newDeck=false;startSession();revision++ } catch(e: SQLiteConstraintException) {error=t("Dieser Stapel existiert bereits","This deck already exists")}
        }) {Text(t("Anlegen","Create"))}},dismissButton={TextButton(onClick={newDeck=false}) {Text(t("Abbrechen","Cancel"))}})
    }
    @Composable private fun EditCardDialog(id: Long) {
        val card=remember(id) {if(id>=0)store.card(id) else null}
        var front by rememberSaveable(id) {mutableStateOf(card?.front ?: "")}
        var back by rememberSaveable(id) {mutableStateOf(card?.back ?: "")}
        var selectedRating by rememberSaveable(id) {mutableIntStateOf((card?.rating ?: -1)+1)}
        var invalid by rememberSaveable(id) {mutableStateOf(false)}
        AlertDialog(onDismissRequest={editorId=-2},title={Text(t(if(id<0)"Neue Karte" else "Karte bearbeiten",if(id<0)"New card" else "Edit card"))},text={
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(front,{front=it},label={Text(t("Vorderseite","Front"))},isError=invalid && front.isBlank(),supportingText={if(invalid && front.isBlank())Text(t("Pflichtfeld","Required"))},modifier=Modifier.fillMaxWidth())
                OutlinedTextField(back,{back=it},label={Text(t("Rückseite","Back"))},minLines=3,isError=invalid && back.isBlank(),supportingText={if(invalid && back.isBlank())Text(t("Pflichtfeld","Required"))},modifier=Modifier.fillMaxWidth())
                if(id>=0) {
                    ChoiceField(t("Bewertung","Rating"),listOf(t("Unbewertet","Unrated"))+(0..3).map {rating(it)},selectedRating,{selectedRating=it})
                    TextButton(onClick={confirm(t("Karte löschen","Delete card"),t("Diese Karte dauerhaft löschen?","Permanently delete this card?")) {store.deleteCard(id);editorId=-2;startSession();revision++}},colors=ButtonDefaults.textButtonColors(contentColor=MaterialTheme.colorScheme.error)) {
                        Symbol(R.drawable.ic_delete);Spacer(Modifier.width(8.dp));Text(t("Karte löschen","Delete card"))
                    }
                }
            }
        },confirmButton={TextButton(onClick={
            invalid=front.isBlank() || back.isBlank()
            if(!invalid) {store.saveCard(id,deck,front.trim(),back.trim(),if(selectedRating==0)null else selectedRating-1);editorId=-2;startSession();revision++}
        }) {Text(t("Speichern","Save"))}},dismissButton={TextButton(onClick={editorId=-2}) {Text(t("Abbrechen","Cancel"))}})
    }
    @Composable private fun SettingsScreen() {
        var selectedLanguage by rememberSaveable {mutableStateOf(english)}
        var selectedAppearance by rememberSaveable {mutableIntStateOf(appearance)}
        var dynamic by rememberSaveable {mutableStateOf(prefs.getBoolean("dynamic",true))}
        val keys=listOf("window","base","text","button","accent")
        var custom by rememberSaveable {mutableStateOf(keys.any {prefs.contains(it)})}
        val scheme=MaterialTheme.colorScheme
        val colors=listOf(scheme.surface,scheme.surfaceContainerLow,scheme.onSurface,scheme.secondaryContainer,scheme.primary)
        val drafts=keys.mapIndexed { i,key -> rememberSaveable(key) {mutableStateOf(prefs.getString(key,null) ?: String.format("#%06X",colors[i].toArgb() and 0xffffff))} }
        var invalid by rememberSaveable {mutableStateOf(false)}
        val labels=listOf(t("Hintergrund","Background"),t("Karten und Listen","Cards and lists"),t("Text","Text"),t("Buttons","Buttons"),t("Akzent","Accent"))
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(20.dp)) {
            Text(t("Sprache","Language"),style=MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                FilterChip(selected=!selectedLanguage,onClick={selectedLanguage=false},label={Text("Deutsch")})
                FilterChip(selected=selectedLanguage,onClick={selectedLanguage=true},label={Text("English")})
            }
            HorizontalDivider()
            Text(t("Erscheinungsbild","Appearance"),style=MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                listOf(t("System","System"),t("Hell","Light"),t("Dunkel","Dark")).forEachIndexed { i,label ->
                    FilterChip(selected=selectedAppearance==i,onClick={selectedAppearance=i},label={Text(label)})
                }
            }
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(t("Farben vom Hintergrundbild","Wallpaper colors"),style=MaterialTheme.typography.bodyLarge)
                    Text(t("Material You · ab Android 12","Material You · Android 12 and newer"),style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked=dynamic && !custom,onCheckedChange={dynamic=it;if(it)custom=false},enabled=Build.VERSION.SDK_INT>=31)
            }
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                Text(t("Eigene Farben","Custom colors"),Modifier.weight(1f),style=MaterialTheme.typography.bodyLarge)
                Switch(checked=custom,onCheckedChange={custom=it})
            }
            if(custom) drafts.forEachIndexed { i,value ->
                OutlinedTextField(value.value,{value.value=it},label={Text(labels[i])},singleLine=true,isError=invalid && !value.value.matches(Regex("#[0-9a-fA-F]{6}")),supportingText={if(invalid && !value.value.matches(Regex("#[0-9a-fA-F]{6}")))Text(t("Beispiel: #6750A4","Example: #6750A4"))},modifier=Modifier.fillMaxWidth())
            }
            Button(onClick={
                invalid=custom && drafts.any {!it.value.matches(Regex("#[0-9a-fA-F]{6}"))}
                if(!invalid) {
                    prefs.edit().apply {
                        putBoolean("english",selectedLanguage);putInt("appearance",selectedAppearance);putBoolean("dynamic",dynamic)
                        keys.forEachIndexed { i,key -> if(custom)putString(key,drafts[i].value) else remove(key) }
                    }.apply()
                    english=selectedLanguage;appearance=selectedAppearance;revision++;feedback=t("Einstellungen gespeichert","Settings saved")
                }
            },modifier=Modifier.fillMaxWidth()) {Text(t("Speichern","Save"))}
            TextButton(onClick={
                prefs.edit().apply {keys.forEach {remove(it)};putBoolean("dynamic",true);putInt("appearance",0)}.apply()
                dynamic=true;custom=false;selectedAppearance=0;appearance=0;revision++;feedback=t("Systemdesign wiederhergestellt","System theme restored")
            },modifier=Modifier.fillMaxWidth()) {Text(t("Auf Systemdesign zurücksetzen","Reset to system theme"))}
        }
    }
}
