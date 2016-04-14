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
        capability "Lock"
	}
     preferences {
         input("pianoIP", "text", title:"Piano IP Address", description: "Please Enter Piano IP Address",defaultValue: "192.168.3.60", required: false, displayDuringSetup: true)
         input("pianoPort", "text", title:"Piano Port", description: "Please enter your Piano's Port", defaultValue: 80 , required: false, displayDuringSetup: true)
	}
	simulator {
		// TODO: define status and reply messages here
	}

	tiles(scale:2) {
		multiAttributeTile(name:"piano", type:"lighting", width:6, height:4) {
            tileAttribute("device.switch", key: "PRIMARY_CONTROL") {
                attributeState "on", label:'${name}', action:"switch.off", icon:"st.Electronics.electronics11", backgroundColor:"#79b821", nextState:"turningOff"
                attributeState "off", label:'${name}', action:"switch.on", icon:"st.Electronics.electronics11", backgroundColor:"#ffffff", nextState:"turningOn"
                attributeState "turningOn", label:'${name}', action:"switch.off", icon:"st.Electronics.electronics11", backgroundColor:"#79b821", nextState:"turningOff"
                attributeState "turningOff", label:'${name}', action:"switch.on", icon:"st.Electronics.electronics11", backgroundColor:"#ffffff", nextState:"turningOn"
            }
  		}
        
		controlTile("levelSliderControl", "device.level", "slider", height: 1, width:6, inactiveLabel: false, range:"(1..10)", decoration: "flat") {
            state "level", action:"switch level.setLevel"
		}
        
        standardTile("play", "device.lock", width: 6, height: 3) {
            state "locked", icon:"st.Entertainment.entertainment2", action:"lock.unlock", backgroundColor:"#ffffff"
            state "locking", icon:"st.Entertainment.entertainment2", action:"lock.unlock", backgroundColor:"#ffffff"
            state "unlocked", icon:"st.Entertainment.entertainment2", action:"lock.lock", backgroundColor:"#53a7c0"
            state "unlocking", icon:"st.Entertainment.entertainment2", action:"lock.lock", backgroundColor:"#53a7c0"
		}

	main "piano"
	details (["piano","levelSliderControl", "play"])
	}
}

// parse events into attributes
def parse(String description) {
    def msg = parseLanMessage(description)
	def evt = []
    def headersAsString = msg.header // => headers as a string
    def headerMap = msg.headers      // => headers as a Map
    def body = msg.body              // => request body as a string
    def status = msg.status          // => http status code of the response
    def json = msg.json              // => any JSON included in response body, as a data structure of lists and maps
    def xml = msg.xml                // => any XML included in response body, as a document tree structure
    def data = msg.data              // => either JSON or XML in response body (whichever is specified by content-type header
	//log.debug "Description: ${msg}"
    log.debug "Entered Parse"
    def lastStatus = device.latestValue("switch")
    if (lastStatus == "turningOn")
    {
    	if (headersAsString?.contains("200"))
        {
            log.debug "Successfully turned on piano!"
            evt << createEvent(name: "switch", value: "on", isStateChange: true)
        }
        else
        {
        	evt << createEvent(name: "switch", value: "off", isStateChange: true)
        }  
    } 
    else if (lastStatus == "turningOff")
    {
    	if (headersAsString?.contains("200"))
        {
            log.debug "Successfully turned off piano!"
            evt << createEvent(name: "switch", value: "off", isStateChange: true)
        }
        else
        {
        	evt << createEvent(name: "switch", value: "on", isStateChange: true)
        } 
    }
    lastStatus = device.latestValue("lock")
    if (lastStatus == "locking")
    {
    	if (headersAsString?.contains("200"))
        {
            log.debug "Successfully Paused piano!"
            evt << createEvent(name: "lock", value: "locked", isStateChange: true)
        }
        else
        {
        	evt << createEvent(name: "lock", value: "unlocked", isStateChange: true)
        } 
    }
    else if (lastStatus == "unlocking")
    {
    	if (headersAsString?.contains("200"))
        {
            log.debug "Successfully Played piano!"
            evt << createEvent(name: "lock", value: "unlocked", isStateChange: true)
        }
        else
        {
        	evt << createEvent(name: "lock", value: "locked", isStateChange: true)
        } 
    }
    evt
}

def pianoCmd(String path) {
	log.debug "pianoCmd: Executing ${path}"
    if (!device.deviceNetworkId.contains(":"))
    {
        def hosthex = convertIPtoHex(pianoIP).toUpperCase() //thanks to @foxxyben for catching this
        def porthex = convertPortToHex(pianoPort).toUpperCase()
        device.deviceNetworkId = "$hosthex:$porthex" 
    }
	new physicalgraph.device.HubAction(
        method: "GET",
        path: path,
        headers: [
            HOST: pianoIP +":" + pianoPort,//getHostAddress(),
        ],
    )
}

// handle commands
def on() {
	log.debug "Executing 'on'"
    sendEvent(name: "switch", value: "turningOn",isStateChange: true)
	return pianoCmd("/cgi-bin/midi9cgi?power=on&get=ack")
}

def off() {
	def cmds = []
	log.debug "Executing 'off'"
    sendEvent(name: "switch", value: "turningOff",isStateChange: true)
    cmds << lock()
    cmds << pianoCmd("/cgi-bin/midi9cgi?power=standby&get=ack")
	// TODO: handle 'off' command
}

def setLevel(val) {
	def valAsInt = val.intValue()
    def idx
    def cmds =[]
	def playlists = [
    				 "Playlist_1.pls",
                     "Stephanie%20list.pls"
                    ]
	log.debug "Executing 'setLevel' ${valAsInt}"
    if (valAsInt >= playlists.size())
    	idx = playlists.size()-1
    else
    	idx = valAsInt
    log.debug "Playlist: ${playlists[idx]}"
    if (device.latestValue("switch")=="off")
    	cmds << on()
	// TODO: handle 'setLevel' command
    cmds << pianoCmd("/cgi-bin/midi9cgi?playlist=select&file=${playlists[idx]}&action=load&get=ack")
    cmds << unlock()
    return cmds
}

def lock() {
	log.debug "Executing Pause"
    //sendEvent(name: "lock", value: "locked",isStateChange: true)
    sendEvent(name: "lock", value: "locking",isStateChange: true)
    return pianoCmd("/cgi-bin/midi9cgi?get=ack&navigation=pause")
}	

def unlock() {
	log.debug "Executing Play"
    //sendEvent(name: "lock", value: "unlocked",isStateChange: true)
    sendEvent(name: "lock", value: "unlocking",isStateChange: true)
    return pianoCmd("/cgi-bin/midi9cgi?get=ack&navigation=play")
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
