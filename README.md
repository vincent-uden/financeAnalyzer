# financeAnalyzer

A desktop application for personal finance management. Import your bank statements, categorize transactions, and get insights into your spending and income patterns.

## Features

- **Import Bank Statements**: Easily import transactions from Handelsbanken Excel files (.xlsx)
- **Transaction Categorization**: Organize expenses and income into custom categories
- **Vendor Management**: Set default categories for recurring merchants
- **Interactive Dashboard**: View monthly breakdowns with visual charts
- **Account Tracking**: Monitor balances across multiple accounts
- **Undo/Redo**: Full support for reverting changes

## Getting Started

### Requirements
- Windows, macOS, or Linux
- Java 11 or higher

### Installation
1. Download the latest release from the releases page
2. Run the executable file for your platform
3. The app will start with an empty database

### First Use
1. Click the "XLSX" tab to import your first bank statement
2. Select your Handelsbanken Excel file
3. Review and confirm the imported transactions
4. Use the "Categories" tab to organize your transactions
5. Switch to the main dashboard to view your financial overview

---

## For Developers

### Technology Stack
- Kotlin Multiplatform 2.3.0
- Compose Multiplatform 1.9.3
- Room database 2.8.4
- Apache POI 5.2.5 (Excel parsing)

### Building from Source
There are some pre-configured gradle actions that can be used from IntelliJ. If running from the terminal some sample commands are listed below.
```bash
# Clone the repository
git clone https://github.com/vincent-uden/financeAnalyzer
cd financeAnalyzer

# Build the application
./gradlew build

# Run in development mode
./gradlew composeApp:run

# Run tests
./gradlew composeApp:jvmTest
```

### Architecture
The app uses a multiplatform structure with shared database and business logic in case more platforms than Desktop would ever be supported. The UI is built with Compose Unstyled components, focusing on custom design over Material UI.
