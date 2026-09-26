package juranometria.ui.placeandtime;

import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.time.Instant;
import java.util.prefs.Preferences;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import juranometria.meridian.MeridianModule;
import juranometria.sky.Observer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What Place and Time's size declaration means (#380).
 *
 * <p>The capture coordinator treats a policy's declared content size
 * as authoritative and cannot check it: on an unshown macOS window
 * the policy's own {@code setSize} is read back already rolled back.
 * So the declaration's truth is proved here, where it is defined:
 * the packed preference, with the width raised to the reviewed
 * {@link PlaceAndTimeDialog#ORDINARY_WIDTH} floor.
 *
 * <p>Every expected value comes from the content pane's own layout
 * preference and the window's insets - never from the window's size
 * and never through the coordinator, which would force the declared
 * size and make any declaration look right.
 */
class PlaceAndTimeSizeDeclarationTest {

    private static final Instant WHEN =
            Instant.parse("2026-03-20T21:00:00Z");

    @Test
    void theFloorRaisesANarrowPackedWidthAndTheHeightIsThePackedHeight()
            throws Exception {
        declared(null, (dialog, chrome, preferred, said) -> {
            assertTrue(preferred.width + chrome.left + chrome.right
                            < PlaceAndTimeDialog.ORDINARY_WIDTH,
                    "the premise: this dialog packs narrower than its"
                            + " floor, so the floor is what decides");
            assertEquals(PlaceAndTimeDialog.ORDINARY_WIDTH
                            - chrome.left - chrome.right, said.width,
                    "the width is the 420 floor, less the window's"
                            + " sides");
            assertEquals(preferred.height, said.height,
                    "the height is the packed height: the content's"
                            + " own preference");
        });
    }

    @Test
    void aPackedWidthAboveTheFloorIsKept() throws Exception {
        declared(new Dimension(500, 300),
                (dialog, chrome, preferred, said) -> {
            assertEquals(new Dimension(500, 300), said,
                    "a content that prefers more than the floor keeps"
                            + " its packed width and height");
        });
    }

    @Test
    void aNarrowContentIsRaisedToTheFloorAndKeepsItsHeight()
            throws Exception {
        declared(new Dimension(200, 150),
                (dialog, chrome, preferred, said) -> {
            assertEquals(new Dimension(PlaceAndTimeDialog.ORDINARY_WIDTH
                            - chrome.left - chrome.right, 150), said,
                    "the floor raises the width and leaves the packed"
                            + " height alone");
        });
    }

    /** The checks, given the dialog and what it declared. */
    private interface Check {
        void on(PlaceAndTimeDialog dialog, Insets chrome,
                Dimension preferred, Dimension said);
    }

    private static void declared(Dimension contentPrefers, Check check)
            throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
                "a dialog needs a display");
        Preferences node = Preferences.userRoot()
                .node("juranometria-declaration-" + System.nanoTime());
        JFrame[] owner = new JFrame[1];
        PlaceAndTimeDialog[] dialog = new PlaceAndTimeDialog[1];
        try {
            SwingUtilities.invokeAndWait(() -> {
                MeridianModule module = new MeridianModule(
                        new Observer(59.913, 10.752, WHEN));
                owner[0] = new JFrame("declaration");
                dialog[0] = PlaceAndTimeDialog.packedForStudy(owner[0],
                        module, PlaceStore.forNode(node),
                        juranometria.ui.language.InterfaceText
                                .forLanguage("en"));
                JComponent content =
                        (JComponent) dialog[0].getContentPane();
                if (contentPrefers != null) {
                    content.setPreferredSize(contentPrefers);
                }
                dialog[0].addNotify();
                Dimension said = dialog[0].sizePolicyContent();
                check.on(dialog[0], dialog[0].getInsets(),
                        content.getPreferredSize(), said);
            });
        } finally {
            SwingUtilities.invokeAndWait(() -> {
                if (dialog[0] != null) {
                    dialog[0].dispose();
                }
                if (owner[0] != null) {
                    owner[0].dispose();
                }
            });
            node.removeNode();
        }
    }
}
