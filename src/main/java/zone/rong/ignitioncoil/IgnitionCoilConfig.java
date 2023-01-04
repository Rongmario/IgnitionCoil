package zone.rong.ignitioncoil;

import net.fabricmc.loader.api.FabricLoader;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

public class IgnitionCoilConfig {

    static final Properties configuration = new Properties();

    static void prepare() {
        File file = new File(FabricLoader.getInstance().getConfigDirectory(), "ignitioncoil.properties");
        if (file.exists()) {
            try (InputStream is = FileUtils.openInputStream(file)) {
                configuration.load(is);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (configuration.isEmpty()) {
            configuration.setProperty("version", "1.0");
            configuration.setProperty("profiling_launch", "true");
            configuration.setProperty("heap_dump_launch", "false");
            configuration.setProperty("heap_summary_launch", "false");
            configuration.setProperty("profiling_world", "true");
            configuration.setProperty("heap_dump_world", "false");
            configuration.setProperty("heap_summary_world", "false");
            configuration.setProperty("do_not_monitor_spark_workers", "true");
            try (OutputStream os = FileUtils.openOutputStream(file)) {
                configuration.store(os, "This is IgnitionCoil's Configuration File");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static boolean isProfilingLaunch() {
        return Boolean.parseBoolean((String) configuration.get("profiling_launch"));
    }

    public static boolean isHeapDumpLaunch() {
        return Boolean.parseBoolean((String) configuration.get("heap_dump_launch"));
    }

    public static boolean isHeapSummaryLaunch() {
        return Boolean.parseBoolean((String) configuration.get("heap_summary_launch"));
    }

    public static boolean isProfilingWorld() {
        return Boolean.parseBoolean((String) configuration.get("profiling_world"));
    }

    public static boolean isHeapDumpWorld() {
        return Boolean.parseBoolean((String) configuration.get("heap_dump_world"));
    }

    public static boolean isHeapSummaryWorld() {
        return Boolean.parseBoolean((String) configuration.get("heap_summary_world"));
    }

    public static boolean doNotMonitorSparkWorkers() {
        return !Boolean.parseBoolean((String) configuration.get("do_not_monitor_spark_workers"));
    }

}
