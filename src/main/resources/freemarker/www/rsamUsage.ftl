<script>
	function buildRsamUrl() {
		var urlDiv = document.getElementById("rsamUrl");
		var rsamW = document.getElementById("rsamW");
		var rsamH = document.getElementById("rsamH");
		var rsamP = document.getElementById("rsamP");
		var rsamDT = document.getElementById("rsamDT");
		var rsamDS = document.getElementById("rsamDS");
		var rsamDSP = document.getElementById("rsamDSP");
		var rsamRM = document.getElementById("rsamRM");
		var rsamRMP = document.getElementById("rsamRMP");
		var rsamMIN = document.getElementById("rsamMIN");
		var rsamMAX = document.getElementById("rsamMAX");
		var rsamTZ = document.getElementById("rsamTZ");
		var rsamT1 = document.getElementById("rsamT1");
		var rsamT2 = document.getElementById("rsamT2");
		var rsamP = document.getElementById("rsamP");
		var csv = document.getElementById("rsamCSV");
		var rsamCODE = document.getElementById("rsamCODE");

		var a = document.createElement('a');

		var linkUrl = "http://${host}/rsam?";
		linkUrl += "code=" + rsamCODE.value;

		if (rsamT1.value != "${RSAM_START_TIME}" && rsamT1.value != "") { 
			linkUrl += "&t1=" + rsamT1.value;
		}

		if (rsamT2.value != "${RSAM_END_TIME}" && rsamT2.value != "") { 
			linkUrl += "&t2=" + rsamT2.value;
		}

		if (rsamP.value != "${RSAM_RSAM_PERIOD}" && rsamP.value != "") { 
			linkUrl += "&rsamP=" + rsamP.value;
		}

		if (rsamW.value != "${RSAM_WIDTH}" && rsamW.value != "") { 
			linkUrl += "&w=" + rsamW.value;
		}
		
		if (rsamH.value != "${RSAM_HEIGHT}" && rsamH.value != "") { 
			linkUrl += "&h=" + rsamH.value;
		}

		if (rsamMIN.value != "${RSAM_MIN}" && rsamMIN.value != "") { 
			linkUrl += "&min=" + rsamMIN.value;
		}

		if (rsamMAX.value != "${RSAM_MAX}" && rsamMAX.value != "") { 
			linkUrl += "&max=" + rsamMAX.value;
		}
		
		if (${RSAM_DETREND} == 1 && rsamDT.checked == "") { 
			linkUrl += "&dt=0";
		} else if (${RSAM_DETREND} == 0 && rsamDT.checked != "") {
      linkUrl += "&dt=1";
    }

		if (${RSAM_CSV} == 1 && rsamCSV.checked == "") { 
			linkUrl += "&csv=0";
		} else if (${RSAM_CSV} == 0 && rsamCSV.checked != "") {
      linkUrl += "&csv=1";
    }


 

    if (${RSAM_DOWN_SAMPLE} == 1) {
      if (rsamDS.checked == "") { 
      	linkUrl += "&ds=0"; 
      } else if (rsamDSP.value != "${RSAM_DOWN_SAMPLE_PERIOD}" && rsamDSP.value != "") { 
      	linkUrl += "&dsp=" + rsamDSP.value;
      }
    } else {
      if (rsamDS.checked != "") { 
      	linkUrl += "&ds=1"; 
      	if (rsamDSP.value != "${RSAM_DOWN_SAMPLE_PERIOD}" && rsamDSP.value != "") { 
      		linkUrl += "&dsp=" + rsamDSP.value;
      	}
    	}
    }

    if (${RSAM_REMOVE_MEAN} == 1) {
 			if (rsamRM.checked == "") { 
 				linkUrl += "&rm=0";
 			} else if (rsamRMP.value != "${RSAM_REMOVE_MEAN_PERIOD}" && rsamRMP.value != "") { 
 				linkUrl += "&rmp=" + rsamRMP.value;
 			}
    } else {
      if (rsamRM.checked != "") { 
      	linkUrl += "&rm=1"; 
      	if (rsamRMP.value != "${RSAM_REMOVE_MEAN_PERIOD}" && rsamRMP.value != "") { 
      		linkUrl += "&rmp=" + rsamRMP.value;
      	}
      }
    }

		if (rsamTZ.value != "${TIME_ZONE}") { 
			linkUrl += "&tz=" + rsamTZ.value;
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
    rsamDSP.disabled = !rsamDS.checked; 
    rsamRMP.disabled = !rsamRM.checked;
	}
</script>

Plots precomputed RSAM values.
<div class="tabContentTitle">URL Builder</div>
<div class="tabContent">
	<FORM>
		<div class="left">
			<div class="left">
				Channel<BR>
				<select id="rsamCODE" onchange="buildRsamUrl()" name="code" class="channel" size=8></select>
			</div>
			<div class="right">
				Time zone<br>
        <select onchange="buildRsamUrl()" class="timeZone" id="rsamTZ" name="tz" size=8></select>
			</div>
			<div class="clear"></div>
			<P><br><p>
			<div class="left">
				<div class="timeInput">
					<label for="t1">Start Time</label>
				</div>
				<input type=text id="rsamT1" onchange="buildRsamUrl()" name="t1" size=10 value="${RSAM_START_TIME}">
			</div>
			<br>
			<div class="left">
				<div class="timeInput">
					<label for="t2">End Time</label>
				</div>
				<input type=text id="rsamT2" onchange="buildRsamUrl()" name="t2" size=10 value="${RSAM_END_TIME}">
			</div>
			<br>
		</div>
		<div class="right" style="padding-right:15px;">
			<div class="input">
				<label for="rsamP">RSAM period</label>
				<input type=text id="rsamP" onchange="buildRsamUrl()" name="rsamP" size=3 value="${RSAM_RSAM_PERIOD}">
			</div>
			<div class="input">
				<label for="w">Width</label>
				<input type=text id="rsamW" onchange="buildRsamUrl()" name="w" size=3 value="${RSAM_WIDTH}">
			</div>
			<div class="input">
				<label for="h">Height</label>
				<input type=text id="rsamH" onchange="buildRsamUrl()" name="h" size=3 value="${RSAM_HEIGHT}">
			</div>
			<div class="input">
				<label for="max">Plot Max</label>
				<input type=text id="rsamMAX" onchange="buildRsamUrl()" name="max" size=3 value="${RSAM_MAX}">
			</div>
			<div class="input">
				<label for="min">Plot Min</label>
				<input type=text id="rsamMIN" onchange="buildRsamUrl()" name="min" size=3 value="${RSAM_MIN}">
			</div>
			<div class="input">
				<label for="dt">Detrend?</label>
				<input class="checkbox" type=checkbox id="rsamDT" onchange="buildRsamUrl()" name="dt">
			</div>
			<div class="input">
				<label for="ds">Filter Spikes?</label>
				<input class="checkbox" type=checkbox id="rsamDS" onchange="buildRsamUrl()" name="ds">
			</div>
			<div class="input">
				<label for="dsp">Period</label>
				<input type=text id="rsamDSP" onchange="buildRsamUrl()" name="pds" size=3 value="${RSAM_DOWN_SAMPLE_PERIOD}">
			</div>
			<div class="input">
				<label for="rm">Remove Median?</label>
				<input class="checkbox" type=checkbox id="rsamRM" onchange="buildRsamUrl()" name="rm" checked>
			</div>
			<div class="input">
				<label for="rmp">Period</label>
				<input type=text id="rsamRMP" onchange="buildRsamUrl()" name="rmp" size=3 value="600">
			</div>
			<div class="input">
				<label for="csv">Return CSV?</label>
				<input class="checkbox" type=checkbox id="rsamCSV" onchange="buildRsamUrl()" name="csv">
			</div>
		</div>
		<br>
	</FORM>
	<div class="clear"></div>
	<HR class="urlBuilder">
	<b>URL:</b><BR>
	<div id="rsamUrl"></div>
</div>
<div class="tabContentTitle">Arguments</div>
<div class="tabContent">
	The options (separated by the & character, all optional except for <code>code</code> ) are defined as follows:<br><br>
	<code>w</code>: <b>Width</b> in pixels of the returned image (default = ${RSAM_WIDTH}).<br><br>
	<code>h</code>: <b>Height</b> in pixels of the returned image (default = ${RSAM_HEIGHT}).<br><br>
	<code>t1</code>: <b>Start Time</b> The start time (local) of the rsam plot as given by the number of days before present 
		or a specific time in the format YYYYMMDDHHMM.  Note that, in the first case, this is a negative number (default = 
		${RSAM_START_TIME}).<br><br>
	<code>t2</code>: <b>End Time</b> The end time (local) of the rsam plot as given by the format YYYYMMDDHHMM or 'now' 
		(default = ${RSAM_END_TIME}).<br><br>
	<code>dt</code>: <b>Detrend</b> Whether to detrend (linear) the plot, 1 is yes, 0 is no (default = ${RSAM_DETREND}).<br><br>
	<code>ds</code>: <b>Despike</b> Whether to despike (mean) the plot, 1 is yes, 0 is no (default = ${RSAM_DOWN_SAMPLE}).<br><br>"
	<code>dsp</code>: <b>Despike Period</b> Period to use for despike (default = ${RSAM_DOWN_SAMPLE_PERIOD}).<br><br>
	<code>max</code>: <b>Plot Max</b> Largest value to plot<BR><BR>
	<code>min</code>: <b>Plot Min</b> Smallest value to plot<BR><BR>
	<code>csv</code>: <b>CSV</b> Whether to return a CSV file rather than a plot, 1 is yes, 0 is no (default = ${RSAM_CSV}).<br><br>
	<code>rm</code>: <b>Running Median</b>Whether to apply a running median filter<br><br>
	<code>rmp</code>: <b>Running Median Period</b> Period to use for running medial, in seconds (defualt = ${RSAM_REMOVE_MEAN_PERIOD})<br><br>
	<code>tz</code>: <b>Time Zone</b> The time zone, a complete list of time zones that WWS understands is shown below.<br><br>
</div>