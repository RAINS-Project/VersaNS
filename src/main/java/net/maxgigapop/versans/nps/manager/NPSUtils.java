/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.versans.nps.manager;

import net.maxgigapop.versans.nps.device.DeviceException;
import net.maxgigapop.versans.nps.api.ServiceException;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.StringReader;
import java.io.Writer;
import java.util.Map;
import org.hibernate.SQLQuery;
import org.hibernate.Session;

import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;
import freemarker.template.TemplateException;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.Marshaller;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.*;
import javax.xml.transform.stream.*;
import javax.xml.transform.dom.*;
import javax.xml.namespace.QName;

import org.w3c.dom.*;

/**
 *
 * @author xyang
 */
public class NPSUtils {
    
    private static Session session;
    private static org.hibernate.Transaction tx;

    public static void executeDirectStatement(String sql) throws ServiceException {
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            tx = session.beginTransaction();
            SQLQuery q = session.createSQLQuery(sql);
            q.executeUpdate();
            tx.commit();
        } catch (Exception e) {
            tx.rollback();
            throw new ServiceException(e.getMessage());
        } finally {
            if (session.isOpen()) session.close();
        }
    }

    public static String generateConfig(Map input, String templateFile) throws DeviceException {
        String templateDir = NPSGlobalState.getTemplateDir();
        String config = "";
        Template temp = null;
        Configuration cfg = new Configuration();
        try {
            cfg.setDirectoryForTemplateLoading(new File(templateDir));
            cfg.setObjectWrapper(new DefaultObjectWrapper());
            Writer out = new StringWriter();
            temp = cfg.getTemplate(templateFile);
            temp.process(input, out);
            out.flush();
            config = out.toString();
        } catch (IOException e) {
            throw new DeviceException(e.getMessage());
        } catch (TemplateException e) {
            throw new DeviceException(e.getMessage());
        } catch (Exception e) {
            throw new DeviceException(e.getMessage());
        }
        return config;
    }

    public static Boolean isDcnUrn(String urn) {
        if (urn.contains("urn:ogf:network:domain"))
            return true;
        return false;
    }

    public static String getDcnUrnField(String urn, String field) {
        int start = urn.indexOf(field+"=");
        if (start == -1)
            return null;
        start += field.length()+1;
        int end = urn.indexOf(':', start);
        if (end == -1 )
            end = urn.length();
        return urn.substring(start, end);
    }
    
    public static String extractDomainUrn(String urn) {
        String domain = getDcnUrnField(urn, "domain");
        if (domain == null)
            return null;
        urn = "urn:ogf:network:domain=";
        urn += domain;
        return urn;
    }

    public static String extractDeviceUrn(String urn) {
        String domain = getDcnUrnField(urn, "domain");
        if (domain == null)
            return null;
        String node = getDcnUrnField(urn, "node");
        if (node == null)
            return null;
        urn = "urn:ogf:network:domain=";
        urn += domain;
        urn += ":node=";
        urn += node;
        return urn;
    }

    public static String extractInterfaceUrn(String urn) {
        String domain = getDcnUrnField(urn, "domain");
        if (domain == null)
            return null;
        String node = getDcnUrnField(urn, "node");
        if (node == null)
            return null;
        String port = getDcnUrnField(urn, "port");
        if (port == null)
            return null;
        urn = "urn:ogf:network:domain=";
        urn += domain;
        urn += ":node=";
        urn += node;
        urn += ":port=";
        urn += port;
        return urn;
    }
     
    public static String extractIpAddress(String text) {
        String pattern = "(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}).*";
        Pattern compiledPattern = Pattern.compile(pattern);
        if (compiledPattern == null)
            return null;
        Matcher matcher = compiledPattern.matcher(text);
        while (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
    
    public static String getSlash30Peer(String prefix) {
        String pattern = "(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3})\\.(\\d{1,3})/(\\d{1,2})";
        Pattern compiledPattern = Pattern.compile(pattern);
        if (compiledPattern == null)
            return null;
        Matcher matcher = compiledPattern.matcher(prefix);
        if (matcher.find()) {
            int ip4th = Integer.valueOf(matcher.group(2));
            if (ip4th % 2 == 1)
                ip4th += 1;
            else
                ip4th -= 1;
            int mask = Integer.valueOf(matcher.group(3));
            return String.format("%s.%d/%d", matcher.group(1), ip4th, mask);
        }
        return null;
    }    
    
    public static long bandwdithToBps(String bwString) {
        long ret = 0;
        Pattern pattern = Pattern.compile("(\\d+)([mM]|[gG]|[kK]|[bB]).*");
        Matcher matcher = pattern.matcher(bwString);
        if (matcher.find()) {
            String bw = matcher.group(1);
            ret = Long.valueOf(bw);
            String m = matcher.group(2);
            if (m.equalsIgnoreCase("g"))
                ret *= 1000000000;
            else if (m.equalsIgnoreCase("m"))
                ret *= 1000000;
            else if (m.equalsIgnoreCase("k"))
                ret *= 1000;
        } else {
            ret = Long.valueOf(bwString);
        }
        return ret;
    }

    public static String parseVlanTag(String vlanTag, boolean getSrc) {
        String[] vtags = vlanTag.split("-");
        if (!getSrc && vtags.length == 2)
            return vtags[1];
        return vtags[0];
    }

    public static String concatStringsWSep(Iterable<String> strings, String separator) {
        StringBuilder sb = new StringBuilder();
        String sep = "";
        for(String s: strings) {
            sb.append(sep).append(s);
            sep = separator;
        }
        return sb.toString();                           
    }

}
