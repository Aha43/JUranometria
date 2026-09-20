package juranometria.ui.language;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.UIDefaults;

/**
 * The words the toolkit puts around the atlas's own dialogs
 * (Sprint 33, issue #350).
 *
 * <p>Every sentence in the export and failure dialogs is the atlas's
 * and has been translated. The frame around them was not: the
 * buttons a reader presses to answer, and every label, hover and
 * spoken name in the file chooser, come from Swing's own resource
 * bundle - resolved against {@code Locale.getDefault()}, which is the
 * <em>operating system</em> and not the language the reader chose.
 *
 * <p>So a Norwegian reader was asked <em>"Erstatt orion.svg i …?"</em>
 * and answered it with <strong>Yes</strong> or <strong>No</strong>.
 * Worse on inspection: the JDK ships no Norwegian bundle for Swing at
 * all, so that happened on <em>every</em> operating system, while a
 * reader on a German desktop got German buttons under Norwegian
 * sentences. One surface, two or three languages.
 *
 * <p><strong>English is installed too.</strong> An English interface
 * on a German desktop had the same defect in the other direction.
 * This is not a promise to reproduce the platform bundle's wording -
 * it is the atlas saying what it says, in the language its reader
 * asked for, on any machine.
 *
 * <p><strong>Forty-one keys, and not one of them is in the defaults
 * table.</strong> Swing adds a {@code ResourceBundle} to
 * {@code UIDefaults} and resolves these through it, so there is
 * nothing to read back and nothing to restore: the installer
 * {@code put}s explicit values, and a test that wants the old
 * behaviour has to remove them again. The list is exhaustive for the
 * surfaces the atlas shows and was built by probing each key under
 * the application's own look and feel <em>and</em> by building each
 * dialog and walking it - which is how the six hover/spoken pairs
 * were found, where a chooser button says one thing to a pointer and
 * another to a screen reader.
 *
 * <p><strong>One of the forty-one is not a lookup.</strong>
 * {@code FileChooser.other.newFolder} is copied into a
 * {@code static final} field when {@code FileSystemView} initialises -
 * which the first {@code JFileChooser} anywhere in the JVM causes -
 * and every folder created in that JVM is named from that copy.
 * Installing after that instant is ignored, silently. So these words
 * go in at startup, before anything can ask for a chooser, and
 * {@code SwingTextTest} asks the question in child processes because
 * a shared JVM has already answered it.
 *
 * <p><strong>Access keys are not here.</strong> Swing's
 * {@code …Mnemonic} entries belong with the eighteen access letters
 * already deferred, and installing some of them would make that
 * surface half-owned.
 */
public final class SwingText {

    /**
     * Toolkit key to pack key, in the order a reader meets them.
     *
     * <p>The pack key mirrors the toolkit key deliberately, so the
     * mapping can be checked by eye rather than by reading code.
     */
    private static final Map<String, String> KEYS = new LinkedHashMap<>();

    static {
        // The option panes: the atlas asks, and the reader answers.
        put("OptionPane.okButtonText", "swing.optionPane.ok");
        put("OptionPane.cancelButtonText", "swing.optionPane.cancel");
        put("OptionPane.yesButtonText", "swing.optionPane.yes");
        put("OptionPane.noButtonText", "swing.optionPane.no");
        put("OptionPane.titleText", "swing.optionPane.title");
        put("OptionPane.messageDialogTitle",
                "swing.optionPane.messageTitle");
        put("OptionPane.inputDialogTitle", "swing.optionPane.inputTitle");

        // The file chooser: where an exported sheet is put.
        put("FileChooser.saveButtonText", "swing.fileChooser.save");
        put("FileChooser.openButtonText", "swing.fileChooser.open");
        put("FileChooser.cancelButtonText", "swing.fileChooser.cancel");
        put("FileChooser.updateButtonText", "swing.fileChooser.update");
        put("FileChooser.helpButtonText", "swing.fileChooser.help");
        put("FileChooser.saveButtonToolTipText",
                "swing.fileChooser.save.hover");
        put("FileChooser.openButtonToolTipText",
                "swing.fileChooser.open.hover");
        put("FileChooser.cancelButtonToolTipText",
                "swing.fileChooser.cancel.hover");
        put("FileChooser.updateButtonToolTipText",
                "swing.fileChooser.update.hover");
        put("FileChooser.helpButtonToolTipText",
                "swing.fileChooser.help.hover");
        put("FileChooser.saveDialogTitleText",
                "swing.fileChooser.saveTitle");
        put("FileChooser.openDialogTitleText",
                "swing.fileChooser.openTitle");
        put("FileChooser.lookInLabelText", "swing.fileChooser.lookIn");
        put("FileChooser.saveInLabelText", "swing.fileChooser.saveIn");
        put("FileChooser.fileNameLabelText", "swing.fileChooser.fileName");
        put("FileChooser.filesOfTypeLabelText",
                "swing.fileChooser.filesOfType");
        // Six pairs: a hover for a pointer, a name for a screen
        // reader, and they are not the same words.
        put("FileChooser.upFolderToolTipText",
                "swing.fileChooser.upFolder.hover");
        put("FileChooser.upFolderAccessibleName",
                "swing.fileChooser.upFolder.a11y");
        put("FileChooser.homeFolderToolTipText",
                "swing.fileChooser.homeFolder.hover");
        put("FileChooser.homeFolderAccessibleName",
                "swing.fileChooser.homeFolder.a11y");
        put("FileChooser.newFolderToolTipText",
                "swing.fileChooser.newFolder.hover");
        put("FileChooser.newFolderAccessibleName",
                "swing.fileChooser.newFolder.a11y");
        put("FileChooser.listViewButtonToolTipText",
                "swing.fileChooser.listView.hover");
        put("FileChooser.listViewButtonAccessibleName",
                "swing.fileChooser.listView.a11y");
        put("FileChooser.detailsViewButtonToolTipText",
                "swing.fileChooser.detailsView.hover");
        put("FileChooser.detailsViewButtonAccessibleName",
                "swing.fileChooser.detailsView.a11y");
        put("FileChooser.fileNameHeaderText",
                "swing.fileChooser.header.name");
        put("FileChooser.fileSizeHeaderText",
                "swing.fileChooser.header.size");
        put("FileChooser.fileTypeHeaderText",
                "swing.fileChooser.header.type");
        put("FileChooser.fileDateHeaderText",
                "swing.fileChooser.header.modified");
        put("FileChooser.acceptAllFileFilterText",
                "swing.fileChooser.allFiles");
        put("FileChooser.directoryOpenButtonText",
                "swing.fileChooser.openDirectory");
        put("FileChooser.newFolderErrorText",
                "swing.fileChooser.newFolder.failed");
        put("FileChooser.other.newFolder",
                "swing.fileChooser.newFolder.name");
    }

    private static void put(String toolkitKey, String packKey) {
        KEYS.put(toolkitKey, packKey);
    }

    private final InterfaceText said;

    private SwingText(InterfaceText said) {
        this.said = said;
    }

    /** The toolkit's words in a language a caller states. */
    public static SwingText in(InterfaceText said) {
        if (said == null) {
            throw new IllegalArgumentException(
                    "the toolkit says what it says in some language");
        }
        return new SwingText(said);
    }

    /** Every toolkit key this claims, in the order a reader meets them. */
    public static List<String> toolkitKeys() {
        return List.copyOf(KEYS.keySet());
    }

    /** The pack key a toolkit key is answered from. */
    public static String packKeyFor(String toolkitKey) {
        return KEYS.get(toolkitKey);
    }

    /**
     * Writes every claimed word into a defaults table.
     *
     * <p>Takes the table rather than reaching for {@code UIManager},
     * so a test can hand it a fresh one and read the result back
     * without touching anything the rest of the JVM can see.
     *
     * <p><strong>Call this after every look and feel change.</strong>
     * Installing a look and feel replaces the defaults table, and
     * every value written here goes with it. The application's
     * coordinator pairs the two; nothing here can enforce that, which
     * is why a contract does.
     */
    public void installInto(UIDefaults defaults) {
        if (defaults == null) {
            throw new IllegalArgumentException(
                    "there is nowhere to install these words");
        }
        for (Map.Entry<String, String> entry : KEYS.entrySet()) {
            defaults.put(entry.getKey(), required(entry.getValue()));
        }
    }

    /** What this language says for one toolkit key. */
    public String say(String toolkitKey) {
        String packKey = KEYS.get(toolkitKey);
        if (packKey == null) {
            throw new IllegalStateException("the atlas claims no word"
                    + " for the toolkit key \"" + toolkitKey + "\"."
                    + " Claiming one it has not inventoried is how a"
                    + " dialog ends up half translated.");
        }
        return required(packKey);
    }

    /** Every word this language installs, for a report to list. */
    public List<String> words() {
        List<String> said = new ArrayList<>();
        for (String packKey : KEYS.values()) {
            said.add(required(packKey));
        }
        return said;
    }

    /**
     * A value the pack must carry.
     *
     * <p>These replace words the toolkit would otherwise have
     * supplied, so a gap does not degrade to English - it degrades to
     * the key itself appearing on a button. The set is closed and a
     * test walks it.
     */
    private String required(String packKey) {
        String value = said.say(packKey);
        if (value == null || value.isBlank() || value.equals(packKey)) {
            throw new IllegalStateException("no word for \"" + packKey
                    + "\". These are installed over the toolkit's own,"
                    + " so a missing one does not fall back to"
                    + " English - it puts a resource key on a button.");
        }
        return value;
    }
}
