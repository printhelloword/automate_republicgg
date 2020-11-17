package bot.republic.utility;

import bot.republic.model.Outbox;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;

public class DBUtilOutboxes {

    private static SessionFactory factory;

    static {
        factory = new Configuration().configure().buildSessionFactory();
    }

    public static Session getSession() {
        return factory.openSession();
    }

    public static Integer saveOutbox(Outbox outbox) {
        Integer generatedOutboxId=null;

        Session session = getSession();

        Transaction tx = null;

        try {
            tx = session.beginTransaction();

            session.persist(outbox);

            tx.commit();
            generatedOutboxId = outbox.getId();

        } catch (HibernateException e) {
            if (tx != null) {
                tx.rollback();
            }
            e.printStackTrace();
        } finally {
            session.close();
        }

        return generatedOutboxId;
    }

}
