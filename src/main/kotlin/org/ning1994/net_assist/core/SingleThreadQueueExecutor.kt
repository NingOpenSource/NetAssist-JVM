package org.ning1994.net_assist.core

import java.util.*
import java.util.concurrent.Executor

class SingleThreadQueueExecutor : Executor {
    @Volatile
    private var thread: Thread? = null
    private val runnables = Vector<Runnable>()
    fun executeFirst(command: Runnable) {
        if (!runnables.contains(command)) {
            runnables.add(0, command)
            apply()
        }
    }

    override fun execute(command: Runnable) {
        if (!runnables.contains(command)) {
            runnables.add(command)
            apply()
        }
    }

    private fun apply() {
        if (thread == null) {
            thread = object : Thread() {
                override fun run() {
                    super.run()
                    if (!runnables.isEmpty()) {
                        try {
                            runnables[0].run()
                        } catch (t: Throwable) {
                            t.printStackTrace()
                        }
                        runnables.removeAt(0)
                        run()
                    } else {
                        thread = null
                        //结束
                    }
                }
            }
            thread?.start()
        }
    }
}