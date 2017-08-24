# Winston 3 Commands

## Protocol Description
Winston commands take the form of a single line of text terminated by <CRLF>. The line of text begins with a command name followed by a request id, separated by a space. Arguments for a command follow the request id separated by a space. 

	<req> = <cmd> <sp> <id> [<sp> <args> ] <crlf>
	<args> = <command-specific arg 1> [ <sp> <command-specific arg 2> ]
	       | <channel spec> <sp> [ <command-specific args> ]
	<id> = 
	<channel spec> = <SCNL> [ <sp> <time span> ]
	<SCNL> = <station> <sp> <channel> <sp> <network> <sp> [ <location> ]
	<time span> = <start time> <sp> <end time>
	<start time> = <J2kSec>
	<end time> = <J2kSec>

The Winston protocol is descended from the protocol used by Earthworm's wave_serverV.

## Supported Earthworm WaveserverV commands

The Winston Wave Server supports a subset of the Earthworm WaveserverV command set. Full details on the Earthworm protocol are available at http://love.isti.com/trac/ew/wiki/Wave_Server_Protocol_Doc

## MENU
### Description
Request listing of known stations and metadata.

### Request
    <cmd> = "MENU" <sp> <id> [<sp> "SCNL"]

If the SCNL argument is provided, location codes will always be included. This argument is not supported by Eaerthworm, which will always include location codes.

### Response
The server responds with one header line followed by one line per channel. 

#### Response Header

#### Repsonse Body

## GETSCNRAW
This is an alias for GETSCNLRAW. Either command will accept a SCN or SCNL and return dat in the form it was received in.


## GETSCNLRAW
### Request
    <cmd> = "GETSCNLRAW" <sp> <id> <sp> <scnl> <sp> <time span>
    <time span> = <epoch time> <sp> <epoch time>

### Response
#### Response Header
    <hdr> = <id> <sp> <pin #> <sp> <scnl> <sp> F <sp> <data type> <sp> <time span> <length> <cr>
          | <id> <sp> <pin #> <sp> <scnl> <sp> FG <sp> <data type> <cr>
          | <id> <sp> <pin #> <sp> <scnl> <sp> FR <sp> <data type> <youngest time> <cr>
          | <id> <sp> <pin #> <sp> <scnl> <sp> FL <sp> <data type> <oldest time> <cr>

If the request header contains the FR flags, then the requested time period is prior to all available data. No rquest body will be returned.
If the request header contains the FL flags, then the requested time period is after available data. No rquest body will be returned.
If the request header contains the FG flags, then the requested time period lies fully within a gap in data. No request body will be returned.
If the request head contains only the F flag, then data was found and a request body will be returned.

#### Response Body

    <response> = 1*<tracebuf bytes>

If data is found, it will be returned following the header as a stream of tracebuf or tracebuf2 structures. 

## GETSCN
This is an alias for SCNL. Either command will accept a SCN or SCNL.

## GETSCNL

### Request
    <cmd> = "GETSCNL" <sp> <id> <sp> <scnl> <sp> <time span>

### Response

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
