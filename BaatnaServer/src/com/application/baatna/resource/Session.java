package com.application.baatna.resource;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.apache.commons.mail.EmailException;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.application.baatna.bean.Location;
import com.application.baatna.bean.User;
import com.application.baatna.dao.FeedDAO;
import com.application.baatna.dao.UserDAO;
import com.application.baatna.util.CommonLib;
import com.application.baatna.util.JsonUtil;
import com.application.baatna.util.mailer.EmailModel;
import com.application.baatna.util.mailer.EmailUtil;

@Path("/auth")
public class Session {

	/**
	 * Login Api call.
	 */
	@Path("/login")
	@POST
	@Produces("application/json")
	@Consumes("application/x-www-form-urlencoded")
	public JSONObject authorization(@DefaultValue("images/default.jpg") @FormParam("profile_pic") String profilePic,
			@FormParam("client_id") String clientId, @FormParam("user_name") String userName,
			@FormParam("email") String email, @FormParam("app_type") String appType,
			@FormParam("password") String password, @FormParam("address") String address,
			@FormParam("phone") String phone, @FormParam("bio") String bio, @FormParam("registration_id") String regId,
			@FormParam("latitude") double latitude, @FormParam("longitude") double longitude,
			@FormParam("fbid") String fbId, @FormParam("fbdata") String fbData, @FormParam("fb_token") String fbToken,
			@FormParam("fb_permission") String fb_permissions, @QueryParam("isFacebookLogin") boolean isFacebookLogin) {

		// null checks, invalid request
		if (clientId == null || appType == null)
			return CommonLib.getResponseString("Invalid params", "", CommonLib.RESPONSE_INVALID_PARAMS);

		// check for client_id
		if (!clientId.equals(CommonLib.ANDROID_CLIENT_ID))
			return CommonLib.getResponseString("Invalid client id", "", CommonLib.RESPONSE_INVALID_CLIENT_ID);

		// check for app type
		if (!appType.equals(CommonLib.ANDROID_APP_TYPE))
			return CommonLib.getResponseString("Invalid params", "", CommonLib.RESPONSE_INVALID_APP_TYPE);

		if (!((isFacebookLogin && fbToken != null && !fbToken.isEmpty())) && (email == null || password == null))
			return CommonLib.getResponseString("Invalid params", "", CommonLib.RESPONSE_INVALID_PARAMS);

		UserDAO userDao = new UserDAO();
		User user = userDao.getUserDetails(email, password);

		// user does not exist
		if ((user == null || user.getUserId() <= 0) && !(isFacebookLogin && fbToken != null && !fbToken.isEmpty()))
			return CommonLib.getResponseString("Invalid user", "Invalid login credentials",
					CommonLib.RESPONSE_INVALID_PARAMS);

		// create user if it does not exist, else generate the access token
		if (user == null || user.getUserId() <= 0) {

			user = userDao.getUserDetails(fbId);

			if (user == null || user.getUserId() <= 0) {
				user = userDao.addUserDetails(profilePic, userName, password, email, address, phone, bio, fbId, fbData,
						fbToken, fb_permissions);

				if (user != null) {
					EmailModel emailModel = new EmailModel();
					emailModel.setTo(user.getEmail());
					emailModel.setFrom(CommonLib.BAPP_ID);
					emailModel.setSubject("Welcome to your local Baatna Community!!");
					emailModel.setContent(
							"Hey," + "\n\nWelcome to Baatna! Thank you for becoming a member of the local Baatna community!"
									+ "\n\nBaatna enables you to borrow the things you need from people in your neighborhood. Right here, right now, for free."
									+ "\n\nHow does it work?"
									+ "\n\nYou know those moments where you need to use something that you do not need to own? Tell Baatna what you are looking for and we'll find friendly neighbors willing to lend it to you. Looking for something right now? Just go to the app and post your need."
									+ "\n\nIn return you can share your stuff when it's convenient. If one of your neighbors is looking for something, we will let you know. It's up to you if you want to lend out your stuff. Be an awesome neighbor and share the love!"
									+ "\n\nWe're doing our best to make Baatna more efficient and useful for you everyday. Incase you have any feedback, please get back to us at -hello@baatna.com."
									+ "\nWe would love to hear from you." + "\n\nCheers" + "\nBaatna Team");
					EmailUtil.getInstance().sendEmail(emailModel);
					FeedDAO feedDao = new FeedDAO();
					boolean returnFeedResult = feedDao.addFeedItem(FeedDAO.USER_JOINED, System.currentTimeMillis(),
							user.getUserId(), -1, -1);
					if (returnFeedResult) {
						System.out.println("Success type 1");
					} else {
						System.out.println("Failure type 1");
					}
				}
			}
		}

		if (user == null || user.getUserId() <= 0)
			return CommonLib.getResponseString("Error", "Some error occured", CommonLib.RESPONSE_INVALID_PARAMS);

		// email verification check
		int status = CommonLib.RESPONSE_SUCCESS;
		// : CommonLib.RESPONSE_INVALID_USER;

		// Generate Access Token
		String accessToken = userDao.generateAccessToken(user.getUserId());

		// TODO: send the complete user object in json
		Location location = new Location(latitude, longitude);
		boolean sessionAdded = userDao.addSession(user.getUserId(), accessToken, regId, location);
		if (sessionAdded) {
			JSONObject responseObject = new JSONObject();
			try {
				responseObject.put("access_token", accessToken);
				responseObject.put("user_id", user.getUserId());
				responseObject.put("email", user.getEmail());
				responseObject.put("profile_pic", user.getProfilePic());
				responseObject.put("username", user.getUserName());
				if (user.getIsInstitutionVerified() == 1) {
					responseObject.put("HSLogin", false);// flag which checks if
															// the user is
															// verified or not
					responseObject.put("instutionLogin", true);// flag to
																// disable the
																// institution
																// login
					responseObject.put("INSTITUTION_NAME", user.getInstitutionName());
					responseObject.put("STUDENT_ID", user.getStudentId());
				}
				responseObject.put("user", JsonUtil.getUserJson(user));
			} catch (JSONException e) {
				e.printStackTrace();
			}
			return CommonLib.getResponseString(responseObject.toString(), "", status);
		} else
			return CommonLib.getResponseString("failed", "", status);
	}

	@Path("/logout")
	@POST
	@Produces(MediaType.TEXT_PLAIN)
	@Consumes("application/x-www-form-urlencoded")
	public String userLogout(@FormParam("access_token") String accessToken) {

		UserDAO dao = new UserDAO();
		int userId = dao.userActive(accessToken);

		boolean returnValue = dao.nullifyAccessToken(userId, accessToken);

		if (accessToken != null && !returnValue)
			return "FAILURE";

		return "SUCCESS";
	}

	@Path("/appConfig")
	@POST
	@Produces("application/json")
	@Consumes("application/x-www-form-urlencoded")
	public JSONObject userBlock(@FormParam("access_token") String accessToken) {

		UserDAO dao = new UserDAO();
		int userId = dao.userActive(accessToken);

		if (userId > 0) {
			return CommonLib.getResponseString("", "You cannot use this app anymot, fuck off!",
					CommonLib.RESPONSE_SUCCESS);
		}
		return CommonLib.getResponseString("", "You cannot use this app anymot, fuck off!", CommonLib.RESPONSE_FAILURE);
	}

}
