package cerisenoire;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;

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
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.repackaged.com.google.gson.Gson;

/**
 * used by cron, to weekly set the menu
 * 
 * @author gonzo
 * 
 */
public class WeekMenu extends HttpServlet {

	private static final long serialVersionUID = 1L;
	private static final String price = "(&euro;6)";

	private String[] orderDays = new String[] { "maandag", "dinsdag", "woensdag", "donderdag", "vrijdag" };

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		NamespaceManager.set("cerisenoire");
		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		List<Entity> allDishes = datastore.prepare(new Query("allDishes")).asList(FetchOptions.Builder.withDefaults());
		if (allDishes != null) {
			Entity dishes = datastore.prepare(new Query("dishes")).asSingleEntity();
			Map<String, String> generatedDishes = generateDishes(dishes, allDishes);
			for (String orderDay : orderDays) {
				dishes.setProperty(orderDay, generatedDishes.get(orderDay));
			}
			datastore.put(dishes);
		} else {
			System.err.println("AllDishes not found in datastore");
		}
	}

	private Map<String, String> generateDishes(Entity dishes, List<Entity> allDishes) {
		HashMap<String, String> generatedDishes = new HashMap<String, String>();
		Random random = new Random();
		for (String orderDay : orderDays) {
			Entity randomDish = allDishes.remove(random.nextInt(allDishes.size()));
			generatedDishes.put(orderDay, randomDish.getProperty("name") + " " + price+"||"+randomDish.getProperty("description"));
		}
		return generatedDishes;
	}

}