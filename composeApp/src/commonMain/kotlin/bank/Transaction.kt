package bank

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.composeunstyled.Text
import com.composeunstyled.UnstyledButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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

expect fun pickXlsxFile(): String?

@Composable
fun BankStatementImporter() {
    Column(Modifier.safeContentPadding().fillMaxSize()) {
        var statement by remember { mutableStateOf<HandelsbankenStatement?>(null) }
        var isLoading by remember { mutableStateOf(false) }
        var error by remember { mutableStateOf<String?>(null) }
        val scope = rememberCoroutineScope()

        UnstyledButton(onClick = {
            val path = pickXlsxFile()
            if (path != null) {
                isLoading = true
                error = null
                scope.launch(Dispatchers.IO) {
                    try {
                        val stmt = HandelsbankenStatement.fromXlsx(path)
                        statement = stmt
                    } catch (e: Exception) {
                        error = e.localizedMessage ?: "Failed to parse file"
                    } finally {
                        isLoading = false
                    }
                }
            }
        }) {
            Text(if (isLoading) "Parsing..." else "Select XLSX File")
        }

        error?.let {
            Text("Error: $it")
        }

        statement?.let { stmt ->
            val account = stmt.getAccount()
            Text("Account: ${account.name} (${account.clearingNumber} ${account.accountNumber})")
            Text("Transactions:")
            LazyColumn {
                item {
                    Row(Modifier.fillMaxWidth()) {
                        Text("Date", Modifier.weight(1f))
                        Text("Vendor", Modifier.weight(2f))
                        Text("Amount (SEK)", Modifier.weight(1f))
                    }
                }
                items(stmt.getTransactions()) { trans ->
                    Row(Modifier.fillMaxWidth()) {
                        Text(trans.transactionDate.toString(), Modifier.weight(1f))
                        Text(trans.vendorName, Modifier.weight(2f))
                        Text(String.format("%.2f", trans.amount / 100.0), Modifier.weight(1f))
                    }
                }
            }
        }
    }
}