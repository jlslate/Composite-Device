/*
 * This is free and unencumbered software released into the public domain.
 * For more information, please refer to <https://unlicense.org>
 */

/**
 * Composite Device Manager v1.0.1
 *
 * Parent app for Composite Device child apps. Each child app instance
 * builds and maintains one virtual "Composite Sensor" device made up of
 * real devices you choose across several capabilities (water, lock, door,
 * window, smoke, motion, garage door, temperature, humidity, illuminance).
 *
 * Install this app, then use "Add a new Composite Device" to create as
 * many composite devices as you need -- one child app instance per
 * composite device.
 */

definition(
    name: "Composite Device Manager",
    namespace: "jlslate",
    author: "jlslate (slate)",
    description: "Parent app for building virtual devices out of real devices (water, lock, door, window, smoke, motion, garage door, temperature, humidity, illuminance)",
    category: "Convenience",
    iconUrl: "",
    iconX2Url: "",
    installOnOpen: true
)

preferences {
    page(name: "mainPage")
}

def mainPage() {
    dynamicPage(name: "mainPage", title: "Composite Device Manager", install: true, uninstall: true) {
        section {
            paragraph "Each Composite Device instance creates one virtual device built from real devices you select: water, lock, door, window, smoke, motion, garage door, temperature, humidity, illuminance."
            app(name: "childApps", appName: "Composite Device", namespace: "jlslate", title: "Add a new Composite Device", multiple: true)
        }
    }
}

def installed() {
    initialize()
}

def updated() {
    initialize()
}

def initialize() {
    // No parent-level state -- each child app owns its own composite device.
}
