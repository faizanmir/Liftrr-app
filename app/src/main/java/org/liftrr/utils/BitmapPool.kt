package org.liftrr.utils

import android.graphics.Bitmap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger

/**
 * Lock-free bitmap pool using ConcurrentLinkedQueue.
 * Safe for concurrent access from camera, MediaPipe, and UI threads.
 */
class BitmapPool(
    private val capacity: Int,
    private val width: Int,
    private val height: Int,
    private val config: Bitmap.Config = Bitmap.Config.ARGB_8888
) {
    private val pool = ConcurrentLinkedQueue<Bitmap>()
    private val poolSize = AtomicInteger(0)

    fun acquire(): Bitmap {
        val bitmap = pool.poll()
        if (bitmap != null) {
            poolSize.decrementAndGet()
            return bitmap
        }
        return Bitmap.createBitmap(width, height, config)
    }

    fun release(bitmap: Bitmap) {
        if (bitmap.isRecycled) return

        if (bitmap.width == width &&
            bitmap.height == height &&
            bitmap.config == config &&
            poolSize.get() < capacity
        ) {
            pool.offer(bitmap)
            poolSize.incrementAndGet()
        } else {
            bitmap.recycle()
        }
    }

    fun clear() {
        var bitmap = pool.poll()
        while (bitmap != null) {
            if (!bitmap.isRecycled) bitmap.recycle()
            bitmap = pool.poll()
        }
        poolSize.set(0)
    }
}
