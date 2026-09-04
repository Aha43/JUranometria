# Codex review: Sprint 27 chart-status column

Reviewed PR #265 at `401fae8` against issue #257. The compact vocabulary is
clear, preserves all five renderer-derived meanings, and now sorts by the
`PageVisibility` state rather than by display spelling. Moving the labels into
the enum also removed a real stale-string comparison from the Sprint 24
journey. The default-width and enlarged-text evidence keep the existing rule:
fit when meaning fits, scroll when it does not.

## [P1] Reapply widths by model identity after columns are reordered

`OnThisPageTable.sizeColumns()` computes widths for Object, Mag, From and
Chart, but `setColumn(0..3, ...)` applies them to the columns currently at
those **view positions**. After a reader drags Chart to the front, the next
page change calls `sizeColumns()` and gives Chart the Object width, Object the
Mag width, Mag the From width and From the Chart width. A component resize
takes the same path. The new reordering test stops immediately after the drag,
before either production event reapplies the widths, so it cannot see the
defect.

Resolve each width by model index (for example through the current model-to-
view conversion or by finding the `TableColumn.getModelIndex()`), and extend
the reordered-column test through a real `pageChanged` or resize before
asserting that every model column still has room for the content whose policy
it owns. Reader-controlled reordering must survive the normal events that
follow it, not only the instant of the drag.

## [P1] Attach the full question to the Chart header cell, not the whole header

The tooltip correctly finds the column whose model index is 3, but the
accessible description is installed on the `JTableHeader` itself. That makes
“Whether and why this object is drawn on the chart” the description of the
entire four-column header rather than of Chart; the accessible Chart header
entry has no corresponding description, and the parent description remains
even if Chart is moved or absent. The test repeats that mismatch by reading
only `table.getTableHeader().getAccessibleContext()`.

Carry the complete question on the accessible representation of the Chart
header cell, keyed by model identity so reordering cannot detach it. Test the
accessible child or header-renderer component for Chart after moving it, and
also require a different header such as Object not to inherit Chart's
description. This keeps the compact visual word from becoming private while
avoiding a screen reader being told that every column answers the visibility
question.
