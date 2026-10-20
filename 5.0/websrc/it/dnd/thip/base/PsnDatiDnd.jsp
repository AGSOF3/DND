<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.0 Transitional//EN"
                      "file:///K:/Thip/5.1.0/websrcsvil/dtd/xhtml1-transitional.dtd">
<html>
<!-- WIZGEN Therm 2.0.0 as Form - multiBrowserGen = true -->
<%=WebGenerator.writeRuntimeInfo()%>
<head>
<%@ page contentType="text/html; charset=Cp1252"%>
<%@ page import= " 
  java.sql.*, 
  java.util.*, 
  java.lang.reflect.*, 
  javax.naming.*, 
  com.thera.thermfw.common.*, 
  com.thera.thermfw.type.*, 
  com.thera.thermfw.web.*, 
  com.thera.thermfw.security.*, 
  com.thera.thermfw.base.*, 
  com.thera.thermfw.ad.*, 
  com.thera.thermfw.persist.*, 
  com.thera.thermfw.gui.cnr.*, 
  com.thera.thermfw.setting.*, 
  com.thera.thermfw.collector.*, 
  com.thera.thermfw.batch.web.*, 
  com.thera.thermfw.batch.*, 
  com.thera.thermfw.pref.* 
"%> 
<%
  ServletEnvironment se = (ServletEnvironment)Factory.createObject("com.thera.thermfw.web.ServletEnvironment"); 
  BODataCollector PsnDatiDndBODC = null; 
  List errors = new ArrayList(); 
  WebJSTypeList jsList = new WebJSTypeList(); 
  WebForm PsnDatiDndForm =  
     new com.thera.thermfw.web.WebForm(request, response, "PsnDatiDndForm", "PsnDatiDnd", null, "com.thera.thermfw.web.servlet.FormActionAdapter", false, false, true, true, true, true, null, 0, true, "it/dnd/thip/base/PsnDatiDnd.js"); 
  PsnDatiDndForm.setServletEnvironment(se); 
  PsnDatiDndForm.setJSTypeList(jsList); 
  PsnDatiDndForm.setHeader("it.thera.thip.cs.PantheraHeader.jsp"); 
  PsnDatiDndForm.setFooter("com.thera.thermfw.common.Footer.jsp"); 
  PsnDatiDndForm.setWebFormModifierClass("it.dnd.thip.base.web.PsnDatiDndFormModifier"); 
  PsnDatiDndForm.setDeniedAttributeModeStr("hideNone"); 
  int mode = PsnDatiDndForm.getMode(); 
  String key = PsnDatiDndForm.getKey(); 
  String errorMessage; 
  boolean requestIsValid = false; 
  boolean leftIsKey = false; 
  boolean conflitPresent = false; 
  String leftClass = ""; 
  try 
  {
     se.initialize(request, response); 
     if(se.begin()) 
     { 
        PsnDatiDndForm.outTraceInfo(getClass().getName()); 
        String collectorName = PsnDatiDndForm.findBODataCollectorName(); 
                PsnDatiDndBODC = (BODataCollector)Factory.createObject(collectorName); 
        if (PsnDatiDndBODC instanceof WebDataCollector) 
            ((WebDataCollector)PsnDatiDndBODC).setServletEnvironment(se); 
        PsnDatiDndBODC.initialize("PsnDatiDnd", true, 0); 
        PsnDatiDndForm.setBODataCollector(PsnDatiDndBODC); 
        int rcBODC = PsnDatiDndForm.initSecurityServices(); 
        mode = PsnDatiDndForm.getMode(); 
        if (rcBODC == BODataCollector.OK) 
        { 
           requestIsValid = true; 
           PsnDatiDndForm.write(out); 
           if(mode != WebForm.NEW) 
              rcBODC = PsnDatiDndBODC.retrieve(key); 
           if(rcBODC == BODataCollector.OK) 
           { 
              PsnDatiDndForm.writeHeadElements(out); 
           // fine blocco XXX  
           // a completamento blocco di codice YYY a fine body con catch e gestione errori 
%> 
<% 
  WebMenuBar menuBar = new com.thera.thermfw.web.WebMenuBar("HM_Array1", "150", "#000000","#000000","#A5B6CE","#E4EAEF","#FFFFFF","#000000"); 
  menuBar.setParent(PsnDatiDndForm); 
   request.setAttribute("menuBar", menuBar); 
%> 
<jsp:include page="/it/thera/thip/cs/defObjMenu.jsp" flush="true"> 
<jsp:param name="partRequest" value="menuBar"/> 
</jsp:include> 
<% 
  menuBar.write(out); 
  menuBar.writeChildren(out); 
%> 
<% 
  WebToolBar myToolBarTB = new com.thera.thermfw.web.WebToolBar("myToolBar", "24", "24", "16", "16", "#f7fbfd","#C8D6E1"); 
  myToolBarTB.setParent(PsnDatiDndForm); 
   request.setAttribute("toolBar", myToolBarTB); 
%> 
<jsp:include page="/it/thera/thip/cs/defObjMenu.jsp" flush="true"> 
<jsp:param name="partRequest" value="toolBar"/> 
</jsp:include> 
<% 
   myToolBarTB.write(out); 
%> 
</head>
<body onbeforeunload="<%=PsnDatiDndForm.getBodyOnBeforeUnload()%>" onload="<%=PsnDatiDndForm.getBodyOnLoad()%>" onunload="<%=PsnDatiDndForm.getBodyOnUnload()%>" style="margin: 0px; overflow: hidden;"><%
   PsnDatiDndForm.writeBodyStartElements(out); 
%> 

	<table width="100%" height="100%" cellspacing="0" cellpadding="0">
<tr>
<td style="height:0" valign="top">
<% String hdr = PsnDatiDndForm.getCompleteHeader();
 if (hdr != null) { 
   request.setAttribute("dataCollector", PsnDatiDndBODC); 
   request.setAttribute("servletEnvironment", se); %>
  <jsp:include page="<%= hdr %>" flush="true"/> 
<% } %> 
</td>
</tr>

<tr>
<td valign="top" height="100%">
<form action="<%=PsnDatiDndForm.getServlet()%>" method="post" name="PsnDatiDndForm" style="height:100%"><%
  PsnDatiDndForm.writeFormStartElements(out); 
%>

		<table cellpadding="0" cellspacing="0" height="100%" id="emptyborder" width="100%">
			<tr>
				<td style="height: 0"><% menuBar.writeElements(out); %> 
</td>
			</tr>
			<tr>
				<td style="height: 0"><% myToolBarTB.writeChildren(out); %> 
</td>
			</tr>
			<tr>
				<td height="100%"><!--<span class="tabbed" id="mytabbed">-->
<table width="100%" height="100%" cellpadding="0" cellspacing="0" style="padding-right:1px">
   <tr valign="top">
     <td><% 
  WebTabbed mytabbed = new com.thera.thermfw.web.WebTabbed("mytabbed", "100%", "100%"); 
  mytabbed.setParent(PsnDatiDndForm); 
 mytabbed.addTab("tab1", "it.dnd.thip.base.resources.PsnDatiDnd", "tab1", "PsnDatiDnd", null, null, null, null); 
  mytabbed.write(out); 
%>

     </td>
   </tr>
   <tr>
     <td height="100%"><div class="tabbed_pagine" id="tabbedPagine" style="position: relative; width: 100%; height: 100%;"> <div class="tabbed_page" id="<%=mytabbed.getTabPageId("tab1")%>" style="width:100%;height:100%;overflow:auto;"><% mytabbed.startTab("tab1"); %>
							<table>
								<tr>
									<td colspan="2">
										<fieldset>
											<legend>Gestione Scarti - Raccolta Dati</legend>
											<table>
												<tr>
													<td valign="top"><%{  WebLabelCompound label = new com.thera.thermfw.web.WebLabelCompound(null, null, "PsnDatiDnd", "IdMagScartiDichPrel", null); 
   label.setParent(PsnDatiDndForm); 
%><label class="<%=label.getClassType()%>" for="MagazzinoScartiDP"><%label.write(out);%></label><%}%></td>
													<td valign="top"><% 
  WebMultiSearchForm PsnDatiDndMagazzinoScartiDP =  
     new com.thera.thermfw.web.WebMultiSearchForm("PsnDatiDnd", "MagazzinoScartiDP", false, false, true, 1, null, null); 
  PsnDatiDndMagazzinoScartiDP.setParent(PsnDatiDndForm); 
  PsnDatiDndMagazzinoScartiDP.write(out); 
%>
<!--<span class="multisearchform" id="MagazzinoScartiDP"></span>--></td>
												</tr>
												<tr>
													<td valign="top"><%{  WebLabelCompound label = new com.thera.thermfw.web.WebLabelCompound(null, null, "PsnDatiDnd", "CodiceUbicazioneScarico", null); 
   label.setParent(PsnDatiDndForm); 
%><label class="<%=label.getClassType()%>" for="UbicazioneScaricoDefault"><%label.write(out);%></label><%}%></td>
													<td valign="top"><% 
  WebMultiSearchForm PsnDatiDndUbicazioneScaricoDefault =  
     new com.thera.thermfw.web.WebMultiSearchForm("PsnDatiDnd", "UbicazioneScaricoDefault", false, false, true, 2, null, null); 
  PsnDatiDndUbicazioneScaricoDefault.setParent(PsnDatiDndForm); 
  PsnDatiDndUbicazioneScaricoDefault.write(out); 
%>
<!--<span class="multisearchform" id="UbicazioneScaricoDefault"></span>--></td>
												</tr>
												<tr>
													<td colspan="2" valign="top"><!--<span class="editgrid" id="CollegamentoMagPrlMagScr">--><% 
  WebEditGrid PsnDatiDndCollegamentoMagPrlMagScr =  
     new com.thera.thermfw.web.WebEditGrid("PsnDatiDnd", "CollegamentoMagPrlMagScr", 8, null, 1, null, null,false,"com.thera.thermfw.web.servlet.GridActionAdapterForIndependentRow"); 
 PsnDatiDndCollegamentoMagPrlMagScr.setParent(PsnDatiDndForm); 
 PsnDatiDndCollegamentoMagPrlMagScr.setNoControlRowKeys(false); 
 PsnDatiDndCollegamentoMagPrlMagScr.write(out); 
%>
<BR><% 
   request.setAttribute("parentForm", PsnDatiDndForm); 
   String CDForCollegamentoMagPrlMagScr = "CollegamentoMagPrlMagScr"; 
%>
<jsp:include page="/it/dnd/thip/base/CollMagPrlOperazioneMovim.jsp" flush="true"> 
<jsp:param name="EditGridCDName" value="<%=CDForCollegamentoMagPrlMagScr%>"/> 
<jsp:param name="Mode" value="NEW"/> 
</jsp:include> 
<!--</span>--></td>
												</tr>
											</table>
										</fieldset>
									</td>
								</tr>
								<tr>
									<td colspan="2">
										<fieldset>
											<legend>Ordini conf. da MV</legend>
											<table>
												<tr>
													<td valign="top"><%{  WebLabelCompound label = new com.thera.thermfw.web.WebLabelCompound(null, null, "PsnDatiDnd", "IdSerieODP", null); 
   label.setParent(PsnDatiDndForm); 
%><label class="<%=label.getClassType()%>" for="SerieODP"><%label.write(out);%></label><%}%></td>
													<td valign="top"><% 
  WebMultiSearchForm PsnDatiDndSerieODP =  
     new com.thera.thermfw.web.WebMultiSearchForm("PsnDatiDnd", "SerieODP", false, false, true, 2, null, null); 
  PsnDatiDndSerieODP.setParent(PsnDatiDndForm); 
  PsnDatiDndSerieODP.write(out); 
%>
<!--<span class="multisearchform" id="SerieODP"></span>--></td>
												</tr>
												<tr>
													<td valign="top"><%{  WebLabelCompound label = new com.thera.thermfw.web.WebLabelCompound(null, null, "PsnDatiDnd", "IdAttivitaODP", null); 
   label.setParent(PsnDatiDndForm); 
%><label class="<%=label.getClassType()%>" for="AttivitaODP"><%label.write(out);%></label><%}%></td>
													<td valign="top"><% 
  WebMultiSearchForm PsnDatiDndAttivitaODP =  
     new com.thera.thermfw.web.WebMultiSearchForm("PsnDatiDnd", "AttivitaODP", false, false, true, 1, null, null); 
  PsnDatiDndAttivitaODP.setParent(PsnDatiDndForm); 
  PsnDatiDndAttivitaODP.write(out); 
%>
<!--<span class="multisearchform" id="AttivitaODP"></span>--></td>
												</tr>
											</table>
										</fieldset>
									</td>
								</tr>
								
								<tr>
									<td colspan="2">
										<fieldset>
											<legend>Riallineamento giacenze logis</legend>
											<table>
												<tr>
													<td valign="top"><%{  WebLabelCompound label = new com.thera.thermfw.web.WebLabelCompound(null, null, "PsnDatiDnd", "SerieDocGnrRiallLogis", null); 
   label.setParent(PsnDatiDndForm); 
%><label class="<%=label.getClassType()%>" for="SerieDocGnr"><%label.write(out);%></label><%}%></td>
													<td valign="top"><% 
  WebMultiSearchForm PsnDatiDndSerieDocGnr =  
     new com.thera.thermfw.web.WebMultiSearchForm("PsnDatiDnd", "SerieDocGnr", false, false, true, 2, null, null); 
  PsnDatiDndSerieDocGnr.setParent(PsnDatiDndForm); 
  PsnDatiDndSerieDocGnr.write(out); 
%>
<!--<span class="multisearchform" id="SerieDocGnr"></span>--></td>
												</tr>
												<tr>
													<td valign="top"><%{  WebLabelCompound label = new com.thera.thermfw.web.WebLabelCompound(null, null, "PsnDatiDnd", "RCauDocGnrRiallLogis", null); 
   label.setParent(PsnDatiDndForm); 
%><label class="<%=label.getClassType()%>" for="CauDocGenRiallLogis"><%label.write(out);%></label><%}%></td>
													<td valign="top"><% 
  WebMultiSearchForm PsnDatiDndCauDocGenRiallLogis =  
     new com.thera.thermfw.web.WebMultiSearchForm("PsnDatiDnd", "CauDocGenRiallLogis", false, false, true, 1, null, null); 
  PsnDatiDndCauDocGenRiallLogis.setParent(PsnDatiDndForm); 
  PsnDatiDndCauDocGenRiallLogis.write(out); 
%>
<!--<span class="multisearchform" id="CauDocGenRiallLogis"></span>--></td>
												</tr>
											</table>
										</fieldset>
									</td>
								</tr>
								
							</table>
					<% mytabbed.endTab(); %> 
</div>
				</div><% mytabbed.endTabbed();%> 

     </td>
   </tr>
</table><!--</span>--></td>
			</tr>
			<tr>
				<td style="height: 0"><% 
  WebErrorList errorList = new com.thera.thermfw.web.WebErrorList(); 
  errorList.setParent(PsnDatiDndForm); 
  errorList.write(out); 
%>
<!--<span class="errorlist"></span>--></td>
			</tr>
		</table>
	<%
  PsnDatiDndForm.writeFormEndElements(out); 
%>
</form></td>
</tr>

<tr>
<td style="height:0">
<% String ftr = PsnDatiDndForm.getCompleteFooter();
 if (ftr != null) { 
   request.setAttribute("dataCollector", PsnDatiDndBODC); 
   request.setAttribute("servletEnvironment", se); %>
  <jsp:include page="<%= ftr %>" flush="true"/> 
<% } %> 
</td>
</tr>
</table>


<%
           // blocco YYY  
           // a completamento blocco di codice XXX in head 
              PsnDatiDndForm.writeBodyEndElements(out); 
           } 
           else 
              errors.addAll(0, PsnDatiDndBODC.getErrorList().getErrors()); 
        } 
        else 
           errors.addAll(0, PsnDatiDndBODC.getErrorList().getErrors()); 
           if(PsnDatiDndBODC.getConflict() != null) 
                conflitPresent = true; 
     } 
     else 
        errors.add(new ErrorMessage("BAS0000010")); 
  } 
  catch(NamingException e) { 
     errorMessage = e.getMessage(); 
     errors.add(new ErrorMessage("CBS000025", errorMessage));  } 
  catch(SQLException e) {
     errorMessage = e.getMessage(); 
     errors.add(new ErrorMessage("BAS0000071", errorMessage));  } 
  catch(Throwable e) {
     e.printStackTrace(Trace.excStream);
  }
  finally 
  {
     if(PsnDatiDndBODC != null && !PsnDatiDndBODC.close(false)) 
        errors.addAll(0, PsnDatiDndBODC.getErrorList().getErrors()); 
     try 
     { 
        se.end(); 
     }
     catch(IllegalArgumentException e) { 
        e.printStackTrace(Trace.excStream); 
     } 
     catch(SQLException e) { 
        e.printStackTrace(Trace.excStream); 
     } 
  } 
  if(!errors.isEmpty())
  { 
      if(!conflitPresent)
  { 
     request.setAttribute("ErrorMessages", errors); 
     String errorPage = PsnDatiDndForm.getErrorPage(); 
%> 
     <jsp:include page="<%=errorPage%>" flush="true"/> 
<% 
  } 
  else 
  { 
     request.setAttribute("ConflictMessages", PsnDatiDndBODC.getConflict()); 
     request.setAttribute("ErrorMessages", errors); 
     String conflictPage = PsnDatiDndForm.getConflictPage(); 
%> 
     <jsp:include page="<%=conflictPage%>" flush="true"/> 
<% 
   } 
   } 
%> 
</body>
</html>
