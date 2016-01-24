<htmlL>
	<head>
		<title>Winston Status</title>
		<script language="javascript" type="text/javascript">
    </script>
	</head>
	<body>
		<script type="text/javascript">
			var count1Title = "&le; 1 minute old";
			var count1 = [<#list oneMinChannels?keys as chan>"${chan}",</#list>];
	
	    var countFreshTitle = "&gt; 1 minute &and; &le; 5 minutes old";
			var countFresh = [<#list fiveMinChannels?keys as chan>"${chan}",</#list>];
	    
			var count5Title = "&gt; 5 minutes &and; &le; 1 hour old";
			var count5 = [<#list oneHourChannels?keys as chan>"${chan}",</#list>];
			
			var countHourTitle = "&gt; 1 hour &and; &le; 1 day old";
			var countHour = [<#list oneDayChannels?keys as chan>"${chan}",</#list>];
			
			var countDayTitle = "> 1 day &and; &le; 4 weeks old";
			var countDay = [<#list oneMonthChannels?keys as chan>"${chan}",</#list>];
			
			var countMonthTitle = "> 4 weeks old";
			var countMonth = [<#list ancientChannels?keys as chan>"${chan}",</#list>];
		</script>
	
		<TABLE>
			<TR>
				<TD VALIGN=top>
					<TABLE CELLSPACING=0 CELLPADDING=5 STYLE="border-width: 2; border-style: solid;">
						<TR STYLE="background: #eeffee;">
							<TD ALIGN=center COLSPAN=2><B>Winston Status</B></TD>
						</TR>
						<TR STYLE="background: #eeeeff;">
							<TD>channel count</TD>
							<TD><A HREF="/menu">${channelCount}</A></TD>
						</TR>
						<TR>
							<TD>connection count</TD>
							<TD>${connectionCount}</TD>
						</TR>
						<TR STYLE="background: #eeeeff;">
							<TD>median data age</TD>
							<TD>${medianDataAge} seconds</TD>
						</TR>
						<TR>
							<TD>most recent</TD>
							<TD>${mostRecentChan} ${mostRecentTime} seconds ago</TD>
						</TR>
						<TR STYLE="background: #eeffee;">
							<TD ALIGN=center COLSPAN=2><B>Data Freshness</B></TD>
						</TR>
						<TR STYLE="background: #eeeeff;">
							<TD>&le; 1 minute</TD>
							<#assign percent = 100 * oneMinChannels?size / channelCount>
							<TD><A HREF="javascript:popup(count1Title,count1);">${oneMinChannels?size} channels</A> (${percent?round}%)</TD>
						</TR>
						<TR>
							<TD>&le; 5 minutes</TD>
							<#assign percent = 100 * fiveMinChannels?size / channelCount>
							<TD><A HREF="javascript:popup(countFreshTitle,countFresh);">${fiveMinChannels?size} channels</A> (${percent?round}%)</TD>
						</TR>
						<TR STYLE="background: #eeeeff;">
							<TD>&le; 1 hour</TD>
							<#assign percent = 100 * oneHourChannels?size / channelCount>
							<TD><A HREF="javascript:popup(count5Title,count5);">${oneHourChannels?size} channels</A> (${percent?round}%)</TD>
						</TR>
						<TR>
							<TD>&le; 1 day</TD>
							<#assign percent = 100 * oneDayChannels?size / channelCount>
							<TD><A HREF="javascript:popup(countHourTitle,countHour);">${oneDayChannels?size} channels</A> (${percent?round}%)</TD>
						</TR>
						<TR STYLE="background: #eeeeff;"><TD>&le; 4 weeks</TD>
							<#assign percent = 100 * oneMonthChannels?size / channelCount>
						<TD><A HREF="javascript:popup(countDayTitle,countDay);">${oneMonthChannels?size} channels</A> (${percent?round}%)</TD>
					</TR>
					<TR>
						<TD>&gt; 4 weeks</TD>
							<#assign percent = 100 * ancientChannels?size / channelCount>
						<TD><A HREF="javascript:popup(countMonthTitle,countMonth);">${ancientChannels?size} channels</A> (${percent?round}%)</TD>
					</TR>
				</TABLE>
				</TD>
				<TD VALIGN=top>
					<#assign interestingChannels = oneHourChannels + oneDayChannels />
					<TABLE CELLSPACING=0 CELLPADDING=5 STYLE="border-width: 2; border-style: solid;">
						<TR STYLE="background: #eeffee;">
							<TD ALIGN=center COLSPAN=2>
								<B>${interestingChannels?size} channels between <BR>5 minutes and 24 hours old</B>
							</TD>
						</TR>
							<#list interestingChannels?keys?sort as chan>
							 <#assign bgColor = (chan?counter % 2 == 0)?string("#eeeeff;","#ffffff;")>
								<TR style="background-color: ${bgColor}">
									<TD ALIGN=right>${chan}</TD>
									<TD>${(interestingChannels[chan]/60)?round} minutes</TD>
								</TR>
							</#list>
					</TABLE>
				</TD>
			</TR>
		</TABLE>
	</BODY>
</HTML>