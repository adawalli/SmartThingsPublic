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
    description: "Checks to see if a contact has closed after a chosen number of minutes.",
    category: "",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
    section("What Sensors?") {
            input "mysensor", "capability.contactSensor", required: false, title: "Which?"
            input "lock", "capability.lock", title:"door lock", required: false, multiple: false
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
    subscribe(mysensor, "contact", contactChangeDetected)
    subscribe(lock, "lock", lockChangeDetected)
}

def contactChangeDetected(evt) {
	log.debug "contactChangeDetected : detected ${evt.value}"
	
    switch (evt.value) {
    	case "open":
    		runIn(60 * delayTime, verifyContactClosed)
        	break
        case "closed":
        	unschedule(verifyContactClosed)
        	break
    }
}

def lockChangeDetected(evt) {
	log.debug "lockChangeDetected : detected ${evt.value}"
    switch (evt.value) {
    	case "locked":
        	unschedule(verifyLockLocked)
        	break
        case "unlocked":
        	runIn(60 * delayTime, verifyLockLocked)
        	break
    }
}

def verifyLockLocked() {
	log.debug "verifyLockLocked : enter"
    def currentState = lock.currentState("lock")
    log.debug "verifyLockLocked : ${currentState.value}"
    
    if (currentState.value == "unlocked") {
    	def elapsed = now() - currentState.date.time
        def threshold = 1000 * 60 * delayTime
        
        if (elapsed > threshold) {
        	//notify
            log.debug "verifyLockLocked : ${elapsed}s elapsed. Need to notify user."
            sendPush("The ${lock.displayName} was left open longer than $delayTime minutes!")
        } else {
        	log.debug "verifyLockLocked : I think some logic bug has happened here..."
        }
    } else {
    	log.debug "verifyLockLocked : Contact already closed"
    }
}

def verifyContactClosed() {
	log.debug "verifyContactClosed : Enter"
    
    def currentState = mysensor.currentState("contact")
    log.debug "verifyContactClosed : ${currentState.value}"
    
    if (currentState.value == "open") {
    	def elapsed = now() - currentState.date.time
        def threshold = 1000 * 60 * delayTime
        
        if (elapsed > threshold) {
        	//notify
            log.debug "verifyContactClosed : ${elapsed}s elapsed. Need to notify user."
            sendPush("The ${mysensor.displayName} was left open longer than $delayTime minutes!")
        } else {
        	log.debug "verifyContactClosed : I think some logic bug has happened here..."
        }
    } else {
    	log.debug "verifyContactClosed : Contact already closed"
    }
}