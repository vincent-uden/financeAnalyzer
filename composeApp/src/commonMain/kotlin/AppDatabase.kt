import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.TypeConverters
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import bank.Account
import bank.AccountDao
import bank.BankStatement
import bank.Category
import bank.CategoryDao
import bank.Converters
import bank.TransactionWithCategory
import bank.Transaction
import bank.TransactionDao
import bank.Vendor
import bank.VendorDao
import bank.VendorWithCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Database(entities = [TodoEntity::class, Account::class, Transaction::class, Vendor::class, Category::class], version = 4)
@ConstructedBy(AppDatabaseConstructor::class)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun todoDao(): TodoDao
    abstract fun accountDao(): AccountDao
    abstract fun transactionDao(): TransactionDao
    abstract fun vendorDao(): VendorDao
    abstract fun categoryDao(): CategoryDao
}

@Suppress("KotlinNoActualForExpect")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}

fun getRoomDatabase(builder: RoomDatabase.Builder<AppDatabase>): AppDatabase {
    return builder.setDriver(BundledSQLiteDriver()).setQueryCoroutineContext(Dispatchers.IO).fallbackToDestructiveMigration(false).build()
}

class BankRepository(
    private val accountDao: AccountDao,
    private val transactionDao: TransactionDao,
    private val vendorDao: VendorDao,
    private val categoryDao: CategoryDao,
) {
    constructor(db: AppDatabase) : this(db.accountDao(), db.transactionDao(), db.vendorDao(), db.categoryDao()) {}

    suspend fun importStatement(stmt: BankStatement) = withContext(Dispatchers.IO) {
        var account = accountDao.getByName(stmt.getAccount().name)
        if (account == null) {
            val id = accountDao.insert(stmt.getAccount())
            account = accountDao.getById(id)
        }

        stmt.getTransactions().forEach { (transactionDate, amount, vendorName, saldo) ->
            var vendor = vendorDao.getByName(vendorName)
            if (vendor == null) {
                val id = vendorDao.insert(Vendor(
                    name = vendorName,
                    userDefinedName = null,
                    categoryId = null
                ))
                vendor = vendorDao.getById(id)
            }
            transactionDao.insert(Transaction(
                transactionDate = transactionDate,
                amount = amount,
                vendor = vendor!!.id,
                account = account!!.id,
                categoryId = vendor.categoryId,
            ))
        }
    }

    suspend fun getCategories(): List<Category> = categoryDao.getAll()

    suspend fun insertCategory(name: String): Long = categoryDao.insert(Category(name = name))

    suspend fun deleteCategory(category: Category) = categoryDao.delete(category)

    suspend fun getTransactionsWithCategories(): List<TransactionWithCategory> = transactionDao.getAllWithCategories()

    suspend fun updateTransaction(transaction: Transaction) = transactionDao.update(transaction)

    suspend fun getTransactionById(id: Long): Transaction? = transactionDao.getById(id)

    suspend fun setVendorCategory(vendorId: Long, categoryId: Long?) {
        val vendor = vendorDao.getById(vendorId) ?: return
        vendorDao.update(vendor.copy(categoryId = categoryId))
        applyVendorCategoryToTransactions(vendorId)
    }

    suspend fun getVendorsWithCategories(): List<VendorWithCategory> = vendorDao.getAllWithCategories()

    suspend fun applyVendorCategoryToTransactions(vendorId: Long) {
        val vendor = vendorDao.getById(vendorId) ?: return
        val transactionsToUpdate = transactionDao.getAllWithCategories().filter {
            it.vendor == vendorId && it.categoryId == null
        }
        transactionsToUpdate.forEach { transaction ->
            transactionDao.update(Transaction(
                id = transaction.id,
                transactionDate = transaction.transactionDate,
                amount = transaction.amount,
                vendor = transaction.vendor,
                account = transaction.account,
                categoryId = vendor.categoryId
            ))
        }
    }

    suspend fun insertTransaction(transaction: Transaction): Long {
        val vendor = vendorDao.getById(transaction.vendor)
        val categoryId = transaction.categoryId ?: vendor?.categoryId
        val transactionToInsert = transaction.copy(categoryId = categoryId)
        return transactionDao.insert(transactionToInsert)
    }
}