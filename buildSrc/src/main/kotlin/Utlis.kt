// SPDX-License-Identifier: MIT OR Apache-2.0
object RuntimeUtlis {
    val os by lazy { org.gradle.internal.os.OperatingSystem.current()!! }
    val isRunningInIde: Boolean = System.getProperty("idea.active") == "true"
    val isSupportModuleVM = System.getProperty("java.specification.version").toFloat() > 1.8
}
