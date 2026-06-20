//taken from jitsi-videobridge
package net.betrayd.webspeak.webrtc.utils;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.jitsi.utils.concurrent.CustomizableThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

public class TaskPools {
    public static final Logger LOGGER = LoggerFactory.getLogger(TaskPools.class);
    public static final ExecutorService IO_POOL = Executors.newCachedThreadPool(new CustomizableThreadFactory("Global IO pool", false));

    public static final ExecutorService CPU_POOL =
            Executors.newFixedThreadPool(
                    Runtime.getRuntime().availableProcessors(),
                    new CustomizableThreadFactory("Global CPU pool", false)
            );

    private final static ScheduledExecutorService DEFAULT_SCHEDULED_POOL =
            Executors.newSingleThreadScheduledExecutor(new CustomizableThreadFactory("Global scheduled pool", false));
    public static ScheduledExecutorService SCHEDULED_POOL = DEFAULT_SCHEDULED_POOL;

    public static void resetScheduledPool()
    {
        SCHEDULED_POOL = DEFAULT_SCHEDULED_POOL;
    }

    public static ObjectNode getStatsJson(ExecutorService es)
    {
        ObjectNode debugState = JsonNodeFactory.instance.objectNode();
        debugState.put("executor_class", es.getClass().getSimpleName());

        if (es instanceof ThreadPoolExecutor)
        {
            ThreadPoolExecutor ex = (ThreadPoolExecutor)es;
            debugState.put("pool_size", ex.getPoolSize());
            debugState.put("active_task_count", ex.getActiveCount());
            debugState.put("completed_task_count", ex.getCompletedTaskCount());
            debugState.put("core_pool_size", ex.getCorePoolSize());
            debugState.put("maximum_pool_size", ex.getMaximumPoolSize());
            debugState.put("largest_pool_size", ex.getLargestPoolSize());
            debugState.put("queue_class", ex.getQueue().getClass().getSimpleName());
            debugState.put("pending_task_count", ex.getQueue().size());
        }

        return debugState;
    }

    public static ObjectNode getStatsJson()
    {
        ObjectNode debugState = JsonNodeFactory.instance.objectNode();

        debugState.set("IO_POOL", getStatsJson(IO_POOL));
        debugState.set("CPU_POOL", getStatsJson(CPU_POOL));

        return debugState;
    }

    static {
        LOGGER.info("TaskPools detected {} processors, creating the CPU pool with that many threads", Runtime.getRuntime().availableProcessors());

    }
}
