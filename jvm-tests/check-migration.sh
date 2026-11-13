#!/usr/bin/env bash
# Room migration check on a real SQLite (needs the `sqlite3` program).
#
# A migration is right when a database of the OLD version with data in it, after the step, has exactly the
# table layout that a NEW database has: that is what Room compares when the app opens the file. The CREATE
# TABLE statements below are what Room generates for the entities. After a Gradle build, compare them with
# "createSql" in app/schemas/com.clipsync.app.data.AppDatabase/<version>.json - this check is only as good as
# that copy.
#
# Covers MIGRATION_1_2. A new step (2 -> 3, ...) adds its own old/new statements and its own run below.
set -euo pipefail

HERE="$(cd "$(dirname "$0")" && pwd)"
KT="$HERE/../app/src/main/kotlin/com/clipsync/app/data/AppDatabase.kt"
command -v sqlite3 >/dev/null || { echo "sqlite3 is needed"; exit 2; }
WORK="$(mktemp -d)"; trap 'rm -rf "$WORK"' EXIT
fail() { echo "FAIL: $*"; exit 1; }

V1_ENTRIES='CREATE TABLE IF NOT EXISTS `entries` (`id` TEXT NOT NULL, `entryType` TEXT NOT NULL, `encryptedContent` BLOB NOT NULL, `createdAt` INTEGER NOT NULL, `pinned` INTEGER NOT NULL, `direction` TEXT NOT NULL, PRIMARY KEY(`id`))'
V2_ENTRIES='CREATE TABLE IF NOT EXISTS `entries` (`id` TEXT NOT NULL, `entryType` TEXT NOT NULL, `encryptedContent` BLOB NOT NULL, `createdAt` INTEGER NOT NULL, `pinned` INTEGER NOT NULL, `direction` TEXT NOT NULL, `delivery` INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(`id`))'
PAIRED='CREATE TABLE IF NOT EXISTS `paired_pc` (`deviceId` TEXT NOT NULL, `name` TEXT NOT NULL, `publicKey` BLOB NOT NULL, `address` TEXT NOT NULL, `certHash` BLOB NOT NULL, PRIMARY KEY(`deviceId`))'

# The SQL of the step comes from the source itself, so that what is checked is what ships.
STEP="$(sed -n '/MIGRATION_1_2: Migration/,/^        }/p' "$KT" | grep -o 'execSQL("[^"]*")' | sed 's/execSQL("//; s/")$//')"
[ -n "$STEP" ] || fail "no SQL found in MIGRATION_1_2 ($KT)"

# A version 1 database with data in it.
sqlite3 "$WORK/old.db" "$V1_ENTRIES; $PAIRED;
INSERT INTO entries VALUES ('a','text',x'0102',1700000000000,1,'incoming'),('b','image',x'ff00ff',1700000001000,0,'local');
INSERT INTO paired_pc VALUES ('pc1','pc',x'aa','10.0.0.5:47800',x'bb');"
sqlite3 "$WORK/old.db" "$STEP"
sqlite3 "$WORK/new.db" "$V2_ENTRIES; $PAIRED;"

for table in entries paired_pc; do
  diff <(sqlite3 "$WORK/old.db" "PRAGMA table_info($table)") <(sqlite3 "$WORK/new.db" "PRAGMA table_info($table)") >/dev/null \
    || fail "table $table differs from a fresh version 2 database after the migration"
done
[ "$(sqlite3 "$WORK/old.db" "SELECT count(*) FROM entries")" = 2 ] || fail "rows were lost"
[ "$(sqlite3 "$WORK/old.db" "SELECT count(*) FROM entries WHERE delivery = 0")" = 2 ] || fail "existing rows must get delivery = 0"
[ "$(sqlite3 "$WORK/old.db" "SELECT hex(encryptedContent) FROM entries WHERE id = 'b'")" = FF00FF ] || fail "a BLOB changed"
[ "$(sqlite3 "$WORK/old.db" "SELECT address FROM paired_pc")" = "10.0.0.5:47800" ] || fail "the paired PC changed"
sqlite3 "$WORK/old.db" "INSERT INTO entries(id,entryType,encryptedContent,createdAt,pinned,direction) VALUES ('c','text',x'00',1,0,'local')" \
  || fail "an insert without the new column no longer works"
[ "$(sqlite3 "$WORK/old.db" "PRAGMA integrity_check")" = ok ] || fail "integrity_check"
echo "OK: migration 1 -> 2 gives the layout of a fresh version 2 database, rows and BLOBs intact"
