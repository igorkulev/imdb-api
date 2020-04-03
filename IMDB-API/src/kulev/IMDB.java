package kulev;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Iterator;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509ExtendedTrustManager;

import org.apache.commons.io.IOUtils;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class IMDB {
	
	static HttpClient client = HttpClientBuilder.create().build();
	
	static String safelyGetPageContent(String strUrl) throws Exception {
		String res = null;
		boolean cont = true;
		while (cont == true) {
			cont = false;
			try {
				res = GetPageContent(strUrl);
			}
			catch (FileNotFoundException exc) {
				System.out.println("An exception occured: " + exc.toString());
				exc.printStackTrace();
				Thread.sleep(2000);
				res = "";
			}
			catch (Exception exc) {
				System.out.println("An exception occured: " + exc.toString());
				exc.printStackTrace();
				
				cont = true;
				Thread.sleep(2000);
				client = HttpClientBuilder.create().build();
				
			}
		}
		return res;
	}
	
	static String GetPageContent(String strUrl) throws Exception {
		
		SSLContext context = SSLContext.getInstance("TLS");

		context.init(null, new TrustManager[]{new X509ExtendedTrustManager() {
		    @Override
		    public void checkClientTrusted(X509Certificate[] chain, String authType, Socket socket)
		    throws CertificateException {
		        // empty
		    }

		    @Override
		    public void checkServerTrusted(X509Certificate[] chain, String authType, Socket socket)
		    throws CertificateException {
		        // empty
		    }

		    @Override
		    public void checkClientTrusted(X509Certificate[] chain, String authType, SSLEngine engine)
		    throws CertificateException {
		        // empty
		    }

		    @Override
		    public void checkServerTrusted(X509Certificate[] chain, String authType, SSLEngine engine)
		    throws CertificateException {
		        // empty
		    }

		    @Override
		    public void checkClientTrusted(X509Certificate[] chain, String authType)
		    throws CertificateException {
		        // empty
		    }

		    @Override
		    public void checkServerTrusted(X509Certificate[] chain, String authType)
		    throws CertificateException {
		        // empty
		    }

		    @Override
		    public X509Certificate[] getAcceptedIssuers() {
		        return new X509Certificate[0];
		    }
		}}, null);

		HttpsURLConnection.setDefaultSSLSocketFactory(context.getSocketFactory());
		
		// Now you can access an https URL without having the certificate in the
		// truststore

		URL url = new URL(strUrl);
		URLConnection con = url.openConnection();
		

		con.addRequestProperty("User-Agent", "Mozilla");
		con.addRequestProperty("Accept", "*/*");
		
		InputStream in = con.getInputStream();
		
		String encoding = "UTF-8";
		String body = IOUtils.toString(in, encoding);
		
		// System.out.println(body);

		return body;
	}
	
	static String[] getBracketWords(String s) {
		int i,j,k;
		
		boolean open = false;
		ArrayList<String> arr = new ArrayList<String>();
		
		StringBuilder sb = null;
		for (i=0;i<s.length();i++) {
			if (open == false) {
				if (s.charAt(i) == '(') {
					open = true;
					sb = new StringBuilder();
				}
			} else {
				if (s.charAt(i) == ')') {
					open = false;
					arr.add(sb.toString());
				} else {
					sb.append(s.charAt(i));
				}
			}
		}
		
		String res[] = new String[arr.size()];
		Iterator<String> it = arr.iterator();
		for (i=0;i<res.length;i++) {
			res[i] = it.next();
		}
		
		return res;
	}
	
	static boolean isYear(String s) {
		if (s.length() != 4) {
			return false;
		}
		for (int i=0;i<s.length();i++) {
			if ((s.charAt(i) < '0')||(s.charAt(i) > '9')) {
				return false;
			}
		}
		return true;
	}
	
	static boolean isType(String s) {
		
		// Feature Film	 TV Movie	 TV Series	 TV Episode
		// TV Special	 Mini-Series	 Documentary	 Video Game
		// Short Film	 Video	 TV Short
		 
		if (s.equals("Short")||s.equals("TV Episode")||s.equals("TV Series")||s.equals("TV Movie")||
				s.equals("Video")||s.equals("TV Mini-Series")||s.equals("TV Special")||s.equals("in development")) {
			return true;
		}
		return false;
	}
	
	static boolean isRomanNumeral(String s) {
		for (int i=0;i<s.length();i++) {
			char c = s.charAt(i);
			if ((c != 'I')&&(c != 'V')&&(c != 'X')&&(c != 'L')&&(c != 'C')&&(c != 'D')&&(c != 'M')) {
				return false;
			}
		}
		return true;
	}
	
	static JSONObject search(String title, boolean exact) throws Exception {
		int i,j,k;
		
		String encodedTitle = URLEncoder.encode(title, StandardCharsets.UTF_8.toString());
		
		String url = "https://www.imdb.com/find?q=" + encodedTitle + "&s=tt";
		if (exact == true) {
			url += "&exact=true";
		}
		
		//System.out.println(url);
		String res = safelyGetPageContent(url);
		System.out.println(res);
		
		JSONArray arr = new JSONArray();
		
		Document doc = Jsoup.parse(res);
		String s = doc.select("h1[class=\"findHeader\"]").text();
		if (s.startsWith("No results found for ")) {
			//System.out.println("No results found");
			
			JSONObject jo = new JSONObject();
			jo.put("results", new JSONArray());
			
			return jo;
		}
		
		Elements elements = doc.select("table[class=\"findList\"]").get(0).select("tr");
		
		int counter = 0;
		Iterator<Element> it = elements.iterator();
		while (it.hasNext()) {
			
			counter++;
			//System.out.println("\nResult:   " + counter);
			
			JSONObject jo = new JSONObject();
			
			Element current = it.next();
			Element a = current.select("td[class=\"result_text\"]").select("a").get(0);
			
			url = "https://www.imdb.com" + a.attr("href");
			int ind = url.lastIndexOf('/');
			jo.put("url", url.substring(0, ind));
			jo.put("id", jo.getString("url").substring(27));
			jo.put("title", a.text());
			
			//System.out.println("url:      "+jo.getString("url"));
			//System.out.println("id:       "+jo.getString("id"));
			//System.out.println("title:    "+jo.getString("title"));
			
			if (current.select("td[class=\"result_text\"]").select("br").size() == 0) {
				String tmp = current.select("td[class=\"result_text\"]").text();
				
				String words[] = getBracketWords(tmp.substring(jo.getString("title").length()));
				
				jo.put("type", "Movie");
				
				for (j=0;j<words.length;j++) {
					if (isYear(words[j])) {
						jo.put("year", words[j]);
					} else if (isRomanNumeral(words[j])) {
						jo.put("multiple", words[j]);
					} else {
						jo.put("type",  words[j]);
					}
				}
				
				//System.out.println("year:     "+((jo.has("year")) ? jo.getString("year") : ""));
				//System.out.println("multiple: "+((jo.has("multiple")) ? jo.getString("multiple") : ""));
				//System.out.println("type:     "+((jo.has("type")) ? jo.getString("type") : ""));
				
			} else {
				String tmp = current.select("td[class=\"result_text\"]").html();
				i = tmp.indexOf("</a>") + 4;
				j = tmp.indexOf("<br>");
				String words[] = getBracketWords(tmp.substring(i, j));
				
				jo.put("type", "Movie");
				
				for (j=0;j<words.length;j++) {
					if (isYear(words[j])) {
						jo.put("year", words[j]);
					} else if (isRomanNumeral(words[j])) {
						jo.put("multiple", words[j]);
					} else {
						jo.put("type",  words[j]);
					}
				}
				
				//System.out.println("year:     "+((jo.has("year")) ? jo.getString("year") : ""));
				//System.out.println("multiple: "+((jo.has("multiple")) ? jo.getString("multiple") : ""));
				//System.out.println("type:     "+((jo.has("type")) ? jo.getString("type") : ""));
				
				Elements el = current.select("td[class=\"result_text\"]").select("small");
				
				Iterator<Element> ie = el.iterator();
				while (ie.hasNext()) {
					Element currentElement = ie.next();
					if (currentElement.select("span[class=\"ghost\"]").size() == 1) {
						String season = currentElement.html().substring(1, currentElement.html().indexOf("<span")).trim();
						String episode = currentElement.html().substring(currentElement.html().indexOf("</span>")+7).trim();
						jo.put("season", season);
						jo.put("episode", episode);
						//System.out.println("season:   "+season);
						//System.out.println("episode:  "+episode);
					} else {
						//System.out.println(currentElement.html());
						
						String parent_url = "https://www.imdb.com" + currentElement.select("a").attr("href");
						j = url.lastIndexOf('/');
						parent_url = parent_url.substring(0, j);
						JSONObject parent = new JSONObject();
						parent.put("url", parent_url);
						//System.out.println("p.url:    "+parent.getString("url"));
						
						parent.put("id", parent.getString("url").substring(27));
						//System.out.println("p.id:     "+parent.getString("id"));
						parent.put("title", currentElement.select("a").text());
						//System.out.println("p.title:  "+parent.getString("title"));
						
						words = getBracketWords(currentElement.html().substring(currentElement.html().indexOf("</a>")+4));
						for (i=0;i<words.length;i++) {
							//System.out.println(words[i]);
						}
						
						parent.put("type", "Movie");
						
						for (j=0;j<words.length;j++) {
							if (isYear(words[j])) {
								parent.put("year", words[j]);
							} else if (isRomanNumeral(words[j])) {
								parent.put("multiple", words[j]);
							} else {
								parent.put("type",  words[j]);
							}
						}
						
						jo.put("parent", parent);
						
					}
				}
				
			}
			
			//System.out.println(current.select("td[class=\"result_text\"]").select("br").size());
			arr.put(jo);
		}
		//System.out.println(elements.size());
		//System.out.println(elements.html());
		JSONObject jo = new JSONObject();
		jo.put("results", arr);
		
		return jo;
	}
	
	static JSONObject getInfo(String id) throws Exception {
		
		String url = "https://www.imdb.com/title/" + id;
		
		String res = safelyGetPageContent(url);
		
		JSONObject jo = new JSONObject();
		
		Document doc = Jsoup.parse(res);
		String title = doc.select("div[class=\"title_wrapper\"]").select("h1").text().trim();
		String summary = doc.select("div[class=\"summary_text\"]").text().trim();
		
		jo.put("id", id);
		jo.put("title", title);
		jo.put("short_summary", summary);
		
		return jo;
	}
	
	public static void main(String[] args) throws Exception {
		
		//JSONObject res = search("Gone girl", true);
		JSONObject res = search("The Chosen", true);
		System.out.println(res.toString(4));
		
		res = getInfo("tt2267998");
		System.out.println(res.toString(4));
		
	}

}
