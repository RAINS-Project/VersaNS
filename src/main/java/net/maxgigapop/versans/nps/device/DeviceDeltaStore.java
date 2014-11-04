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
public class DeviceDeltaStore {
    
    private static Session session;
    private static org.hibernate.Transaction tx;
    private org.apache.log4j.Logger log;
    private List<DeviceDelta> cachedDeviceDeltas = null;

    public DeviceDeltaStore() {
        log = org.apache.log4j.Logger.getLogger(this.getClass());
    }

    public synchronized boolean add(DeviceDelta n) {
        synchronized (this) {
            if (n.getContractId().isEmpty() || n.getDeviceId() == 0) {
                throw new IllegalArgumentException("DeviceDelta object must have valid contractId and deviceId fields");
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

    public synchronized boolean update(DeviceDelta n) {
        synchronized (this) {
            if (n.getContractId().isEmpty() || n.getDeviceId() == 0) {
                throw new IllegalArgumentException("DeviceDelta object must have valid contractId and deviceId fields");
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

    public synchronized boolean delete(DeviceDelta n) {
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

    public synchronized List<DeviceDelta> getAll() {
        synchronized (this) {
            try {
                session = HibernateUtil.getSessionFactory().openSession();
                tx = session.beginTransaction();
                Query q = session.createQuery("from DeviceDelta");
                return (List<DeviceDelta>) q.list();
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
    public synchronized DeviceDelta getById(int id) {
        synchronized (this) {
            try {
                session = HibernateUtil.getSessionFactory().openSession();
                tx = session.beginTransaction();
                Query q = session.createQuery("from DeviceDelta as deviceDelta where deviceDelta.id=" + Integer.toString(id));
                if (q.list().size() == 0) {
                    return null;
                }
                return (DeviceDelta) q.list().get(0);
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
     * get by device ID and contractId
     */
    public synchronized DeviceDelta getByDeviceAndContractId(int deviceId, String contractId) {
        synchronized (this) {
            try {
                session = HibernateUtil.getSessionFactory().openSession();
                tx = session.beginTransaction();
                Query q = session.createQuery("from DeviceDelta as deviceDelta where deviceDelta.deviceId=" 
                        + Integer.toString(deviceId)
                        + " and deviceDelta.contractId='" + contractId + "'");
                if (q.list().size() == 0) {
                    return null;
                }
                return (DeviceDelta) q.list().get(0);
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
    public synchronized List<DeviceDelta> getByDeviceId(int deviceId) {
        synchronized (this) {
            try {
                session = HibernateUtil.getSessionFactory().openSession();
                tx = session.beginTransaction();
                Query q = session.createQuery("from DeviceDelta as deviceDelta where deviceDelta.deviceId=" 
                        + Integer.toString(deviceId));
                if (q.list().size() == 0) {
                    return null;
                }
                return (List<DeviceDelta>) q.list();
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
     * get by contractId
     */
    public synchronized List<DeviceDelta> getByContractId(String contractId) {
        synchronized (this) {
            try {
                session = HibernateUtil.getSessionFactory().openSession();
                tx = session.beginTransaction();
                Query q = session.createQuery("from DeviceDelta as deviceDelta where deviceDelta.contractId='" + contractId + "'");
                if (q.list().size() == 0) {
                    return null;
                }
                return (List<DeviceDelta>) q.list();
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
