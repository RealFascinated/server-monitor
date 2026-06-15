import * as React from "react"
import { useLayoutEffect, useRef } from "react"
import { Popover as PopoverPrimitive } from "radix-ui"

import { cn } from "@/lib/utils"

function Popover({
  ...props
}: React.ComponentProps<typeof PopoverPrimitive.Root>) {
  return <PopoverPrimitive.Root data-slot="popover" {...props} />
}

function PopoverTrigger({
  ...props
}: React.ComponentProps<typeof PopoverPrimitive.Trigger>) {
  return <PopoverPrimitive.Trigger data-slot="popover-trigger" {...props} />
}

function PopoverAnchor({
  ...props
}: React.ComponentProps<typeof PopoverPrimitive.Anchor>) {
  return <PopoverPrimitive.Anchor data-slot="popover-anchor" {...props} />
}

function PopoverContent({
  className,
  align = "end",
  sideOffset = 4,
  ref,
  ...props
}: React.ComponentProps<typeof PopoverPrimitive.Content>) {
  const contentRef = useRef<HTMLDivElement>(null)

  useLayoutEffect(() => {
    const content = contentRef.current
    if (!content) {
      return
    }

    const wrapper = content.parentElement
    if (!wrapper?.hasAttribute("data-radix-popper-content-wrapper")) {
      return
    }

    const syncWrapper = () => {
      wrapper.style.setProperty("min-width", "0", "important")
      wrapper.style.setProperty("width", "auto", "important")
      wrapper.style.setProperty("height", "auto", "important")
    }

    syncWrapper()

    const resizeObserver = new ResizeObserver(syncWrapper)
    resizeObserver.observe(content)

    const styleObserver = new MutationObserver(syncWrapper)
    styleObserver.observe(wrapper, {
      attributes: true,
      attributeFilter: ["style"],
    })

    return () => {
      resizeObserver.disconnect()
      styleObserver.disconnect()
    }
  }, [])

  const setContentRef = (node: HTMLDivElement | null) => {
    contentRef.current = node

    if (typeof ref === "function") {
      ref(node)
    } else if (ref) {
      ref.current = node
    }
  }

  return (
    <PopoverPrimitive.Portal>
      <PopoverPrimitive.Content
        ref={setContentRef}
        data-slot="popover-content"
        align={align}
        sideOffset={sideOffset}
        className={cn(
          "z-50 min-w-0 origin-(--radix-popover-content-transform-origin) rounded-none bg-popover/95 p-0 text-popover-foreground shadow-md ring-1 ring-foreground/10 backdrop-blur-2xl outline-none data-[side=bottom]:slide-in-from-top-2 data-[side=left]:slide-in-from-right-2 data-[side=right]:slide-in-from-left-2 data-[side=top]:slide-in-from-bottom-2 data-open:animate-in data-open:fade-in-0 data-open:zoom-in-95 data-closed:animate-out data-closed:fade-out-0 data-closed:zoom-out-95",
          className
        )}
        {...props}
      />
    </PopoverPrimitive.Portal>
  )
}

export { Popover, PopoverAnchor, PopoverContent, PopoverTrigger }
