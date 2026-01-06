package bank

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import androidx.room.TypeConverter
import java.util.Date

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = arrayOf("id"),
            childColumns = arrayOf("categoryId"),
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["name"], unique = true)
    ]
)
data class Vendor(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val userDefinedName: String?,
    val categoryId: Long?,
)

@Dao
interface VendorDao {
    @Insert
    suspend fun insert(value: Vendor): Long

    @Update
    suspend fun update(value: Vendor)

    @Query("SELECT count(*) FROM `Vendor`")
    suspend fun count(): Long

    @Query("SELECT * FROM `Vendor` WHERE name = :name")
    suspend fun getByName(name: String): Vendor?

    @Query("SELECT * FROM `Vendor` WHERE id = :id")
    suspend fun getById(id: Long): Vendor?

    @Query("SELECT v.*, c.name as categoryName FROM `Vendor` v LEFT JOIN Category c ON v.categoryId = c.id ORDER BY v.name")
    suspend fun getAllWithCategories(): List<VendorWithCategory>
}

data class VendorWithCategory(
    val id: Long,
    val name: String,
    val userDefinedName: String?,
    val categoryId: Long?,
    val categoryName: String?,
)

@Entity(
    indices = [
        Index(value = ["name"], unique = true)
    ]
)
data class Account(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val clearingNumber: String,
    val accountNumber: String,
    val name: String,
    val userDefinedName: String?,
)

@Dao
interface AccountDao {
    @Insert
    suspend fun insert(value: Account): Long

    @Query("SELECT count(*) FROM `Account`")
    suspend fun count(): Long

    @Query("SELECT * FROM `Account` WHERE name = :name")
    suspend fun getByName(name: String): Account?

    @Query("SELECT * FROM `Account` WHERE id = :id")
    suspend fun getById(id: Long): Account?
}

@Entity
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
)

@Dao
interface CategoryDao {
    @Insert
    suspend fun insert(value: Category): Long

    @Query("SELECT * FROM Category")
    suspend fun getAll(): List<Category>

    @Query("SELECT * FROM Category WHERE id = :id")
    suspend fun getById(id: Long): Category?

    @androidx.room.Delete
    suspend fun delete(category: Category)
}

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
        ForeignKey(
            entity = Category::class,
            parentColumns = arrayOf("id"),
            childColumns = arrayOf("categoryId"),
            onDelete = ForeignKey.SET_NULL
        ),
    ],
)
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val transactionDate: Date,
    // The amount of money * 100 in SEK
    val amount: Long,
    @ColumnInfo(index = true) val vendor: Long,
    @ColumnInfo(index = true) val account: Long,
    @ColumnInfo(index = true) val categoryId: Long?,
    // The account balance after this transaction * 100 in SEK
    val balance: Long? = null,
)

data class TransactionWithCategory(
    val id: Long,
    val transactionDate: Date,
    val amount: Long,
    val vendor: Long,
    val account: Long,
    val categoryId: Long?,
    val categoryName: String?,
    val vendorName: String?,
    val accountName: String?,
    val balance: Long?,
)

data class AccountBalance(
    val accountId: Long,
    val accountName: String,
    val balance: Long,
)

data class CategorySpending(
    val categoryName: String,
    val total: Long,
)

@Dao
interface TransactionDao {
    @Insert
    suspend fun insert(value: Transaction): Long

    @Update
    suspend fun update(value: Transaction)

    @androidx.room.Delete
    suspend fun delete(transaction: Transaction)

    @Query("SELECT count(*) FROM `Transaction`")
    suspend fun count(): Long

    @Query("SELECT * FROM `Transaction` WHERE id = :id")
    suspend fun getById(id: Long): Transaction?

    @Query("SELECT t.*, c.name as categoryName, v.name as vendorName, a.name as accountName FROM `Transaction` t LEFT JOIN Category c ON t.categoryId = c.id LEFT JOIN Vendor v ON t.vendor = v.id LEFT JOIN Account a ON t.account = a.id ORDER BY t.transactionDate DESC")
    suspend fun getAllWithCategories(): List<TransactionWithCategory>

    @Query("SELECT a.id as accountId, a.name as accountName, COALESCE((SELECT t.balance FROM `Transaction` t WHERE t.account = a.id AND t.balance IS NOT NULL ORDER BY t.transactionDate DESC LIMIT 1), COALESCE(SUM(t.amount), 0)) as balance FROM Account a LEFT JOIN `Transaction` t ON a.id = t.account GROUP BY a.id, a.name")
    suspend fun getAccountBalances(): List<AccountBalance>

    @Query("SELECT COALESCE(c.name, 'Uncategorized') as categoryName, SUM(-t.amount) as total FROM `Transaction` t LEFT JOIN Category c ON t.categoryId = c.id WHERE t.transactionDate >= :startDate AND t.transactionDate < :endDate AND t.amount < 0 AND t.account IN (:accountIds) GROUP BY t.categoryId ORDER BY total DESC")
    suspend fun getCategorySpendingForMonth(startDate: Date, endDate: Date, accountIds: List<Long>): List<CategorySpending>

    @Query("SELECT COALESCE(c.name, 'Uncategorized') as categoryName, SUM(t.amount) as total FROM `Transaction` t LEFT JOIN Category c ON t.categoryId = c.id WHERE t.transactionDate >= :startDate AND t.transactionDate < :endDate AND t.amount > 0 AND t.account IN (:accountIds) GROUP BY t.categoryId ORDER BY total DESC")
    suspend fun getCategoryIncomeForMonth(startDate: Date, endDate: Date, accountIds: List<Long>): List<CategorySpending>
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