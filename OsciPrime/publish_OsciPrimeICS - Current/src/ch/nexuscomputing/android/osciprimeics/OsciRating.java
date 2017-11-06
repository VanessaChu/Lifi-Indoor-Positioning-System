
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

package ch.nexuscomputing.android.osciprimeics;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;

public class OsciRating {
	private static final long LAUNCHES_UNTIL_PROMPT = 7;
	private static final int DAYS_UNTIL_PROMPT = 5;
	private static final String APPLICATION_LINK = "market://details?id=ch.nexuscomputing.android.osciprimeics";

	public static void prompt(Context context, int color) {
		final SharedPreferences prefs = context.getSharedPreferences("rating",Context.MODE_PRIVATE);
		if (prefs.getBoolean("mute", false)) {
			return;
		}
		final SharedPreferences.Editor editor = prefs.edit();
		long launch_count = prefs.getLong("launch_count", 0) + 1;
		editor.putLong("launch_count", launch_count);
		Long date_firstLaunch = prefs.getLong("date_firstlaunch", 0);
		if (date_firstLaunch == 0) {
			date_firstLaunch = System.currentTimeMillis();
			editor.putLong("date_firstlaunch", date_firstLaunch);
		}
		// Wait at least n days before opening
		if (launch_count >= LAUNCHES_UNTIL_PROMPT) {
			if (System.currentTimeMillis() >= date_firstLaunch
					+ (DAYS_UNTIL_PROMPT * 24 * 60 * 60 * 1000)) {
				showRateDialog(context, color);
			}
		}
		editor.commit();
	}

	private static void showRateDialog(final Context context, int color) {
		final Dialog dia = new Dialog(context);
		dia.requestWindowFeature(Window.FEATURE_NO_TITLE);
		dia.setContentView(R.layout.rate_dialog);
		dia.setTitle(R.string.ratetitle);
		((LinearLayout)dia.findViewById(R.id.parent)).setBackgroundColor(color);

		Button rate = (Button) dia.findViewById(R.id.btRate);
		rate.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				context.startActivity(new Intent(Intent.ACTION_VIEW, Uri
						.parse(APPLICATION_LINK)));
				dia.dismiss();
				Editor editor = context.getSharedPreferences("rating",Context.MODE_PRIVATE).edit();
				editor.putBoolean("mute", true);
				editor.commit();
			}
		});
		
		Button later = (Button) dia.findViewById(R.id.btLater);
		later.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dia.dismiss();
				Editor editor = context.getSharedPreferences("rating",Context.MODE_PRIVATE).edit();
				editor.putLong("launch_count", 0);
				editor.commit();
			}
		});
		
		Button mute = (Button) dia.findViewById(R.id.btMute);
		mute.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Editor editor = context.getSharedPreferences("rating",Context.MODE_PRIVATE).edit();
				editor.putBoolean("mute", true);
				editor.commit();
				dia.dismiss();
			}
		});
		
		
		dia.show();
	}
}
