package tech.powerjob.server.core.handler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * WorkerRequestHandlerHolder
 *
 * @author tjq
 * @since 2022/9/11
 */
@Component
public class WorkerRequestHandlerHolder {

    private static IWorkerRequestHandler workerRequestHandler;


    public static IWorkerRequestHandler fetchWorkerRequestHandler() {
        return workerRequestHandler;
    }

    @Autowired
    public void setWorkerRequestHandler(IWorkerRequestHandler workerRequestHandler) {
        WorkerRequestHandlerHolder.workerRequestHandler = workerRequestHandler;
    }
}
