//
// PrivMX Chatee Android.
// Copyright © 2024 Simplito sp. z o.o.
//
// This file is part of demonstration software for the PrivMX Platform (https://privmx.dev).
// This software is Licensed under the MIT License.
//
// See the License for the specific language governing permissions and
// limitations under the License.
//


package com.simplito.chatee.classes.async;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class PrivmxThreadPoolExecutor extends ThreadPoolExecutor {

    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
    private static final int CORE_POOL_SIZE = CPU_COUNT / 2;
    private static final int MAXIMUM_POOL_SIZE = CPU_COUNT * 2 + 1;
    private static final int keepAliveTime = 60;
    private static final TimeUnit keepAliveUnit = TimeUnit.SECONDS;
    private static final RejectedExecutionHandler handler = (Runnable runnable, ThreadPoolExecutor threadPoolExecutor) -> {
        System.out.println("[PrivmxThreadPoolExecutor]: task rejected");
    };

    private static PrivmxThreadPoolExecutor currentPool;

    public static PrivmxThreadPoolExecutor currentPool() {
        if (currentPool == null) {
            currentPool = new PrivmxThreadPoolExecutor();
        }
        return currentPool;
    }

    private static class PrivmxThreadPoolExecutorBlockingQueue<T> extends LinkedBlockingQueue<T> {
        @Override
        public boolean offer(T t) {
            boolean result = super.offer(t);
            if (currentPool != null && (currentPool.getActiveCount() >= CORE_POOL_SIZE && currentPool.getActiveCount() < currentPool.getMaximumPoolSize()) && currentPool.getActiveCount() == currentPool.getPoolSize()) {
                return false;
            }
            return result;
        }
    }

    private PrivmxThreadPoolExecutor() {
        super(CORE_POOL_SIZE, MAXIMUM_POOL_SIZE, keepAliveTime, keepAliveUnit, new PrivmxThreadPoolExecutorBlockingQueue<>(), handler);
        this.allowCoreThreadTimeOut(false);
    }

    @Override
    public void execute(Runnable command) {
        super.execute(command);
    }
}