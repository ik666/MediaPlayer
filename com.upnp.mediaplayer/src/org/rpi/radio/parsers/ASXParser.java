package org.rpi.radio.parsers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.LinkedList;

import org.apache.log4j.Logger;
import org.rpi.mplayer.CloseMe;

public class ASXParser {

	private Logger log = Logger.getLogger(ASXParser.class);

	public LinkedList<String> getStreamingUrl(String url) {
		LinkedList<String> murls = null;
		URLConnection conn = null;
		try {
			conn = getConnection(url);
			return getStreamingUrl(conn);
		} catch (MalformedURLException e) {
			log.error(e);
		} catch (IOException e) {
			log.error(e);
		}finally {
			if(conn !=null) {
				try {
					if(conn.getInputStream() !=null) {
						CloseMe.close(conn.getInputStream());
					}
				}
				catch(Exception e) {
				}
				
				try {
					if(conn.getOutputStream() !=null) {
						CloseMe.close(conn.getOutputStream());
					}
				}
				catch(Exception e) {

				}				
				conn = null;
			}
		}
		return murls;
	}

	public LinkedList<String> getStreamingUrl(URLConnection conn) {

		BufferedReader br = null;
		String murl = null;
		LinkedList<String> murls = null;
		murls = new LinkedList<String>();
		try {
			br = new BufferedReader(new InputStreamReader(conn.getInputStream()));

			while (true) {
				try {
					String line = br.readLine();
					if (line == null) {
						break;
					}
					murl = parseLine(line);
					if (murl != null && !murl.equals("")) {
						murls.add(murl);
					}
				} catch (IOException e) {
					log.error(e);
				}
			}
		} catch (MalformedURLException e) {
			log.error(e);
		} catch (IOException e) {
			log.error(e);
		} finally {
			if (br != null) {
				CloseMe.close(br);
			}
			if (conn != null) {
				conn = null;
			}
		}
		// murls.add(conn.getURL().toString());
		return murls;
	}

	private String parseLine(String line) {
		if (line == null) {
			return null;
		}
		String trimmed = line.trim();
		if (trimmed.toLowerCase().startsWith("<ref href=\"")) {
			trimmed = trimmed.substring(11);
			// trimmed = trimmed.replace("<ref href=\"", "");
			trimmed = trimmed.replace("/>", "").trim();
			if (trimmed.endsWith("\"")) {
				trimmed = trimmed.replace("\"", "");
				log.debug("ASX: " + trimmed);
				return trimmed;
			}
		}
		return "";
	}

	private URLConnection getConnection(String url) throws MalformedURLException, IOException {
		URLConnection mUrl = new URL(url).openConnection();
		return mUrl;
	}

}
