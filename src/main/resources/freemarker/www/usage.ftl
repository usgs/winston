<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
	<head>
		<link href="/style.css" rel="stylesheet" type="text/css">
	

		<script>
			var timeZones = [<#list timeZones as timeZone>"${timeZone}",</#list>];
			var channels = [<#list channels as channel>"${channel}",</#list>]

			function init() {
				initTabs();
				initTimeZones();
				initChannels();
			  buildMenuUrl();
				buildHeliUrl();
				buildRsamUrl();
				buildGapsUrl();
			}
		</script>

		<title>Winston Wave Server</title>
	</head>
	<body>
		<div id="wrapper">
			<div id="intro">
				I'm a Winston Wave Server. I'm here to service to <A HREF="http://volcanoes.usgs.gov/software/swarm">Swarm</A> 
				and <A HREF="http://www.earthwormcentral.org/">Earthworm's</A> Wave Viewer. I will also provide plots and status 
				info if given a carefully crafted URL. See the tabs below for details.
			</div>
			
			<P><BR><P>
			
			<div id="tabContainer">
				<div id="tabs">
					<ul>
						<#list httpCommandNames as httpCommand>
							<li id="tabHeader_${httpCommand?counter}">${httpCommand}</li>
						</#list>
    	  	</ul>
				</div>
				<div id="tabscontent">
					<#list httpCommands as httpCommand>
		      	<div class="tabpage" id="tabpage_${httpCommand?counter}">
			      	<h2>${httpCommand}</h2>
			        <#include httpCommand?lower_case + ".ftl" >
		    		</div>
					</#list>
				</div>
			</div>
		</div>
		<p><br><p>
		<b>${versionString}</b>
	<script src="/tabs.js"></script>
	</body>
</html>
