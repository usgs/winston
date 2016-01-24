<script>
	function buildMenuUrl() {
		var menuOB = document.getElementById("menuOB");
		var menuSO = document.getElementById("menuSO");
		var menuTZ = document.getElementById("menuTZ");
		var a = document.createElement('a');
		var linkUrl = "http://${host}/menu?";
    if (menuOB.value != "${DEFAULT_ORDER_BY}") { 
    	linkUrl += "&ob=" + menuOB.value;
    }
    
    if (menuSO.value != "${DEFAULT_SORT_ORDER}") { 
    	linkUrl += "&so=" + menuSO.value;
    }

    if (menuTZ.value != "${TIME_ZONE}") { 
    	linkUrl += "&tz=" + menuTZ.value;
    }
		linkUrl = linkUrl.replace("?&", "?");
		linkUrl = linkUrl.replace(/\?$/, "");
		a.href = linkUrl;
		a.text = linkUrl;
		a.textContent = linkUrl;
		
		var urlDiv = document.getElementById("menuUrl");		
    while(urlDiv.hasChildNodes()) {
    	urlDiv.removeChild(urlDiv.lastChild);
    }
		urlDiv.appendChild(a);
	}
</script>

Returns the server menu. The menu contains the list of stations in this winston along with the least 
recent and most recent data point for each station.
<div class="tabContentTitle">URL Builder</div>
<div class="tabContent">
	<form>
		<div class="left">
			<div class="left">
				<label for="tz">Time Zone</label><br>
				<select onchange="buildMenuUrl()" class="timeZone" id="menuTZ" name="tz" size=8></select>
			</div>
			<div class="right">
				<br>
				<div class="input" style="width: 20em">
					<label for="ob">Order By</label>
					<select id="menuOB" onchange="buildMenuUrl()" name="ob">
	    			<option value=1>Pin</option>
						<option value=2 selected>Station</option>
		        <option value=3>Component</option>
						<option value=4>Network</option>
						<option value=5>Location</option>
						<option value=6>Earliest</option>
		        <option value=7>Most Recent</option>
						<option value=8>Type</Option>
					</select>
				</div>
				<br>
				<div class="input" style="width: 20em">
					<label for="so">Sort Order</label>
					<select id="menuSO" onchange="buildMenuUrl()" name="so">
						<option value="a" selected>Ascending</option>
	      	  <option value="d">Descending</option>
	  	  	</select>
				</div>
			</div>
		</div>
	</form>
	<div class="clear"></div>
	<HR class="urlBuilder"/>
	<b>URL:</b><BR><div id="menuUrl"></div>
</div>
<div class="tabContentTitle">Arguments</div>
<div class="tabContent">
	<code>ob</code>: <b>Order By</b> The column number used to order the menu (default = ${DEFAULT_ORDER_BY}).<br>
	<br>
	<code>so</code>: <b>Sort Order</b> How to order the menu, a is ascending, d is decending (default = ${DEFAULT_SORT_ORDER}).<br>
	<br>
	<code>tz</code>: <b>Time Zone</b> (deafult = ${TIME_ZONE})<br>
	<br>
</div>
<div class="tabContentTitle">Examples</div>
<div class="tabContent">
	Most recently added channels:<br><a href="http://${host}/menu?ob=6&so=d">http://${host}/menu?ob=6&so=d</a><br><p>
	<br>
  Channels with least recent data:<br><a href="http://${host}/menu?ob=7&so=a">http://${host}/menu?ob=7&so=a</a><p>
</div>
