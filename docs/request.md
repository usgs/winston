# Winston Commands

## Protocol Description
Winston commands take the form of a single line of text terminated by <CRLF>. The line of text begins with a command name followed by a request id, separated by a space. Arguments for a command follow the request id separated by a space. 

	<req> = <cmd> <sp> <id> [<sp> <args> ] <crlf>
	<args> = <command-specific arg 1> [ <sp> <command-specific arg 2> ]
	       | <channel spec> <sp> [ <command-specific args> ]
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

## MENU
<cmd> = MENU <id> ["SCNL"]

The MENU command optionally takes the string "SCNL" as its only argument. If the "SCNL" argument is provided, location codes will be included in the returned menu.

## STATUS
<cmd> = MENU <id> [<timeout>]

The STATUS command optionally takes a single floating point number as its only argument. If a number is given, channels with no data within that number of seconds will not be used in determining the medial data age of operational channels.

## VERSION
<cmd> = VERSION <id>

The VERSION command takes no arguments.

## GETWAVERAW
<cmd> = GETWAVERAW <id> <start time> <end time> <station> <channel> <network> [<location>] <compress>

## GETCHANNELS  

## GETMETADATA

## GETSCNLHELIRAW

## GETWAVERAW

## GETSCNLRSAMRAW

## GETSCNRAW  
<cmd> = GETSCNRAW <id> <start time> <end time> <station> <channel> <network>

## GETSCNLRAW
<cmd> = GETSCNLRAW <id> <start time> <end time> <station> <channel> <network> <location>

## GETSCN

## GETSCNL