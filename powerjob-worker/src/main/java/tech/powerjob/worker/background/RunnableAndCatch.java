package tech.powerjob.worker.background;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ScheduledExecutorService;

/**
 * 使用 {@link ScheduledExecutorService} 执行任务时，推荐继承此类捕获并打印异常，避免因为抛出异常导致周期性任务终止
 *
 * @author songyinyin
 * @since 2023/9/20 15:52
 */
@Slf4j
public abstract class RunnableAndCatch implements Runnable{
  @Override
  public void run() {
    try {
      run0();
    } catch (Exception e) {
      log.error("[RunnableAndCatch] run failed", e);
    }
  }

  protected abstract void run0();
}
