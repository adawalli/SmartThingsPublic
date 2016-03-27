/**
 *  Is It Open
 *
 *  Copyright 2016 Adam Wallis
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
definition(
    name: "Is It Open",
    namespace: "adawalli",
    author: "Adam Wallis",
    description: "Checks to see if a contact/lock has closed after a chosen number of minutes.",
    category: "",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
    section("What Sensors?") {
            input "mysensors", "capability.contactSensor", required: false, multiple: true, title: "Contact Sensors:"
            input "mylocks", "capability.lock", title:"Locks:", required: false, multiple: true
    }
 	   
    section("How Long should this stay open before I notify you?") {
            input "delayTime", "number", required: true, title: "Minutes:", defaultValue: 10
    }
}

def installed() {
	log.debug "Installed with settings: ${settings}"

	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
	initialize()
}

def initialize() {
	// TODO: subscribe to attributes, devices, locations, etc.
    subscribe(mysensors, "contact", contactChangeDetected)
    subscribe(mylocks, "lock", lockChangeDetected)
    }

def contactChangeDetected(evt) {
	log.debug "contactChangeDetected[${evt.displayName}] : detected ${evt.value}"
	
    switch (evt.value) {
    	case "open":
    		runIn(60 * delayTime, verifyContactClosed)
        	break
        default:
        	break
    }
}

def lockChangeDetected(evt) {
	log.debug "lockChangeDetected[${evt.displayName}] : detected ${evt.value}"
    switch (evt.value) {
        case "unlocked":
        	runIn(60 * delayTime, verifyLockLocked)
        	break
        default:
        	break
    }
}

def verifyLockLocked() {
    def openLocks = mylocks.findAll { it?.latestValue("lock") == "unlocked" }
    log.debug "verifyLockLocked : Identified ${openLocks.size()} open locks" 
   
    openLocks.each { lock->
        def currentState = lock.currentState("lock")
        def elapsed = now() - currentState.date.time
        def threshold = 1000 * 60 * delayTime
        
       	log.debug "verifyLockLocked : ${lock.displayName} open and ${elapsed}ms elapsed. Need to notify user."
        sendPush("The ${lock.displayName} was left open longer than $delayTime minutes!")       
        if (elapsed < threshold) {
        	log.debug "verifyLockLocked : I think some logic bug has happened here..."
    	}
    }
}

def verifyContactClosed() {
    def openSensors = mysensors.findAll { it?.latestValue("contact") == "open" }
    log.debug "verifyContactClosed : Identified ${openSensors.size()} open contacts" 
   
    openSensors.each { sensor->
        def currentState = sensor.currentState("contact")
        def elapsed = now() - currentState.date.time
        def threshold = 1000 * 10 * delayTime
        
       	log.debug "verifyContactClosed : ${sensor.displayName} open and ${elapsed}ms elapsed. Need to notify user."
        sendPush("The ${sensor.displayName} was left open longer than $delayTime minutes!")       
        if (elapsed < threshold) {
        	log.debug "verifyContactClosed : I think some logic bug has happened here..."
    	}
    }
}