import { useEffect, useRef, useState } from "react"

const DEFAULT_ROOT_MARGIN = "120px 0px"

/**
 * Reports whether the element has been near the viewport. Latches to `true`
 * once seen so lazily loaded data is not discarded when scrolling away.
 */
function useInView<T extends Element = HTMLElement>(
  rootMargin = DEFAULT_ROOT_MARGIN
) {
  const ref = useRef<T>(null)
  const [seen, setSeen] = useState(false)

  useEffect(() => {
    const element = ref.current
    if (!element || seen) {
      return
    }

    const observer = new IntersectionObserver(
      (entries) => {
        if (entries.some((entry) => entry.isIntersecting)) {
          setSeen(true)
          observer.disconnect()
        }
      },
      { rootMargin }
    )

    observer.observe(element)
    return () => observer.disconnect()
  }, [rootMargin, seen])

  return { ref, seen }
}

export { useInView }
