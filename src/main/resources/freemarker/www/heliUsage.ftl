<!-- BEGIN HELI TAB -->
<script>
	function buildHeliUrl() {
		var urlDiv = document.getElementById("heliUrl");
    var heliW = document.getElementById("heliW");
    var heliH = document.getElementById("heliH");
    var heliTC = document.getElementById("heliTC");
    var heliBR = document.getElementById("heliBR");
    var heliCV = document.getElementById("heliCV");
    var heliSC = document.getElementById("heliSC");
    var heliFC = document.getElementById("heliFC");
    var heliLB = document.getElementById("heliLB");
    var heliTZ = document.getElementById("heliTZ");
    var heliT1 = document.getElementById("heliT1");
		var heliT2 = document.getElementById("heliT2");
    var heliCODE = document.getElementById("heliCODE");
    var a = document.createElement('a');
		var linkUrl = "http://${host}/heli?";
		
		linkUrl += "code=" + heliCODE.value;
    if (heliT1.value != "${DEFAULT_T1}" && heliT1.value != "") { 
    	linkUrl += "&t1=" + heliT1.value;
    }

		if (heliT2.value != "${DEFAULT_T2}" && heliT2.value != "") { 
			linkUrl += "&t2=" + heliT2.value;
		}
		
    if (heliW.value != "${DEFAULT_W}" && heliW.value != "") { 
    	linkUrl += "&w=" + heliW.value;
    }

		if (heliH.value != "${DEFAULT_H}" && heliH.value != "") { 
			linkUrl += "&h=" + heliH.value;
		}

		if (heliTC.value != "${DEFAULT_TC}" && heliTC.value != "") { 
			linkUrl += "&tc=" + heliTC.value;
		}

		if (heliBR.value != "${DEFAULT_BR}" && heliBR.value != "") { 
			linkUrl += "&br=" + heliBR.value;
		}

		if (heliCV.value != "${DEFAULT_CV}" && heliCV.value != "") { 
			linkUrl += "&cv=" + heliCV.value;
		}
		
		if (${DEFAULT_SC} == 1 && heliSC.checked == "") {
			linkUrl += "&sc=0";
		} else if (${DEFAULT_SC} == 0  && heliSC.checked != "") {
      linkUrl += "&sc=1";
    }

		if (${DEFAULT_FC} == 1 && heliFC.checked == "") {
			linkUrl += "&fc=0";
		} else if (${DEFAULT_FC} == 0 && heliFC.checked != "") {
      linkUrl += "&fc=1";
    }
	
		if (${DEFAULT_LB} == 1 && heliLB.checked == "") {
			linkUrl += "&lb=0";
		} else if (${DEFAULT_LB} == 0 && heliLB.checked != "") {
      linkUrl += "&lb=1";
    }

		if (heliTZ.value != "${DEFAULT_TZ}") { 
			linkUrl += "&tz=" + heliTZ.value;
		}

		linkUrl = linkUrl.replace("\?&", "?");
		linkUrl = linkUrl.replace(/\?$/, "");
		
		a.href = linkUrl;
		a.text = linkUrl;
		a.textContent = linkUrl;
    
    while(urlDiv.hasChildNodes()) {
    	urlDiv.removeChild(urlDiv.lastChild);
    }
    
    urlDiv.appendChild(a);
		heliCV.disabled = !heliSC.checked;
	}
</script>

Returns a helicorder plot.
<div class="tabContentTitle">URL Builder</div>
<div class="tabContent">
	<form>
		<div class="left">
			<div class="left">
				Channel<BR>
				<select id="heliCODE" onchange="buildHeliUrl()" name="code" class="channel" size=8></select>
			</div>
			<div class="right">Time zone<br>
				<select onchange="buildHeliUrl()" class="timeZone" id="heliTZ" name="tz" size=8></select>
			</div>
			<div class="clear"></div>
			<P><br><p>
			<div class="left">
				<div class="timeInput">
					<label for="t1">Start Time</label>
					<input type=text id="heliT1" onchange="buildHeliUrl()" name="t1" size=10 value="${DEFAULT_T1}">
				</div>
				<br>
				<div class="clear"></div>
				<div class="left">
					<div class="timeInput">
						<label for="t2">End Time</label>
					</div>
					<input type=text id="heliT2" onchange="buildHeliUrl()" name="t2" size=10 value="${DEFAULT_T2}">
				</div>
			</div>
		</div>
		<div class="right" style="padding-right:15px;">
			<div class="input">
				<label for="w">Width</label>
				<input type=text id="heliW" onchange="buildHeliUrl()" name="w" size=3 value="${DEFAULT_W}">
			</div>
			<div class="input">
				<label for="h">Height</label>
				<input type=text id="heliH" onchange="buildHeliUrl()" name="h" size=3 value="${DEFAULT_H}">
			</div>
			<div class="input">
				<label for="tc">Time Chunk</label>
				<input type=text id="heliTC" onchange="buildHeliUrl()" name="tc" size=3 value="${DEFAULT_TC}">
			</div>
			<div class="input">
				<label for="br">Bar Range</label>
				<input type=text id="heliBR" onchange="buildHeliUrl()" name="br" size=3 value="${DEFAULT_BR}">
			</div>
			<div class="input">
				<label for="sc">Show clip?</label>
				<input class="checkbox" type=checkbox id="heliSC" onchange="buildHeliUrl()" name="sc" checked>
			</div>
			<div class="input">
				<label for="cv">Clip Value</label>
				<input type=text id="heliCV" onchange="buildHeliUrl()" name="cv" size=3 value="${DEFAULT_CV}">
			</div>
			<div class="input">
				<label for="fc">Force center?</label>
				<input class="checkbox" type=checkbox id="heliFC" onchange="buildHeliUrl()" name="fc">
			</div>
			<div class="input">
				<label for="fc">Display Label?</label>
				<input class="checkbox" type=checkbox id="heliLB" onchange="buildHeliUrl()" name="lb" checked>
			</div>
		</div>
	</FORM>
	<div class="clear"></div>
	<HR class="urlBuilder"/>
	<b>URL:</b><BR>
	<div id="heliUrl"></div>
</div>

<div class="tabContentTitle">Arguments</div>
<div class="tabContent">
	<code>w</code>: <b>Width</b> in pixels of the returned image (default = ${DEFAULT_W}).<br><br>
	<code>h</code>: <b>Height</b> in pixels of the returned image (default = ${DEFAULT_H}).<br><br>
	<code>tc</code>: <b>Time Chunk</b> length of x axis in minutes (default = ${DEFAULT_TC}).<br><br>
	<code>t1</code>: <b>Start Time</b> The start time (local) of the helicorder as given by the number 
		of hours before present or a specific time in the format YYYYMMDDHHMM.  Note that, in the first 
		case, this is a negative number (default = ${DEFAULT_T1}).<br><br>
	<code>t2</code>: <b>End Time</b> The end time (local) of the helicorder as given by the format 
		YYYYMMDDHHMM or 'now' (default = ${DEFAULT_T2}).<br><br>
	<code>tz</code>: <b>Time Zone</b> The time zone, a complete list of time zones that WWS understands 
		is shown below.<br><br>
	<code>sc</code>: <b>Show Clip</b> Whether to show a clipped value as red, 1 is yes, 0 is no (default 
		= ${DEFAULT_SC}).<br><br>
	<code>fc</code>: <b>Force Center</b> Whether to center traces, 1 is yes, 0 is no (default = 
		${DEFAULT_FC}).<br><br>
	<code>br</code>: <b>Bar Range</b> Controls the size of helicorder lines (default = ${DEFAULT_BR}).<br><br>
	<code>cv</code>: <b>Clip Value</b> Sets the number of counts above which to clip (default = ${DEFAULT_CV}).
		<br><br>
  <code>lb</code>: <b>Label</b> Whether to display a large label, 1 is yes, 0 is no (default = ${DEFAULT_LB}).
  	<br><br>
  
  The WWS does basic argument checking to prevent malignant attacks (like SQL injection) or just silly requests 
  	(like a 10000 x 10000 pixel graph).

</div>
<!-- END HELI TAB -->