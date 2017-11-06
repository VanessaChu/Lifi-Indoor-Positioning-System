
    /**
    OsciPrime an Open Source Android Oscilloscope
    Copyright (C) 2012  Manuel Di Cerbo, Nexus-Computing GmbH Switzerland
    Copyright (C) 2012  Andreas Rudolf, Nexus-Computing GmbH Switzerland

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
    */

package ch.nexuscomputing.android.osciprimeics.news;

import java.io.StringReader;
import java.util.HashMap;

import org.xmlpull.v1.XmlPullParser;

import android.util.Xml;
import ch.nexuscomputing.android.osciprimeics.L;

public class NewsParser {
	
	protected static enum Tag {
		ISSUE, TITLE, TEXT, IMAGE, LINK, EXTENDED
	}
	
	private final static HashMap<String, Tag> MAP = new HashMap<String,Tag>();
	static{
		MAP.put("issue",Tag.ISSUE);
		MAP.put("title",Tag.TITLE);
		MAP.put("text",Tag.TEXT);
		MAP.put("image",Tag.IMAGE);
		MAP.put("link",Tag.LINK);
		MAP.put("extended",Tag.EXTENDED);
	}
	
	public static News parse(String xml){
		try {
			long issue = -1;
			String title = "";
			String text = "";
			String image = "";
			String link = "";
			String extended = "";
			
			XmlPullParser parser = Xml.newPullParser();
			parser.setInput(new StringReader(xml));
			int eventType = parser.getEventType();
			while (eventType != XmlPullParser.END_DOCUMENT) {
				switch (eventType) {
				case XmlPullParser.START_TAG:
					String tagName = parser.getName().toLowerCase();
					Tag t = MAP.get(tagName);
					if (t == null) {
						break;
					}
					switch (t) {
					case ISSUE: 
						issue = Long.parseLong(parser.nextText());
						break;
					case TEXT:
						text = parser.nextText();
						break;
					case TITLE:
						title = parser.nextText();
						break;
					case IMAGE:
						image = parser.nextText();
						break;
					case LINK:
						link = parser.nextText();
						break;
					case EXTENDED:
						extended  = parser.nextText();
						break;
					}
				default:
					break;
				}
				eventType = parser.next();
			}
			return new News(title, issue, link, image, extended, text);
		} catch (Exception e) {
			L.e(e);
			return null;
		}
		
	}

}
