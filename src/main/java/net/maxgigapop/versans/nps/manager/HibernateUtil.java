/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.versans.nps.manager;

import java.util.Properties;
import org.hibernate.cfg.Configuration;
import org.hibernate.SessionFactory;

/**
 *
 * @author xyang
 */
public class HibernateUtil {
    private static SessionFactory sessionFactory;

    @SuppressWarnings("deprecation")
	public static void initSessionFactory() {
        try {
            // Create the SessionFactory from standard (hibernate.cfg.xml)
            // config file.
            Properties props = new Properties();
            props.put("hibernate.connection.username", NPSGlobalState.getDbUser());
            props.put("hibernate.connection.password", NPSGlobalState.getDbPwd());
            props.put("hibernate.connection.autoReconnect", "true");
            props.put("hibernate.connection.autoReconnectForPools", "true");
            Configuration cfg = new Configuration();
            cfg.setProperties(props);
            sessionFactory = cfg.configure().buildSessionFactory();
        } catch (Throwable e) {
            // Log the exception.
            System.err.println("Initial SessionFactory creation failed." + e);
            e.printStackTrace();
            throw new ExceptionInInitializerError(e);
        }
    }

    public static SessionFactory getSessionFactory() {
        return sessionFactory;
    }
    
}
