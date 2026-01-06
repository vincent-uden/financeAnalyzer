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
    indices = [
        Index(value = ["name"], unique = true)
    ]
)
data class Vendor(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val userDefinedName: String?,
)

@Dao
interface VendorDao {
    @Insert
    suspend fun insert(value: Vendor): Long

    @Query("SELECT count(*) FROM `Vendor`")
    suspend fun count(): Long

    @Query("SELECT * FROM `Vendor` WHERE name = :name")
    suspend fun getByName(name: String): Vendor?

    @Query("SELECT * FROM `Vendor` WHERE id = :id")
    suspend fun getById(id: Long): Vendor?
}

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
)

@Dao
interface TransactionDao {
    @Insert
    suspend fun insert(value: Transaction)

    @Update
    suspend fun update(value: Transaction)

    @Query("SELECT count(*) FROM `Transaction`")
    suspend fun count(): Long

    @Query("SELECT * FROM `Transaction` WHERE id = :id")
    suspend fun getById(id: Long): Transaction?

    @Query("SELECT t.*, c.name as categoryName, v.name as vendorName, a.name as accountName FROM `Transaction` t LEFT JOIN Category c ON t.categoryId = c.id LEFT JOIN Vendor v ON t.vendor = v.id LEFT JOIN Account a ON t.account = a.id ORDER BY t.transactionDate DESC")
    suspend fun getAllWithCategories(): List<TransactionWithCategory>
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