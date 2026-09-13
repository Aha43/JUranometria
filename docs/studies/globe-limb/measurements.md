# Whether the globe draws its own limb

The disc's outline breaks up as the globe turns, and the cause is not clipping:
no limb is drawn at all. What reads as an outline is assembled from whatever
sky ink ends at the clip, so rotating the sphere moves the gaps around.

Three were weighed: no limb at all, one at 25% of the grid's strength, and one at 35%.

A quarter was chosen. Both candidates closed the outline completely, so the
question was hierarchy rather than coverage: a quarter is the strength the grid
itself reaches at the edge, so the boundary and the last parallel beside it weigh
the same, and a third begins to rebuild the rim that fading the grid removed.

Whether a reader should be able to set this is left open, and deliberately not
built: one successful request to adjust a weight is not evidence that readers
want a control over it, and a preference costs a setting, a stored value and a
second appearance to hold to forever. It is recorded here as a possible
refinement, to be reopened if the need turns up rather than because it could.

Drawn after the sky rather than before it, because a boundary a constellation
figure can break is not a boundary - and because an outline assembled out of
line ends is the very thing being replaced. It is furniture: it says where the
visible hemisphere stops, which is a fact about the page and not about the sky.

How much of each limb carries ink and how heavy the circle is against the
paper are in docs/studies/globe-limb/platform.md. The candidates themselves are
written to build/globe-study/limb.
