package bank

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.TypeConverter
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = Vendor::class,
            parentColumns = arrayOf("id"),
            childColumns = arrayOf("vendor"),
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Account::class,
            parentColumns = arrayOf("id"),
            childColumns = arrayOf("account"),
            onDelete = ForeignKey.CASCADE
        ),
    ]
)
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val transactionDate: Date,
    // The amount of money * 100 in SEK
    val amount: Long,
    @ColumnInfo(index = true) val vendor: Long,
    @ColumnInfo(index = true) val account: Long,
)

@Dao
interface TransactionDao {
    @Insert
    suspend fun insert(value: Transaction)

    @Query("SELECT count(*) FROM `Transaction`")
    suspend fun count()
}

@Entity
data class Vendor(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val userDefinedName: String?,
)

@Dao
interface VendorDao {
    @Insert
    suspend fun insert(value: Vendor)

    @Query("SELECT count(*) FROM `Vendor`")
    suspend fun count()
}

@Entity
data class Account(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val clearingNumber: String,
    val accountNumber: String,
    val name: String,
)

@Dao
interface AccountDao {
    @Insert
    suspend fun insert(value: Account)

    @Query("SELECT count(*) FROM `Account`")
    suspend fun count()
}

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter()
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time?.toLong()
    }
}

data class ImportedTransaction(
    val transactionDate: Date,
    // The amount of money * 100 in SEK
    val amount: Long,
    val vendorName: String,
)

interface BankStatement {
    fun getAccount(): Account
    fun getTransactions(): List<ImportedTransaction>
}

@Suppress("SpellCheckingInspection")
class HandelsbankenStatement(account: Account, transactions: List<ImportedTransaction>): BankStatement {
    val _account: Account = account
    val _transactions: List<ImportedTransaction> = transactions
    override fun getAccount(): Account {
        return _account
    }

    override fun getTransactions(): List<ImportedTransaction> {
        return _transactions
    }

    companion object {
        fun fromXlsx(path: String): HandelsbankenStatement {
            val workbook = XSSFWorkbook(File(path))
            val sheet = workbook.getSheetAt(0)

            val accountInfo = sheet.getRow(3)?.getCell(0).toString().split(" ")
            val accountName = accountInfo[0]
            val accountNumber = accountInfo.drop(1).joinToString(" ")

            val clearingInfo = sheet.getRow(5)?.getCell(1).toString().split(" ")
            val clearingNumber = clearingInfo[1]

            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val transactionRows = sheet.mapIndexed { index, cells ->
                if (index < 9) {
                    null
                } else {
                    val transactionLocalDate = formatter.parse(cells.getCell(1).toString())
                    val transactionDate =
                        Date.from(LocalDate.from(transactionLocalDate).atStartOfDay(ZoneId.systemDefault()).toInstant())
                    val vendorName = cells.getCell(3).toString()
                    val amount = BigDecimal(cells.getCell(4).toString().replace(",", "")).movePointRight(2).toLong()
                    ImportedTransaction(transactionDate, amount, vendorName)
                }
            }

            workbook.close()
            return HandelsbankenStatement(
                account = Account(
                    id = 0,
                    clearingNumber = clearingNumber,
                    accountNumber = accountNumber,
                    name = accountName,
                ), transactions = transactionRows.filterNotNull()
            )
        }
    }
}

@Composable
fun BankStatementImporter() {
    Column(Modifier.safeContentPadding().fillMaxSize()) {
    }
}