package bank

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerKeyboardModifiers
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import colors
import AppDatabase
import BankRepository
import androidx.compose.foundation.text.input.TextFieldState
import bank.Transaction
import bank.TransactionWithCategory
import com.composeunstyled.Text
import com.composeunstyled.TextField
import com.composeunstyled.TextInput
import com.composeunstyled.UnstyledButton
import com.composeunstyled.theme.Theme
import foreground
import green
import kotlinx.coroutines.launch
import red
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// Data class for undo/redo batches
data class DeletedBatch(
    val timestamp: Long,
    val transactions: List<TransactionWithCategory>
)

@Composable
fun CategoriesView(db: AppDatabase) {
    var categories by remember { mutableStateOf<List<Category>>(emptyList()) }
    var vendors by remember { mutableStateOf<List<VendorWithCategory>>(emptyList()) }
    var transactions by remember { mutableStateOf<List<TransactionWithCategory>>(emptyList()) }
    var selectedTransactionId by remember { mutableStateOf<Long?>(null) }
    var selectedVendorId by remember { mutableStateOf<Long?>(null) }
    var selectedCategoryIds by remember { mutableStateOf<Set<Long?>>(emptySet()) }
    var selectedVendorIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var isShiftPressed by remember { mutableStateOf(false) }
    var isCtrlPressed by remember { mutableStateOf(false) }

    // Transaction selection state
    var selectedTransactionIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var lastClickedIndex by remember { mutableStateOf<Int?>(null) }
    var selectionAnchorIndex by remember { mutableStateOf<Int?>(null) }

    // Undo/redo state
    var undoStack by remember { mutableStateOf<List<DeletedBatch>>(emptyList()) }
    var redoStack by remember { mutableStateOf<List<DeletedBatch>>(emptyList()) }

    // Dialog state
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    val vendorCategoryMap = remember(vendors) { vendors.associate { it.id to it.categoryId } }
    val scope = rememberCoroutineScope()
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val repo = BankRepository(db)

    LaunchedEffect(Unit) {
        categories = repo.getCategories()
        vendors = repo.getVendorsWithCategories()
        transactions = repo.getTransactionsWithCategories()
    }

    val reloadCategories = {
        scope.launch {
            categories = repo.getCategories()
        }
    }

    val reloadVendors = {
        scope.launch {
            vendors = repo.getVendorsWithCategories()
        }
    }

    val reloadTransactions = {
        scope.launch {
            transactions = repo.getTransactionsWithCategories()
        }
    }

    // Undo/Redo functions
    val performUndo = {
        if (undoStack.isNotEmpty()) {
            val batch = undoStack.last()
            undoStack = undoStack.dropLast(1)
            redoStack = redoStack + batch

            // Restore transactions to database
            scope.launch {
                batch.transactions.forEach { transaction ->
                    repo.insertTransaction(Transaction(
                        id = transaction.id,
                        transactionDate = transaction.transactionDate,
                        amount = transaction.amount,
                        vendor = transaction.vendor,
                        account = transaction.account,
                        categoryId = transaction.categoryId
                    ))
                }
                reloadTransactions()
                reloadVendors()
                reloadCategories()
            }
        }
    }

    val performRedo = {
        if (redoStack.isNotEmpty()) {
            val batch = redoStack.last()
            redoStack = redoStack.dropLast(1)
            undoStack = undoStack + batch

            // Delete transactions from database
            scope.launch {
                batch.transactions.forEach { transaction ->
                    val transactionToDelete = Transaction(
                        id = transaction.id,
                        transactionDate = transaction.transactionDate,
                        amount = transaction.amount,
                        vendor = transaction.vendor,
                        account = transaction.account,
                        categoryId = transaction.categoryId
                    )
                    repo.deleteTransaction(transactionToDelete)
                }
                reloadTransactions()
                reloadVendors()
                reloadCategories()
            }
        }
    }

    // Filter transactions based on selected categories and vendors
    val filteredTransactions = remember(transactions, selectedCategoryIds, selectedVendorIds) {
        transactions.filter { trans ->
            val categoryMatches = selectedCategoryIds.isEmpty() ||
                    (trans.categoryId in selectedCategoryIds) ||
                    (trans.categoryId == null && null in selectedCategoryIds)
            val vendorMatches = selectedVendorIds.isEmpty() || (trans.vendor in selectedVendorIds)
            categoryMatches && vendorMatches
        }
    }

    Row(modifier = Modifier.safeContentPadding().fillMaxSize().onKeyEvent { keyEvent ->
        when (keyEvent.key) {
            Key.ShiftLeft, Key.ShiftRight -> {
                when (keyEvent.type) {
                    KeyEventType.KeyDown -> {
                        isShiftPressed = true
                        true
                    }
                    KeyEventType.KeyUp -> {
                        isShiftPressed = false
                        true
                    }
                    else -> false
                }
            }
            Key.CtrlLeft, Key.CtrlRight -> {
                when (keyEvent.type) {
                    KeyEventType.KeyDown -> {
                        isCtrlPressed = true
                        true
                    }
                    KeyEventType.KeyUp -> {
                        isCtrlPressed = false
                        true
                    }
                    else -> false
                }
            }
            Key.Delete, Key.Backspace -> {
                if (keyEvent.type == KeyEventType.KeyDown && selectedTransactionIds.isNotEmpty()) {
                    showDeleteConfirmation = true
                    true
                } else {
                    false
                }
            }
            Key.Z -> {
                if (keyEvent.type == KeyEventType.KeyDown && isCtrlPressed) {
                    if (isShiftPressed) {
                        // Ctrl+Shift+Z - Redo
                        performRedo()
                        true
                    } else {
                        // Ctrl+Z - Undo
                        performUndo()
                        true
                    }
                } else {
                    false
                }
            }
            Key.Y -> {
                if (keyEvent.type == KeyEventType.KeyDown && isCtrlPressed) {
                    // Ctrl+Y - Redo (alternative)
                    performRedo()
                    true
                } else {
                    false
                }
            }
            else -> false
        }
    }) {
                // Sidebar for categories
                Column(modifier = Modifier.width(200.dp).fillMaxHeight().padding(8.dp)) {
            Text("Categories", modifier = Modifier.padding(bottom = 8.dp))
            LazyColumn(modifier = Modifier.weight(1f)) {
                // "None" category (uncategorized)
                item {
                    val isSelected = null in selectedCategoryIds
                    Box(
                        modifier = Modifier.fillMaxWidth().background(if (isSelected) Color(0xFF2A2D3A) else Color.Transparent)
                            .clickable {
                                if (isShiftPressed) {
                                    // Toggle this category
                                    selectedCategoryIds = if (null in selectedCategoryIds) {
                                        selectedCategoryIds - null
                                    } else {
                                        selectedCategoryIds + null
                                    }
                                } else {
                                    // Replace selection with just this category
                                    selectedCategoryIds = setOf(null)
                                }
                            }
                    ) {
                        Text("None", color = Theme[colors][foreground].copy(alpha = 0.5f), modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp))
                    }
                }
                // Regular categories
                itemsIndexed(categories) { _, category ->
                    val isSelected = category.id in selectedCategoryIds
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).background(if (isSelected) Color(0xFF2A2D3A) else Color.Transparent),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier.weight(1f).clickable {
                                if (isShiftPressed) {
                                    // Toggle this category
                                    selectedCategoryIds = if (category.id in selectedCategoryIds) {
                                        selectedCategoryIds - category.id
                                    } else {
                                        selectedCategoryIds + category.id
                                    }
                                } else {
                                    // Replace selection with just this category
                                    selectedCategoryIds = setOf(category.id)
                                }
                            }
                        ) {
                            Text(category.name, modifier = Modifier.fillMaxWidth())
                        }
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
            val newCategoryState = remember { TextFieldState() }
            Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextField(
                    state = newCategoryState,
                    modifier = Modifier.weight(1f).background(Color(0xFF35374B))
                        .border(1.dp, Color.White, RoundedCornerShape(4.dp))
                ) {
                    TextInput(modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp))
                }
                AppButton(
                    content = "Add",
                    onClick = {
                        val name = newCategoryState.text.toString().trim()
                        if (name.isNotEmpty()) {
                            scope.launch {
                                repo.insertCategory(name)
                                newCategoryState.edit { replace(0, length, "") }
                                reloadCategories()
                            }
                        }
                    },
                    type = AppButtonVariant.CONFIRM
                )
            }
        }

        // Main area for transactions
        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
            Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                // Top row with Undo/Redo and Clear Filters
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    // Undo/Redo buttons
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        AppButton(
                            content = "Undo",
                            onClick = { performUndo() },
                            type = AppButtonVariant.CONFIRM,
                            enabled = undoStack.isNotEmpty()
                        )
                        AppButton(
                            content = "Redo",
                            onClick = { performRedo() },
                            type = AppButtonVariant.CONFIRM,
                            enabled = redoStack.isNotEmpty()
                        )
                    }

                    // Clear Filters button
                    AppButton(
                        content = "Clear Filters",
                        onClick = {
                            selectedCategoryIds = emptySet()
                            selectedVendorIds = emptySet()
                        },
                        type = AppButtonVariant.CONFIRM
                    )
                }

                // Transactions header with selection count
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Transactions")
                    if (selectedTransactionIds.isNotEmpty()) {
                        Text(
                            "${selectedTransactionIds.size} selected",
                            color = Theme[colors][green]
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().background(Color(0xFF35374B)).padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("Date", modifier = Modifier.width(120.dp), color = Color.White)
                    Text("Vendor", modifier = Modifier.width(150.dp), color = Color.White)
                    Text(
                        "Amount (SEK)",
                        modifier = Modifier.width(100.dp),
                        textAlign = TextAlign.Right,
                        color = Color.White
                    )
                    Text("Category", modifier = Modifier.width(120.dp), color = Color.White)
                    Text("Actions", modifier = Modifier.width(100.dp), color = Color.White)
                }
                LazyColumn(modifier = Modifier.weight(1f)) {
                    itemsIndexed(filteredTransactions) { index, trans ->
                        val isSelected = trans.id in selectedTransactionIds
                        val baseBgColor = if (index % 2 == 0) Color(0xFF1A1B26) else Color(0xFF35374B)
                        val bgColor = if (isSelected) Color(0xFF4A5568) else baseBgColor
                        val amountColor = if (trans.amount > 0) Theme[colors][green] else Theme[colors][red]

                        Row(
                            modifier = Modifier.fillMaxWidth().background(bgColor)
                                .padding(vertical = 4.dp, horizontal = 8.dp)
                                .clickable {
                                    // Handle selection based on modifier keys
                                    when {
                                        isShiftPressed && selectionAnchorIndex != null -> {
                                            // Range selection from anchor to current
                                            val startIndex = minOf(selectionAnchorIndex!!, index)
                                            val endIndex = maxOf(selectionAnchorIndex!!, index)
                                            val rangeIds = filteredTransactions.subList(startIndex, endIndex + 1).map { it.id }.toSet()
                                            selectedTransactionIds = rangeIds
                                            lastClickedIndex = index
                                        }
                                        isCtrlPressed -> {
                                            // Toggle this transaction
                                            selectedTransactionIds = if (trans.id in selectedTransactionIds) {
                                                selectedTransactionIds - trans.id
                                            } else {
                                                selectedTransactionIds + trans.id
                                            }
                                            // Don't update anchor for Ctrl+click
                                        }
                                        else -> {
                                            // Single selection
                                            selectedTransactionIds = setOf(trans.id)
                                            lastClickedIndex = index
                                            selectionAnchorIndex = index
                                        }
                                    }
                                },
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
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
                            Text(
                                String.format("%.2f", trans.amount / 100.0),
                                modifier = Modifier.width(100.dp),
                                textAlign = TextAlign.Right,
                                color = amountColor
                            )
                            val isAuto = trans.categoryId != null && trans.categoryId == vendorCategoryMap[trans.vendor]
                            Text(
                                "${trans.categoryName ?: "None"}${if (isAuto) " (Auto)" else ""}",
                                modifier = Modifier.width(120.dp),
                                color = if (trans.categoryName == null) Theme[colors][foreground].copy(alpha = 0.5f) else Theme[colors][foreground]
                            )
                            UnstyledButton(
                                onClick = { selectedTransactionId = trans.id },
                                modifier = Modifier.width(100.dp)
                            ) {
                                Text("Assign")
                            }
                        }
                    }
                }
            }
        }

        // Sidebar for vendors
        Column(modifier = Modifier.width(500.dp).fillMaxHeight().padding(8.dp)) {
            Text("Vendors", modifier = Modifier.padding(bottom = 8.dp))
            LazyColumn(modifier = Modifier.weight(1f)) {
                itemsIndexed(vendors) { _, vendor ->
                    val isSelected = vendor.id in selectedVendorIds
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).background(if (isSelected) Color(0xFF2A2D3A) else Color.Transparent),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.weight(1f).clickable {
                                if (isShiftPressed) {
                                    // Toggle this vendor
                                    selectedVendorIds = if (vendor.id in selectedVendorIds) {
                                        selectedVendorIds - vendor.id
                                    } else {
                                        selectedVendorIds + vendor.id
                                    }
                                } else {
                                    // Replace selection with just this vendor
                                    selectedVendorIds = setOf(vendor.id)
                                }
                            }
                        ) {
                            Text(vendor.name, modifier = Modifier.fillMaxWidth())
                        }
                        Text(
                            vendor.categoryName ?: "None",
                            modifier = Modifier.weight(1f),
                            color = if (vendor.categoryName == null) Theme[colors][foreground].copy(alpha = 0.5f) else Theme[colors][foreground]
                        )
                        UnstyledButton(onClick = {
                            selectedVendorId = vendor.id
                        }) {
                            Text("Edit")
                    }
                }
            }
        }
    }
}

            // Unified category selection overlay
            if (selectedTransactionId != null || selectedVendorId != null) {
                val modalTitle = if (selectedTransactionId != null) {
                    "Select Category for Transaction"
                } else {
                    val vendor = vendors.find { it.id == selectedVendorId }
                    "Select Category for Vendor: ${vendor?.name ?: "Unknown"}"
                }

                val onCategorySelected: suspend (Long?) -> Unit = { catId ->
                    if (selectedTransactionId != null) {
                        val trans = repo.getTransactionById(selectedTransactionId!!)
                        if (trans != null) {
                            repo.updateTransaction(trans.copy(categoryId = catId))
                            reloadTransactions()
                        }
                    } else if (selectedVendorId != null) {
                        repo.setVendorCategory(selectedVendorId!!, catId)
                        reloadVendors()
                        reloadTransactions()
                    }
                    selectedTransactionId = null
                    selectedVendorId = null
                }

                val onCancel = {
                    selectedTransactionId = null
                    selectedVendorId = null
                }

                CategorySelectionModal(
                    title = modalTitle,
                    categories = categories,
                    onCategorySelected = onCategorySelected,
                    onCancel = onCancel
                )
            }

            // Deletion confirmation modal
            if (showDeleteConfirmation) {
                DeleteConfirmationModal(
                    transactionCount = selectedTransactionIds.size,
                    onConfirm = {
                        // Perform deletion
                        val transactionsToDelete = filteredTransactions.filter { it.id in selectedTransactionIds }
                        val batch = DeletedBatch(
                            timestamp = System.currentTimeMillis(),
                            transactions = transactionsToDelete
                        )
                        undoStack = undoStack + batch

                        scope.launch {
                            transactionsToDelete.forEach { transaction ->
                                val transactionToDelete = Transaction(
                                    id = transaction.id,
                                    transactionDate = transaction.transactionDate,
                                    amount = transaction.amount,
                                    vendor = transaction.vendor,
                                    account = transaction.account,
                                    categoryId = transaction.categoryId
                                )
                                repo.deleteTransaction(transactionToDelete)
                            }
                            selectedTransactionIds = emptySet()
                            lastClickedIndex = null
                            selectionAnchorIndex = null
                            reloadTransactions()
                            reloadVendors()
                            reloadCategories()
                        }
                        showDeleteConfirmation = false
                    },
                    onCancel = {
                        showDeleteConfirmation = false
                    }
                )
            }
}

@Composable
fun DeleteConfirmationModal(
    transactionCount: Int,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.background(Color(0xFF1A1B26)).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Confirm Deletion"
            )
            Text(
                "Delete $transactionCount selected transaction${if (transactionCount != 1) "s" else ""}? " +
                "This action can be undone.",
                textAlign = TextAlign.Center
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AppButton(
                    content = "Delete",
                    onClick = onConfirm,
                    type = AppButtonVariant.DANGER
                )
                AppButton(
                    content = "Cancel",
                    onClick = onCancel,
                    type = AppButtonVariant.CONFIRM
                )
            }
        }
    }
}

@Composable
fun CategorySelectionModal(
    title: String,
    categories: List<Category>,
    onCategorySelected: suspend (Long?) -> Unit,
    onCancel: () -> Unit
) {
    val scope = rememberCoroutineScope()
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)).padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.background(Color(0xFF1A1B26)).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, modifier = Modifier.padding(bottom = 8.dp))
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 120.dp),
                modifier = Modifier.height(200.dp)
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    val interactionSource = remember { MutableInteractionSource() }
                    val isHovered by interactionSource.collectIsHoveredAsState()
                    UnstyledButton(
                        onClick = { scope.launch { onCategorySelected(null) } },
                        modifier = Modifier.hoverable(interactionSource).background(if (isHovered) Color(0xFF2A2D3A) else Color.Transparent)
                    ) {
                        Text("None", modifier = Modifier.fillMaxWidth().padding(8.dp))
                    }
                }
                items(categories.sortedBy { it.name }) { category ->
                    val interactionSource = remember { MutableInteractionSource() }
                    val isHovered by interactionSource.collectIsHoveredAsState()
                    UnstyledButton(
                        onClick = { scope.launch { onCategorySelected(category.id) } },
                        modifier = Modifier.hoverable(interactionSource).background(if (isHovered) Color(0xFF2A2D3A) else Color.Transparent)
                    ) {
                        Text(category.name, modifier = Modifier.fillMaxWidth().padding(8.dp))
                    }
                }
            }
            UnstyledButton(onClick = onCancel) {
                Text("Cancel", color = Theme[colors][red])
            }
        }
    }
}