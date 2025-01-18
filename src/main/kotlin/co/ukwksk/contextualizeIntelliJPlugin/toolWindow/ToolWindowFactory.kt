package co.ukwksk.contextualizeIntelliJPlugin.toolWindow

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.content.ContentFactory
import java.io.File
import javax.swing.*

class ToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val mainPanel = JPanel()
        mainPanel.layout = BoxLayout(mainPanel, BoxLayout.Y_AXIS)

        val promptLabel = JLabel("Enter prompt")
        val promptArea = JTextArea(5, 50)
        val promptScrollPane = JBScrollPane(promptArea)

        val filesLabel = JLabel("Enter file paths (one per line):")
        val filesArea = JTextArea(5, 50)
        val filesScrollPane = JBScrollPane(filesArea)

        val processButton = JButton("Process Files")

        val outputLabel = JLabel("Output:")
        val outputArea = JTextArea(10, 50)
        val outputScrollPane = JBScrollPane(outputArea)
        outputArea.isEditable = false

        val copyToClipboardButton = JButton("Copy to Clipboard")
        copyToClipboardButton.addActionListener {
            outputArea.selectAll()
            outputArea.copy()
        }

        processButton.addActionListener {

            val promptText = promptArea.text

            val filesText = filesArea.text
            val lines = filesText.split("\n")

            outputArea.text = ""
            if (promptText.isNotEmpty()) {
                outputArea.append("Prompt: $promptText")
            }
            if (filesText.isNotEmpty()) {
                outputArea.append("\n\nFiles:\n")
            }

            val notFoundFiles = mutableListOf<String>()

            for (rawLine in lines) {
                val line = rawLine.trim()
                if (line.isEmpty()) {
                    // 空行は無視
                    continue
                }
                val file = resolveFile(project, line)

                if (file.exists()) {
                    val content = try {
                        file.readText()
                    } catch (e: Exception) {
                        "(binary file)"
                    }

                    outputArea.append("//#region $line\n")
                    outputArea.append(content)
                    outputArea.append("\n//#endregion\n\n")

                } else {
                    notFoundFiles.add(line)
                }
            }

            if (notFoundFiles.isNotEmpty()) {
                outputArea.append("=== Files to create ===\n")
                notFoundFiles.forEach {
                    outputArea.append("$it\n")
                }
            }
        }

        mainPanel.add(promptLabel)
        mainPanel.add(promptScrollPane)

        mainPanel.add(Box.createVerticalStrut(5))

        mainPanel.add(filesLabel)
        mainPanel.add(filesScrollPane)

        mainPanel.add(Box.createVerticalStrut(5))

        mainPanel.add(processButton)

        mainPanel.add(Box.createVerticalStrut(10))

        mainPanel.add(outputLabel)
        mainPanel.add(outputScrollPane)

        mainPanel.add(copyToClipboardButton)

        // ToolWindow の Content を生成してメインパネルを埋め込む
        val contentFactory = ContentFactory.getInstance()
        val content = contentFactory.createContent(mainPanel, "", false)
        toolWindow.contentManager.addContent(content)
    }

    private fun resolveFile(project: Project, line: String): File {
        val basePath = project.basePath ?: return File(line)
        val f = File(line)
        return if (f.isAbsolute) {
            f
        } else {
            File(basePath, line)
        }
    }
}