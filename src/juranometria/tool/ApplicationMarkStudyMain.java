package juranometria.tool;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.imageio.ImageIO;

import juranometria.tool.ApplicationMark.Candidate;

/**
 * The coded visual gate for the JUranometria application mark
 * (Sprint 23, issue #200).
 *
 * <p>Four compositions, drawn from one geometry, exported at every
 * size a desktop actually asks for, and measured rather than admired.
 * A beautiful 1024 px illustration that becomes an anonymous smudge
 * at 16 px fails, and the only way to know which one does that is to
 * look at 16 px - so every candidate is written at its native pixel
 * dimensions, not scaled down from a large one.
 *
 * <p>Nothing here is packaged. The gate ends with a choice.
 */
public final class ApplicationMarkStudyMain {

    private ApplicationMarkStudyMain() {
    }

    private static final File DIR = new File("docs/studies/application-mark");

    /** Every size a dock, a task switcher or a Start menu asks for. */
    private static final int[] SIZES =
            {16, 24, 32, 48, 64, 128, 256, 512, 1024};

    /** The sizes where a mark either reads or does not. */
    private static final int[] SMALL = {16, 24, 32, 48};

    public static void main(String[] args) throws IOException {
        DIR.mkdirs();

        System.out.println("# The application mark: four candidates");
        System.out.println();
        System.out.println("Measured by `make application-mark-study`."
                + " Every image is drawn at its own pixel size from"
                + " the one geometry in `ApplicationMark`, never"
                + " resampled from a larger one, so what is inspected"
                + " at 16 px is what a dock would actually draw.");
        System.out.println();

        for (Candidate candidate : Candidate.values()) {
            for (int size : SIZES) {
                ImageIO.write(render(candidate, size, -1), "png",
                        new File(DIR, name(candidate, size)));
            }
        }
        contactSheet();
        desktopGrounds();

        census();
        smallSizes();
        distinguishability();

        System.out.println("## What the images are");
        System.out.println();
        System.out.println("- `contact-sheet.png` - every candidate at"
                + " every size, each drawn at its own dimensions.");
        System.out.println("- `grounds-light.png`, `grounds-dark.png` -"
                + " the candidates at 32 and 64 px on light and dark"
                + " desktop grounds, which is where a reader meets"
                + " them. The mark itself stays white paper in both:"
                + " the atlas does not follow the desktop's theme.");
        System.out.println("- `<candidate>-<size>.png` - the native"
                + " exports, for inspection at their own size.");
        System.out.println();
        System.out.println("A recommendation is written in"
                + " [the decision](../../decisions/application-mark.md)."
                + " **The choice is the owner's**; this issue ends"
                + " there, and #202 carries the chosen geometry into"
                + " the platforms.");
    }

    private static String name(Candidate candidate, int size) {
        return candidate.name().toLowerCase(Locale.ROOT) + "-" + size + ".png";
    }

    private static BufferedImage render(Candidate candidate, int size,
                                        int omitDot) {
        BufferedImage image = new BufferedImage(size, size,
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        try {
            ApplicationMark.paint(g, candidate, size, omitDot);
        } finally {
            g.dispose();
        }
        return image;
    }

    // ------------------------------------------------------------------
    // What each candidate is made of.

    private static void census() {
        System.out.println("## The four candidates");
        System.out.println();
        System.out.println("| mark | what it is | galaxies | stars |");
        System.out.println("|---|---|---:|---:|");
        for (Candidate candidate : Candidate.values()) {
            System.out.printf(Locale.ROOT, "| **%s** | %s | %d | %d |%n",
                    candidate.label(), candidate.prose(),
                    ApplicationMark.galaxies(candidate, 1024).size(),
                    ApplicationMark.dots(candidate).size());
        }
        System.out.println();

        System.out.println("### Silhouette and clearance");
        System.out.println();
        System.out.println("**Occupied area** is the share of the"
                + " card the mark's ink covers - too little and the"
                + " icon is a white square, too much and it is a"
                + " blot. **Edge clearance** is the smallest gap"
                + " between ink and the card's border, in pixels at"
                + " 1024, and a cropped ellipse deliberately has"
                + " none: it is meant to leave the frame.");
        System.out.println();
        System.out.println("| mark | occupied area | edge clearance |"
                + " ellipse leaves the frame |");
        System.out.println("|---|---:|---:|---|");
        for (Candidate candidate : Candidate.values()) {
            BufferedImage large = render(candidate, 1024, -1);
            System.out.printf(Locale.ROOT, "| %s | %.1f%% | %s | %s |%n",
                    candidate.label(), 100.0 * inkFraction(large),
                    clearance(candidate),
                    leavesFrame(candidate) ? "**yes**" : "no");
        }
        System.out.println();
    }

    /** The share of the card that carries ink rather than paper. */
    private static double inkFraction(BufferedImage image) {
        long card = 0;
        long ink = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int argb = image.getRGB(x, y);
                if ((argb >>> 24) < 128) {
                    continue;
                }
                card++;
                if ((argb & 0xffffff) != 0xffffff) {
                    ink++;
                }
            }
        }
        return card == 0 ? 0.0 : (double) ink / card;
    }

    /** Whether a galaxy crosses the card's own edge. */
    private static boolean leavesFrame(Candidate candidate) {
        java.awt.geom.Area card = new java.awt.geom.Area(
                ApplicationMark.card(1024));
        for (java.awt.Shape galaxy
                : ApplicationMark.galaxies(candidate, 1024)) {
            java.awt.geom.Area outside = new java.awt.geom.Area(galaxy);
            outside.subtract(card);
            if (!outside.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private static String clearance(Candidate candidate) {
        if (leavesFrame(candidate)) {
            return "none, by design";
        }
        java.awt.geom.Rectangle2D ink =
                ApplicationMark.inkArea(candidate, 1024).getBounds2D();
        java.awt.geom.Rectangle2D card =
                ApplicationMark.card(1024).getBounds2D();
        double gap = Math.min(
                Math.min(ink.getMinX() - card.getMinX(),
                        card.getMaxX() - ink.getMaxX()),
                Math.min(ink.getMinY() - card.getMinY(),
                        card.getMaxY() - ink.getMaxY()));
        return String.format(Locale.ROOT, "%.0f px", gap);
    }

    // ------------------------------------------------------------------
    // Whether it survives the sizes that matter.

    private static void smallSizes() {
        System.out.println("## At the sizes that decide it");
        System.out.println();
        System.out.println("**Surviving stars** is measured, not"
                + " counted: each dot is left out of a second"
                + " rendering at the same size, and a dot survives"
                + " when leaving it out changes the image. A dot that"
                + " changes nothing is a dot the reader does not"
                + " have.");
        System.out.println();
        System.out.println("**Ink islands** counts the separate pieces"
                + " of ink a reader can see. A mark whose ellipse"
                + " stays continuous reads as one shape plus its"
                + " stars; one that breaks up reads as more, and"
                + " looks like scattered dirt at small sizes.");
        System.out.println();
        System.out.print("| mark |");
        for (int size : SMALL) {
            System.out.print(" " + size + " px stars |");
        }
        for (int size : SMALL) {
            System.out.print(" " + size + " px islands |");
        }
        System.out.println();
        System.out.print("|---|");
        for (int i = 0; i < SMALL.length * 2; i++) {
            System.out.print("---:|");
        }
        System.out.println();
        for (Candidate candidate : Candidate.values()) {
            StringBuilder row = new StringBuilder();
            row.append("| ").append(candidate.label()).append(" |");
            int total = ApplicationMark.dots(candidate).size();
            for (int size : SMALL) {
                row.append(' ').append(survivingStars(candidate, size))
                        .append(" of ").append(total).append(" |");
            }
            for (int size : SMALL) {
                row.append(' ').append(inkIslands(render(candidate, size, -1)))
                        .append(" |");
            }
            System.out.println(row);
        }
        System.out.println();
    }

    private static int survivingStars(Candidate candidate, int size) {
        BufferedImage all = render(candidate, size, -1);
        int survivors = 0;
        for (int i = 0; i < ApplicationMark.dots(candidate).size(); i++) {
            BufferedImage without = render(candidate, size, i);
            if (differs(all, without)) {
                survivors++;
            }
        }
        return survivors;
    }

    private static boolean differs(BufferedImage a, BufferedImage b) {
        for (int y = 0; y < a.getHeight(); y++) {
            for (int x = 0; x < a.getWidth(); x++) {
                if (a.getRGB(x, y) != b.getRGB(x, y)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Connected pieces of ink, four-connected, ignoring the frame. */
    private static int inkIslands(BufferedImage image) {
        int w = image.getWidth();
        int h = image.getHeight();
        boolean[] ink = new boolean[w * h];
        // The card's own border is ink and is not part of the
        // drawing, so it is masked out by the card's own shape - not
        // by a rectangle, which would slice a cropped ellipse into
        // pieces and count the study's own margin as fragmentation.
        java.awt.Shape card = ApplicationMark.card(w);
        float border = (float) Math.max(1.0, w / 128.0);
        java.awt.geom.Area inside = new java.awt.geom.Area(card);
        inside.subtract(new java.awt.geom.Area(
                new java.awt.BasicStroke(border * 3f)
                        .createStrokedShape(card)));
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (!inside.contains(x + 0.5, y + 0.5)) {
                    continue;
                }
                int argb = image.getRGB(x, y);
                ink[y * w + x] = (argb >>> 24) >= 128
                        && (argb & 0xff) < 200;
            }
        }
        boolean[] seen = new boolean[w * h];
        int islands = 0;
        int[] stack = new int[w * h];
        for (int i = 0; i < ink.length; i++) {
            if (!ink[i] || seen[i]) {
                continue;
            }
            islands++;
            int top = 0;
            stack[top++] = i;
            seen[i] = true;
            while (top > 0) {
                int at = stack[--top];
                int x = at % w;
                int y = at / w;
                for (int[] step : new int[][] {{1, 0}, {-1, 0}, {0, 1},
                        {0, -1}}) {
                    int nx = x + step[0];
                    int ny = y + step[1];
                    if (nx < 0 || ny < 0 || nx >= w || ny >= h) {
                        continue;
                    }
                    int next = ny * w + nx;
                    if (ink[next] && !seen[next]) {
                        seen[next] = true;
                        stack[top++] = next;
                    }
                }
            }
        }
        return islands;
    }

    // ------------------------------------------------------------------
    // Whether it is this application's mark rather than any.

    private static void distinguishability() {
        System.out.println("## Told apart from what it replaces");
        System.out.println();
        System.out.println("At 16 px, against the mark the atlas"
                + " ships today (the Tabler north-star in a rounded"
                + " square) and against a bare card - the shape a"
                + " reader sees when an application has no identity"
                + " at all. The figure is the share of the card's"
                + " pixels that differ.");
        System.out.println();
        System.out.println("| mark | unlike a bare card | unlike"
                + " today's north star |");
        System.out.println("|---|---:|---:|");
        BufferedImage bare = bareCard(16);
        BufferedImage today = northStar(16);
        for (Candidate candidate : Candidate.values()) {
            BufferedImage mark = render(candidate, 16, -1);
            System.out.printf(Locale.ROOT, "| %s | %.1f%% | %.1f%% |%n",
                    candidate.label(),
                    100.0 * differenceFraction(mark, bare),
                    100.0 * differenceFraction(mark, today));
        }
        System.out.println();
    }

    private static double differenceFraction(BufferedImage a,
                                             BufferedImage b) {
        long counted = 0;
        long different = 0;
        for (int y = 0; y < a.getHeight(); y++) {
            for (int x = 0; x < a.getWidth(); x++) {
                if ((a.getRGB(x, y) >>> 24) < 128) {
                    continue;
                }
                counted++;
                if (a.getRGB(x, y) != b.getRGB(x, y)) {
                    different++;
                }
            }
        }
        return counted == 0 ? 0.0 : (double) different / counted;
    }

    private static BufferedImage bareCard(int size) {
        BufferedImage image = new BufferedImage(size, size,
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(ApplicationMark.PAPER);
            g.fill(ApplicationMark.card(size));
            g.setColor(ApplicationMark.FRAME);
            g.setStroke(new java.awt.BasicStroke(
                    (float) Math.max(1.0, size / 128.0)));
            g.draw(ApplicationMark.card(size));
        } finally {
            g.dispose();
        }
        return image;
    }

    /** Today's mark, drawn the way packaging/icon/IconGen draws it. */
    private static BufferedImage northStar(int size) {
        BufferedImage image = bareCard(size);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            double pad = size * 0.20;
            double s = (size - 2 * pad) / 24.0;
            g.translate(pad, pad);
            g.setColor(ApplicationMark.INK);
            g.setStroke(new java.awt.BasicStroke((float) (2.0 * s),
                    java.awt.BasicStroke.CAP_ROUND,
                    java.awt.BasicStroke.JOIN_ROUND));
            g.draw(new java.awt.geom.Line2D.Double(3 * s, 12 * s, 21 * s, 12 * s));
            g.draw(new java.awt.geom.Line2D.Double(12 * s, 21 * s, 12 * s, 3 * s));
            g.draw(new java.awt.geom.Line2D.Double(7.5 * s, 7.5 * s, 16.5 * s, 16.5 * s));
            g.draw(new java.awt.geom.Line2D.Double(7.5 * s, 16.5 * s, 16.5 * s, 7.5 * s));
        } finally {
            g.dispose();
        }
        return image;
    }

    // ------------------------------------------------------------------
    // The sheets a reader looks at.

    private static void contactSheet() throws IOException {
        int gap = 16;
        int labelWidth = 120;
        int rowHeight = 0;
        for (int size : SIZES) {
            rowHeight = Math.max(rowHeight, size);
        }
        // 1024 in its own row would dwarf the sheet; the sheet shows
        // every size a desktop asks for up to 256, and the large
        // exports stand on their own.
        int[] shown = {16, 24, 32, 48, 64, 128, 256};
        int width = labelWidth;
        for (int size : shown) {
            width += size + gap;
        }
        int height = gap;
        for (Candidate ignored : Candidate.values()) {
            height += 256 + gap;
        }
        BufferedImage sheet = new BufferedImage(width, height,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = sheet.createGraphics();
        try {
            g.setColor(new Color(245, 245, 245));
            g.fillRect(0, 0, width, height);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int y = gap;
            for (Candidate candidate : Candidate.values()) {
                g.setColor(new Color(51, 51, 51));
                g.drawString(candidate.label(), 8, y + 20);
                int x = labelWidth;
                for (int size : shown) {
                    g.drawImage(render(candidate, size, -1), x,
                            y + (256 - size) / 2, null);
                    x += size + gap;
                }
                y += 256 + gap;
            }
        } finally {
            g.dispose();
        }
        ImageIO.write(sheet, "png", new File(DIR, "contact-sheet.png"));
    }

    /**
     * The candidates where a reader meets them: on a desktop, at the
     * sizes a dock and a task switcher use. The grounds stand in for
     * macOS, Windows and Linux surroundings; the mark itself is the
     * same white paper on all of them, because the atlas's palette
     * does not follow the desktop's theme.
     */
    private static void desktopGrounds() throws IOException {
        Map<String, Color> grounds = new LinkedHashMap<>();
        grounds.put("light", new Color(236, 236, 236));
        grounds.put("dark", new Color(28, 28, 30));
        for (Map.Entry<String, Color> ground : grounds.entrySet()) {
            int[] sizes = {32, 64};
            int gap = 24;
            int width = gap;
            for (Candidate ignored : Candidate.values()) {
                width += 64 + gap;
            }
            int height = gap;
            for (int size : sizes) {
                height += size + gap;
            }
            BufferedImage image = new BufferedImage(width, height,
                    BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics();
            try {
                g.setColor(ground.getValue());
                g.fillRect(0, 0, width, height);
                int y = gap;
                for (int size : sizes) {
                    int x = gap;
                    for (Candidate candidate : Candidate.values()) {
                        g.drawImage(render(candidate, size, -1),
                                x + (64 - size) / 2, y, null);
                        x += 64 + gap;
                    }
                    y += size + gap;
                }
            } finally {
                g.dispose();
            }
            ImageIO.write(image, "png",
                    new File(DIR, "grounds-" + ground.getKey() + ".png"));
        }
    }
}
