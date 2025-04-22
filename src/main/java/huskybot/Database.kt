package huskybot

import com.zaxxer.hikari.HikariDataSource
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.SQLException
import java.time.Instant

object Database {
    private val pool = HikariDataSource()
    var calls = 0L
        private set

    val connection: Connection
        get() {
            calls++
            return pool.connection
        }

    init {
        if (!pool.isRunning) {
            pool.jdbcUrl = "jdbc:sqlite:huskybot.db"
        }

        setupTables()
    }

    /**
     * Private function that sets up the sql tables
     */
    private fun setupTables() = runSuppressed {
        connection.use {
            it.createStatement().apply {
                // Guild Settings
                addBatch("CREATE TABLE IF NOT EXISTS prefixes (id INTEGER PRIMARY KEY, prefix TEXT NOT NULL)")
                addBatch("CREATE TABLE IF NOT EXISTS guildsettings (guildid INTEGER PRIMARY KEY, admin_id INTEGER, mod_id INTEGER, modmail_category INTEGER, modmail_log INTEGER)")
            }.executeBatch()
        }
    }

    /* Server specififc admin-role */
    fun getAdminRole(guildId: Long) = getFromDatabase("guildsettings", guildId, "admin_id")?.toLong()

    fun setAdminRole(guildId: Long, roleId: Long?) = runSuppressed {
        connection.use {
            if (roleId == null) {
                buildStatement(it, "UPDATE guildsettings SET admin_id = null WHERE guildid = ?", guildId).executeUpdate()
                return@runSuppressed
            }

            buildStatement(
                it, "INSERT INTO guildsettings(guildid, admin_id) VALUES (?, ?) ON CONFLICT(guildid) DO UPDATE SET admin_id = ?",
                guildId, roleId, roleId
            ).executeUpdate()
        }
    }

    /* Server specififc mod-role */
    fun getModRole(guildId: Long) = getFromDatabase("guildsettings", guildId, "mod_id")?.toLong()

    fun setModRole(guildId: Long, roleId: Long?) = runSuppressed {
        connection.use {
            if (roleId == null) {
                buildStatement(it, "UPDATE guildsettings SET mod_id = null WHERE guildid = ?", guildId).executeUpdate()
                return@runSuppressed
            }

            buildStatement(
                it, "INSERT INTO guildsettings(guildid, mod_id) VALUES (?, ?) ON CONFLICT(guildid) DO UPDATE SET mod_id = ?",
                guildId, roleId, roleId
            ).executeUpdate()
        }
    }

    /* User Data */

    fun removeUserData(guildId: Long, userId: Long) = runSuppressed {
        connection.use {
            buildStatement(
                it, "DELETE FROM userlevel WHERE guildid = ?, userid = ?",
                guildId, userId
            ).executeUpdate()
        }
    }

    /*
     * +=================================================+
     * |                IGNORE BELOW THIS                |
     * +=================================================+
     */
    private fun getFromDatabase(table: String, id: Long, columnId: String): String? =
        suppressedWithConnection({ null }) {
            val idColumn = if (table.contains("userinfo") || table.contains("confirmation"))
                "id" else "guildid" // I'm an actual idiot I stg

            val results = buildStatement(it, "SELECT * FROM $table WHERE $idColumn = ?", id)
                .executeQuery()

            if (results.next()) results.getString(columnId) else null
        }

    private fun getSpecificFromDatabase(table: String, id: Long, columnId: String): String? =
        suppressedWithConnection({ null }) {
            val idColumn = if (table.contains("userinfo")) "id" else "guildid"

            val results = buildStatement(it, "SELECT $columnId FROM $table WHERE $idColumn = ?", id)
                .executeQuery()

            if (results.next()) results.getString(columnId) else null
        }

    private fun getValueFromDatabase(table: String, id1: Long, id2: Long, columnId: String): String? =
        suppressedWithConnection({ null }) {
            val results = buildStatement(it, "SELECT $columnId FROM $table WHERE guildid = ? AND userid = ?", id1, id2)
                .executeQuery()

            if (results.next()) results.getString(columnId) else null
        }

    private fun setEnabled(table: String, id: Long, enable: Boolean) = runSuppressed {
        connection.use {
            val stmt = if (!enable) {
                buildStatement(it, "DELETE FROM $table WHERE id = ?", id)
            } else {
                buildStatement(it, "INSERT OR IGNORE INTO $table (id) VALUES (?)", id)
            }

            stmt.execute()
        }
    }

    fun buildStatement(con: Connection, sql: String, vararg obj: Any): PreparedStatement {
        val statement = con.prepareStatement(sql)

        for ((i, o) in obj.withIndex()) {
            when (o) {
                is String -> statement.setString(i + 1, o)
                is Int -> statement.setInt(i + 1, o)
                is Long -> statement.setLong(i + 1, o)
                is Double -> statement.setDouble(i + 1, o)
            }
        }

        return statement
    }

    fun runSuppressed(block: () -> Unit) = runCatching(block).onFailure{e -> e.stackTrace}

    fun <T> suppressedWithConnection(default: () -> T, block: (Connection) -> T) = try {
        connection.use {
            block(it)
        }
    } catch (e: SQLException) {
        default()
    }
}