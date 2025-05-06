package com.dashtiss.tpsnitch.platform;

import com.dashtiss.tpsnitch.Constants;
import com.dashtiss.tpsnitch.platform.services.IPlatformHelper;

import java.util.ServiceLoader;

/**
 * Provides access to platform-specific services for TPSnitch.
 * Uses Java's ServiceLoader to load implementations.
 */
public class Services {

    /**
     * Provides platform-specific helper implementation (e.g., Forge, Fabric).
     * Used to check environment/platform and mod loading status.
     */
    public static final IPlatformHelper PLATFORM = load(IPlatformHelper.class);

    /**
     * Loads a service implementation for the given interface using ServiceLoader.
     * @param clazz Service interface class
     * @param <T> Service type
     * @return Loaded service implementation
     * @throws NullPointerException if no implementation is found
     */
    public static <T> T load(Class<T> clazz) {
        final T loadedService = ServiceLoader.load(clazz)
                .findFirst()
                .orElseThrow(() -> new NullPointerException("Failed to load service for " + clazz.getName()));
        Constants.LOG.debug("Loaded {} for service {}", loadedService, clazz);
        return loadedService;
    }
}