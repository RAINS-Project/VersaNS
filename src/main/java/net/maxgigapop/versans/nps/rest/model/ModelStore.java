/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.versans.nps.rest.model;

import java.util.List;
import net.maxgigapop.versans.nps.manager.HibernateUtil;
import org.hibernate.Query;
import org.hibernate.Session;

/**
 *
 * @author xyang
 */
public class ModelStore {
    
    private static Session session;
    private static org.hibernate.Transaction tx;
    private org.apache.log4j.Logger log;
    private List<ModelBase> cachedModelBases = null;

    public ModelStore() {
        log = org.apache.log4j.Logger.getLogger(this.getClass());
    }

    public synchronized boolean add(ModelBase n) {
        synchronized (this) {
            if (n.getVersion().isEmpty()) {
                throw new IllegalArgumentException("ModelBase object must have valid version field");
            } try {
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

    public synchronized boolean update(ModelBase n) {
        synchronized (this) {
            if (n.getVersion().isEmpty()) {
                throw new IllegalArgumentException("ModelBase object must have valid version field");
            } try {
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

    public synchronized boolean delete(ModelBase n) {
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

    public synchronized List<ModelBase> getAll() {
        synchronized (this) {
            try {
                session = HibernateUtil.getSessionFactory().openSession();
                tx = session.beginTransaction();
                Query q = session.createQuery("from ModelBase");
                return (List<ModelBase>) q.list();
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
     * get latest model
     */
    public synchronized ModelBase getHead() {
        synchronized (this) {
            try {
                session = HibernateUtil.getSessionFactory().openSession();
                tx = session.beginTransaction();
                Query q = session.createQuery("from ModelBase as model where model.id=(select max(model.id) from model)");
                if (q.list().size() == 0) {
                    return null;
                }
                return (ModelBase) q.list().get(0);
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
    public synchronized ModelBase getById(int id) {
        synchronized (this) {
            try {
                session = HibernateUtil.getSessionFactory().openSession();
                tx = session.beginTransaction();
                Query q = session.createQuery("from ModelBase as model where model.id=" + Long.toString(id));
                if (q.list().size() == 0) {
                    return null;
                }
                return (ModelBase) q.list().get(0);
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
     * get by model version
     */
    public synchronized ModelBase getByVersion(String version) {
        synchronized (this) {
            try {
                session = HibernateUtil.getSessionFactory().openSession();
                tx = session.beginTransaction();
                Query q = session.createQuery("from ModelBase as model where model.version='" 
                        + version + "'");
                if (q.list().size() == 0) {
                    return null;
                }
                return (ModelBase) q.list().get(0);
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
