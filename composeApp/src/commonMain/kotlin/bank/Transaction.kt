package bank

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.TypeConverter
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
    ],
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
    suspend fun count(): Long
}

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