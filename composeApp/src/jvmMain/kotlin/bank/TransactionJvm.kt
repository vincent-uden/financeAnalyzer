package bank

import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

actual fun pickXlsxFile(): String? {
    val fileChooser = JFileChooser()
    fileChooser.fileFilter = FileNameExtensionFilter("Excel Files", "xlsx")
    val result = fileChooser.showOpenDialog(null)
    if (result == JFileChooser.APPROVE_OPTION) {
        return fileChooser.selectedFile.absolutePath
    }
    return null
}