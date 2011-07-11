<%@page import="org.springframework.web.context.*,org.springframework.web.context.support.*" contentType="text/html" pageEncoding="UTF-8"%>

<%-- 
    Document   : version
    Created on : Jul 10, 2011, 11:15:08 AM
    Author     : jim
--%>


<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>JSP Page</title>
    </head>
    <body>
        <h1>Hello World!</h1>


        <!--
        
        This seems a little cleaner, use the attribute explorer
        
        http://andykayley.blogspot.com/2007/11/how-to-inject-spring-beans-into.html
        
        -->


        <br/>POM: <%=this.getServletContext().getAttribute("pomName")%>
        <br/>Version: <%=this.getServletContext().getAttribute("pomVersion")%>
        <br/>Build Timestamp: <%=this.getServletContext().getAttribute("mavenBuildTimestamp")%>



        <UL>
            <%
                /**
                 * Dump the system properties
                 */
                java.util.Enumeration e = null;
                try {
                    e = System.getProperties().propertyNames();
                } catch (SecurityException se) {
                }
                if (e != null) {
                    out.write("<pre>");
                    for (; e.hasMoreElements();) {
                        String key = (String) e.nextElement();
                        out.write(key + "=" + System.getProperty(key) + "\n");
                    }
                    out.write("</pre><p>");
                }
            %>
        </UL>
        <hr>
        <%= getServletConfig().getServletContext().getServerInfo()%>



        <font size="+2" color="#7E354D"><br>Information about request header</font>
        <br>
        <TABLE style="background-color: #ECE5B6;" WIDTH="30%" border="1">
            <tr>
                <th>method used to send request</th>
                <!-- getMethod() returns the name of the HTTP 
     method with which this request was made,
                  for example, GET, POST, or PUT -->
                <td><%= request.getMethod()%></td>
            </tr>
            <tr>
                <th>URI of the request</th>
                <!-- getRequestURI() returns the part of this request's URL -->
                <td><%= request.getRequestURI()%></td>
            </tr>
            <%
                /*This method returns an enumeration of all the header names this 
                request contains.*/
                java.util.Enumeration names = request.getHeaderNames();
                while (names.hasMoreElements()) {
                    String hname = (String) names.nextElement();
            %>
            <tr>
                <th> <%= hname%> </th>
                <!-- This method returns the value of the 
specified request header as a String. -->
                <td><%= request.getHeader(hname)%></td>
            </tr>
            <%
                }
            %>



    </body>
</html>
