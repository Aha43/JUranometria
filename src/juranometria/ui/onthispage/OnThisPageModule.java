package juranometria.ui.onthispage;

import java.util.ArrayList;
import java.util.List;

import juranometria.module.ChartModule;
import juranometria.module.ChartServices;
import juranometria.module.InkRole;
import juranometria.module.OverlayContribution;
import juranometria.page.PageContents;
import juranometria.page.PageEntry;
import juranometria.page.PageVisibility;

/**
 * The <strong>On this page</strong> module (Sprint 24, issue #216).
 *
 * <p>The first module built on the seam #215 opened: it reads the
 * page inventory, keeps the reader's working marks, shows them as a
 * table, and offers a cross for each marked object the page does not
 * already draw. It owns its own panel, its own state and its own
 * lifecycle, and removing it removes the feature and nothing else.
 *
 * <p>What it deliberately does not do: move the chart on its own,
 * paint anything, invent a symbol, or remember anything between
 * sessions.
 */
public final class OnThisPageModule implements ChartModule {

    /** The name its contributed geometry is owned under. */
    public static final String ID = "on-this-page";

    private ChartServices services;
    private OnThisPageTable table;
    private final List<Runnable> released = new ArrayList<>();

    @Override
    public String name() {
        return ID;
    }

    @Override
    public void attach(ChartServices services) {
        if (this.services != null) {
            throw new IllegalStateException("already attached");
        }
        this.services = services;
        this.table = new OnThisPageTable(services);
        released.add(services.contribute(ID, this::crosses));
        released.add(services.onPageChange(this::pageChanged));
    }

    @Override
    public void detach() {
        for (Runnable release : released) {
            release.run();
        }
        released.clear();
        if (table != null) {
            table.release();
        }
        table = null;
        services = null;
    }

    /** The panel a window puts beside the chart. */
    public OnThisPageTable panel() {
        return table;
    }

    /**
     * A cross for every selected object on the page that the page
     * does not draw (issue #261's generalised Sprint 24 rule).
     *
     * <p>Only those. A drawn member wears the chart's own selection
     * ring, and adding a cross over it would say the reader had
     * selected something other than the thing they can see - two
     * marks for one object, in a vocabulary the atlas has not
     * decided. An off-page member contributes nothing and stays in
     * the set: navigation never edits membership, so the crosses
     * are filtered here, per page, rather than by pruning anything.
     *
     * <p>The identity is the catalogue's own, so the chart can
     * recognise the lead and give it the selection treatment without
     * being told what a lead is.
     */
    List<OverlayContribution> crosses() {
        if (services == null) {
            return List.of();
        }
        PageContents page = services.inventory();
        List<OverlayContribution> geometry = new ArrayList<>();
        for (String identity : services.workingSelection().members()) {
            PageEntry entry = page.find(identity).orElse(null);
            if (entry == null || entry.visibility() == PageVisibility.DRAWN) {
                continue;
            }
            geometry.add(new OverlayContribution.Point(entry.identity(),
                    "working mark on " + entry.identity() + ", "
                            + entry.visibility().prose(),
                    entry.position(), InkRole.INTERACTION));
        }
        return geometry;
    }

    /**
     * A new page: the table follows it. The selection does not -
     * navigation never mutates the set (#258) - so the rows the new
     * page holds show their membership and the rest of the set waits
     * off-page, in the Inspector's working-set section.
     */
    private void pageChanged(PageContents page) {
        if (table != null) {
            table.pageChanged(page);
        }
    }
}
