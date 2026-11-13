package com.clipsync.app.data

import android.content.Context
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "entries")
data class ClipEntryRow(
    @PrimaryKey val id: String,
    val entryType: String, // "text" | "image"
    val encryptedContent: ByteArray,
    val createdAt: Long,
    val pinned: Boolean,
    val direction: String, // "incoming" (from PC) | "outgoing" (sent to PC) | "local" (copied on this phone, not sent yet)
    /**
     * 0 = nothing waits for the PC (an entry that came from it, an old entry, a manual send);
     * 1 = copied while the PC could not be reached and still waits to be delivered.
     * Added by [AppDatabase.MIGRATION_1_2] for the queue phone -> PC (ROADMAP 1.1). Nothing reads or
     * writes it yet: the schema change is shipped alone, so that it is proven on a real phone before
     * anything depends on it.
     */
    @ColumnInfo(defaultValue = "0") val delivery: Int = 0,
)

/**
 * What the history list needs about an entry, *without* its content.
 *
 * Android reads a SQLite query result through a CursorWindow of about 2 MB: one row whose
 * BLOB does not fit makes the whole query throw SQLiteBlobTooBigException. A screenshot
 * easily is bigger than that, so the old `SELECT *` for the list broke as soon as one large
 * image had been stored - taking the entire history screen down with it. The list therefore
 * never touches `encryptedContent`; content is read per entry, in slices, only when needed.
 */
data class EntryMeta(
    val id: String,
    val entryType: String, // "text" | "image"
    val createdAt: Long,
    val pinned: Boolean,
    val direction: String, // "incoming" (from PC) | "outgoing" (sent to PC) | "local" (copied on this phone, not sent yet)
    val size: Int, // size of the encrypted content in bytes
)

@Dao
interface ClipDao {
    @Query("SELECT id, entryType, createdAt, pinned, direction, length(encryptedContent) AS size FROM entries ORDER BY pinned DESC, createdAt DESC LIMIT :limit")
    fun observeMeta(limit: Int = 300): Flow<List<EntryMeta>>

    @Query("SELECT id, entryType, createdAt, pinned, direction, length(encryptedContent) AS size FROM entries WHERE id = :id")
    fun observeMetaById(id: String): Flow<EntryMeta?>

    @Query("SELECT id, entryType, createdAt, pinned, direction, length(encryptedContent) AS size FROM entries WHERE id = :id")
    suspend fun getMeta(id: String): EntryMeta?

    /** Together with [contentSlice]: reads a BLOB bigger than the CursorWindow in pieces. */
    @Query("SELECT length(encryptedContent) FROM entries WHERE id = :id")
    suspend fun contentLength(id: String): Int?

    /** `start` is 1-based, as in SQL's substr(); for a BLOB the unit is bytes. */
    @Query("SELECT substr(encryptedContent, :start, :count) FROM entries WHERE id = :id")
    suspend fun contentSlice(id: String, start: Int, count: Int): ByteArray?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: ClipEntryRow)

    @Query("UPDATE entries SET pinned = :pinned WHERE id = :id")
    suspend fun setPinned(id: String, pinned: Boolean)

    @Query("UPDATE entries SET createdAt = :createdAt WHERE id = :id")
    suspend fun touch(id: String, createdAt: Long)

    @Query("UPDATE entries SET direction = :direction WHERE id = :id")
    suspend fun setDirection(id: String, direction: String)

    @Query("DELETE FROM entries WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM entries WHERE pinned = 0")
    suspend fun clearUnpinned()

    /** Keeps the newest [keep] unpinned entries. Images make the table grow fast; nothing used to trim it. */
    @Query("DELETE FROM entries WHERE pinned = 0 AND id NOT IN (SELECT id FROM entries WHERE pinned = 0 ORDER BY createdAt DESC LIMIT :keep)")
    suspend fun pruneUnpinned(keep: Int)
}

/** v0.2 pairs with exactly one PC - this table only ever holds one row. */
@Entity(tableName = "paired_pc")
data class PairedPc(
    @PrimaryKey val deviceId: String,
    val name: String,
    val publicKey: ByteArray,
    val address: String, // "ip:port"
    val certHash: ByteArray, // raw SHA-256 of the PC's self-signed TLS cert, pinned on every connection
)

@Dao
interface PairedPcDao {
    @Query("SELECT * FROM paired_pc LIMIT 1")
    fun observePc(): Flow<PairedPc?>

    @Query("SELECT * FROM paired_pc LIMIT 1")
    suspend fun getPc(): PairedPc?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(pc: PairedPc)

    @Query("DELETE FROM paired_pc")
    suspend fun clear()
}

@Database(entities = [ClipEntryRow::class, PairedPc::class], version = 2, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun clipDao(): ClipDao
    abstract fun pairedPcDao(): PairedPcDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        /**
         * Version 1 -> 2: the `delivery` column of `entries`. The same definition as the entity gives
         * (`INTEGER NOT NULL DEFAULT 0`), so that Room finds a migrated database and a new one identical.
         * Rows that exist get 0: nothing waits.
         *
         * A migration is a one-way door: a phone that has opened the database as version 2 cannot go back
         * to an app that expects version 1 (Room refuses; the app's data has to be cleared).
         */
        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `entries` ADD COLUMN `delivery` INTEGER NOT NULL DEFAULT 0")
            }
        }

        /** Every step, in order. A new version adds its step here and nowhere else. */
        val MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_1_2)

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "clipsync.db",
                ).addMigrations(*MIGRATIONS).build().also { instance = it }
            }
    }
}
