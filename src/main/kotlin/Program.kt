import javax.swing.SwingUtilities

object Program {

    @JvmStatic
    fun main(args: Array<String>) {
        SwingUtilities.invokeLater { MainFrame().isVisible = true }
    }
}
