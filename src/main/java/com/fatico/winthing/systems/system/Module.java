package com.fatico.winthing.systems.system;

import com.google.inject.AbstractModule;

public class Module extends AbstractModule {

    @Override
    protected void configure() {
        bind(SystemController.class).asEagerSingleton();
    }

}
