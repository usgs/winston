<script>
	function buildGapsUrl() {
		var urlDiv = document.getElementById("gapsUrl");
		var gapsWC = document.getElementById("gapsWC");
		var gapsMGD = document.getElementById("gapsMGD");
		var gapsTZ = document.getElementById("gapsTZ");
		var gapsT1 = document.getElementById("gapsT1");
		var gapsT2 = document.getElementById("gapsT2");
		var gapsCODE = document.getElementById("gapsCODE");
		var a = document.createElement('a');
		var linkUrl = "http://${host}/gaps?";
		linkUrl += "code=" + gapsCODE.value;
		if (gapsT1.value != "${GAPS_START_TIME}" && gapsT1.value != "") { 
			linkUrl += "&t1=" + gapsT1.value;
		}
		if (gapsT2.value != "${GAPS_END_TIME}" && gapsT2.value != "") { 
			linkUrl += "&t2=" + gapsT2.value;
		}
		if (gapsMGD.value != "${GAPS_MINIMUM_DURATION}" && gapsMGD.value != "") { 
			linkUrl += "&mgd=" + gapsMGD.value;
		}
		
    if (${GAPS_WC} == 1 && gapsWC.checked != "") { 
    	linkUrl += "&wc=0";
    }
    
    if (${GAPS_WC} == 0 && gapsWC.checked == "") { 
    	linkUrl += "&wc=1";
    }
    
    if (gapsTZ.value != "${TIME_ZONE}") { 
    	linkUrl += "&tz=" + gapsTZ.value;
    }
    
    linkUrl = linkUrl.replace("?&", "?");
    linkUrl = linkUrl.replace(/\?$/, "");
    a.href = linkUrl;
    a.text = linkUrl;
    a.textContent = linkUrl;
    while(urlDiv.hasChildNodes()) {
    	urlDiv.removeChild(urlDiv.lastChild);
    }
    urlDiv.appendChild(a);
	}
</script>

Locates gaps in continous data.
<div class="tabContentTitle">URL Builder</div>
<div class="tabContent">
	<FORM>
		<div class="left">
			<div class="left">
				Channel<BR>
				<select id="gapsCODE" onchange="buildGapsUrl()" name="code" class="channel" size=8></select>
			</div>
			<div class="right">Time zone<br>
				<select onchange="buildGapsUrl()" class="timeZone" id="gapsTZ" name="tz" size=8></select>
			</div>
			<div class="clear"></div>
			<P><br><p>
			<div class="label">
				<label for="t1">Start Time</label>
			</div>
			<input type=text id="gapsT1" onchange="buildGapsUrl()" name="t1" size=10 value="${GAPS_START_TIME}"><br>
			<div class="label">
				<label for="t2">End Time</label>
			</div>
			<input type=text id="gapsT2" onchange="buildGapsUrl()" name="t2" size=10 value="${GAPS_END_TIME}"><br>
		</div>
		<div class="right" style="padding-right:15px;">
			<div class="input">
				<label for="mgd">Min Gap</label>
				<input type=text id="gapsMGD" onchange="buildGapsUrl()" name="mgd" size=3 value="${GAPS_MINIMUM_DURATION}">
			</div>
			<div class="input">
				<label for="dt">Human Readable?</label>
				<input type=checkbox id="gapsWC" onchange="buildGapsUrl()" name="wc" checked>
			</div>
		</div>
	</FORM>
	<div class="clear"></div>
	<HR class="urlBuilder">
	<b>URL:</b><BR>
	<div id="gapsUrl"></div>
</div>
<div class="tabContentTitle">Arguments</div>
<div class="tabContent">
	The options (separated by the & character, all optional except for <code>code</code> ) are defined as follows:<br><br>
	This url will return data, data gaps, and analysis for the channel PS4A EHZ AV, for the last 24 hours, in Alaskan time, 
		with a minimum gap duration of 30 seconds, written for humans.<br>
	The options (separated by the & character, all optional except for code ) are defined as follows<br><br>
  code: <b>Station Name</b> The name of the Station desired<br><br>
  t1: <b>Start Time</b> The start time (local) of the gap analysis as given by the number of hours before present or a 
  	specific time in the format YYYYMMDDHHMM.
  Note that, in the first case, this is a negative number (default = ${GAPS_START_TIME}).<br><br>
  t2: <b>End Time</b> The end time (local) of the gap analysis as given by the format YYYYMMDDHHMM or 'now' (default = ${GAPS_END_TIME}).<br><br>
  tz: <b>Time Zone</b> The time zone, a complete list of time zones that WWS understands is shown below (default = ${TIME_ZONE}).<br><br>
  mgd: <b>Minimumm Gap Duration</b> The minimum gap duration desired in seconds (default = ${GAPS_MINIMUM_DURATION})<br><br>
  wc: <b>Write Computer</b> Whether to show data gaps as the computer sees, 1 is yes, 0 is no (default = ${GAPS_WC})
</div>
