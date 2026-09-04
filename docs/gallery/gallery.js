// The whole script (issue #252): arrow keys follow the slide page's
// own previous/next links. The links are the interface; this only
// saves reaching for the pointer. Everything works with it absent.
document.addEventListener("keydown", function (event) {
  if (event.altKey || event.ctrlKey || event.metaKey || event.shiftKey) {
    return;
  }
  var rel = event.key === "ArrowLeft" ? "prev"
      : event.key === "ArrowRight" ? "next" : null;
  if (!rel) {
    return;
  }
  var link = document.querySelector('a[rel="' + rel + '"]');
  if (link) {
    link.click();
  }
});
