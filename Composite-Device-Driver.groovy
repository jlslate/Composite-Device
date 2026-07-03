/*
 * This is free and unencumbered software released into the public domain.
 * For more information, please refer to <https://unlicense.org>
 */

/**
 * Composite Sensor Driver v1.0.0
 *
 * Virtual device driver used by the Composite Device child app. Holds no
 * logic of its own -- every attribute is set by the parent child app as it
 * aggregates real devices, and every command is relayed back up to the
 * parent to act on the real devices it manages.
 *
 * Attributes:
 * - water           (WaterSensor)              wet / dry
 * - lock             (Lock)                     locked / unlocked
 * - contact          (ContactSensor)            open / closed -- aggregate of door + window
 * - doorContact      (custom)                   open / closed
 * - windowContact    (custom)                   open / closed
 * - smoke            (SmokeDetector)            detected / clear / tested
 * - motion           (MotionSensor)             active / inactive
 * - door             (GarageDoorControl)        open / closed / opening / closing / unknown
 * - temperature      (TemperatureMeasurement)
 * - humidity         (RelativeHumidityMeasurement)
 * - illuminance      (IlluminanceMeasurement)
 */

metadata {
    definition(name: "Composite Sensor", namespace: "jlslate", author: "jlslate (slate)") {
        capability "Sensor"
        capability "Actuator"
        capability "WaterSensor"
        capability "Lock"
        capability "ContactSensor"
        capability "SmokeDetector"
        capability "MotionSensor"
        capability "GarageDoorControl"
        capability "TemperatureMeasurement"
        capability "RelativeHumidityMeasurement"
        capability "IlluminanceMeasurement"
        capability "Refresh"

        attribute "doorContact", "enum", ["open", "closed"]
        attribute "windowContact", "enum", ["open", "closed"]
    }
}

def installed() {
    initialize()
}

def updated() {
    initialize()
}

def initialize() {
    if (device.currentValue("water") == null) sendEvent(name: "water", value: "dry")
    if (device.currentValue("lock") == null) sendEvent(name: "lock", value: "unlocked")
    if (device.currentValue("contact") == null) sendEvent(name: "contact", value: "closed")
    if (device.currentValue("doorContact") == null) sendEvent(name: "doorContact", value: "closed")
    if (device.currentValue("windowContact") == null) sendEvent(name: "windowContact", value: "closed")
    if (device.currentValue("smoke") == null) sendEvent(name: "smoke", value: "clear")
    if (device.currentValue("motion") == null) sendEvent(name: "motion", value: "inactive")
    if (device.currentValue("door") == null) sendEvent(name: "door", value: "closed")
}

// Lock capability commands -- relayed to the real lock(s) chosen in the child app
def lock() {
    parent?.relayLock()
}

def unlock() {
    parent?.relayUnlock()
}

// GarageDoorControl capability commands -- relayed to the real garage door(s)
def open() {
    parent?.relayOpen()
}

def close() {
    parent?.relayClose()
}

// Refresh capability -- re-polls source devices (where supported) and re-syncs
def refresh() {
    parent?.relayRefresh()
}
