package de.moritz.karteikarten

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.time.LocalDate

data class Deck(val id: Long, val name: String)
data class Card(val id: Long, val deckId: Long, val front: String, val back: String,
                val rating: Int?)

/** Legacy scheduling columns remain only for database compatibility; learning ignores them. */
class CardStore(private val context: Context) : AutoCloseable {
    private lateinit var db: SQLiteDatabase
    init { open() }
    private fun open() {
        db = context.openOrCreateDatabase("karten.db", Context.MODE_PRIVATE, null)
        db.setForeignKeyConstraintsEnabled(true)
        db.execSQL("CREATE TABLE IF NOT EXISTS decks(id INTEGER PRIMARY KEY, name TEXT NOT NULL UNIQUE)")
        db.execSQL("CREATE TABLE IF NOT EXISTS cards(id INTEGER PRIMARY KEY, deck_id INTEGER NOT NULL REFERENCES decks(id) ON DELETE CASCADE, front TEXT NOT NULL, back TEXT NOT NULL, interval INTEGER NOT NULL DEFAULT 0, ease REAL NOT NULL DEFAULT 2.5, due TEXT NOT NULL, rating INTEGER)")
    }
    fun decks(): List<Deck> = db.rawQuery("SELECT id,name FROM decks ORDER BY name COLLATE NOCASE", null).use { c ->
        buildList { while(c.moveToNext()) add(Deck(c.getLong(0), c.getString(1))) }
    }
    fun addDeck(name: String): Long = db.insertOrThrow("decks", null, ContentValues().apply { put("name", name) })
    fun deleteDeck(id: Long) { db.delete("decks", "id=?", arrayOf(id.toString())) }
    fun cards(deck: Long, filter: Int = 0, sort: Int = 0): List<Card> {
        val where = listOf("1=1", "rating IN (0,1)", "rating=2", "rating=3")[filter]
        val order = listOf("id", "CASE WHEN rating IS NULL THEN 9 ELSE rating END,id", "CASE WHEN rating IS NULL THEN 9 ELSE 3-rating END,id")[sort]
        return db.rawQuery("SELECT id,deck_id,front,back,rating FROM cards WHERE deck_id=? AND $where ORDER BY $order", arrayOf(deck.toString())).use { c ->
            buildList { while(c.moveToNext()) add(Card(c.getLong(0),c.getLong(1),c.getString(2),c.getString(3),if(c.isNull(4)) null else c.getInt(4))) }
        }
    }
    fun card(id: Long): Card? = db.rawQuery("SELECT id,deck_id,front,back,rating FROM cards WHERE id=?", arrayOf(id.toString())).use { c ->
        if(c.moveToFirst()) Card(c.getLong(0),c.getLong(1),c.getString(2),c.getString(3),if(c.isNull(4)) null else c.getInt(4)) else null
    }
    fun session(deck: Long, mode: Int): List<Long> {
        val where = listOf("1=1", "rating IN (0,1)", "rating=3")[mode]
        val args = arrayOf(deck.toString())
        return db.rawQuery("SELECT id FROM cards WHERE deck_id=? AND $where ORDER BY id", args).use { c ->
            buildList { while(c.moveToNext()) add(c.getLong(0)) }
        }
    }
    fun saveCard(id: Long, deck: Long, front: String, back: String, rating: Int?) {
        val values = ContentValues().apply {
            put("front", front); put("back", back)
            if(rating == null) putNull("rating") else put("rating", rating)
        }
        if(id < 0) {
            values.put("deck_id", deck); values.put("due", "1970-01-01")
            db.insertOrThrow("cards", null, values)
        } else db.update("cards", values, "id=?", arrayOf(id.toString()))
    }
    fun deleteCard(id: Long) { db.delete("cards", "id=?", arrayOf(id.toString())) }
    fun rate(card: Card, rating: Int) {
        db.execSQL("UPDATE cards SET rating=? WHERE id=?", arrayOf<Any>(rating,card.id))
    }
    fun exportTo(output: OutputStream) {
        db.close()
        try { context.getDatabasePath("karten.db").inputStream().use { it.copyTo(output) } } finally { open() }
    }
    fun importFrom(input: InputStream) {
        val temp = File(context.cacheDir, "import.db")
        var source: SQLiteDatabase? = null
        try {
            temp.outputStream().use { input.copyTo(it) }
            source = SQLiteDatabase.openDatabase(temp.path, null, SQLiteDatabase.OPEN_READONLY)
            source.rawQuery("PRAGMA integrity_check", null).use { check ->
                require(check.moveToFirst() && check.getString(0)=="ok") { "SQLite integrity check" }
            }
            val hasRating = source.rawQuery("PRAGMA table_info(cards)", null).use { c ->
                var found = false
                while(c.moveToNext()) if(c.getString(1)=="rating") found=true
                found
            }
            db.beginTransaction()
            try {
                db.delete("cards",null,null); db.delete("decks",null,null)
                source.rawQuery("SELECT id,name FROM decks",null).use { c ->
                    while(c.moveToNext()) {
                        require(!c.isNull(1) && c.getString(1).isNotBlank()) { "Invalid deck" }
                        db.insertOrThrow("decks",null,ContentValues().apply { put("id",c.getLong(0)); put("name",c.getString(1)) })
                    }
                }
                source.rawQuery("SELECT id,deck_id,front,back,interval,ease,due,${if(hasRating) "rating" else "NULL"} FROM cards",null).use { c ->
                    while(c.moveToNext()) {
                        require(!c.isNull(2) && !c.isNull(3) && !c.isNull(5) && c.getInt(4)>=0 && c.getDouble(5).isFinite() && c.getDouble(5)>=1.3) { "Invalid card" }
                        LocalDate.parse(c.getString(6))
                        require(c.isNull(7) || c.getInt(7) in 0..3) { "Invalid rating" }
                        db.insertOrThrow("cards",null,ContentValues().apply {
                            put("id",c.getLong(0)); put("deck_id",c.getLong(1)); put("front",c.getString(2)); put("back",c.getString(3))
                            put("interval",c.getInt(4)); put("ease",c.getDouble(5)); put("due",c.getString(6))
                            if(c.isNull(7)) putNull("rating") else put("rating",c.getInt(7))
                        })
                    }
                }
                db.setTransactionSuccessful()
            } finally { db.endTransaction() }
        } finally { source?.close(); temp.delete() }
    }
    override fun close() { db.close() }
}
