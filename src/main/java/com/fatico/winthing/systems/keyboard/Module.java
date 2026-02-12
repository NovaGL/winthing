package com.fatico.winthing.systems.keyboard;

import com.google.inject.AbstractModule;

public class Module extends AbstractModule {

    @Override
    protected void configure() {
        bind(KeyboardController.class).asEagerSingleton();
    }

}
