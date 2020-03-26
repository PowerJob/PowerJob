package com.github.kfcfans.oms.worker.common;

import com.github.kfcfans.oms.worker.sdk.TaskContext;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 存储一些不方便直接传递的东西
 * #attention：警惕内存泄漏问题，最好在 ProcessorTracker destroy 时，执行 remove
 *
 * @author tjq
 * @since 2020/3/18
 */
public class ThreadLocalStore {

    public static final ThreadLocal<TaskContext> TASK_CONTEXT_THREAD_LOCAL = new ThreadLocal<>();

    public static final ThreadLocal<AtomicLong> TASK_ID_THREAD_LOCAL = new ThreadLocal<>();

}
