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
     preferences {
         input("pianoIP", "text", title:"Piano IP Address", description: "Please Enter Piano IP Address",defaultValue: "192.168.3.60", required: false, displayDuringSetup: true)
         input("pianoPort", "text", title:"Piano Port", description: "Please enter your Piano's Port", defaultValue: 80 , required: false, displayDuringSetup: true)
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
    def msg = parseLanMessage(description)

    def headersAsString = msg.header // => headers as a string
    def headerMap = msg.headers      // => headers as a Map
    def body = msg.body              // => request body as a string
    def status = msg.status          // => http status code of the response
    def json = msg.json              // => any JSON included in response body, as a data structure of lists and maps
    def xml = msg.xml                // => any XML included in response body, as a document tree structure
    def data = msg.data              // => either JSON or XML in response body (whichever is specified by content-type header
	log.debug "Description: ${msg}"
}

// gets the address of the device
private getHostAddress() {
    def ip = getDataValue("ip")
    def port = getDataValue("port")
	log.debug "**IP**: ${ip} PORT: ${port}"
    if (!ip || !port) {
        def parts = device.deviceNetworkId.split(":")
        if (parts.length == 2) {
            ip = parts[0]
            port = parts[1]
        } else {
            log.warn "Can't figure out ip and port for device: ${device.id}"
        }
    }

    log.debug "Using IP: $ip and port: $port for device: ${device.id}"
    return convertHexToIP(ip) + ":" + convertHexToInt(port)
}

def pianoCmd(String path) {
	log.debug "pianoCmd: Executing ${path}"
	new physicalgraph.device.HubAction(
        method: "GET",
        path: path,
        headers: [
            HOST: getHostAddress(),
        ],
    )
}

// handle commands
def on() {
	log.debug "Executing 'on'"
    def host = pianoIP
    def hosthex = convertIPtoHex(pianoIP).toUpperCase() //thanks to @foxxyben for catching this
    def porthex = convertPortToHex(pianoPort).toUpperCase()
    device.deviceNetworkId = "$hosthex:$porthex" 

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

private Integer convertHexToInt(hex) {
    return Integer.parseInt(hex,16)
}

private String convertHexToIP(hex) {
    return [convertHexToInt(hex[0..1]),convertHexToInt(hex[2..3]),convertHexToInt(hex[4..5]),convertHexToInt(hex[6..7])].join(".")
}

private String convertIPtoHex(ipAddress) { 
    String hex = ipAddress.tokenize( '.' ).collect {  String.format( '%02x', it.toInteger() ) }.join()
    log.debug "IP address entered is $ipAddress and the converted hex code is $hex"
    return hex

}

private String convertPortToHex(port) {
	String hexport = port.toString().format( '%04x', port.toInteger() )
    log.debug hexport
    return hexport
}
