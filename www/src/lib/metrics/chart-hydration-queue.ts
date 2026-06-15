import type uPlot from "uplot"

const CHARTS_PER_FRAME = 4

type HydrationCallback = () => void

const queue: HydrationCallback[] = []
let flushScheduled = false

function scheduleFlush() {
  if (flushScheduled) {
    return
  }

  flushScheduled = true

  requestAnimationFrame(() => {
    flushScheduled = false

    let processed = 0
    while (queue.length > 0 && processed < CHARTS_PER_FRAME) {
      queue.shift()?.()
      processed++
    }

    if (queue.length > 0) {
      scheduleFlush()
    }
  })
}

function enqueueChartHydration(callback: HydrationCallback): () => void {
  queue.push(callback)
  scheduleFlush()

  return () => {
    const index = queue.indexOf(callback)
    if (index >= 0) {
      queue.splice(index, 1)
    }
  }
}

const destroyQueue: uPlot[] = []
let destroyFlushScheduled = false

function destroyChart(chart: uPlot) {
  try {
    chart.destroy()
  } catch {
    // Chart DOM may already be detached during React unmount.
  }
}

function scheduleDestroyFlush() {
  if (destroyFlushScheduled) {
    return
  }

  destroyFlushScheduled = true

  requestAnimationFrame(() => {
    destroyFlushScheduled = false

    let processed = 0
    while (destroyQueue.length > 0 && processed < CHARTS_PER_FRAME) {
      destroyChart(destroyQueue.shift()!)
      processed++
    }

    if (destroyQueue.length > 0) {
      scheduleDestroyFlush()
    }
  })
}

function enqueueChartDestroy(chart: uPlot) {
  destroyQueue.push(chart)
  scheduleDestroyFlush()
}

export { enqueueChartDestroy, enqueueChartHydration }
