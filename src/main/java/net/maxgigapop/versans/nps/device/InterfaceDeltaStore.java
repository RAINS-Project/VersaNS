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
public class InterfaceDeltaStore {
   
    private static Session session;
    private static org.hibernate.Transaction tx;
    private org.apache.log4j.Logger log;
    private List<InterfaceDelta> cachedInterfaceDeltas = null;

    public InterfaceDeltaStore() {
        log = org.apache.log4j.Logger.getLogger(this.getClass());
    }

    public synchronized boolean add(InterfaceDelta n) {
        synchronized (this) {
            if (n.getDeviceDeltaId() == 0 || n.getInterfaceId() == 0) {
                throw new IllegalArgumentException("InterfaceDelta object must have valid interfaceId and deviceDeltaId fields");
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

    public synchronized boolean update(InterfaceDelta n) {
        synchronized (this) {
            if (n.getDeviceDeltaId() == 0 || n.getInterfaceId() == 0) {
                throw new IllegalArgumentException("InterfaceDelta object must have valid interfaceId and deviceDeltaId fields");
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

    public synchronized boolean delete(InterfaceDelta n) {
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
        }
        return true;
    }

    public synchronized List<InterfaceDelta> getAll() {
        synchronized (this) {
            try {
                session = HibernateUtil.getSessionFactory().openSession();
                tx = session.beginTransaction();
                Query q = session.createQuery("from InterfaceDelta");
                return (List<InterfaceDelta>) q.list();
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
    public synchronized InterfaceDelta getById(int id) {
        synchronized (this) {
            try {
                session = HibernateUtil.getSessionFactory().openSession();
                tx = session.beginTransaction();
                Query q = session.createQuery("from InterfaceDelta as interfaceDelta where interfaceDelta.id=" + Integer.toString(id));
                if (q.list().size() == 0) {
                    return null;
                }
                return (InterfaceDelta) q.list().get(0);
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
     * get by deviceDelta and Interface ID
     */
    public synchronized InterfaceDelta getByDeviceDeltaAndInterfaceId(int deviceDeltaId, int interfaceId) {
        synchronized (this) {
            try {
                session = HibernateUtil.getSessionFactory().openSession();
                tx = session.beginTransaction();
                Query q = session.createQuery("from InterfaceDelta as interfaceDelta where interfaceDelta.deviceDeltaId=" 
                        + Integer.toString(deviceDeltaId)
                        + " and interfaceDelta.interfaceId=" + Integer.toString(interfaceId));
                if (q.list().size() == 0) {
                    return null;
                }
                return (InterfaceDelta) q.list().get(0);
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
     * get by deviceDelta ID
     */
    public synchronized List<InterfaceDelta> getByDeviceDeltaId(int deviceDeltaId) {
        synchronized (this) {
            try {
                session = HibernateUtil.getSessionFactory().openSession();
                tx = session.beginTransaction();
                Query q = session.createQuery("from InterfaceDelta as interfaceDelta where interfaceDelta.deviceDeltaId=" 
                        + Integer.toString(deviceDeltaId));
                if (q.list().size() == 0) {
                    return null;
                }
                return (List<InterfaceDelta>) q.list();
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
     * get by interfaceId ID
     */
    public synchronized List<InterfaceDelta> getByInterfaceId(int interfaceId) {
        synchronized (this) {
            try {
                session = HibernateUtil.getSessionFactory().openSession();
                tx = session.beginTransaction();
                Query q = session.createQuery("from InterfaceDelta as interfaceDelta where interfaceDelta.interfaceId=" 
                        + Integer.toString(interfaceId));
                if (q.list().size() == 0) {
                    return null;
                }
                return (List<InterfaceDelta>) q.list();
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
