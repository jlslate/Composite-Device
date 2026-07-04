/*
 * This is free and unencumbered software released into the public domain.
 * For more information, please refer to <https://unlicense.org>
 */

/**
 * Composite Device v1.3.0
 *
 * Child app of Composite Device Manager. Builds and maintains one virtual
 * "Composite Sensor" device that mirrors real devices of a single chosen
 * capability: water, lock, door contact, window contact, smoke, motion,
 * garage door, temperature, humidity, or illuminance.
 *
 * Pick the sensor type from the dropdown; the device picker beneath it
 * then only offers devices of that type. You can select multiple devices
 * of that one type (e.g. several motion sensors) -- one composite device
 * instance always represents exactly one sensor type, so create another
 * Composite Device instance (via the parent app) for a different type.
 *
 * Aggregation rules (when multiple devices of the chosen type are picked):
 * - water / smoke / motion / door / window: any-active wins (any wet /
 *   detected / active / open makes the composite match)
 * - lock: all-locked-to-lock (composite is "locked" only if every selected
 *   lock reports locked -- unlocked otherwise)
 * - garage door: "open" if any is open, else "opening"/"closing" if any is
 *   mid-transit, else "closed" if all are closed, else "unknown"
 * - temperature / humidity / illuminance: numeric average across selected
 *   devices
 *
 * Commands sent to the composite device (lock/unlock, open/close, refresh)
 * are relayed down to every real device selected (only meaningful for the
 * lock and garage door types).
 *
 * Changelog:
 * v1.3.0 -- Each composite device instance now represents exactly one
 *           sensor type: a single "Sensor type" dropdown filters a single
 *           device picker beneath it. Removed the v1.1.0 slot/"Add another
 *           sensor" mechanism entirely -- there is no per-instance list of
 *           slots anymore, just one type + its device picker.
 * v1.2.0 -- Fixed one-section-per-capability layout (superseded by v1.3.0).
 * v1.1.0 -- Repeatable type+device slots (superseded by v1.3.0).
 * v1.0.0 -- Initial release.
 */

definition(
    name: "Composite Device",
    namespace: "jlslate",
    author: "jlslate (slate)",
    description: "Builds one virtual device out of real devices of a single chosen type (water/lock/door/window/smoke/motion/garage/temperature/humidity/illumination)",
    category: "Convenience",
    parent: "jlslate:Composite Device Manager",
    iconUrl: "",
    iconX2Url: ""
)

preferences {
    page(name: "mainPage")
}

def mainPage() {
    dynamicPage(name: "mainPage", title: "Composite Device", install: true, uninstall: true) {
        section("Name") {
            label title: "Name this Composite Device instance", required: true
            input name: "deviceName", type: "text", title: "Virtual device name", required: true, submitOnChange: true
        }
        section("Sensor Type") {
            input name: "deviceType",
                  type: "enum",
                  title: "Sensor type",
                  options: typeOptions(),
                  required: true,
                  submitOnChange: true

            if (deviceType) {
                input name: "typeDevices",
                      type: capabilityFor(deviceType),
                      title: "${typeOptions()[deviceType]} device(s)",
                      multiple: true,
                      required: false
            }
        }
        section {
            paragraph "The virtual device is created/renamed on Done and kept in sync as source devices change."
        }
    }
}

def installed() {
    initialize()
}

def updated() {
    unsubscribe()
    initialize()
}

def initialize() {
    createOrUpdateChildDevice()
    subscribeAll()
    updateType()
}

private compositeDni() {
    "compositeDevice_${app.id}"
}

private compositeDevice() {
    getChildDevice(compositeDni())
}

private createOrUpdateChildDevice() {
    def dev = compositeDevice()
    def label = deviceName ?: app.label ?: "Composite Device"
    if (!dev) {
        dev = addChildDevice("jlslate", "Composite Sensor", compositeDni(), null, [name: "Composite Sensor", label: label, isComponent: false])
    } else if (dev.label != label) {
        dev.setLabel(label)
    }
}

// ── Type mapping ─────────────────────────────────────────────────────────────

private Map typeOptions() {
    ["door": "Door",
     "garage": "Garage door",
     "humidity": "Humidity",
     "illumination": "Illumination",
     "lock": "Lock",
     "motion": "Motion",
     "smoke": "Smoke",
     "temperature": "Temperature",
     "water": "Water",
     "window": "Window"]
}

private String capabilityFor(String type) {
    switch (type) {
        case "water": return "capability.waterSensor"
        case "lock": return "capability.lock"
        case "door": return "capability.contactSensor"
        case "window": return "capability.contactSensor"
        case "smoke": return "capability.smokeDetector"
        case "motion": return "capability.motionSensor"
        case "garage": return "capability.garageDoorControl"
        case "temperature": return "capability.temperatureMeasurement"
        case "humidity": return "capability.relativeHumidityMeasurement"
        case "illumination": return "capability.illuminanceMeasurement"
        default: return "capability.sensor"
    }
}

private String attributeForType(String type) {
    switch (type) {
        case "water": return "water"
        case "lock": return "lock"
        case "door": return "contact"
        case "window": return "contact"
        case "smoke": return "smoke"
        case "motion": return "motion"
        case "garage": return "door"
        case "temperature": return "temperature"
        case "humidity": return "humidity"
        case "illumination": return "illuminance"
        default: return null
    }
}

// ── Subscription & sync ─────────────────────────────────────────────────────

private subscribeAll() {
    if (deviceType && typeDevices) {
        subscribe(typeDevices, attributeForType(deviceType), "typeHandler")
    }
}

def typeHandler(evt) {
    updateType()
}

// ---------- Aggregation ----------

private void updateType() {
    def dev = compositeDevice()
    if (!dev || !deviceType || !typeDevices) return

    switch (deviceType) {
        case "water":
            def wet = typeDevices.any { it.currentValue("water") == "wet" }
            dev.sendEvent(name: "water", value: wet ? "wet" : "dry")
            break

        case "lock":
            def allLocked = typeDevices.every { it.currentValue("lock") == "locked" }
            dev.sendEvent(name: "lock", value: allLocked ? "locked" : "unlocked")
            break

        case "door":
            def open = typeDevices.any { it.currentValue("contact") == "open" }
            dev.sendEvent(name: "doorContact", value: open ? "open" : "closed")
            dev.sendEvent(name: "contact", value: open ? "open" : "closed")
            break

        case "window":
            def open = typeDevices.any { it.currentValue("contact") == "open" }
            dev.sendEvent(name: "windowContact", value: open ? "open" : "closed")
            dev.sendEvent(name: "contact", value: open ? "open" : "closed")
            break

        case "smoke":
            def detected = typeDevices.any { it.currentValue("smoke") == "detected" }
            dev.sendEvent(name: "smoke", value: detected ? "detected" : "clear")
            break

        case "motion":
            def active = typeDevices.any { it.currentValue("motion") == "active" }
            dev.sendEvent(name: "motion", value: active ? "active" : "inactive")
            break

        case "garage":
            def states = typeDevices.collect { it.currentValue("door") }
            def value = states.contains("open") ? "open" :
                        states.contains("opening") ? "opening" :
                        states.contains("closing") ? "closing" :
                        states.every { it == "closed" } ? "closed" : "unknown"
            dev.sendEvent(name: "door", value: value)
            break

        case "temperature":
            def values = typeDevices.collect { it.currentValue("temperature") }.findAll { it != null }
            if (values) {
                def avg = (values*.toBigDecimal().sum() / values.size()) as BigDecimal
                dev.sendEvent(name: "temperature", value: avg.setScale(1, java.math.RoundingMode.HALF_UP), unit: "°" + (location.temperatureScale ?: "F"))
            }
            break

        case "humidity":
            def values = typeDevices.collect { it.currentValue("humidity") }.findAll { it != null }
            if (values) {
                def avg = (values*.toBigDecimal().sum() / values.size()) as BigDecimal
                dev.sendEvent(name: "humidity", value: avg.setScale(1, java.math.RoundingMode.HALF_UP), unit: "%")
            }
            break

        case "illumination":
            def values = typeDevices.collect { it.currentValue("illuminance") }.findAll { it != null }
            if (values) {
                def avg = (values*.toBigDecimal().sum() / values.size()) as BigDecimal
                dev.sendEvent(name: "illuminance", value: avg.setScale(0, java.math.RoundingMode.HALF_UP), unit: "lx")
            }
            break
    }
}

// ---------- Command relays (called by Composite Sensor driver) ----------

def relayLock() {
    if (deviceType == "lock") typeDevices?.each { it.lock() }
}

def relayUnlock() {
    if (deviceType == "lock") typeDevices?.each { it.unlock() }
}

def relayOpen() {
    if (deviceType == "garage") typeDevices?.each { it.open() }
}

def relayClose() {
    if (deviceType == "garage") typeDevices?.each { it.close() }
}

def relayRefresh() {
    typeDevices?.each { dev ->
        if (dev.hasCommand("refresh")) dev.refresh()
    }
    updateType()
}
