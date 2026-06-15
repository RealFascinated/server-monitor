import { useEffect, useRef } from "react"

const listeners = new Map<string, Set<() => void>>()

function subscribeLocalStorage(key: string, listener: () => void) {
  let keyListeners = listeners.get(key)
  if (!keyListeners) {
    keyListeners = new Set()
    listeners.set(key, keyListeners)
  }

  keyListeners.add(listener)
  return () => keyListeners.delete(listener)
}

function notifyLocalStorage(key: string) {
  listeners.get(key)?.forEach((listener) => listener())
}

function writeLocalStorage(key: string, value: string) {
  localStorage.setItem(key, value)
  notifyLocalStorage(key)
}

function useLocalStorageSync(key: string, sync: () => void) {
  const syncRef = useRef(sync)
  syncRef.current = sync

  useEffect(() => {
    const run = () => syncRef.current()
    run()

    const unsubscribe = subscribeLocalStorage(key, run)
    const onStorage = (event: StorageEvent) => {
      if (event.key === key) {
        run()
      }
    }

    window.addEventListener("storage", onStorage)
    return () => {
      unsubscribe()
      window.removeEventListener("storage", onStorage)
    }
  }, [key])
}

export { subscribeLocalStorage, useLocalStorageSync, writeLocalStorage }
