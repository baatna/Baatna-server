package com.application.baatna.dao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.jivesoftware.smack.XMPPException;

import com.application.baatna.bean.User;
import com.application.baatna.bean.Wish;
import com.application.baatna.util.CommonLib;
import com.application.baatna.util.DBUtil;
import com.application.baatna.util.GCM;

public class WishDAO {

	public WishDAO() {
	}

	public Wish addWishPost(String title, String description, long timeOfPost,
			int userId) {

		Wish wish;
		Session session = null;
		try {
			session = DBUtil.getSessionFactory().openSession();
			Transaction transaction = session.beginTransaction();

			wish = new Wish();
			wish.setTitle(title);
			wish.setDescription(description);
			wish.setTimeOfPost(timeOfPost);
			wish.setUserId(userId);

			session.save(wish);
			transaction.commit();
			session.close();

		} catch (HibernateException e) {
			System.out.println(e.getMessage());
			e.printStackTrace();

			System.out.println("error");
			return null;
		} finally {
			if(session != null && session.isOpen())
				session.close();
		}
		return wish;
	}

	public ArrayList<Wish> getAllWishes(int userId, int start, int count) {
		ArrayList<Wish> wishes;
		Session session = null;
		try {
			session = DBUtil.getSessionFactory().openSession();
			Transaction transaction = session.beginTransaction();

			// finding user info
			Wish wish = null;
			int i = 0;
			wishes = new ArrayList<Wish>();

			String sql = "SELECT * FROM WISH WHERE USERID = :userid LIMIT :start , :count";
			SQLQuery query = session.createSQLQuery(sql);
			query.addEntity(Wish.class);
			query.setParameter("userid", userId);
			query.setParameter("start", start);
			query.setParameter("count", count);

			java.util.List results = (java.util.List) query.list();

			for (Iterator iterator = ((java.util.List) results).iterator(); iterator
					.hasNext();) {

				wish = (Wish) iterator.next();
				wishes.add(wish);
				i++;

			}

			transaction.commit();
			session.close();

		} catch (HibernateException e) {
			System.out.println(e.getMessage());
			e.printStackTrace();

			System.out.println("error");
			return null;
		} finally {
			if(session != null && session.isOpen())
				session.close();
		}

		return wishes;
	}

	public Wish getWish(int wishId) {
		Wish wish = null;
		Session session = null;
		try {
			session = DBUtil.getSessionFactory().openSession();
			Transaction transaction = session.beginTransaction();

			// finding user info

			String sql = "SELECT * FROM WISH WHERE WISHID = :wishId";
			SQLQuery query = session.createSQLQuery(sql);
			query.addEntity(Wish.class);
			query.setParameter("wishId", wishId);

			java.util.List results = (java.util.List) query.list();

			for (Iterator iterator = ((java.util.List) results).iterator(); iterator
					.hasNext();) {

				wish = (Wish) iterator.next();

			}

			transaction.commit();
			session.close();

		} catch (HibernateException e) {
			System.out.println(e.getMessage());
			e.printStackTrace();

			System.out.println("error");
			return null;
		} finally {
			if(session != null && session.isOpen())
				session.close();
		}

		return wish;
	}

	public boolean deleteWish(int wishId) {

		Session session = null;
		try {
			session = DBUtil.getSessionFactory().openSession();
			Transaction transaction = session.beginTransaction();

			String hql = "DELETE FROM Wish WHERE wishId = :wish_id";
			Query query = session.createQuery(hql);
			query.setParameter("wish_id", wishId);
			int result = query.executeUpdate();
			CommonLib.BLog(result + "");
			session.flush();
			transaction.commit();
			session.close();

		} catch (HibernateException e) {
			System.out.println(e.getMessage());
			e.printStackTrace();

			System.out.println("error");
			return false;
		} finally {
			if(session != null && session.isOpen())
				session.close();
		}

		return true;

	}

	public void sendPushToNearbyUsers(String notification) {

		UserDAO userDao = new UserDAO();
		ArrayList<com.application.baatna.bean.Session> nearbyUsers = userDao
				.getNearbyUsers();
		GCM ccsClient = new GCM();
		String userName = CommonLib.projectId + "@gcm.googleapis.com";
		String password = CommonLib.apiKey;
		try {
			ccsClient.connect(userName, password);
		} catch (XMPPException e) {
			e.printStackTrace();
		}
		String messageId = ccsClient.getRandomMessageId();

		Map<String, String> payload = new HashMap<String, String>();
		payload.put("command", "something");
		payload.put("Notification", notification);
		
		JSONObject object = new JSONObject();
		try {
			object.put("Notification", notification);
			object.put("actionId", "id");
			object.put("additionalParam", "value");
		} catch (JSONException exp) {
			// String error = LogMessages.FETCH_ERROR + exp.getMessage();
			// logger.log(Level.INFO, error);
			exp.printStackTrace();
		}
		payload.put("value", object.toString());
		payload.put("EmbeddedMessageId", messageId);
		Long timeToLive = 10000L;
		Boolean delayWhileIdle = false;

		for (com.application.baatna.bean.Session nearbyUser : nearbyUsers) {
			// send push notif to all
			ccsClient.send(GCM.createJsonMessage(nearbyUser.getPushId(),
					messageId, payload, null, timeToLive, delayWhileIdle));
		}
		ccsClient.disconnect();

	}
	
	public boolean updateWishedUsers(int userId, int type, int wishId) {

		Session session = null;
		Wish wish = null;
		try {
			session = DBUtil.getSessionFactory().openSession();
			Transaction transaction = session.beginTransaction();

			String sql = "SELECT * FROM WISH WHERE WISHID = :wishId";
			SQLQuery query = session.createSQLQuery(sql);
			query.addEntity(Wish.class);
			query.setParameter("wishId", wishId);

			java.util.List results = (java.util.List) query.list();

			for (Iterator iterator = ((java.util.List) results).iterator(); iterator
					.hasNext();) {

				wish = (Wish) iterator.next();

			}

			//Get the user for the particular id
			User user = new User();

			String sql1 = "SELECT * FROM USER WHERE USERID = :userid";
			SQLQuery query2 = session.createSQLQuery(sql1);
			query2.addEntity(User.class);
			query2.setParameter("userid", userId);
			java.util.List results3 = (java.util.List) query2.list();

			for (Iterator iterator = ((java.util.List) results3).iterator(); iterator
					.hasNext();) {
				user = (User) iterator.next();
			}

			if (type == CommonLib.ACTION_ACCEPT_WISH) {
				if(wish.getAcceptedUsers() != null)
					wish.getAcceptedUsers().add(user);
				else 
					System.out.println("List not initialized");
			}
			else if (type == CommonLib.ACTION_DECLINE_WISH) {
				if(wish.getAcceptedUsers() != null)
					wish.getDeclinedUsers().add(user);
				else 
					System.out.println("List not initialized");
			}

			session.update(wish);

			transaction.commit();
			session.close();
			return true;

		} catch (HibernateException e) {
			System.out.println(e.getMessage());
			System.out.println("error");
			e.printStackTrace();

		} finally {
			if(session != null && session.isOpen())
				session.close();
		}
		return false;
	}

	public Set getWishedUsers(int type, int wishId) {

		Session session = null;
		Wish wish = null;
		Set users = null;
		try {
			session = DBUtil.getSessionFactory().openSession();
			Transaction transaction = session.beginTransaction();

			String sql = "SELECT * FROM WISH WHERE WISHID = :wishId";
			SQLQuery query = session.createSQLQuery(sql);
			query.addEntity(Wish.class);
			query.setParameter("wishId", wishId);

			java.util.List results = (java.util.List) query.list();

			for (Iterator iterator = ((java.util.List) results).iterator(); iterator
					.hasNext();) {

				wish = (Wish) iterator.next();

			}
			
			if (type == 1)
				users = wish.getAcceptedUsers();
			else if (type == 2)
				users = wish.getDeclinedUsers();

			transaction.commit();
			session.close();

		} catch (HibernateException e) {
			System.out.println(e.getMessage());
			System.out.println("error");
			e.printStackTrace();

		} finally {
			if(session != null && session.isOpen())
				session.close();
		}
		return users;
	}

}
