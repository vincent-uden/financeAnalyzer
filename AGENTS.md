# AGENTS.md

This file contains instructions for agentic coding assistants working in this repository.

## Build Commands

### Full Build
```bash
./gradlew build
```

### Build Specific Module
```bash
./gradlew composeApp:build
```

### Clean Build
```bash
./gradlew clean build
```

### Run Application
```bash
./gradlew composeApp:run
```

### Hot Reload Development
```bash
./gradlew composeApp:hotRunJvm
```

## Testing Commands

### Run All Tests
```bash
./gradlew composeApp:jvmTest
```

### Run All Tests with Coverage
```bash
./gradlew composeApp:jvmTest
```

### Run Single Test Class
```bash
./gradlew composeApp:jvmTest --tests "com.vincentuden.financeanalyzer.ComposeAppDesktopTest"
```

### Run Single Test Method
```bash
./gradlew composeApp:jvmTest --tests "com.vincentuden.financeanalyzer.ComposeAppDesktopTest.example"
```

### Run Tests and Generate Reports
```bash
./gradlew composeApp:allTests
```

## Code Quality & Linting

No dedicated linting tools are configured. The project uses:
- Kotlin compiler warnings
- Gradle build validation

## Code Style Guidelines

### Kotlin Official Style
- Use official Kotlin coding style (configured in `gradle.properties`)
- 4-space indentation (no tabs)
- Maximum line length: 120 characters
- No semicolons at end of statements

### Imports
```kotlin
// Group imports by package hierarchy
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.room.*

// Separate standard library imports
import kotlinx.coroutines.*

// Blank line between import groups
import java.time.LocalDate
```

### Naming Conventions

#### Classes and Data Classes
```kotlin
// PascalCase for classes
data class TodoEntity(
    // ...
)

// PascalCase for interfaces
interface TodoDao {
    // ...
}

// PascalCase for enums and sealed classes
sealed class Result<T> {
    // ...
}
```

#### Functions and Methods
```kotlin
// camelCase for functions
fun getTodoById(id: Long): TodoEntity?

// camelCase for methods
suspend fun insert(item: TodoEntity)

// Boolean functions start with 'is', 'has', 'can', etc.
fun isValidTransaction(): Boolean
```

#### Variables and Properties
```kotlin
// camelCase for variables
val userName: String
var itemCount: Int

// Private properties with underscore prefix
private val _todos = mutableStateListOf<TodoEntity>()

// Backing properties exposed as immutable
val todos: List<TodoEntity> get() = _todos

// Constants in UPPER_SNAKE_CASE
const val MAX_RETRY_ATTEMPTS = 3
```

#### Database and Room
```kotlin
// Table names in PascalCase
@Entity
data class Transaction(
    // Primary keys
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // Foreign keys with descriptive names
    @ColumnInfo(index = true)
    val vendorId: Long,

    // Indexed columns
    @ColumnInfo(index = true)
    val accountId: Long,
)

// DAO interfaces
@Dao
interface TransactionDao {
    // Query methods with descriptive names
    @Query("SELECT * FROM Transaction WHERE id = :id")
    suspend fun findById(id: Long): Transaction?

    // Insert methods
    @Insert
    suspend fun insert(transaction: Transaction)

    // Count methods
    @Query("SELECT COUNT(*) FROM Transaction")
    suspend fun count(): Int
}
```

### Type Annotations
```kotlin
// Always specify types for public APIs
fun processTransaction(transaction: Transaction): Result<Unit>

// Use type inference for local variables when clear
val result = processTransaction(transaction)

// Specify types when inference might be unclear
val filteredList: List<Transaction> = transactions.filter { it.amount > 0 }
```

### Null Safety
```kotlin
// Use nullable types when null is possible
fun findTransaction(id: Long): Transaction?

// Use Elvis operator for defaults
val name = transaction.vendorName ?: "Unknown"

// Use safe calls
val amount = transaction.amount?.let { it / 100.0 }

// Require non-null assertion only when certain
val requiredId = transaction.id!!
```

### Collections and Generics
```kotlin
// Use immutable collections by default
val transactions: List<Transaction> = emptyList()

// Use mutable collections only when necessary
private val _transactions = mutableListOf<Transaction>()

// Generic functions with constraints
fun <T : Number> sum(values: List<T>): T {
    // implementation
}
```

### Error Handling
```kotlin
// Use Result type for operations that can fail
fun validateTransaction(transaction: Transaction): Result<Unit> {
    return try {
        // validation logic
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}

// Use try-catch for exceptional cases
try {
    val result = riskyOperation()
    processResult(result)
} catch (e: IOException) {
    logger.error("IO operation failed", e)
    // handle error
}
```

### Coroutines and Flow
```kotlin
// Repository pattern with suspend functions
class TransactionRepository(private val dao: TransactionDao) {
    suspend fun getAllTransactions(): List<Transaction> {
        return dao.getAll()
    }

    fun getTransactionsAsFlow(): Flow<List<Transaction>> {
        return dao.getAllAsFlow()
    }
}

// ViewModel with coroutine scope
class TransactionViewModel : ViewModel() {
    fun loadTransactions() {
        viewModelScope.launch {
            try {
                val transactions = repository.getAllTransactions()
                _uiState.value = UiState.Success(transactions)
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}
```

### UI Framework Preference

**IMPORTANT**: Use Compose Unstyled components to the greatest possible extent. Avoid Material UI components unless absolutely necessary for specific functionality that Compose Unstyled doesn't provide.

#### Preferred Components
Use these Compose Unstyled components instead of Material equivalents:
- `Text` instead of `androidx.compose.material.Text`
- `UnstyledButton` instead of `Button`
- `TextField` and `TextInput` instead of Material `TextField`
- `Tab`, `TabGroup`, `TabList`, `TabPanel` for navigation
- Custom layouts with `Column`, `Row`, `Box` instead of Material cards/layouts

#### When to Use Material UI
Only use Material UI components when:
- Specific Material Design functionality is required
- Compose Unstyled doesn't provide the needed component
- Working with existing Material-based design systems

### Compose UI Patterns
```kotlin
// State management
@Composable
fun TransactionList() {
    var transactions by remember { mutableStateOf(emptyList<Transaction>()) }
    val scope = rememberCoroutineScope()

    // State hoisting
    TransactionListContent(
        transactions = transactions,
        onTransactionClick = { transaction ->
            // handle click
        },
        onRefresh = {
            scope.launch {
                transactions = repository.getAllTransactions()
            }
        }
    )
}

// Parameter naming
@Composable
fun TransactionListContent(
    transactions: List<Transaction>,
    onTransactionClick: (Transaction) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    // implementation
}

// Theme and styling
val AppTheme = buildPlatformTheme {
    // theme configuration
}

@Composable
fun App() {
    AppTheme {
        // UI content
    }
}

// Compose Unstyled components usage
@Composable
fun ExampleForm() {
    var text by remember { mutableStateOf("") }
    val textState = rememberTextFieldState(text)

    Column {
        Text("Enter your name:")
        TextField(state = textState) {
            TextInput()
        }
        UnstyledButton(onClick = { /* handle click */ }) {
            Text("Submit")
        }
    }
}
```

### File Organization
```
composeApp/
├── src/
│   ├── commonMain/
│   │   ├── kotlin/
│   │   │   ├── AppDatabase.kt          # Database setup
│   │   │   ├── TodoEntity.kt           # Data models
│   │   │   ├── TodoDao.kt              # Data access objects
│   │   │   └── bank/                   # Feature modules
│   │   │       └── Transaction.kt
│   └── jvmMain/
│       ├── kotlin/
│       │   └── com/vincentuden/financeanalyzer/   # Platform-specific code
│       │       ├── App.kt              # Main UI
│       │       ├── main.kt             # Application entry point
│       │       └── Platform.kt         # Platform utilities
│       └── resources/                  # Resources
└── src/jvmTest/
    └── kotlin/
        └── com/vincentuden/financeanalyzer/       # Tests
            └── ComposeAppDesktopTest.kt
```

### Architecture Patterns

#### Multiplatform Structure
- `commonMain`: Shared code across platforms
- `jvmMain`: JVM-specific implementations
- `commonTest`: Shared tests (when applicable)

#### Database Layer
```kotlin
// Entity definition in commonMain
@Entity
data class TodoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val content: String,
)

// DAO interface in commonMain
@Dao
interface TodoDao {
    @Insert
    suspend fun insert(item: TodoEntity)

    @Query("SELECT * FROM TodoEntity")
    fun getAllAsFlow(): Flow<List<TodoEntity>>
}

// Database setup with expect/actual
@Database(entities = [TodoEntity::class], version = 1)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun getDao(): TodoDao
}

expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase>

// Platform-specific implementation
actual object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    actual override fun initialize(): AppDatabase {
        // platform-specific initialization
    }
}
```

#### UI Layer
```kotlin
// Compose UI with state management
@Composable
fun TodoScreen(viewModel: TodoViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    when (uiState) {
        is UiState.Loading -> LoadingIndicator()
        is UiState.Success -> TodoList(
            todos = (uiState as UiState.Success).todos,
            onTodoClick = viewModel::onTodoSelected
        )
        is UiState.Error -> ErrorMessage(
            message = (uiState as UiState.Error).message
        )
    }
}
```

## AI Assistant Rules

No Cursor rules (.cursorrules) or Copilot instructions (.github/copilot-instructions.md) were found in this repository.

## Development Workflow

1. **Before committing**: Run `./gradlew build` to ensure everything compiles
2. **Testing**: Run `./gradlew composeApp:jvmTest` to execute tests
3. **Code style**: Follow Kotlin official conventions
4. **Database changes**: Update schema version and migration scripts if needed
5. **Multiplatform**: Test changes on all target platforms when applicable

## Common Patterns

### Dependency Injection
```kotlin
// Manual dependency injection
class TodoViewModel(
    private val repository: TodoRepository = TodoRepository()
) : ViewModel()
```

### State Management
```kotlin
sealed class UiState {
    object Loading : UiState()
    data class Success(val data: List<TodoEntity>) : UiState()
    data class Error(val message: String) : UiState()
}
```

### Resource Management
```kotlin
// Compose resources
import financeanalyzer.composeapp.generated.resources.Res
import financeanalyzer.composeapp.generated.resources.compose_multiplatform

@Composable
fun AppIcon() {
    Image(
        painter = painterResource(Res.drawable.compose_multiplatform),
        contentDescription = "App Icon"
    )
}
```