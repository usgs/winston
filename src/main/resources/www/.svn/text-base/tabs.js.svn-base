
function initTabs() {
        var container = document.getElementById("tabContainer");
        var tabcon = document.getElementById("tabscontent");
        var navitem = document.getElementById("tabHeader_1");
                
        var ident = navitem.id.split("_")[1];
        navitem.parentNode.setAttribute("data-current",ident);
        navitem.setAttribute("class","tabActiveHeader");

        var pages = tabcon.getElementsByTagName("div");
        for (var i = 1; i < pages.length; i++) {
		if (pages.item(i).parentElement == tabcon) {
                	pages.item(i).style.display="none";
		}
        };

        var tabs = container.getElementsByTagName("li");
        for (var i = 0; i < tabs.length; i++) {
                tabs[i].onclick=displayPage;
        }
}

function initTimeZones() {
	var selectors = document.getElementsByClassName("timeZone");
	for (var i = 0; i < selectors.length; i++) {
		for (var j = 0; j < timeZones.length; j++) {
			option = document.createElement("option");
        		option.value = timeZones[j];
        		option.innerHTML = timeZones[j];
        		selectors[i].appendChild(option);
		}
		selectors[i].value = "UTC";
	}
}

function initChannels() {
	var selectors = document.getElementsByClassName("channel");
	for (var i = 0; i < selectors.length; i++) {
		for (var j = 0; j < channels.length; j++) {
			option = document.createElement("option");
        		option.value = channels[j];
        		option.innerHTML = channels[j];
        		selectors[i].appendChild(option);
		}
		selectors[i].selectedIndex=0;
	}
}

function displayPage() {
        var current = this.parentNode.getAttribute("data-current");
        document.getElementById("tabHeader_" + current).removeAttribute("class");
        document.getElementById("tabpage_" + current).style.display="none";

        var ident = this.id.split("_")[1];
        this.setAttribute("class","tabActiveHeader");
	var tab = document.getElementById("tabpage_" + ident);
        tab.style.display="block";
        this.parentNode.setAttribute("data-current",ident);
}

window.onload=init
