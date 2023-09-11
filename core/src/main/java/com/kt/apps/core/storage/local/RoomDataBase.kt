package com.kt.apps.core.storage.local

import android.content.Context
import android.database.AbstractWindowedCursor
import android.database.Cursor
import android.database.MatrixCursor
import android.os.Build
import android.os.CancellationSignal
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteQuery
import androidx.sqlite.db.SupportSQLiteStatement
import com.kt.apps.core.extensions.ExtensionsChannel
import com.kt.apps.core.extensions.ExtensionsConfig
import com.kt.apps.core.extensions.model.TVScheduler
import com.kt.apps.core.logging.Logger
import com.kt.apps.core.storage.local.converters.RoomDBTypeConverters
import com.kt.apps.core.storage.local.dao.*
import com.kt.apps.core.storage.local.databaseviews.ExtensionChannelDatabaseViews
import com.kt.apps.core.storage.local.databaseviews.ExtensionsChannelDBWithCategoryViews
import com.kt.apps.core.storage.local.databaseviews.ExtensionsChannelFts4
import com.kt.apps.core.storage.local.dto.*
import com.kt.apps.core.utils.TAG
import com.kt.apps.core.utils.removeAllSpecialChars
import com.kt.apps.core.utils.replaceVNCharsToLatinChars
import java.util.Arrays

@TypeConverters(
    RoomDBTypeConverters::class
)
@Database(
    entities = [
        MapChannel::class,
        TVChannelEntity::class,
        FootballMatchEntity::class,
        FootballTeamEntity::class,
        ExtensionsConfig::class,
        TVChannelDTO::class,
        TVChannelDTO.TVChannelUrl::class,
        ExtensionsChannel::class,
        ExtensionChannelCategory::class,
        TVScheduler.Programme::class,
        TVScheduler::class,
        ExtensionsChannelFts4::class,
        HistoryMediaItemDTO::class,
        TVChannelFts4::class,
        VideoFavoriteDTO::class
    ],
    version = 13,
    exportSchema = true,
    views = [
        ExtensionChannelDatabaseViews::class,
        ExtensionsChannelDBWithCategoryViews::class
    ]
)
abstract class RoomDataBase : RoomDatabase() {
    abstract fun mapChannelDao(): MapChannelDao
    abstract fun tvChannelRecommendationDao(): TVChannelRecommendationDAO
    abstract fun footballTeamDao(): FootballTeamDAO
    abstract fun footballMatchDao(): FootballMatchDAO
    abstract fun extensionsConfig(): ExtensionsConfigDAO
    abstract fun tvChannelDao(): TVChannelListDAO
    abstract fun tvChannelUrlDao(): TVChannelUrlDAO
    abstract fun extensionsChannelDao(): ExtensionsChannelDAO
    abstract fun extensionsChannelCategoryDao(): ExtensionsChannelCategoryDao
    abstract fun extensionsTVChannelProgramDao(): TVProgramScheduleDao
    abstract fun tvSchedulerDao(): TVSchedulerDAO
    abstract fun historyItemDao(): HistoryMediaDAO
    abstract fun videoFavoriteDao(): VideoFavoriteDAO

    companion object {
        private val MIGRATE_1_2 by lazy {
            object : Migration(1, 2) {
                override fun migrate(database: SupportSQLiteDatabase) {
                    database.execSQL("CREATE TABLE IF NOT EXISTS `TVChannelEntity` (`tvGroup` TEXT NOT NULL, `logoChannel` TEXT NOT NULL, `tvChannelName` TEXT NOT NULL, `tvChannelWebDetailPage` TEXT NOT NULL, `sourceFrom` TEXT NOT NULL, `channelId` TEXT NOT NULL, `channelPreviewProviderId` INTEGER NOT NULL, PRIMARY KEY(`channelId`))")
                    database.execSQL("CREATE TABLE IF NOT EXISTS `FootballMatchEntity` (`homeTeam` TEXT NOT NULL, `awayTeam` TEXT NOT NULL, `kickOffTime` TEXT NOT NULL, `kickOffTimeInSecond` INTEGER NOT NULL, `statusStream` TEXT NOT NULL, `detailPage` TEXT NOT NULL, `sourceFrom` TEXT NOT NULL, `league` TEXT NOT NULL, `matchId` TEXT NOT NULL, PRIMARY KEY(`matchId`))")
                    database.execSQL("CREATE TABLE IF NOT EXISTS `FootballTeamEntity` (`name` TEXT NOT NULL, `id` TEXT NOT NULL, `league` TEXT NOT NULL, `logo` TEXT NOT NULL, PRIMARY KEY(`id`))")
                    database.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
                }
            }
        }
        private val MIGRATE_2_3 by lazy {
            object : Migration(2, 3) {
                override fun migrate(database: SupportSQLiteDatabase) {
                    database.execSQL("CREATE TABLE IF NOT EXISTS `ExtensionsConfig` (`sourceName` TEXT NOT NULL, `sourceUrl` TEXT NOT NULL, `type` TEXT NOT NULL, PRIMARY KEY(`sourceUrl`))")
                }
            }
        }
        private val MIGRATE_3_4 by lazy {
            object : Migration(3, 4) {
                override fun migrate(database: SupportSQLiteDatabase) {
                    database.execSQL("CREATE TABLE IF NOT EXISTS `TVChannelDTO` (`tvGroup` TEXT NOT NULL, `logoChannel` TEXT NOT NULL, `tvChannelName` TEXT NOT NULL, `sourceFrom` TEXT NOT NULL, `channelId` TEXT NOT NULL, PRIMARY KEY(`channelId`))")
                    database.execSQL("CREATE TABLE IF NOT EXISTS `TVChannelUrl` (`src` TEXT, `type` TEXT NOT NULL, `url` TEXT NOT NULL, `tvChannelId` TEXT NOT NULL, PRIMARY KEY(`tvChannelId`, `url`))")
                }
            }
        }
        private val MIGRATE_4_5 by lazy {
            object : Migration(4, 5) {
                override fun migrate(database: SupportSQLiteDatabase) {
                    database.execSQL("CREATE TABLE IF NOT EXISTS `ExtensionsChannel` (`tvGroup` TEXT NOT NULL, `logoChannel` TEXT NOT NULL, `tvChannelName` TEXT NOT NULL, `tvStreamLink` TEXT NOT NULL, `sourceFrom` TEXT NOT NULL, `channelId` TEXT NOT NULL, `channelPreviewProviderId` INTEGER NOT NULL, `isHls` INTEGER NOT NULL, `catchupSource` TEXT NOT NULL, `userAgent` TEXT NOT NULL, `referer` TEXT NOT NULL, `props` TEXT NOT NULL, `extensionSourceId` TEXT NOT NULL, PRIMARY KEY(`channelId`))")
                }
            }
        }

        private val MIGRATE_5_6 by lazy {
            object : Migration(5, 6) {
                override fun migrate(database: SupportSQLiteDatabase) {
                    database.execSQL("CREATE TABLE IF NOT EXISTS `ExtensionsChannel_New` (`tvGroup` TEXT NOT NULL, `logoChannel` TEXT NOT NULL, `tvChannelName` TEXT NOT NULL, `tvStreamLink` TEXT NOT NULL, `sourceFrom` TEXT NOT NULL, `channelId` TEXT NOT NULL, `channelPreviewProviderId` INTEGER NOT NULL, `isHls` INTEGER NOT NULL, `catchupSource` TEXT NOT NULL, `userAgent` TEXT NOT NULL, `referer` TEXT NOT NULL, `props` TEXT NOT NULL, `extensionSourceId` TEXT NOT NULL, PRIMARY KEY(`channelId`, `tvStreamLink`))")
                    database.execSQL("INSERT INTO `ExtensionsChannel_New` (`tvGroup`, `logoChannel`, `tvChannelName`, `tvStreamLink`, `sourceFrom`, `channelId`, `channelPreviewProviderId`, `isHls`, `catchupSource`, `userAgent`, `referer`, `props`, `extensionSourceId`) SELECT `tvGroup`, `logoChannel`, `tvChannelName`, `tvStreamLink`, `sourceFrom`, `channelId`, `channelPreviewProviderId`, `isHls`, `catchupSource`, `userAgent`, `referer`, `props`, `extensionSourceId` FROM ExtensionsChannel")
                    database.execSQL("DROP TABLE ExtensionsChannel")
                    database.execSQL("ALTER TABLE ExtensionsChannel_New RENAME TO ExtensionsChannel")
                }
            }
        }

        private val MIGRATE_6_7 by lazy {
            object : Migration(6, 7) {
                override fun migrate(database: SupportSQLiteDatabase) {
                    database.execSQL("CREATE TABLE IF NOT EXISTS `ExtensionChannelCategory` (`configSourceUrl` TEXT NOT NULL, `name` TEXT NOT NULL, PRIMARY KEY(`configSourceUrl`, `name`))")
                }
            }
        }

        private val MIGRATE_7_8 by lazy {
            object : Migration(7, 8) {
                override fun migrate(database: SupportSQLiteDatabase) {
                    database.execSQL("CREATE TABLE IF NOT EXISTS `Programme` (`channel` TEXT NOT NULL, `channelNumber` TEXT NOT NULL, `start` TEXT NOT NULL, `stop` TEXT NOT NULL, `title` TEXT NOT NULL, `description` TEXT NOT NULL, `extensionsConfigId` TEXT NOT NULL, `extensionEpgUrl` TEXT NOT NULL, PRIMARY KEY(`channel`, `title`, `start`))")
                    database.execSQL("CREATE TABLE IF NOT EXISTS `TVScheduler` (`date` TEXT NOT NULL, `sourceInfoName` TEXT NOT NULL, `generatorInfoName` TEXT NOT NULL, `generatorInfoUrl` TEXT NOT NULL, `extensionsConfigId` TEXT NOT NULL, `epgUrl` TEXT NOT NULL, PRIMARY KEY(`epgUrl`))")

                }
            }
        }

        private val MIGRATE_8_9 by lazy {
            object : Migration(8, 9) {
                override fun migrate(database: SupportSQLiteDatabase) {
                    database.execSQL(
                        "CREATE TABLE IF NOT EXISTS `ExtensionsChannel_New` (`tvGroup` TEXT NOT NULL, " +
                                "`logoChannel` TEXT NOT NULL, " +
                                "`tvChannelName` TEXT NOT NULL, " +
                                "`tvStreamLink` TEXT NOT NULL, " +
                                "`sourceFrom` TEXT NOT NULL, " +
                                "`channelId` TEXT NOT NULL, " +
                                "`channelPreviewProviderId` INTEGER NOT NULL, " +
                                "`isHls` INTEGER NOT NULL, " +
                                "`catchupSource` TEXT NOT NULL, " +
                                "`userAgent` TEXT NOT NULL, " +
                                "`referer` TEXT NOT NULL, " +
                                "`props` TEXT, " +
                                "`extensionSourceId` TEXT NOT NULL, " +
                                "PRIMARY KEY(`channelId`, `tvStreamLink`))"
                    )
                    database.execSQL("INSERT INTO `ExtensionsChannel_New` (`tvGroup`, `logoChannel`, `tvChannelName`, `tvStreamLink`, `sourceFrom`, `channelId`, `channelPreviewProviderId`, `isHls`, `catchupSource`, `userAgent`, `referer`, `props`, `extensionSourceId`) SELECT `tvGroup`, `logoChannel`, `tvChannelName`, `tvStreamLink`, `sourceFrom`, `channelId`, `channelPreviewProviderId`, `isHls`, `catchupSource`, `userAgent`, `referer`, `props`, `extensionSourceId` FROM ExtensionsChannel")
                    database.execSQL("DROP TABLE ExtensionsChannel")
                    database.execSQL("ALTER TABLE ExtensionsChannel_New RENAME TO ExtensionsChannel")
                }
            }
        }

        private val MIGRATE_9_10 by lazy {
            object : Migration(9, 10) {
                override fun migrate(database: SupportSQLiteDatabase) {
                    database.execSQL(
                        "CREATE VIEW `ExtensionChannelDatabaseViews` AS SELECT * FROM ExtensionsChannel ORDER BY CASE WHEN LENGTH(tvChannelName) < 2 THEN tvChannelName WHEN tvChannelName LIKE 'A%' OR tvChannelName LIKE 'Á%' OR tvChannelName LIKE 'À%' OR tvChannelName LIKE 'Ả%' OR tvChannelName LIKE 'Ã%' OR tvChannelName LIKE 'Ạ%' THEN 'A'||substr(tvChannelName, 1, 3) WHEN tvChannelName LIKE 'Â%' OR tvChannelName LIKE 'Ấ%' OR tvChannelName LIKE 'Ầ%' OR tvChannelName LIKE 'Ẩ%' OR tvChannelName LIKE 'Ẫ%' OR tvChannelName LIKE 'Ậ%' THEN 'Azz' WHEN tvChannelName LIKE 'Ă%' OR tvChannelName LIKE 'Ắ%' OR tvChannelName LIKE 'Ằ%' OR tvChannelName LIKE 'Ẳ%' OR tvChannelName LIKE 'Ẵ%' OR tvChannelName LIKE 'Ặ%' THEN 'Az'||substr(tvChannelName, 1 , 2) WHEN tvChannelName LIKE 'Đ%' THEN 'Dz'||substr(tvChannelName, 1 , 2) WHEN tvChannelName LIKE 'Ê%' OR tvChannelName LIKE 'Ế%' OR tvChannelName LIKE 'Ề%' OR tvChannelName LIKE 'Ể%' OR tvChannelName LIKE 'Ễ%' OR tvChannelName LIKE 'Ệ%' THEN 'Ez'||substr(tvChannelName, 1 , 2) WHEN tvChannelName LIKE 'Ô%' OR tvChannelName LIKE 'Ố%' OR tvChannelName LIKE 'Ồ%' OR tvChannelName LIKE 'Ổ%' OR tvChannelName LIKE 'Ỗ%' OR tvChannelName LIKE 'Ộ%' THEN 'Oz'||substr(tvChannelName, 1 , 2) WHEN tvChannelName LIKE 'Ơ%' OR tvChannelName LIKE 'Ớ%' OR tvChannelName LIKE 'Ờ%' OR tvChannelName LIKE 'Ở%' OR tvChannelName LIKE 'Ỡ%' OR tvChannelName LIKE 'Ợ%' THEN 'Ozz' WHEN tvChannelName LIKE 'Ư%' OR tvChannelName LIKE 'Ứ%' OR tvChannelName LIKE 'Ừ%' OR tvChannelName LIKE 'Ử%' OR tvChannelName LIKE 'Ữ%' OR tvChannelName LIKE 'Ự%' THEN 'Uz'||substr(tvChannelName, 1 , 2) ELSE substr(tvChannelName, 0, 3) END"
                    )
                }
            }
        }

        private val MIGRATE_10_11 by lazy {
            object : Migration(10, 11) {
                override fun migrate(database: SupportSQLiteDatabase) {
                    database.execSQL("CREATE VIEW `ExtensionsChannelDBWithCategoryViews` AS SELECT configSourceUrl, name as categoryName, tvChannelName, logoChannel, tvStreamLink, sourceFrom FROM ExtensionChannelCategory AS Category INNER JOIN ExtensionChannelDatabaseViews ON Category.name = tvGroup");
                    database.execSQL("CREATE VIRTUAL TABLE IF NOT EXISTS `ExtensionsChannelFts4` USING FTS4(tokenize=unicode61, content=`ExtensionsChannel`)")
                    database.execSQL("CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_ExtensionsChannelFts4_BEFORE_UPDATE BEFORE UPDATE ON `ExtensionsChannel` BEGIN DELETE FROM `ExtensionsChannelFts4` WHERE `docid`=OLD.`rowid`; END")
                    database.execSQL("CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_ExtensionsChannelFts4_BEFORE_DELETE BEFORE DELETE ON `ExtensionsChannel` BEGIN DELETE FROM `ExtensionsChannelFts4` WHERE `docid`=OLD.`rowid`; END")
                    database.execSQL("CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_ExtensionsChannelFts4_AFTER_UPDATE AFTER UPDATE ON `ExtensionsChannel` BEGIN INSERT INTO `ExtensionsChannelFts4`(`docid`, `tvGroup`, `logoChannel`, `tvChannelName`, `tvStreamLink`, `sourceFrom`, `channelId`, `channelPreviewProviderId`, `isHls`, `catchupSource`, `userAgent`, `referer`, `props`, `extensionSourceId`) VALUES (NEW.`rowid`, NEW.`tvGroup`, NEW.`logoChannel`, NEW.`tvChannelName`, NEW.`tvStreamLink`, NEW.`sourceFrom`, NEW.`channelId`, NEW.`channelPreviewProviderId`, NEW.`isHls`, NEW.`catchupSource`, NEW.`userAgent`, NEW.`referer`, NEW.`props`, NEW.`extensionSourceId`); END")
                    database.execSQL("CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_ExtensionsChannelFts4_AFTER_INSERT AFTER INSERT ON `ExtensionsChannel` BEGIN INSERT INTO `ExtensionsChannelFts4`(`docid`, `tvGroup`, `logoChannel`, `tvChannelName`, `tvStreamLink`, `sourceFrom`, `channelId`, `channelPreviewProviderId`, `isHls`, `catchupSource`, `userAgent`, `referer`, `props`, `extensionSourceId`) VALUES (NEW.`rowid`, NEW.`tvGroup`, NEW.`logoChannel`, NEW.`tvChannelName`, NEW.`tvStreamLink`, NEW.`sourceFrom`, NEW.`channelId`, NEW.`channelPreviewProviderId`, NEW.`isHls`, NEW.`catchupSource`, NEW.`userAgent`, NEW.`referer`, NEW.`props`, NEW.`extensionSourceId`); END")
                    database.execSQL("CREATE TABLE IF NOT EXISTS `HistoryMediaItemDTO` (`itemId` TEXT NOT NULL, `category` TEXT NOT NULL, `displayName` TEXT NOT NULL, `thumb` TEXT NOT NULL, `currentPosition` INTEGER NOT NULL, `contentDuration` INTEGER NOT NULL, `isLiveStreaming` INTEGER NOT NULL, `description` TEXT NOT NULL, `linkPlay` TEXT NOT NULL, `type` TEXT NOT NULL, `lastViewTime` INTEGER NOT NULL, PRIMARY KEY(`itemId`, `linkPlay`))")
                }
            }
        }

        private val MIGRATE_11_12 by lazy {
            object : Migration(11, 12) {
                override fun migrate(database: SupportSQLiteDatabase) {
                    database.execSQL("CREATE TABLE IF NOT EXISTS `TVChannelDTO_New` (`tvGroup` TEXT NOT NULL, `logoChannel` TEXT NOT NULL, `tvChannelName` TEXT NOT NULL, `sourceFrom` TEXT NOT NULL, `channelId` TEXT NOT NULL, `searchKey` TEXT NOT NULL, PRIMARY KEY(`channelId`))")
                    migrateCopyData11to12(database)
                    database.execSQL("DROP TABLE TVChannelDTO")
                    database.execSQL("ALTER TABLE TVChannelDTO_New RENAME TO TVChannelDTO")

                    database.execSQL("CREATE VIRTUAL TABLE IF NOT EXISTS `TVChannelFts4` USING FTS4(`tvGroup` TEXT NOT NULL, `logoChannel` TEXT NOT NULL, `tvChannelName` TEXT NOT NULL, `sourceFrom` TEXT NOT NULL, `channelId` TEXT NOT NULL, `searchKey` TEXT NOT NULL, tokenize=unicode61, content=`TVChannelDTO`)")

                    database.execSQL(
                        "CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_TVChannelFts4_BEFORE_UPDATE " +
                                "BEFORE UPDATE ON `TVChannelDTO` BEGIN DELETE FROM `TVChannelFts4` " +
                                "WHERE `docid`=OLD.`rowid`; END"
                    )
                    database.execSQL(
                        "CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_TVChannelFts4_BEFORE_DELETE " +
                                "BEFORE DELETE ON `TVChannelDTO` BEGIN DELETE FROM `TVChannelFts4` " +
                                "WHERE `docid`=OLD.`rowid`; END"
                    )
                    database.execSQL(
                        "CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_TVChannelFts4_AFTER_UPDATE " +
                                "AFTER UPDATE ON `TVChannelDTO` BEGIN INSERT INTO `TVChannelFts4`(" +
                                "`docid`, `tvGroup`, `logoChannel`, `tvChannelName`, `sourceFrom`, `channelId`) " +
                                "VALUES (NEW.`rowid`, NEW.`tvGroup`, NEW.`logoChannel`, NEW.`tvChannelName`, " +
                                "NEW.`sourceFrom`, NEW.`channelId`); END"
                    )
                    database.execSQL(
                        "CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_TVChannelFts4_AFTER_INSERT " +
                                "AFTER INSERT ON `TVChannelDTO` BEGIN INSERT INTO `TVChannelFts4`(" +
                                "`docid`, `tvGroup`, `logoChannel`, `tvChannelName`, `sourceFrom`, `channelId`) " +
                                "VALUES (NEW.`rowid`, NEW.`tvGroup`, NEW.`logoChannel`, NEW.`tvChannelName`, " +
                                "NEW.`sourceFrom`, NEW.`channelId`); END"
                    )
                }
            }
        }

        private val MIGRATE_12_13 by lazy {
            object : Migration(12, 13) {
                override fun migrate(database: SupportSQLiteDatabase) {
                    database.execSQL("CREATE TABLE IF NOT EXISTS `VideoFavoriteDTO` (`id` TEXT NOT NULL, `url` TEXT NOT NULL, `title` TEXT NOT NULL, `category` TEXT NOT NULL, `logoUrl` TEXT NOT NULL, `sourceFrom` TEXT NOT NULL, `type` TEXT NOT NULL, PRIMARY KEY(`id`, `url`))")
                }
            }
        }
        fun query(
            db: SupportSQLiteDatabase, sqLiteQuery: SupportSQLiteQuery,
            maybeCopy: Boolean, signal: CancellationSignal?
        ): Cursor {
            val cursor = db.query(sqLiteQuery, signal)
            if (maybeCopy && cursor is AbstractWindowedCursor) {
                val rowsInCursor = cursor.count
                val rowsInWindow: Int = if (cursor.hasWindow()) {
                    cursor.window.numRows
                } else {
                    rowsInCursor
                }
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || rowsInWindow < rowsInCursor) {
                    return copyAndClose(cursor)
                }
            }
            return cursor
        }
        private fun copyAndClose(cursor: Cursor): Cursor {
            val matrixCursor: MatrixCursor
            cursor.use { c ->
                matrixCursor = MatrixCursor(c.columnNames, c.count)
                while (c.moveToNext()) {
                    val row = arrayOfNulls<Any>(c.columnCount)
                    for (i in 0 until c.columnCount) {
                        when (c.getType(i)) {
                            Cursor.FIELD_TYPE_NULL -> row[i] = null
                            Cursor.FIELD_TYPE_INTEGER -> row[i] = c.getLong(i)
                            Cursor.FIELD_TYPE_FLOAT -> row[i] = c.getDouble(i)
                            Cursor.FIELD_TYPE_STRING -> row[i] = c.getString(i)
                            Cursor.FIELD_TYPE_BLOB -> row[i] = c.getBlob(i)
                            else -> throw IllegalStateException()
                        }
                    }
                    matrixCursor.addRow(row)
                }
            }
            return matrixCursor
        }
        fun findColumnIndexBySuffix(columnNames: Array<String>, name: String): Int {
            val dotSuffix = ".$name"
            val backtickSuffix = ".$name`"
            for (index in columnNames.indices) {
                val columnName = columnNames[index]
                // do not check if column name is not long enough. 1 char for table name, 1 char for '.'
                if (columnName.length >= name.length + 2) {
                    if (columnName.endsWith(dotSuffix)) {
                        return index
                    } else if (columnName[0] == '`'
                        && columnName.endsWith(backtickSuffix)
                    ) {
                        return index
                    }
                }
            }
            return -1
        }
        private fun findColumnIndexBySuffix(cursor: Cursor, name: String): Int {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N_MR1) {
                // we need this workaround only on APIs < 26. So just return not found on newer APIs
                return -1
            }
            if (name.isEmpty()) {
                return -1
            }
            val columnNames = cursor.columnNames
            return findColumnIndexBySuffix(columnNames, name)
        }
        fun getColumnIndex(c: Cursor, name: String): Int {
            var index = c.getColumnIndex(name)
            if (index >= 0) {
                return index
            }
            index = c.getColumnIndex("`$name`")
            return if (index >= 0) {
                index
            } else findColumnIndexBySuffix(c, name)
        }
        fun getColumnIndexOrThrow(c: Cursor, name: String): Int {
            val index = getColumnIndex(c, name)
            if (index >= 0) {
                return index
            }
            var availableColumns = ""
            try {
                availableColumns = Arrays.toString(c.columnNames)
            } catch (e: Exception) {
                Logger.e("RoomCursorUtil", message = "Cannot collect column names for debug purposes")
                Logger.e("RoomCursorUtil", exception = e)
            }
            throw IllegalArgumentException(
                "column '" + name
                        + "' does not exist. Available columns: " + availableColumns
            )
        }
        private fun migrateCopyData11to12(sqLiteDatabase: SupportSQLiteDatabase) {
            val tvChannelDTOS = ArrayList<TVChannelDTO>()
            try {
                sqLiteDatabase.beginTransaction()
                val cursor = query(sqLiteDatabase, SimpleSQLiteQuery("SELECT * FROM TVChannelDTO"),
                    true,
                    null)
                cursor.use { c ->
                    val indexOfTvGroup = getColumnIndexOrThrow(c, "tvGroup")
                    val indexOfLogoChannel = getColumnIndexOrThrow(c, "logoChannel")
                    val indexOfTvChannelName = getColumnIndexOrThrow(c, "tvChannelName")
                    val indexOfSourceFrom = getColumnIndexOrThrow(c, "sourceFrom")
                    val indexOfChannelId = getColumnIndexOrThrow(c, "channelId")
                    while (c.moveToNext()) {
                        val channelId = c.getString(indexOfChannelId) ?: ""
                        val tvGroup = c.getString(indexOfTvGroup) ?: ""
                        val logoChannel = c.getString(indexOfLogoChannel) ?: ""
                        val tvChannelName = c.getString(indexOfTvChannelName) ?: ""
                        val tvSourceFrom = c.getString(indexOfSourceFrom) ?: ""
                        tvChannelDTOS.add(
                            TVChannelDTO(
                                channelId = channelId,
                                tvGroup = tvGroup,
                                logoChannel = logoChannel,
                                tvChannelName = tvChannelName,
                                sourceFrom = tvSourceFrom,
                                searchKey = tvChannelName.lowercase()
                                    .replaceVNCharsToLatinChars()
                                    .removeAllSpecialChars()
                            )
                        )
                    }
                    c.moveToPosition(-1)
                    sqLiteDatabase.setTransactionSuccessful()
                }
            } catch (e: Exception) {
                Logger.e("RoomDatabase", exception = e)
            } finally {
                sqLiteDatabase.endTransaction()
            }

            val insertQuery = "INSERT OR REPLACE INTO `TVChannelDTO_New` (`tvGroup`,`logoChannel`,`tvChannelName`,`sourceFrom`,`channelId`,`searchKey`) VALUES (?,?,?,?,?,?)"
            sqLiteDatabase.beginTransaction()
            try {
                val stmt = sqLiteDatabase.compileStatement(insertQuery)
                for (item in tvChannelDTOS) {
                    bind(stmt, item)
                    stmt.executeInsert()
                }
                sqLiteDatabase.setTransactionSuccessful()
            } finally {
                sqLiteDatabase.endTransaction()
            }
        }
        fun bind(stmt: SupportSQLiteStatement, value: TVChannelDTO) {
            stmt.bindString(1, value.tvGroup)
            stmt.bindString(2, value.logoChannel)
            stmt.bindString(3, value.tvChannelName)
            stmt.bindString(4, value.sourceFrom)
            stmt.bindString(5, value.channelId)
            stmt.bindString(6, value.searchKey)
        }

        @Volatile
        var INSTANCE: RoomDataBase? = null
        fun getInstance(context: Context) = INSTANCE ?: synchronized(this) {
            Room.databaseBuilder(context, RoomDataBase::class.java, "TV")
                .addMigrations(
                    MIGRATE_1_2, MIGRATE_2_3, MIGRATE_3_4,
                    MIGRATE_4_5, MIGRATE_5_6, MIGRATE_6_7,
                    MIGRATE_7_8, MIGRATE_8_9, MIGRATE_9_10,
                    MIGRATE_10_11, MIGRATE_11_12, MIGRATE_12_13
                )
                .build()
                .also {
                    INSTANCE = it
                }
        }
    }
}