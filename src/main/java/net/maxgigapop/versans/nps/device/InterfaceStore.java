/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.versans.nps.device;

import java.util.List;
import net.maxgigapop.versans.nps.manager.HibernateUtil;
import org.hibernate.Query;
import org.hibernate.Session;

/**
 *
 * @author xyang
 */
public class InterfaceStore {

    private static Session session;
    private static org.hibernate.Transaction tx;
    private org.apache.log4j.Logger log;
    private List<Interface> cachedInterfaces = null;

    public InterfaceStore() {
        log = org.apache.log4j.Logger.getLogger(this.getClass());
    }

    public synchronized boolean add(Interface n) {
        synchronized (this) {
            if (n.getUrn().isEmpty() || n.getMakeModel().isEmpty() || n.getDeviceId() == 0) {
                throw new IllegalArgumentException("Interface object must have valid urn, makeModel deviceId fields");
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
        }
        return true;
    }

    public synchronized boolean update(Interface n) {
        synchronized (this) {
            if (n.getUrn().isEmpty() || n.getMakeModel().isEmpty() || n.getDeviceId() == 0) {
                throw new IllegalArgumentException("Interface object must have valid urn, makeModel deviceId fields");
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

    public synchronized boolean delete(Interface n) {
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
            cachedInterfaces.remove(n);
        }
        return true;
    }

    public synchronized List<Interface> getAll() {
        synchronized (this) {
            try {
                session = HibernateUtil.getSessionFactory().openSession();
                tx = session.beginTransaction();
                Query q = session.createQuery("from Interface");
                return (List<Interface>) q.list();
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
    public synchronized Interface getById(int id) {
        synchronized (this) {
            try {
                session = HibernateUtil.getSessionFactory().openSession();
                tx = session.beginTransaction();
                Query q = session.createQuery("from Interface as interface where interface.id=" + Integer.toString(id));
                if (q.list().size() == 0) {
                    return null;
                }
                return (Interface) q.list().get(0);
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
     * get by urn
     */
    public synchronized Interface getByUrn(String urn) {
        synchronized (this) {
            try {
                session = HibernateUtil.getSessionFactory().openSession();
                tx = session.beginTransaction();
                Query q = session.createQuery("from Interface as interface where interface.urn='" + urn + "'");
                if (q.list().size() == 0) {
                    return null;
                }
                return (Interface) q.list().get(0);
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
     * get by device ID
     */
    public synchronized List<Interface> getByDeviceId(int deviceId) {
        synchronized (this) {
            try {
                session = HibernateUtil.getSessionFactory().openSession();
                tx = session.beginTransaction();
                Query q = session.createQuery("from Interface as interface where interface.deviceId=" + Integer.toString(deviceId));
                if (q.list().size() == 0) {
                    return null;
                }
                return (List<Interface>) q.list();
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
     * get by device urn
     */
    public synchronized List<Interface> getByDeviceUrn(String deviceUrn) {
        synchronized (this) {
            try {
                session = HibernateUtil.getSessionFactory().openSession();
                tx = session.beginTransaction();
                Query q = session.createQuery("from Interface as interface where interface.urn like '" + deviceUrn + ":port=%'");
                if (q.list().size() == 0) {
                    return null;
                }
                return (List<Interface>) q.list();
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
}
