package com.fatico.winthing;

import com.fatico.winthing.messaging.MessagingModule;
import com.fatico.winthing.windows.WindowsModule;
import com.google.gson.Gson;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigParseOptions;
import com.typesafe.config.ConfigSyntax;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.File;
import java.util.Locale;

public class ApplicationModule extends AbstractModule {
    public static final String ConfigFile = "winthing.conf";

    @Override
    protected void configure() {
        bind(Gson.class).in(Singleton.class);

        install(new MessagingModule());
        install(new com.fatico.winthing.homeassistant.Module());
        if (System.getProperty("os.name").toLowerCase(Locale.getDefault()).contains("win")) {
            install(new WindowsModule());
            install(new com.fatico.winthing.systems.system.Module());
            install(new com.fatico.winthing.systems.keyboard.Module());
            install(new com.fatico.winthing.systems.desktop.Module());
            install(new com.fatico.winthing.systems.monitoring.Module());
        }
    }

    @Provides
    @Singleton
    @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT")
    Config config() {
        // Load in order of precedence (highest to lowest):
        // 1. System properties (-Dkey=value)
        // 2. Environment variables
        // 3. winthing.conf file
        // 4. Default values

        Config cfg = ConfigFactory.empty();

        // Load defaults
        cfg = cfg.withFallback(getDefaults());

        // Load from file
        String path = System.getProperty("user.dir") + File.separator + ConfigFile;
        File fp = new File(path);
        if (fp.exists()) {
            ConfigParseOptions options = ConfigParseOptions.defaults();
            options.setSyntax(ConfigSyntax.CONF);
            cfg = ConfigFactory.parseFile(fp, options).withFallback(cfg);
        }

        // Load from environment variables (WINTHING_BROKER, WINTHING_PASSWORD, etc.)
        cfg = cfg.withFallback(getEnvironmentConfig());

        // Load system properties (highest priority)
        cfg = ConfigFactory.systemProperties().withFallback(cfg);

        return cfg.resolve();
    }

    private Config getEnvironmentConfig() {
        Config config = ConfigFactory.empty();

        // Map environment variables to configuration keys
        config = setFromEnv(config, "WINTHING_BROKER", Settings.BROKER_URL);
        config = setFromEnv(config, "WINTHING_USERNAME", Settings.BROKER_USERNAME);
        config = setFromEnv(config, "WINTHING_PASSWORD", Settings.BROKER_PASSWORD);
        config = setFromEnv(config, "WINTHING_CLIENT_ID", Settings.CLIENT_ID);
        config = setFromEnv(config, "WINTHING_PREFIX", Settings.TOPIC_PREFIX);
        config = setFromEnv(config, "WINTHING_MQTT_PROTOCOL", Settings.MQTT_PROTOCOL);
        config = setFromEnv(config, "WINTHING_DEVICE_NAME", Settings.DEVICE_NAME);
        config = setFromEnv(config, "WINTHING_HA_DISCOVERY", Settings.HOMEASSISTANT_DISCOVERY);
        config = setFromEnv(config, "WINTHING_HA_PREFIX", Settings.HOMEASSISTANT_PREFIX);

        return config;
    }

    private Config setFromEnv(Config config, String envVar, String configKey) {
        String value = System.getenv(envVar);
        if (value != null && !value.isEmpty()) {
            config = ConfigFactory.parseString(configKey + "=" + value).withFallback(config);
        }
        return config;
    }

    private Config getDefaults() {
        String defaults = """
            broker = "127.0.0.1:1883"
            username = "mqtt"
            password = "mqtt"
            clientid = "WinThing"
            prefix = "winthing"
            reconnect = 5
            monitoring_interval = 30
            mqtt_protocol = "ssl"
            homeassistant_discovery = true
            homeassistant_prefix = "homeassistant"
            """;
        return ConfigFactory.parseString(defaults);
    }
}
