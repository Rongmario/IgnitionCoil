package zone.rong.ignitioncoil;

import me.lucko.spark.common.heapdump.HeapDump;
import me.lucko.spark.common.heapdump.HeapDumpSummary;
import me.lucko.spark.common.sampler.Sampler;
import me.lucko.spark.common.sampler.SamplerBuilder;
import me.lucko.spark.common.sampler.ThreadDumper;
import me.lucko.spark.common.sampler.ThreadGrouper;
import me.lucko.spark.common.sampler.node.MergeMode;
import me.lucko.spark.common.sampler.source.ClassSourceLookup;
import me.lucko.spark.common.util.MethodDisambiguator;
import me.lucko.spark.lib.protobuf.AbstractMessageLite;
import me.lucko.spark.proto.SparkHeapProtos.HeapData;
import me.lucko.spark.proto.SparkSamplerProtos.SamplerData;
import me.lucko.spark.proto.SparkSamplerProtos.SamplerMetadata;
import me.lucko.spark.proto.SparkSamplerProtos.SamplerMetadata.ThreadDumper.Type;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

public class IgnitionCoil {

    public static final boolean CLIENT_SIDE = FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT;

    static final IgnitionCoilPlugin plugin = new IgnitionCoilPlugin();

    static Sampler currentSampler;

    static {
        IgnitionCoilConfig.prepare();
    }

    public static void early() {
        if (IgnitionCoilConfig.doNotMonitorSparkWorkers()) {
            ThreadDumper allWithoutSparkWorkers = new ThreadDumper() {

                @Override
                public ThreadInfo[] dumpThreads(ThreadMXBean threadMXBean) {
                    ThreadInfo[] all = threadMXBean.dumpAllThreads(false, false);
                    if (IgnitionCoilConfig.doNotMonitorSparkWorkers()) {
                        return Arrays.stream(all).filter(info -> !info.getThreadName().startsWith("spark")).toArray(ThreadInfo[]::new);
                    }
                    return all;
                }

                @Override
                public SamplerMetadata.ThreadDumper getMetadata() {
                    return SamplerMetadata.ThreadDumper.newBuilder().setType(Type.ALL).build();
                }

            };
            try {
                Field all = ThreadDumper.class.getField("ALL");
                all.setAccessible(true);
                all.set(null, allWithoutSparkWorkers);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        }
        if (IgnitionCoilConfig.isProfilingLaunch()) {
            startSampler();
        }
    }

    public static void late() {
        if (CLIENT_SIDE) {
            ClientLifecycleEvents.CLIENT_STARTED.register(mc -> {
                if (currentSampler != null) {
                    endSampler("Launch");
                }
                if (IgnitionCoilConfig.isHeapDumpLaunch()) {
                    createHeapDump();
                } else if (IgnitionCoilConfig.isHeapSummaryLaunch()) {
                    createHeapSummary();
                }
            });
        }
        if ((!CLIENT_SIDE && IgnitionCoilConfig.isProfilingLaunch()) || IgnitionCoilConfig.isProfilingWorld()) {
            ServerLifecycleEvents.SERVER_STARTING.register(ms -> {
                if (currentSampler != null) {
                    endSampler("Launch");
                }
                if (IgnitionCoilConfig.isProfilingWorld()) {
                    startSampler();
                }
            });
        }
        if (IgnitionCoilConfig.isProfilingWorld() || IgnitionCoilConfig.isHeapDumpWorld() || IgnitionCoilConfig.isHeapSummaryWorld()) {
            ServerLifecycleEvents.SERVER_STARTED.register(ms -> {
                if (currentSampler != null) {
                    endSampler("World Load");
                }
                if (IgnitionCoilConfig.isHeapDumpWorld()) {
                    createHeapDump();
                } else if (IgnitionCoilConfig.isHeapSummaryWorld()) {
                    createHeapSummary();
                }
            });
        }
    }

    private static void startSampler() {
        currentSampler = new SamplerBuilder()
                .samplingInterval(1D)
                .threadGrouper(ThreadGrouper.BY_POOL)
                .background(true)
                .threadDumper(ThreadDumper.ALL)
                .start(plugin.platform);
    }

    private static void endSampler(String comment) {
        if (currentSampler != null) {
            currentSampler.stop(false);
            MergeMode mergeMode = MergeMode.sameMethod(new MethodDisambiguator());
            SamplerData output = currentSampler.toProto(plugin.platform, plugin.sender, comment, mergeMode, ClassSourceLookup.create(plugin.platform));
            plugin.logger.warn("{} profiler finished! Results here: {}", comment, postAndGetSampler(output));
            currentSampler = null;
        }
    }

    private static void createHeapSummary() {
        plugin.logger.warn("Creating heap dump summary...");
        HeapData heapData = HeapDumpSummary.createNew().toProto(plugin.platform, plugin.sender);
        plugin.logger.warn("Heap dump summary finished! Results here: {}", postAndGetHeapSummary(heapData));
    }

    private static void createHeapDump() {
        try {
            plugin.logger.warn("Creating heap dump...");
            Path path = FabricLoader.getInstance().getConfigDir()
                    .resolve("heap-" + DateTimeFormatter.ofPattern("yyyy-MM-dd_HH.mm.ss").format(LocalDateTime.now()) + "." + (HeapDump.isOpenJ9() ? "phd" : "hprof"));
            HeapDump.dumpHeap(path, true);
            plugin.logger.warn("Finished creating heap dump! Output resides in: {}", path);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String postAndGetSampler(AbstractMessageLite<?, ?> message) {
        try {
            String key = plugin.platform.getBytebinClient().postContent(message, "application/x-spark-sampler").key();
            return plugin.platform.getViewerUrl() + key;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String postAndGetHeapSummary(AbstractMessageLite<?, ?> message) {
        try {
            String key = plugin.platform.getBytebinClient().postContent(message, "application/x-spark-heap").key();
            return plugin.platform.getViewerUrl() + key;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
