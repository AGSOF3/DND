<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.0 Transitional//EN"
                      "file:///W:\PthDev\Projects\Panthera\DND\WebContent\dtd/xhtml1-transitional.dtd">
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
  BODataCollector YSubcontinenteBODC = null; 
  List errors = new ArrayList(); 
  WebJSTypeList jsList = new WebJSTypeList(); 
  WebForm YSubcontinenteForm =  
     new com.thera.thermfw.web.WebForm(request, response, "YSubcontinenteForm", "YSubcontinente", null, "com.thera.thermfw.web.servlet.FormActionAdapter", false, false, true, true, true, true, null, 0, true, "it/dnd/thip/base/partner/YSubcontinente.js"); 
  YSubcontinenteForm.setServletEnvironment(se); 
  YSubcontinenteForm.setJSTypeList(jsList); 
  YSubcontinenteForm.setHeader("it.thera.thip.cs.PantheraHeader.jsp"); 
  YSubcontinenteForm.setFooter("com.thera.thermfw.common.Footer.jsp"); 
  YSubcontinenteForm.setDeniedAttributeModeStr("hideNone"); 
  int mode = YSubcontinenteForm.getMode(); 
  String key = YSubcontinenteForm.getKey(); 
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
        YSubcontinenteForm.outTraceInfo(getClass().getName()); 
        String collectorName = YSubcontinenteForm.findBODataCollectorName(); 
                YSubcontinenteBODC = (BODataCollector)Factory.createObject(collectorName); 
        if (YSubcontinenteBODC instanceof WebDataCollector) 
            ((WebDataCollector)YSubcontinenteBODC).setServletEnvironment(se); 
        YSubcontinenteBODC.initialize("YSubcontinente", true, 0); 
        YSubcontinenteForm.setBODataCollector(YSubcontinenteBODC); 
        int rcBODC = YSubcontinenteForm.initSecurityServices(); 
        mode = YSubcontinenteForm.getMode(); 
        if (rcBODC == BODataCollector.OK) 
        { 
           requestIsValid = true; 
           YSubcontinenteForm.write(out); 
           if(mode != WebForm.NEW) 
              rcBODC = YSubcontinenteBODC.retrieve(key); 
           if(rcBODC == BODataCollector.OK) 
           { 
              YSubcontinenteForm.writeHeadElements(out); 
           // fine blocco XXX  
           // a completamento blocco di codice YYY a fine body con catch e gestione errori 
%> 
<% 
  WebMenuBar menuBar = new com.thera.thermfw.web.WebMenuBar("HM_Array1", "150", "#000000","#000000","#A5B6CE","#E4EAEF","#FFFFFF","#000000"); 
  menuBar.setParent(YSubcontinenteForm); 
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
  myToolBarTB.setParent(YSubcontinenteForm); 
   request.setAttribute("toolBar", myToolBarTB); 
%> 
<jsp:include page="/it/thera/thip/cs/defObjMenu.jsp" flush="true"> 
<jsp:param name="partRequest" value="toolBar"/> 
</jsp:include> 
<% 
   myToolBarTB.write(out); 
%> 
</head>
  <body onbeforeunload="<%=YSubcontinenteForm.getBodyOnBeforeUnload()%>" onload="<%=YSubcontinenteForm.getBodyOnLoad()%>" onunload="<%=YSubcontinenteForm.getBodyOnUnload()%>" style="margin: 0px; overflow: hidden;"><%
   YSubcontinenteForm.writeBodyStartElements(out); 
%> 

    <table width="100%" height="100%" cellspacing="0" cellpadding="0">
<tr>
<td style="height:0" valign="top">
<% String hdr = YSubcontinenteForm.getCompleteHeader();
 if (hdr != null) { 
   request.setAttribute("dataCollector", YSubcontinenteBODC); 
   request.setAttribute("servletEnvironment", se); %>
  <jsp:include page="<%= hdr %>" flush="true"/> 
<% } %> 
</td>
</tr>

<tr>
<td valign="top" height="100%">
<form action="<%=YSubcontinenteForm.getServlet()%>" method="post" name="YSubcontinenteForm" style="height:100%"><%
  YSubcontinenteForm.writeFormStartElements(out); 
%>

      <table cellpadding="0" cellspacing="0" height="100%" id="emptyborder" width="100%">
        <tr>
          <td style="height:0">
            <% menuBar.writeElements(out); %> 

          </td>
        </tr>
        <tr>
          <td style="height:0">
            <% myToolBarTB.writeChildren(out); %> 

          </td>
        </tr>
        <tr>
          <td>
            <% 
  WebTextInput YSubcontinenteIdAzienda =  
     new com.thera.thermfw.web.WebTextInput("YSubcontinente", "IdAzienda"); 
  YSubcontinenteIdAzienda.setParent(YSubcontinenteForm); 
%>
<input class="<%=YSubcontinenteIdAzienda.getClassType()%>" id="<%=YSubcontinenteIdAzienda.getId()%>" maxlength="<%=YSubcontinenteIdAzienda.getMaxLength()%>" name="<%=YSubcontinenteIdAzienda.getName()%>" size="<%=YSubcontinenteIdAzienda.getSize()%>" type="hidden"><% 
  YSubcontinenteIdAzienda.write(out); 
%>

          </td>
        </tr>
        <tr>
          <td height="100%">
            <!--<span class="tabbed" id="mytabbed">-->
<table width="100%" height="100%" cellpadding="0" cellspacing="0" style="padding-right:1px">
   <tr valign="top">
     <td><% 
  WebTabbed mytabbed = new com.thera.thermfw.web.WebTabbed("mytabbed", "100%", "100%"); 
  mytabbed.setParent(YSubcontinenteForm); 
 mytabbed.addTab("tab1", "it.dnd.thip.base.partner.resources.YSubcontinente", "tab1", "YSubcontinente", null, null, null, null); 
  mytabbed.write(out); 
%>

     </td>
   </tr>
   <tr>
     <td height="100%"><div class="tabbed_pagine" id="tabbedPagine" style="position: relative; width: 100%; height: 100%;">
              <div class="tabbed_page" id="<%=mytabbed.getTabPageId("tab1")%>" style="width:100%;height:100%;overflow:auto;"><% mytabbed.startTab("tab1"); %>
                <table style="width: 100%;">
                  <tr>
                    <td valign="top">
                      <%{  WebLabelCompound label = new com.thera.thermfw.web.WebLabelCompound(null, null, "YSubcontinente", "IdSubcontinente", null); 
   label.setParent(YSubcontinenteForm); 
%><label class="<%=label.getClassType()%>" for="IdSubcontinente"><%label.write(out);%></label><%}%>
                    </td>
                    <td valign="top">
                      <% 
  WebTextInput YSubcontinenteIdSubcontinente =  
     new com.thera.thermfw.web.WebTextInput("YSubcontinente", "IdSubcontinente"); 
  YSubcontinenteIdSubcontinente.setParent(YSubcontinenteForm); 
%>
<input class="<%=YSubcontinenteIdSubcontinente.getClassType()%>" id="<%=YSubcontinenteIdSubcontinente.getId()%>" maxlength="<%=YSubcontinenteIdSubcontinente.getMaxLength()%>" name="<%=YSubcontinenteIdSubcontinente.getName()%>" size="<%=YSubcontinenteIdSubcontinente.getSize()%>"><% 
  YSubcontinenteIdSubcontinente.write(out); 
%>

                    </td>
                  </tr>
                  <tr>
                    <td valign="top">
                      <%{  WebLabelCompound label = new com.thera.thermfw.web.WebLabelCompound(null, null, "YSubcontinente", "Descrizione", null); 
   label.setParent(YSubcontinenteForm); 
%><label class="<%=label.getClassType()%>" for="Descrizione"><%label.write(out);%></label><%}%>
                    </td>
                    <td valign="top">
                      <% 
  WebTextInput YSubcontinenteDescrizione =  
     new com.thera.thermfw.web.WebTextInput("YSubcontinente", "Descrizione"); 
  YSubcontinenteDescrizione.setParent(YSubcontinenteForm); 
%>
<input class="<%=YSubcontinenteDescrizione.getClassType()%>" id="<%=YSubcontinenteDescrizione.getId()%>" maxlength="<%=YSubcontinenteDescrizione.getMaxLength()%>" name="<%=YSubcontinenteDescrizione.getName()%>" size="<%=YSubcontinenteDescrizione.getSize()%>"><% 
  YSubcontinenteDescrizione.write(out); 
%>

                    </td>
                  </tr>
                  <tr>
                    <td valign="top">
                      <% 
   request.setAttribute("parentForm", YSubcontinenteForm); 
   String CDForDatiComuniEstesi$it$thera$thip$cs$DatiComuniEstesi$jsp = "DatiComuniEstesi"; 
%>
<jsp:include page="/it/thera/thip/cs/DatiComuniEstesi.jsp" flush="true"> 
<jsp:param name="CDName" value="<%=CDForDatiComuniEstesi$it$thera$thip$cs$DatiComuniEstesi$jsp%>"/> 
</jsp:include> 
<!--<span class="subform" id="DatiComuniEstesi"></span>-->
                    </td>
                    <td valign="top">
                    </td>
                  </tr>
                </table>
              <% mytabbed.endTab(); %> 
</div>
            </div><% mytabbed.endTabbed();%> 

     </td>
   </tr>
</table><!--</span>-->
          </td>
        </tr>
        <tr>
          <td style="height:0">
            <% 
  WebErrorList errorList = new com.thera.thermfw.web.WebErrorList(); 
  errorList.setParent(YSubcontinenteForm); 
  errorList.write(out); 
%>
<!--<span class="errorlist"></span>-->
          </td>
        </tr>
      </table>
    <%
  YSubcontinenteForm.writeFormEndElements(out); 
%>
</form></td>
</tr>

<tr>
<td style="height:0">
<% String ftr = YSubcontinenteForm.getCompleteFooter();
 if (ftr != null) { 
   request.setAttribute("dataCollector", YSubcontinenteBODC); 
   request.setAttribute("servletEnvironment", se); %>
  <jsp:include page="<%= ftr %>" flush="true"/> 
<% } %> 
</td>
</tr>
</table>


  <%
           // blocco YYY  
           // a completamento blocco di codice XXX in head 
              YSubcontinenteForm.writeBodyEndElements(out); 
           } 
           else 
              errors.addAll(0, YSubcontinenteBODC.getErrorList().getErrors()); 
        } 
        else 
           errors.addAll(0, YSubcontinenteBODC.getErrorList().getErrors()); 
           if(YSubcontinenteBODC.getConflict() != null) 
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
     if(YSubcontinenteBODC != null && !YSubcontinenteBODC.close(false)) 
        errors.addAll(0, YSubcontinenteBODC.getErrorList().getErrors()); 
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
     String errorPage = YSubcontinenteForm.getErrorPage(); 
%> 
     <jsp:include page="<%=errorPage%>" flush="true"/> 
<% 
  } 
  else 
  { 
     request.setAttribute("ConflictMessages", YSubcontinenteBODC.getConflict()); 
     request.setAttribute("ErrorMessages", errors); 
     String conflictPage = YSubcontinenteForm.getConflictPage(); 
%> 
     <jsp:include page="<%=conflictPage%>" flush="true"/> 
<% 
   } 
   } 
%> 
</body>
</html>
