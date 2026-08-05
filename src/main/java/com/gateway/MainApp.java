package com.gateway;

import com.gateway.filter.RateLimiterFilter;
import com.gateway.servlet.NotificationServlet;
import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;
import java.io.File;

public class MainApp {
    public static void main(String[] args) throws Exception {
        Tomcat tomcat = new Tomcat();
        tomcat.setPort(8081);
        tomcat.getConnector();

        Context ctx = tomcat.addContext("", new File(".").getAbsolutePath());

        // Register Notification Servlet
        Tomcat.addServlet(ctx, "NotificationServlet", new NotificationServlet());
        ctx.addServletMappingDecoded("/api/v1/notifications", "NotificationServlet");

        // Register Rate Limiter Filter
        var filterDef = new org.apache.tomcat.util.descriptor.web.FilterDef();
        filterDef.setFilterName("RateLimiterFilter");
        filterDef.setFilterClass(RateLimiterFilter.class.getName());
        ctx.addFilterDef(filterDef);

        var filterMap = new org.apache.tomcat.util.descriptor.web.FilterMap();
        filterMap.setFilterName("RateLimiterFilter");
        filterMap.addURLPattern("/api/*");
        ctx.addFilterMap(filterMap);

        tomcat.start();
        System.out.println("\n🚀 Gateway running at http://localhost:8081/api/v1/notifications\n");
        tomcat.getServer().await();
    }
}