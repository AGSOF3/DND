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
  BODataCollector YFatturaVenditaSTATBODC = null; 
  List errors = new ArrayList(); 
  WebJSTypeList jsList = new WebJSTypeList(); 
  WebForm YFatturaVenditaSTATForm =  
     new com.thera.thermfw.web.WebForm(request, response, "YFatturaVenditaSTATForm", "YFatturaVenditaSTAT", null, "com.thera.thermfw.web.servlet.FormActionAdapter", false, false, true, true, true, true, null, 0, true, "it/dnd/thip/vendite/documentoVE/YFatturaVenditaSTAT.js"); 
  YFatturaVenditaSTATForm.setServletEnvironment(se); 
  YFatturaVenditaSTATForm.setJSTypeList(jsList); 
  YFatturaVenditaSTATForm.setHeader("it.thera.thip.cs.PantheraHeader.jsp"); 
  YFatturaVenditaSTATForm.setFooter("com.thera.thermfw.common.Footer.jsp"); 
  YFatturaVenditaSTATForm.setDeniedAttributeModeStr("hideNone"); 
  int mode = YFatturaVenditaSTATForm.getMode(); 
  String key = YFatturaVenditaSTATForm.getKey(); 
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
        YFatturaVenditaSTATForm.outTraceInfo(getClass().getName()); 
        String collectorName = YFatturaVenditaSTATForm.findBODataCollectorName(); 
                YFatturaVenditaSTATBODC = (BODataCollector)Factory.createObject(collectorName); 
        if (YFatturaVenditaSTATBODC instanceof WebDataCollector) 
            ((WebDataCollector)YFatturaVenditaSTATBODC).setServletEnvironment(se); 
        YFatturaVenditaSTATBODC.initialize("YFatturaVenditaSTAT", true, 0); 
        YFatturaVenditaSTATForm.setBODataCollector(YFatturaVenditaSTATBODC); 
        int rcBODC = YFatturaVenditaSTATForm.initSecurityServices(); 
        mode = YFatturaVenditaSTATForm.getMode(); 
        if (rcBODC == BODataCollector.OK) 
        { 
           requestIsValid = true; 
           YFatturaVenditaSTATForm.write(out); 
           if(mode != WebForm.NEW) 
              rcBODC = YFatturaVenditaSTATBODC.retrieve(key); 
           if(rcBODC == BODataCollector.OK) 
           { 
              YFatturaVenditaSTATForm.writeHeadElements(out); 
           // fine blocco XXX  
           // a completamento blocco di codice YYY a fine body con catch e gestione errori 
%> 
<% 
  WebMenuBar menuBar = new com.thera.thermfw.web.WebMenuBar("HM_Array1", "150", "#000000","#000000","#A5B6CE","#E4EAEF","#FFFFFF","#000000"); 
  menuBar.setParent(YFatturaVenditaSTATForm); 
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
  myToolBarTB.setParent(YFatturaVenditaSTATForm); 
   request.setAttribute("toolBar", myToolBarTB); 
%> 
<jsp:include page="/it/thera/thip/cs/defObjMenu.jsp" flush="true"> 
<jsp:param name="partRequest" value="toolBar"/> 
</jsp:include> 
<% 
   myToolBarTB.write(out); 
%> 
</head>
<body onbeforeunload="<%=YFatturaVenditaSTATForm.getBodyOnBeforeUnload()%>" onload="<%=YFatturaVenditaSTATForm.getBodyOnLoad()%>" onunload="<%=YFatturaVenditaSTATForm.getBodyOnUnload()%>" style="margin: 0px; overflow: hidden;"><%
   YFatturaVenditaSTATForm.writeBodyStartElements(out); 
%> 

	<table width="100%" height="100%" cellspacing="0" cellpadding="0">
<tr>
<td style="height:0" valign="top">
<% String hdr = YFatturaVenditaSTATForm.getCompleteHeader();
 if (hdr != null) { 
   request.setAttribute("dataCollector", YFatturaVenditaSTATBODC); 
   request.setAttribute("servletEnvironment", se); %>
  <jsp:include page="<%= hdr %>" flush="true"/> 
<% } %> 
</td>
</tr>

<tr>
<td valign="top" height="100%">
<form action="<%=YFatturaVenditaSTATForm.getServlet()%>" method="post" name="YFatturaVenditaSTATForm" style="height:100%"><%
  YFatturaVenditaSTATForm.writeFormStartElements(out); 
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
				<td><% 
  WebTextInput YFatturaVenditaSTATIdAzienda =  
     new com.thera.thermfw.web.WebTextInput("YFatturaVenditaSTAT", "IdAzienda"); 
  YFatturaVenditaSTATIdAzienda.setParent(YFatturaVenditaSTATForm); 
%>
<input class="<%=YFatturaVenditaSTATIdAzienda.getClassType()%>" id="<%=YFatturaVenditaSTATIdAzienda.getId()%>" maxlength="<%=YFatturaVenditaSTATIdAzienda.getMaxLength()%>" name="<%=YFatturaVenditaSTATIdAzienda.getName()%>" size="<%=YFatturaVenditaSTATIdAzienda.getSize()%>" type="hidden"><% 
  YFatturaVenditaSTATIdAzienda.write(out); 
%>
</td>
			</tr>
			<tr>
				<td height="100%"><!--<span class="tabbed" id="mytabbed">-->
<table width="100%" height="100%" cellpadding="0" cellspacing="0" style="padding-right:1px">
   <tr valign="top">
     <td><% 
  WebTabbed mytabbed = new com.thera.thermfw.web.WebTabbed("mytabbed", "100%", "100%"); 
  mytabbed.setParent(YFatturaVenditaSTATForm); 
 mytabbed.addTab("tab1", "it.dnd.thip.tuttoimballo.stampanti.resources.YInterfStampanti", "tab1", "YFatturaVenditaSTAT", null, null, null, null); 
  mytabbed.write(out); 
%>

     </td>
   </tr>
   <tr>
     <td height="100%"><div class="tabbed_pagine" id="tabbedPagine" style="position: relative; width: 100%; height: 100%;"> <div class="tabbed_page" id="<%=mytabbed.getTabPageId("tab1")%>" style="width:100%;height:100%;overflow:auto;"><% mytabbed.startTab("tab1"); %>
							<table>
								<tr>
									<td valign="top"><% 
  WebCheckBox YFatturaVenditaSTATRoyalty =  
     new com.thera.thermfw.web.WebCheckBox("YFatturaVenditaSTAT", "Royalty"); 
  YFatturaVenditaSTATRoyalty.setParent(YFatturaVenditaSTATForm); 
%>
<input id="<%=YFatturaVenditaSTATRoyalty.getId()%>" name="<%=YFatturaVenditaSTATRoyalty.getName()%>" type="checkbox" value="Y"><%
  YFatturaVenditaSTATRoyalty.write(out); 
%>
</td>
								</tr>
								<tr>
									<td><%{  WebLabelCompound label = new com.thera.thermfw.web.WebLabelCompound(null, null, "YFatturaVenditaSTAT", "PercRoyalty", null); 
   label.setParent(YFatturaVenditaSTATForm); 
%><label class="<%=label.getClassType()%>" for="PercRoyalty"><%label.write(out);%></label><%}%></td>
									<td valign="top"><% 
  WebTextInput YFatturaVenditaSTATPercRoyalty =  
     new com.thera.thermfw.web.WebTextInput("YFatturaVenditaSTAT", "PercRoyalty"); 
  YFatturaVenditaSTATPercRoyalty.setParent(YFatturaVenditaSTATForm); 
%>
<input class="<%=YFatturaVenditaSTATPercRoyalty.getClassType()%>" id="<%=YFatturaVenditaSTATPercRoyalty.getId()%>" maxlength="<%=YFatturaVenditaSTATPercRoyalty.getMaxLength()%>" name="<%=YFatturaVenditaSTATPercRoyalty.getName()%>" size="<%=YFatturaVenditaSTATPercRoyalty.getSize()%>"><% 
  YFatturaVenditaSTATPercRoyalty.write(out); 
%>
</td>
								</tr>
								<tr style="visibility:hidden">
									<td valign="top"><% 
  WebCheckBox YFatturaVenditaSTATSaveVideo =  
     new com.thera.thermfw.web.WebCheckBox("YFatturaVenditaSTAT", "SaveVideo"); 
  YFatturaVenditaSTATSaveVideo.setParent(YFatturaVenditaSTATForm); 
%>
<input id="<%=YFatturaVenditaSTATSaveVideo.getId()%>" name="<%=YFatturaVenditaSTATSaveVideo.getName()%>" type="checkbox" value="Y"><%
  YFatturaVenditaSTATSaveVideo.write(out); 
%>
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
  errorList.setParent(YFatturaVenditaSTATForm); 
  errorList.write(out); 
%>
<!--<span class="errorlist"></span>--></td>
			</tr>
		</table>
	<%
  YFatturaVenditaSTATForm.writeFormEndElements(out); 
%>
</form></td>
</tr>

<tr>
<td style="height:0">
<% String ftr = YFatturaVenditaSTATForm.getCompleteFooter();
 if (ftr != null) { 
   request.setAttribute("dataCollector", YFatturaVenditaSTATBODC); 
   request.setAttribute("servletEnvironment", se); %>
  <jsp:include page="<%= ftr %>" flush="true"/> 
<% } %> 
</td>
</tr>
</table>


<%
           // blocco YYY  
           // a completamento blocco di codice XXX in head 
              YFatturaVenditaSTATForm.writeBodyEndElements(out); 
           } 
           else 
              errors.addAll(0, YFatturaVenditaSTATBODC.getErrorList().getErrors()); 
        } 
        else 
           errors.addAll(0, YFatturaVenditaSTATBODC.getErrorList().getErrors()); 
           if(YFatturaVenditaSTATBODC.getConflict() != null) 
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
     if(YFatturaVenditaSTATBODC != null && !YFatturaVenditaSTATBODC.close(false)) 
        errors.addAll(0, YFatturaVenditaSTATBODC.getErrorList().getErrors()); 
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
     String errorPage = YFatturaVenditaSTATForm.getErrorPage(); 
%> 
     <jsp:include page="<%=errorPage%>" flush="true"/> 
<% 
  } 
  else 
  { 
     request.setAttribute("ConflictMessages", YFatturaVenditaSTATBODC.getConflict()); 
     request.setAttribute("ErrorMessages", errors); 
     String conflictPage = YFatturaVenditaSTATForm.getConflictPage(); 
%> 
     <jsp:include page="<%=conflictPage%>" flush="true"/> 
<% 
   } 
   } 
%> 
</body>
</html>
