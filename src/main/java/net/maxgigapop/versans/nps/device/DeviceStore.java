/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.versans.nps.device;

import net.maxgigapop.versans.nps.manager.HibernateUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.HashMap;
import org.hibernate.Query;
import org.hibernate.Session;

/**
 *
 * @author xyang
 */
public class DeviceStore {

    private static Session session;
    private static org.hibernate.Transaction tx;
    private org.apache.log4j.Logger log;
    private List<Device> cachedDevices = new ArrayList<Device>();

    public DeviceStore() {
        log = org.apache.log4j.Logger.getLogger(this.getClass());
    }

    public synchronized boolean add(Device n) {
        synchronized (this) {
            if (n.getUrn().isEmpty() || n.getMakeModel().isEmpty() || n.getAddress().isEmpty()) {
                throw new IllegalArgumentException("Device object must have urn, makeModel and address fields");
            }
            try {
                session = HibernateUtil.getSessionFactory().openSession();
                tx = session.beginTransaction();
                session.save(n);
                session.flush();
                tx.commit();
            } catch (Exception e) {
                tx.rollback();
                e.printStackTrace();
                return false;
            } finally {
                if (session.isOpen()) {
                    session.close();
                }
            }
            cachedDevices.add(n);
        }
        return true;
    }

    public synchronized boolean update(Device n) {
        synchronized (this) {
            if (n.getUrn().isEmpty() || n.getMakeModel().isEmpty() || n.getAddress().isEmpty()) {
                throw new IllegalArgumentException("Device object must have urn, makemodel and address fields");
            }
            try {
                session = HibernateUtil.getSessionFactory().openSession();
                tx = session.beginTransaction();
                session.update(n);
                session.flush();
                tx.commit();
            } catch (Exception e) {
                tx.rollback();
                e.printStackTrace();
                return false;
            } finally {
                if (session.isOpen()) {
                    session.close();
                }
            }
        }
        return true;
    }

    public synchronized boolean delete(Device n) {
        synchronized (this) {
            if (n == null) {
                return false;
            }
            try {
                session = HibernateUtil.getSessionFactory().openSession();
                tx = session.beginTransaction();
                session.delete(n);
                session.flush();
                tx.commit();
            } catch (Exception e) {
                tx.rollback();
                e.printStackTrace();
                return false;
            } finally {
                if (session.isOpen()) {
                    session.close();
                }
            }
            cachedDevices.remove(n);
        }
        return true;
    }

    public synchronized List<Device> getAll() {
        synchronized (this) {
            try {
                session = HibernateUtil.getSessionFactory().openSession();
                tx = session.beginTransaction();
                Query q = session.createQuery("from Device");
                return (List<Device>) q.list();
            } catch (Exception e) {
                tx.rollback();
                e.printStackTrace();
            } finally {
                if (session.isOpen()) {
                    session.close();
                }
            }
        }
        return null;
    }

    /**
     * get by ID
     */
    public synchronized Device getById(int nid) {
        if (cachedDevices.isEmpty()) {
            cachedDevices = this.getAll();
        }
        synchronized (this) {
            for (Device de : cachedDevices) {
                if (de.getId() == nid) {
                    return de;
                }
            }
        }
        return null;
    }

    /**
     * get by urn
     */
    public synchronized Device getByUrn(String urn) {
        if (cachedDevices.isEmpty()) {
            cachedDevices = this.getAll();
        }
        synchronized (this) {
            for (Device de : cachedDevices) {
                if (de.getUrn().equalsIgnoreCase(urn)) {
                    return de;
                }
            }
        }
        return null;
    }
}
