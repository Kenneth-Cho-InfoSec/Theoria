const focusImage = document.querySelector("#screen-focus");
const focusCaption = document.querySelector("#screen-caption");
const screenChoices = [...document.querySelectorAll("[data-screen-choice]")];

if (focusImage && focusCaption && screenChoices.length) {
  for (const choice of screenChoices) {
    choice.addEventListener("click", (event) => {
      if (event.metaKey || event.ctrlKey || event.shiftKey || event.altKey) return;
      event.preventDefault();

      focusImage.src = choice.dataset.imageSrc || choice.href;
      focusImage.alt = choice.dataset.alt || "Theoria Android interface screenshot.";
      focusCaption.textContent = choice.dataset.caption || "Theoria interface screen.";

      for (const otherChoice of screenChoices) {
        const selected = otherChoice === choice;
        otherChoice.classList.toggle("is-current", selected);
        if (selected) otherChoice.setAttribute("aria-current", "true");
        else otherChoice.removeAttribute("aria-current");
      }
    });
  }
}

const revealTargets = [...document.querySelectorAll("[data-reveal]")];
const reduceMotion = window.matchMedia?.("(prefers-reduced-motion: reduce)").matches;

if (!reduceMotion && revealTargets.length && "IntersectionObserver" in window) {
  const sectionOrder = new Map();

  for (const target of revealTargets) {
    const section = target.closest("section, footer") || document.body;
    const position = sectionOrder.get(section) || 0;
    target.style.setProperty("--reveal-delay", `${Math.min(position * 50, 100)}ms`);
    sectionOrder.set(section, position + 1);

    const bounds = target.getBoundingClientRect();
    if (bounds.top < window.innerHeight * 0.84 && bounds.bottom > 0) {
      target.classList.add("is-visible");
    }
  }

  const revealObserver = new IntersectionObserver(
    (entries, observer) => {
      for (const entry of entries) {
        if (!entry.isIntersecting) continue;
        entry.target.classList.add("is-visible");
        observer.unobserve(entry.target);
      }
    },
    { rootMargin: "0px 0px 16% 0px", threshold: 0.05 },
  );

  for (const target of revealTargets) {
    if (!target.classList.contains("is-visible")) revealObserver.observe(target);
  }

  document.documentElement.classList.add("motion-ready");
}
