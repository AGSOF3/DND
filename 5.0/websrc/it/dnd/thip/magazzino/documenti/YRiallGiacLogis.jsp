<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.0 Transitional//EN"
                      "file:///K:/Thip/5.1.0/websrcsvil/dtd/xhtml1-transitional.dtd">
<html>
<!-- WIZGEN Therm 2.0.0 as Batch form - multiBrowserGen = true -->
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
  BODataCollector YRiallGiacLogisBODC = null; 
  List errors = new ArrayList(); 
  WebJSTypeList jsList = new WebJSTypeList(); 
  WebForm YRiallGiacLogisForm =  
     new com.thera.thermfw.web.WebFormForBatchForm(request, response, "YRiallGiacLogisForm", "YRiallGiacLogis", null, "com.thera.thermfw.batch.web.BatchFormActionAdapter", false, false, true, true, true, true, null, 1, false, null); 
  YRiallGiacLogisForm.setServletEnvironment(se); 
  YRiallGiacLogisForm.setJSTypeList(jsList); 
  YRiallGiacLogisForm.setHeader("it.thera.thip.cs.PantheraHeader.jsp"); 
  YRiallGiacLogisForm.setFooter("com.thera.thermfw.common.Footer.jsp"); 
  ((WebFormForBatchForm)  YRiallGiacLogisForm).setGenerateThReportId(true); 
  ((WebFormForBatchForm)  YRiallGiacLogisForm).setGenerateSSDEnabled(true); 
  YRiallGiacLogisForm.setDeniedAttributeModeStr("hideNone"); 
  int mode = YRiallGiacLogisForm.getMode(); 
  String key = YRiallGiacLogisForm.getKey(); 
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
        YRiallGiacLogisForm.outTraceInfo(getClass().getName()); 
        String collectorName = YRiallGiacLogisForm.findBODataCollectorName(); 
				 YRiallGiacLogisBODC = (BODataCollector)Factory.createObject(collectorName); 
        if (YRiallGiacLogisBODC instanceof WebDataCollector) 
            ((WebDataCollector)YRiallGiacLogisBODC).setServletEnvironment(se); 
        YRiallGiacLogisBODC.initialize("YRiallGiacLogis", true, 1); 
        int rcBODC; 
        if (YRiallGiacLogisBODC.getBo() instanceof BatchRunnable) 
          rcBODC = YRiallGiacLogisBODC.initSecurityServices("RUN", mode, true, false, true); 
        else 
          rcBODC = YRiallGiacLogisBODC.initSecurityServices(mode, true, true, true); 
        if (rcBODC == BODataCollector.OK) 
        { 
           requestIsValid = true; 
           YRiallGiacLogisForm.write(out); 
           if(mode != WebForm.NEW) 
              rcBODC = YRiallGiacLogisBODC.retrieve(key); 
           if(rcBODC == BODataCollector.OK) 
           { 
              YRiallGiacLogisForm.setBODataCollector(YRiallGiacLogisBODC); 
              YRiallGiacLogisForm.writeHeadElements(out); 
           // fine blocco XXX  
           // a completamento blocco di codice YYY a fine body con catch e gestione errori 
%> 

<% 
  WebMenuBar menuBar = new com.thera.thermfw.web.WebMenuBar("HM_Array1", "150", "#000000","#000000","#A5B6CE","#E4EAEF","#FFFFFF","#000000"); 
  menuBar.setParent(YRiallGiacLogisForm); 
   request.setAttribute("menuBar", menuBar); 
%> 
<jsp:include page="/com/thera/thermfw/batch/BatchRunnableMenu.jsp" flush="true"> 
<jsp:param name="partRequest" value="menuBar"/> 
</jsp:include> 
<% 
  menuBar.write(out); 
  menuBar.writeChildren(out); 
%> 
<% 
  WebToolBar myToolBarTB = new com.thera.thermfw.web.WebToolBar("myToolBar", "24", "24", "16", "16", "#f7fbfd","#C8D6E1"); 
  myToolBarTB.setParent(YRiallGiacLogisForm); 
   request.setAttribute("toolBar", myToolBarTB); 
%> 
<jsp:include page="/com/thera/thermfw/batch/BatchRunnableMenu.jsp" flush="true"> 
<jsp:param name="partRequest" value="toolBar"/> 
</jsp:include> 
<% 
   myToolBarTB.write(out); 
%> 
</head>
<body bottommargin="0" leftmargin="0" onbeforeunload="<%=YRiallGiacLogisForm.getBodyOnBeforeUnload()%>" onload="<%=YRiallGiacLogisForm.getBodyOnLoad()%>" onunload="<%=YRiallGiacLogisForm.getBodyOnUnload()%>" rightmargin="0" topmargin="0"><%
   YRiallGiacLogisForm.writeBodyStartElements(out); 
%> 

	<table width="100%" height="100%" cellspacing="0" cellpadding="0">
<tr>
<td style="height:0" valign="top">
<% String hdr = YRiallGiacLogisForm.getCompleteHeader();
 if (hdr != null) { 
   request.setAttribute("dataCollector", YRiallGiacLogisBODC); 
   request.setAttribute("servletEnvironment", se); %>
  <jsp:include page="<%= hdr %>" flush="true"/> 
<% } %> 
</td>
</tr>

<tr>
<td valign="top" height="100%">
<form action="<%=YRiallGiacLogisForm.getServlet()%>" method="post" name="YRiallGiacLogis" style="height:100%"><%
  YRiallGiacLogisForm.writeFormStartElements(out); 
%>

		<table cellpadding="0" cellspacing="0" height="100%" id="emptyborder" width="100%">
			<tr>
				<td><% menuBar.writeElements(out); %> 
</td>
			</tr>
			<tr>
				<td><% myToolBarTB.writeChildren(out); %> 
</td>
			</tr>
			<tr>
				<td height="100%">
					<table border="0" cellpadding="0" cellspacing="7">
						
                  <tr>
                    <td valign="top">
                      <%{  WebLabelCompound label = new com.thera.thermfw.web.WebLabelCompound(null, null, "YRiallGiacLogis", "IdMagazzino", null); 
   label.setParent(YRiallGiacLogisForm); 
%><label class="<%=label.getClassType()%>" for="RelMagazzino"><%label.write(out);%></label><%}%>
                    </td>
                    <td valign="top">
                      <% 
  WebMultiSearchForm YRiallGiacLogisRelMagazzino =  
     new com.thera.thermfw.web.WebMultiSearchForm("YRiallGiacLogis", "RelMagazzino", false, false, true, 1, null, null); 
  YRiallGiacLogisRelMagazzino.setParent(YRiallGiacLogisForm); 
  YRiallGiacLogisRelMagazzino.write(out); 
%>
<!--<span class="multisearchform" id="RelMagazzino"></span>-->
                    </td>
                  </tr>
					</table>
				</td>
			</tr>
			<tr>
				<td height="100%"></td>
			</tr>
			<tr>
				<td style="height: 0"><% 
  WebErrorList errorList = new com.thera.thermfw.web.WebErrorList(); 
  errorList.setParent(YRiallGiacLogisForm); 
  errorList.write(out); 
%>
<!--<span class="errorlist" id="dummy"></span>--></td>
			</tr>
		</table>
	<%
  YRiallGiacLogisForm.writeFormEndElements(out); 
%>
</form></td>
</tr>

<tr>
<td style="height:0">
<% String ftr = YRiallGiacLogisForm.getCompleteFooter();
 if (ftr != null) { 
   request.setAttribute("dataCollector", YRiallGiacLogisBODC); 
   request.setAttribute("servletEnvironment", se); %>
  <jsp:include page="<%= ftr %>" flush="true"/> 
<% } %> 
</td>
</tr>
</table>


<%
           // blocco YYY  
           // a completamento blocco di codice XXX in head 
              YRiallGiacLogisForm.writeBodyEndElements(out); 
           } 
           else 
              errors.addAll(0, YRiallGiacLogisBODC.getErrorList().getErrors()); 
        } 
        else 
           errors.addAll(0, YRiallGiacLogisBODC.getErrorList().getErrors()); 
           if(YRiallGiacLogisBODC.getConflict() != null) 
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
     if(YRiallGiacLogisBODC != null && !YRiallGiacLogisBODC.close(false)) 
        errors.addAll(0, YRiallGiacLogisBODC.getErrorList().getErrors()); 
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
     String errorPage = YRiallGiacLogisForm.getErrorPage(); 
%> 
     <jsp:include page="<%=errorPage%>" flush="true"/> 
<% 
  } 
  else 
  { 
     request.setAttribute("ConflictMessages", YRiallGiacLogisBODC.getConflict()); 
     request.setAttribute("ErrorMessages", errors); 
     String conflictPage = YRiallGiacLogisForm.getConflictPage(); 
%> 
     <jsp:include page="<%=conflictPage%>" flush="true"/> 
<% 
   } 
   } 
%> 
</body>
</html>
