package bank

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import colors
import AppDatabase
import BankRepository
import bank.Transaction
import bank.TransactionWithCategory
import com.composeunstyled.Text
import com.composeunstyled.TextField
import com.composeunstyled.TextInput
import com.composeunstyled.UnstyledButton
import com.composeunstyled.theme.Theme
import green
import kotlinx.coroutines.launch
import red
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun CategoriesView(db: AppDatabase) {
    var categories by remember { mutableStateOf<List<Category>>(emptyList()) }
    var transactions by remember { mutableStateOf<List<TransactionWithCategory>>(emptyList()) }
    var selectedTransactionId by remember { mutableStateOf<Long?>(null) }
    val scope = rememberCoroutineScope()
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val repo = BankRepository(db)

    LaunchedEffect(Unit) {
        categories = repo.getCategories()
        transactions = repo.getTransactionsWithCategories()
    }

    val reloadCategories = {
        scope.launch {
            categories = repo.getCategories()
        }
    }

    val reloadTransactions = {
        scope.launch {
            transactions = repo.getTransactionsWithCategories()
        }
    }

    Row(modifier = Modifier.safeContentPadding().fillMaxSize()) {
        // Sidebar for categories
        Column(modifier = Modifier.weight(1f).fillMaxHeight().padding(8.dp)) {
            Text("Categories", modifier = Modifier.padding(bottom = 8.dp))
            LazyColumn(modifier = Modifier.weight(1f)) {
                itemsIndexed(categories) { _, category ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(category.name, modifier = Modifier.weight(1f))
                        UnstyledButton(onClick = {
                            scope.launch {
                                repo.deleteCategory(category)
                                reloadCategories()
                                reloadTransactions()
                            }
                        }) {
                            Text("Delete", color = Theme[colors][red])
                        }
                    }
                }
            }
            val newCategoryState = remember { androidx.compose.foundation.text.input.TextFieldState() }
            Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                TextField(
                    state = newCategoryState,
                    modifier = Modifier.weight(1f).background(Color(0xFF35374B)).border(1.dp, Color.White, RoundedCornerShape(4.dp))
                ) {
                    TextInput()
                }
                UnstyledButton(onClick = {
                    val name = newCategoryState.text.toString().trim()
                    if (name.isNotEmpty()) {
                        scope.launch {
                            repo.insertCategory(name)
                            newCategoryState.edit { replace(0, length, "") }
                            reloadCategories()
                        }
                    }
                }) {
                    Text("Add")
                }
            }
        }

        // Main area for transactions
        Box(modifier = Modifier.weight(3f).fillMaxHeight()) {
            Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                Text("Transactions", modifier = Modifier.padding(bottom = 8.dp))
                Row(modifier = Modifier.fillMaxWidth().background(Color(0xFF35374B)).padding(8.dp)) {
                    Text("Date", modifier = Modifier.width(120.dp), color = Color.White)
                    Text("Vendor", modifier = Modifier.width(150.dp), color = Color.White)
                    Text("Amount (SEK)", modifier = Modifier.width(100.dp), textAlign = TextAlign.Right, color = Color.White)
                    Text("Category", modifier = Modifier.width(100.dp), color = Color.White)
                    Text("Actions", modifier = Modifier.width(100.dp), color = Color.White)
                }
                LazyColumn(modifier = Modifier.weight(1f)) {
                    itemsIndexed(transactions) { index, trans ->
                        val bgColor = if (index % 2 == 0) Color(0xFF1A1B26) else Color(0xFF35374B)
                        val amountColor = if (trans.amount > 0) Theme[colors][green] else Theme[colors][red]
                        Row(
                            modifier = Modifier.fillMaxWidth().background(bgColor).padding(vertical = 4.dp, horizontal = 8.dp)
                        ) {
                            Text(
                                formatter.format(
                                    LocalDate.ofInstant(
                                        trans.transactionDate.toInstant(),
                                        ZoneId.systemDefault()
                                    )
                                ), modifier = Modifier.width(120.dp)
                            )
                            Text(trans.vendorName ?: "Unknown", modifier = Modifier.width(150.dp))
                            Text(String.format("%.2f", trans.amount / 100.0), modifier = Modifier.width(100.dp), textAlign = TextAlign.Right, color = amountColor)
                            Text(trans.categoryName ?: "None", modifier = Modifier.width(100.dp))
                            UnstyledButton(onClick = { selectedTransactionId = trans.id }, modifier = Modifier.width(100.dp)) {
                                Text("Assign")
                            }
                        }
                    }
                }
            }

            // Overlay for category selection
            if (selectedTransactionId != null) {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier.background(Color(0xFF1A1B26)).padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Select Category for Transaction", modifier = Modifier.padding(bottom = 8.dp))
                        LazyColumn(modifier = Modifier.height(200.dp)) {
                            item {
                                UnstyledButton(onClick = {
                                    scope.launch {
                                        val trans = repo.getTransactionById(selectedTransactionId!!)
                                        if (trans != null) {
                                            repo.updateTransaction(trans.copy(categoryId = null))
                                            reloadTransactions()
                                        }
                                        selectedTransactionId = null
                                    }
                                }) {
                                    Text("None", modifier = Modifier.fillMaxWidth().padding(8.dp))
                                }
                            }
                            itemsIndexed(categories) { _, category ->
                                UnstyledButton(onClick = {
                                    scope.launch {
                                        val trans = repo.getTransactionById(selectedTransactionId!!)
                                        if (trans != null) {
                                            repo.updateTransaction(trans.copy(categoryId = category.id))
                                            reloadTransactions()
                                        }
                                        selectedTransactionId = null
                                    }
                                }) {
                                    Text(category.name, modifier = Modifier.fillMaxWidth().padding(8.dp))
                                }
                            }
                        }
                        UnstyledButton(onClick = { selectedTransactionId = null }) {
                            Text("Cancel", color = Theme[colors][red])
                        }
                    }
                }
            }
        }
    }
}