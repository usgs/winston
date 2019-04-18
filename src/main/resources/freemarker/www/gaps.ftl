<HTML>
	<HEAD>
		<TITLE>Winston Gaps</TITLE>
	</HEAD>
	<BODY>
		<TABLE CELLSPACING=5 CELLPADDING=5 STYLE="border-width: 2; border-style: solid;">
			<THEAD>
				<TD ALIGN=center COLSPAN=3><BIG><B><U>Winston Gaps</U></B></BIG></TD>
			</THEAD>
			<TBODY>
				<TR>
					<TD VALIGN=top><TABLE CELLSPACING=0 CELLPADDING=5 STYLE="border-width: 2; border-style: solid;" WIDTH=400>
						<THEAD>
							<TR>
								<TH ALIGN=center COLSPAN=2><B>Settings</B></TH>
							</TR>
						</THEAD>
						<TBODY>
							<TR>
								<TD>Start Time:</TD>
								<TD>${startTime?number_to_datetime}</TD>
							</TR>
							<TR>
								<TD>End Time:</TD>
								<TD>${endTime?number_to_datetime}</TD>
							</TR>
							<TR>
								<TD>Time Zone:</TD>
								<TD>${timeZone}</TD>
							</TR>
							<TR>
								<TD>Channel:</TD>
								<TD>${channel}</TD>
							</TR>
							<TR>
								<TD>Minimum Gap Duration:</TD>
								<TD>${minimumGapDuration} seconds</TD>
							</TR>
						</TBODY>
					</TABLE>
					<TABLE CELLSPACING=0 CELLPADDING=5 STYLE="border-width: 2; border-style: solid;" WIDTH=400>
						<THEAD>
							<TR>
								<TH ALIGN=center COLSPAN=2 ><B>Analysis</B></TH>
							</TR>
						</THEAD>
						<TR>
							<TD>Number of Gaps:</TD>
							<TD>${gaps?size}</TD>
						</TR>
						<TR>
							<TD>Total Gap Length:</TD>
							<TD>${totalGap} seconds</TD>
						</TR>
						<TR>
							<TD>Average Gap Length:</TD>
							<TD>${totalGap / gaps?size} seconds</TD>
						</TR>
						<TR>
							<TD>Data percentage:</TD>
							<TD>${100 *(1 - (totalGap / totalTime))}%</TD>
						</TR>
					</TABLE>
				</TD>
				<TD VALIGN=top>
					<TABLE CELLSPACING=0 CELLPADDING=5 STYLE="border-width: 2; border-style: solid;" WIDTH=500>
						<THEAD>
							<TR>
								<TH ALIGN=center COLSPAN=3><B>Data Gaps</B></TH>
							</TR>
						</THEAD>
						<TR>
							<TH>Gap Start Time</TH>
							<TH>Gap End Time</TH>
							<TH>Gap Duration</TH>
						</TR>
						<#list gaps as gap>
						<TR STYLE="background: #ffeeee;">
						    <#assign start = gap[0]?number_to_datetime>
						    <#assign end = gap[1]?number_to_datetime>
							<TD>${start?iso_utc}</TD>
							<TD>${end?iso_utc}</TD>
							<TD ALIGN=right>${(gap[1] - gap[0]) / 1000} seconds</TD>
						</TR>
						</#list>
					</TABLE>
				</TD>
			</TR>
			</TBODY>
			<TFOOT>
			</TFOOT>
		</TABLE>
	</BODY>
</HTML>
