package tech.powerjob.common.enhance;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ScheduledExecutorService;

/**
 * 使用 {@link ScheduledExecutorService} 执行任务时，推荐使用此对象包装一层，避免因为抛出异常导致周期性任务终止
 *
 * @author songyinyin
 * @since 2023/9/20 16:04
 */
@Slf4j
public class SafeRunnableWrapper implements Runnable {

  private final Runnable runnable;

  public SafeRunnableWrapper(Runnable runnable) {
    this.runnable = runnable;
  }

  @Override
  public void run() {
    try {
      runnable.run();
    } catch (Exception e) {
      log.error("[SafeRunnableWrapper] run failed", e);
    }
  }
}
