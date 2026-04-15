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
  BODataCollector RilevDatiPrdTSBODC = null; 
  List errors = new ArrayList(); 
  WebJSTypeList jsList = new WebJSTypeList(); 
  WebForm RilevDatiPrdTSForm =  
     new com.thera.thermfw.web.WebForm(request, response, "RilevDatiPrdTSForm", "RilevDatiPrdTS", null, "it.thera.thip.produzione.raccoltaDati.web.RilevDatiPrdTSFormActionAdapter", false, false, false, false, true, true, null, 1, true, "it/dnd/thip/produzione/raccoltaDati/YChiusuraListaTesta.js"); 
  RilevDatiPrdTSForm.setServletEnvironment(se); 
  RilevDatiPrdTSForm.setJSTypeList(jsList); 
  RilevDatiPrdTSForm.setHeader(null); 
  RilevDatiPrdTSForm.setFooter(null); 
  RilevDatiPrdTSForm.setWebFormModifierClass("it.thera.thip.produzione.raccoltaDati.web.RilevDatiPrdTSWebFormModifier"); 
  RilevDatiPrdTSForm.setDeniedAttributeModeStr("hideNone"); 
  int mode = RilevDatiPrdTSForm.getMode(); 
  String key = RilevDatiPrdTSForm.getKey(); 
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
        RilevDatiPrdTSForm.outTraceInfo(getClass().getName()); 
        String collectorName = RilevDatiPrdTSForm.findBODataCollectorName(); 
                RilevDatiPrdTSBODC = (BODataCollector)Factory.createObject(collectorName); 
        if (RilevDatiPrdTSBODC instanceof WebDataCollector) 
            ((WebDataCollector)RilevDatiPrdTSBODC).setServletEnvironment(se); 
        RilevDatiPrdTSBODC.initialize("RilevDatiPrdTS", true, 1); 
        RilevDatiPrdTSForm.setBODataCollector(RilevDatiPrdTSBODC); 
        int rcBODC = RilevDatiPrdTSForm.initSecurityServices(); 
        mode = RilevDatiPrdTSForm.getMode(); 
        if (rcBODC == BODataCollector.OK) 
        { 
           requestIsValid = true; 
           RilevDatiPrdTSForm.write(out); 
           if(mode != WebForm.NEW) 
              rcBODC = RilevDatiPrdTSBODC.retrieve(key); 
           if(rcBODC == BODataCollector.OK) 
           { 
              RilevDatiPrdTSForm.writeHeadElements(out); 
           // fine blocco XXX  
           // a completamento blocco di codice YYY a fine body con catch e gestione errori 
%> 

<title>Rilevazione Dati Prod. TS</title>
<style>
.spinner-overlay {
    position: fixed;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    background: rgba(0, 0, 0, 0.3); /* optional dark overlay */
    display: flex;
    align-items: center;
    justify-content: center;
    z-index: 9999;
}

.spinner {
    width: 50px;
    height: 50px;
    border: 6px solid #ccc;
    border-top: 6px solid #007bff; /* blue spinning part */
    border-radius: 50%;
    animation: spin 1s linear infinite;
}

@keyframes spin {
    0% { transform: rotate(0deg); }
    100% { transform: rotate(360deg); }
}
</style>
</head>
<body bottommargin="0" leftmargin="0" onbeforeunload="<%=RilevDatiPrdTSForm.getBodyOnBeforeUnload()%>" onload="<%=RilevDatiPrdTSForm.getBodyOnLoad()%>" onunload="<%=RilevDatiPrdTSForm.getBodyOnUnload()%>" rightmargin="0" topmargin="0"><%
   RilevDatiPrdTSForm.writeBodyStartElements(out); 
%> 

<div class="spinner-overlay" id="spinner" style="display:none;">
    <div class="spinner"></div>
</div>
	<table width="100%" height="100%" cellspacing="0" cellpadding="0">
<tr>
<td style="height:0" valign="top">
<% String hdr = RilevDatiPrdTSForm.getCompleteHeader();
 if (hdr != null) { 
   request.setAttribute("dataCollector", RilevDatiPrdTSBODC); 
   request.setAttribute("servletEnvironment", se); %>
  <jsp:include page="<%= hdr %>" flush="true"/> 
<% } %> 
</td>
</tr>

<tr>
<td valign="top" height="100%">
<form action="<%=RilevDatiPrdTSForm.getServlet()%>" method="post" name="RilevDatiPrdTSForm" style="height:100%"><%
  RilevDatiPrdTSForm.writeFormStartElements(out); 
%>


		<!--<table class="maintable" style="height:140px"> -->
		<!--Fix 13264 -->
		<!--Fix 13574 -->
		<input name="IdOperatore" type="Hidden">
		<input name="BollaLavorazione" type="Hidden">
		<table class="repartoTable">
			<!--Fix 13264 -->
			<!--Fix 13574 -->
			<tr valign="top">
				<td colspan="2" style="height: 15px"></td>
				<!-- Fix 13175 -->
			</tr>
			<tr valign="top">
				<td width="15px"></td>
				<td>
					<table cellpadding="3" cellspacing="3">
						<tr>
							<td nowrap><%{  WebLabelCompound label = new com.thera.thermfw.web.WebLabelCompound(null, null, "RilevDatiPrdTS", "CodiceTestataLista", null); 
   label.setParent(RilevDatiPrdTSForm); 
%><label class="<%=label.getClassType()%>" for="CodiceTestataLista"><%label.write(out);%></label><%}%></td>
							<td colspan="2"><% 
  WebTextInput RilevDatiPrdTSCodiceTestataLista =  
     new com.thera.thermfw.web.WebTextInput("RilevDatiPrdTS", "CodiceTestataLista"); 
  RilevDatiPrdTSCodiceTestataLista.setParent(RilevDatiPrdTSForm); 
%>
<input class="<%=RilevDatiPrdTSCodiceTestataLista.getClassType()%>" id="<%=RilevDatiPrdTSCodiceTestataLista.getId()%>" maxlength="<%=RilevDatiPrdTSCodiceTestataLista.getMaxLength()%>" name="<%=RilevDatiPrdTSCodiceTestataLista.getName()%>" size="<%=RilevDatiPrdTSCodiceTestataLista.getSize()%>" type="text"><% 
  RilevDatiPrdTSCodiceTestataLista.write(out); 
%>
</td>
						</tr>
					</table>
				</td>
			</tr>
			<tr>
				<td colspan="3"><label class="labelError" id="ErroriList"></label></td>
			</tr>
		</table>
	<%
  RilevDatiPrdTSForm.writeFormEndElements(out); 
%>
</form></td>
</tr>

<tr>
<td style="height:0">
<% String ftr = RilevDatiPrdTSForm.getCompleteFooter();
 if (ftr != null) { 
   request.setAttribute("dataCollector", RilevDatiPrdTSBODC); 
   request.setAttribute("servletEnvironment", se); %>
  <jsp:include page="<%= ftr %>" flush="true"/> 
<% } %> 
</td>
</tr>
</table>


<%
           // blocco YYY  
           // a completamento blocco di codice XXX in head 
              RilevDatiPrdTSForm.writeBodyEndElements(out); 
           } 
           else 
              errors.addAll(0, RilevDatiPrdTSBODC.getErrorList().getErrors()); 
        } 
        else 
           errors.addAll(0, RilevDatiPrdTSBODC.getErrorList().getErrors()); 
           if(RilevDatiPrdTSBODC.getConflict() != null) 
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
     if(RilevDatiPrdTSBODC != null && !RilevDatiPrdTSBODC.close(false)) 
        errors.addAll(0, RilevDatiPrdTSBODC.getErrorList().getErrors()); 
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
     String errorPage = RilevDatiPrdTSForm.getErrorPage(); 
%> 
     <jsp:include page="<%=errorPage%>" flush="true"/> 
<% 
  } 
  else 
  { 
     request.setAttribute("ConflictMessages", RilevDatiPrdTSBODC.getConflict()); 
     request.setAttribute("ErrorMessages", errors); 
     String conflictPage = RilevDatiPrdTSForm.getConflictPage(); 
%> 
     <jsp:include page="<%=conflictPage%>" flush="true"/> 
<% 
   } 
   } 
%> 
</body>
</html>
