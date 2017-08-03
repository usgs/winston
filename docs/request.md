# Winston Commands

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

Example:

    PROTOCOL_VERSION: 3

## MENU
### Description
Request lising of know stations and metadata.

### Request
    <cmd> = "MENU" <sp> <id> [<sp> "SCNL"]
If the "SCNL" argument is provided, location codes will be included in the returned menu.

### Response
The server responds with one header line followed by one line per channel.

#### Response Header

#### Repsonse Body

## STATUS
    <cmd> = "STATUS" <sp> <id> [<sp> <timeout>]

The STATUS command optionally takes a single floating point number as its only argument. If a number is given, channels with no data within that number of seconds will not be used in determining the medial data age of operational channels.

## GETWAVERAW
    <cmd> = "GETWAVERAW" <sp> <id> <sp> <channel spec> <sp> <compress>
    <compress> = 0 | 1

## GETCHANNELS  
    <cmd> = "GETCHANNELS" <sp> <id> [ <sp> "METADATA" ]

## GETMETADATA
    <cmd> = "GETMETADATA" <sp> <id> <sp> ( "INSTRUMENT" | "CHANNEL" )

## GETSCNLHELIRAW
    <cmd> = "GETSCNLHELIRAW" <sp> <id> <sp> <scnl> <sp> <time span>

## GETWAVERAW
    <cmd> = "GETWAVERAW" <sp> <id> <sp> <scnl> <sp> <time span>

## GETSCNLRSAMRAW
    <cmd> = "GETSCNLRSAMRAW" <sp> <id> <sp> <scnl> <sp> <time span> <downsampling factor>

## GETSCNRAW  
    <cmd> = "GETSCNRAW" <sp> <id> <sp> <scnl> <sp> <time span>

## GETSCNLRAW
    <cmd> = "GETSCNLRAW" <sp> <id> <sp> <scnl> <sp> <time span>

## GETSCN
    <cmd> = "GETSCN" <sp> <id>  <sp> <scnl> <sp> <time span>

## GETSCNL
    <cmd> = "GETSCNL" <sp> <id> <sp> <scnl> <sp> <time span>