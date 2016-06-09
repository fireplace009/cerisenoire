package cerisenoire;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.NamespaceManager;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.repackaged.com.google.gson.Gson;

public class Performer extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private String[] orderDays = new String[] { "maandag", "dinsdag", "woensdag", "donderdag", "vrijdag" };

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		Map<String, Object> properties = new HashMap<String, Object>();
		Entity entity = null;
		try{
			NamespaceManager.set("cerisenoire");
			DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
			entity = datastore.prepare(new Query("dishes")).asSingleEntity();
		}catch(Exception ex){
			ex.printStackTrace();
		}
		if (entity == null) {
			System.err.println("Dishes not found in datastore");
			for (String orderDay : orderDays) {
				properties.put(orderDay, "- gesloten -");
			}
		} else {
			properties = entity.getProperties();
		}
		Gson gson = new Gson();
		resp.setContentType("application/json");
		PrintWriter out = resp.getWriter();
		out.print(gson.toJson(properties));
		out.flush();
	}

	@Override
	public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		String locatie = req.getParameter("locatie");
		String email = req.getParameter("email");
		// String tijdstip = req.getParameter("tijdstip");
		StringBuilder msg = new StringBuilder();
		msg.append("Bestelling bevestigd door " + email);
		msg.append("<br/>");
		msg.append("Adres,tijdstip,...: " + (locatie != null ? locatie : "-leeg-"));
		msg.append("<br/>");
		// msg.append("Tijdstip: " + (tijdstip != null ? tijdstip :
		// " rond de middag "));
		msg.append("<br/><br/>");
		for (String day : orderDays) {
			String order = gatherOrders(req, day);
			if (order != null) {
				msg.append("Op " + day + " bestelling voor <b>" + order).append("</b>");
				msg.append("<br/>");
			}
		}
		msg.append("<br/><br/>");
		msg.append("Bedankt en tot dan!");
		msg.append("<br/><br/>");
		msg.append("ps: Indien u dit niet bestelde, of uw bestelling wenst te wijzigen, mail me dan op info@cerisenoire.be");
		try {
			sendMailAppEnging(msg.toString(), locatie, email);
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println(e.getMessage());
		}
	}

	private String gatherOrders(HttpServletRequest req, String day) {
		String orderCount = req.getParameter(day);
		if (orderCount != null && orderCount.matches("[0-9]")) {
			int orderAmount = Integer.parseInt(orderCount);
			return orderAmount == 0 ? null : orderAmount + " x " + req.getParameter(day + "_dish");
		}
		return null;
	}

	private void sendMailAppEnging(String msgBody, String locatie, String email) throws AddressException, MessagingException {
		System.out.println("Bestelling: " + email + " " + msgBody);
		Properties props = new Properties();
		Session session = Session.getDefaultInstance(props, null);
		Multipart mp = new MimeMultipart();
		MimeBodyPart htmlPart = new MimeBodyPart();
		htmlPart.setContent(msgBody, "text/html");
		mp.addBodyPart(htmlPart);
		Message msg = new MimeMessage(session);
		msg.setContent(mp);
		msg.setFrom(new InternetAddress("cerise-noire@appspot.gserviceaccount.com"));
		msg.addRecipient(Message.RecipientType.TO, new InternetAddress(email));
		msg.addRecipient(Message.RecipientType.CC, new InternetAddress("info@cerisenoire.be"));
		msg.addRecipient(Message.RecipientType.BCC, new InternetAddress("patteeuw.bernard@gmail.com"));
		msg.setSubject("Bevesting bestelling lunch.");
		msg.setReplyTo(new InternetAddress[]{new InternetAddress("info@cerisenoire.be")});
		Transport.send(msg);
	}

}
