/**
 *  Piano
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
metadata {
	definition (name: "Piano", namespace: "adawalli", author: "Adam Wallis") {
		capability "Switch"
		capability "Switch Level"
	}

	simulator {
		// TODO: define status and reply messages here
	}

	tiles(scale:2) {
		multiAttributeTile(name:"piano", type:"generic", width:6, height:4) {
		tileAttribute("device.switch", key: "PRIMARY_CONTROL") {
			attributeState( "on", label: '${name}', icon:"st.switches.switch.on", action: "switch.on", nextState:"turningOn")
            attributeState( "off", label: '${name}', icon:"st.switches.switch.off", action: "switch.off", nextState:"turningOff")
    	}
  }

	main "piano"
	details "piano"
	}
}

// parse events into attributes
def parse(String description) {
	log.debug "Parsing '${description}'"
	// TODO: handle 'switch' attribute
	// TODO: handle 'level' attribute

}

def pianoCmd(String path) {
	log.debug "pianoCmd: Executing ${path}"
	new physicalgraph.device.HubAction(
        method: "GET",
        path: path,
        headers: [
            HOST: "192.168.3.200",
        ],
    )
}

// handle commands
def on() {
	log.debug "Executing 'on'"

	return pianoCmd("/cgi-bin/midi9cgi?power=on&get=ack")
}

def off() {
	log.debug "Executing 'off'"
    sendEvent(name: "switch", value: "off",isStateChange: true)
	// TODO: handle 'off' command
}

def setLevel() {
	log.debug "Executing 'setLevel'"
	// TODO: handle 'setLevel' command
}