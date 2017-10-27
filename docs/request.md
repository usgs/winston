# Winston 3 Commands

## Protocol Description
Winston commands take the form of a single line of text terminated by `<CRLF>`. The line of text begins with a command name followed by a request id, separated by a space. Arguments request id separated by a space. 

	request = cmd SP req-id [SP args ] CRLF
	args = command-arg [ SP command-arg ]
	     =/ channel-spec [ SP command-arg ]
	req-id = 1*CHAR
	channel-spec = scnl [ SP time-span ]
	scnl = station SP channel SP network [ SP location ]
	time-span = time-span-j2ksec / time-span-ew
	time-span-j2ksec = j2ksec SP j2ksec
	time-span-ew = unix-time SP unix-time
	J2kSec = 1*DIGIT [ "." *DIGIT ] ; seconds since 2000-01-01T12:00:00+00:00
	unix-time = 1*DIGIT [ "." *DIGIT ] ; seconds since 1970-01-01T00:00:00+00:00
	
The Winston protocol is descended from the protocol used by Earthworm's wave_serverV.

## Supported Earthworm WaveserverV commands

The Winston Wave Server supports a subset of the Earthworm WaveserverV command set. Full details on the Earthworm protocol are available at http://love.isti.com/trac/ew/wiki/Wave_Server_Protocol_Doc

## MENU
### Description
Request listing of known stations and metadata.

### Request
    request =  "MENU" *1":" SP req-id [ SP "SCNL" ] CRLF
              
If the SCNL argument is provided, location codes will always be included.

### Response
	response = req-id SP SP channel-record-list CRLF
	channel-record-list = channel-record *(SP SP channel-record)
	channel-record = pin SP scnl SP time-span-ew SP data-type

## GETSCNRAW
This is an alias for GETSCNLRAW. Either command will accept a SCN or SCNL and return dat in the form it was received in.


## GETSCNLRAW
### Request
    request = "GETSCNLRAW" SP req-id SP scnl SP time-span-ew CRLF

### Response
	response = header CRLF *tracebuf
    header =  req-id SP pin SP scnl SP "F" SP data-tpye ST time-span-ew length CRLF
           =/ req-id SP pin SP scnl SP "FG" SP data-tpye CRLF
           =/ req-id SP pin SP scnl SP "FR"SP data-tpye unix-time CRLF
           =/ req-id SP pin SP scnl SP "FL"SP data-tpye unix-time CRLF
           
If the request header contains the FR flags, then the requested time period is prior to all available data. No request body will be returned.
If the request header contains the FL flags, then the requested time period is after available data. No request body will be returned.
If the request header contains the FG flags, then the requested time period lies fully within a gap in data. No request body will be returned.
If the request head contains only the F flag, then data was found and a request body will be returned.

If data is found, it will be returned following the header as a stream of tracebuf or tracebuf2 structures. 

## GETSCN
This is an alias for SCNL. Either command will accept a SCN or SCNL.

## GETSCNL

### Request
    request = "GETSCNL" SP req-id SP scnl SP time-span-ew CRLF

### Response
	response = header CRLF *samples
    header =  req-id SP pin SP scnl SP "F" SP data-tpye ST time-span-ew length CRLF
           =/ req-id SP pin SP scnl SP "FG" SP data-tpye CRLF
           =/ req-id SP pin SP scnl SP "FR"SP data-tpye unix-time CRLF
           =/ req-id SP pin SP scnl SP "FL"SP data-tpye unix-time CRLF

## Winston Commands
Winston commands consist of a sequence of characters optionally terminated by a colon. Times in winston requests are specified as J2kSec. 
  
## Request Id
The request id is returned with the request response and can be used to pair issued requests with a response.

## VERSION
### Description
Request WWS protocol version. This is the only WWS command which does not accept an ID argument.

### Request
    <req> = "VERSION" <cr>
    
### Response Header
None.

### Response Body
     <response> = "PROTOCOL_VERSION:" <sp> <protocol version><cr>


## STATUS
Retrieve server status.

### Request
    <cmd> = "STATUS" <sp> <id> [<sp> <timeout>]

If the timeout value is given, stations which do not have data within <timeout> seconds will not be considered when calculating median data age.

### Response

The STATUS command optionally takes a single floating point number as its only argument. If a number is given, channels with no data within that number of seconds will not be used in determining the medial data age of operational channels.

## GETWAVERAW
    <cmd> = "GETWAVERAW" <sp> <id> <sp> <channel spec> <sp> <compress>
    <compress> = 0 | 1

## GETCHANNELS  
### Description
Retrieve a list of channels in the Winston.
	
### Request
    <cmd> = "GETCHANNELS" <sp> <id> [ <sp> "METADATA" ]

### Response
One header line, followed by one line for each channel in the Winston. 

The Header line consists of two space-separated fields:
1. request id
1. number of channel lines

Each channel line is a colon-separated string with the following values:
1. station id
1. $-spearated SCNL
1. earliest sample as J2kSec
1. most recent sample as J2kSec
1. instrument longitude
1. instrument latitude
1. alias
1. unit
1. linear a
1. linear b
1. groups 

The last five fields are only provided if the METADATA argument was provided with the request.

## GETMETADATA
    <cmd> = "GETMETADATA" <sp> <id> <sp> ( "INSTRUMENT" | "CHANNEL" )

## GETSCNLHELIRAW
    <cmd> = "GETSCNLHELIRAW" <sp> <id> <sp> <scnl> <sp> <time span>

## GETSCNLRSAMRAW
    <cmd> = "GETSCNLRSAMRAW" <sp> <id> <sp> <scnl> <sp> <time span> <downsampling factor>
