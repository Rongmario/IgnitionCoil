package zone.rong.ignitioncoil;

import me.lucko.spark.common.SparkPlatform;
import me.lucko.spark.common.SparkPlugin;
import me.lucko.spark.common.command.sender.CommandSender;
import me.lucko.spark.common.platform.PlatformInfo;
import me.lucko.spark.common.sampler.source.ClassSourceLookup;
import me.lucko.spark.common.sampler.source.SourceMetadata;
import me.lucko.spark.fabric.FabricClassSourceLookup;
import me.lucko.spark.lib.adventure.text.Component;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.metadata.Person;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class IgnitionCoilPlugin implements SparkPlugin {

    final SparkPlatform platform = new SparkPlatform(this);
    final Logger logger = LoggerFactory.getLogger(IgnitionCoilPlugin.class);
    final CommandSender sender = new IgnitionCoilCommandSender();
    final Executor executor = Executors.newScheduledThreadPool(4, new IgnitionCoilThreadFactory());

    @Override
    public String getVersion() {
        return FabricLoader.getInstance().getModContainer("ignitioncoil").get().getMetadata().getVersion().getFriendlyString();
    }

    @Override
    public Path getPluginDirectory() {
        return FabricLoader.getInstance().getConfigDir().resolve("ignitioncoil");
    }

    @Override
    public String getCommandName() {
        return null;
    }

    @Override
    public Stream<? extends CommandSender> getCommandSenders() {
        return Stream.of();
    }

    @Override
    public void executeAsync(Runnable runnable) {
        this.executor.execute(runnable);
    }

    @Override
    public void log(Level level, String msg) {
        if (level == Level.INFO) {
            this.logger.info(msg);
        } else if (level == Level.WARNING) {
            this.logger.warn(msg);
        } else {
            if (level != Level.SEVERE) {
                throw new IllegalArgumentException(level.getName());
            }
            this.logger.error(msg);
        }
    }

    @Override
    public PlatformInfo getPlatformInfo() {
        return new IgnitionCoilPlatformInfo();
    }

    @Override
    public ClassSourceLookup createClassSourceLookup() {
        return new FabricClassSourceLookup();
    }

    @Override
    public Collection<SourceMetadata> getKnownSources() {
        return SourceMetadata.gather(FabricLoader.getInstance().getAllMods(),
                (mod) -> mod.getMetadata().getId(),
                (mod) -> mod.getMetadata().getVersion().getFriendlyString(),
                (mod) -> mod.getMetadata().getAuthors().stream().map(Person::getName).collect(Collectors.joining(", ")));
    }

    private static class IgnitionCoilThreadFactory implements ThreadFactory, Thread.UncaughtExceptionHandler {

        private final AtomicInteger poolNumber = new AtomicInteger(1);
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;

        IgnitionCoilThreadFactory() {
            this.namePrefix = "ignitioncoil-worker-pool-" + this.poolNumber.getAndIncrement() + "-thread-";
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, this.namePrefix + this.threadNumber.getAndIncrement());
            t.setUncaughtExceptionHandler(this);
            t.setDaemon(true);
            return t;
        }

        @Override
        public void uncaughtException(Thread t, Throwable e) {
            System.err.println("Uncaught exception thrown by thread " + t.getName());
            e.printStackTrace();
        }

    }

    private static class IgnitionCoilPlatformInfo implements PlatformInfo {

        public PlatformInfo.Type getType() {
            return IgnitionCoil.CLIENT_SIDE ? Type.CLIENT : Type.SERVER;
        }

        public String getName() {
            return "IgnitionCoil";
        }

        public String getVersion() {
            return FabricLoader.getInstance().getModContainer("ignitioncoil").get().getMetadata().getVersion().getFriendlyString();
        }

        public String getMinecraftVersion() {
            return FabricLoader.getInstance().getModContainer("minecraft").get().getMetadata().getVersion().getFriendlyString();
        }

    }

    private static class IgnitionCoilCommandSender implements CommandSender {

        @Override
        public String getName() {
            return "IgnitionCoil";
        }

        @Override
        public UUID getUniqueId() {
            return UUID.randomUUID();
        }

        @Override
        public void sendMessage(Component component) { }

        @Override
        public boolean hasPermission(String s) {
            return true;
        }

    }

}
