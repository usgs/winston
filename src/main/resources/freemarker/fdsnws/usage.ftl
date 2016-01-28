<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
	<head>
		<link href="/style.css" rel="stylesheet" type="text/css">
		<link href="/fdsnws.css" rel="stylesheet" type="text/css">
		<script>
			function init() { 
				initTabs(); 
				buildUrl(); 
			}
		</script>
		<title>Winston Wave Server</title>
	</head>
	<body>

		<div id="wrapper">
			<div id="intro">I'm a Winston Wave Server. I'm here to service to <A HREF="http://volcanoes.usgs.gov/software/swarm">Swarm</A> 
			and <A HREF="http://www.earthwormcentral.org/">Earthworm's</A> Wave Viewer. I will also provide plots and status info if given 
			a carefully crafted URL. Winston specific usage is described on my <a href="${baseUrl}">base page</a>. I support a subset of 
			the <a href="http://www.fdsn.org/webservices/">FDSN Web Services</a> spec. See below for more details.
			</div>
			<P><BR><P>
			<div id="tabContainer">

				<div id="tabs">
				<ul>
					<li id="tabHeader_1">URL Builder</li>
					<li id="tabHeader_2">Service Description</li>
				</ul>
			</div>

			<div id="tabscontent">
		
    		<div class="tabpage" id="tabpage_1">
				<#include service + "_UrlBuilder">
			</div>
			
			<div class="tabpage" id="tabpage_2">
				<#include service + "_InterfaceDescription">
			</div>

		</div>
	</div>
	<p><br><p>
	<b>${versionString}</b>
	</div>
	<script src="/tabs.js"></script>
	</body>
</html>

		