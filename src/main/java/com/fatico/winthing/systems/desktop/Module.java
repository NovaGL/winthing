package com.fatico.winthing.systems.desktop;

import com.google.inject.AbstractModule;

public class Module extends AbstractModule {

    @Override
    protected void configure() {
        bind(DesktopController.class).asEagerSingleton();
    }

}
