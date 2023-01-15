package tech.powerjob.server.core.handler;

import org.springframework.stereotype.Component;


/**
 * WorkerRequestHandlerHolder
 *
 * @author tjq
 * @since 2022/9/11
 */
@Component
public class WorkerRequestHandlerHolder {

    private static IWorkerRequestHandler workerRequestHandler;

    public WorkerRequestHandlerHolder(IWorkerRequestHandler injectedWorkerRequestHandler) {
        workerRequestHandler = injectedWorkerRequestHandler;
    }

    public static IWorkerRequestHandler fetchWorkerRequestHandler() {
        if (workerRequestHandler == null){
            throw new IllegalStateException("WorkerRequestHandlerHolder not initialized!");
        }
        return workerRequestHandler;
    }
}
